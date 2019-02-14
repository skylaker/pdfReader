/**
 * Copyright (C) 2003-2017, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit Mobile PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.annots.stamp;

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Stamp;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;

public abstract class StampUndoItem extends AnnotUndoItem {
    int mStampType;
    Bitmap mBitmap;
    String mIconName;
    DynamicStampIconProvider mDsip;
}

class StampAddUndoItem extends StampUndoItem {

    public StampAddUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        StampDeleteUndoItem undoItem = new StampDeleteUndoItem(mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mPageIndex = mPageIndex;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof Stamp)) {
                return false;
            }

            final RectF annotRectF = annot.getRect();

            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
            }

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);

            StampEvent deleteEvent = new StampEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Stamp) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(deleteEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            RectF deviceRectF = new RectF();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRectF, mPageIndex);
                            mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(deviceRectF));
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean redo() {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = page.addAnnot(Annot.e_annotStamp, this.mBBox);

            StampEvent addEvent = new StampEvent(EditAnnotEvent.EVENTTYPE_ADD, this, (Stamp) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, annot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            try {
                                RectF annotRect = annot.getRect();
                                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, mPageIndex);
                                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(annotRect));
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}

class StampModifyUndoItem extends StampUndoItem {

    public RectF mUndoBox;
    public String mUndoContent;

    public RectF mRedoBox;
    public String mRedoContent;

    public StampModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        return modifyAnnot(mUndoBox, mUndoContent);
    }

    @Override
    public boolean redo() {
        return modifyAnnot(mRedoBox, mRedoContent);
    }

    private boolean modifyAnnot(RectF bbox, String content) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof Stamp)) {
                return false;
            }

            final RectF oldBbox = annot.getRect();
            mBBox = new RectF(bbox);
            mContents = content;
            mModifiedDate = AppDmUtil.currentDateToDocumentDate();

            StampEvent modifyEvent = new StampEvent(EditAnnotEvent.EVENTTYPE_MODIFY, this, (Stamp) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(modifyEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                        }

                        DocumentManager.getInstance(mPdfViewCtrl).onAnnotModified(page, annot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            try {
                                RectF annotRect = annot.getRect();
                                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, mPageIndex);
                                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(annotRect));

                                mPdfViewCtrl.convertPdfRectToPageViewRect(oldBbox, oldBbox, mPageIndex);
                                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(oldBbox));
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}

class StampDeleteUndoItem extends StampUndoItem {

    public StampDeleteUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        StampAddUndoItem undoItem = new StampAddUndoItem(mPdfViewCtrl);
        undoItem.mPageIndex = mPageIndex;
        undoItem.mStampType = mStampType;
        undoItem.mDsip = mDsip;
        undoItem.mNM = mNM;
        undoItem.mAuthor = mAuthor;
        undoItem.mFlags = mFlags;
        undoItem.mSubject = mSubject;
        undoItem.mIconName = mIconName;
        undoItem.mCreationDate = mCreationDate;
        undoItem.mModifiedDate = mModifiedDate;
        undoItem.mBBox = new RectF(mBBox);
        undoItem.mBitmap = mBitmap;
        undoItem.mContents = mContents;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Stamp annot = (Stamp) page.addAnnot(Annot.e_annotStamp, mBBox);
            StampEvent addEvent = new StampEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, annot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            try {
                                RectF annotRectF = annot.getRect();
                                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, mPageIndex);
                                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(annotRectF));
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean redo() {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof Stamp)) {
                return false;
            }

            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
            }

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);

            final RectF annotRectF = annot.getRect();
            StampEvent deleteEvent = new StampEvent(EditAnnotEvent.EVENTTYPE_DELETE, this, (Stamp) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(deleteEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            RectF deviceRectF = new RectF();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRectF, mPageIndex);
                            mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(deviceRectF));
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}