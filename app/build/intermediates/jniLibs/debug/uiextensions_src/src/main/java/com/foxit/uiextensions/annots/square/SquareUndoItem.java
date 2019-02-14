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
package com.foxit.uiextensions.annots.square;

import android.graphics.RectF;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Square;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;

public abstract class SquareUndoItem extends AnnotUndoItem {
}

class SquareAddUndoItem extends SquareUndoItem {

    public SquareAddUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        SquareDeleteUndoItem undoItem = new SquareDeleteUndoItem(mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mPageIndex = mPageIndex;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof Square)) {
                return false;
            }

            final RectF annotRectF = annot.getRect();

            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
            }

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);

            SquareEvent deleteEvent = new SquareEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Square) annot, mPdfViewCtrl);
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
            final Annot annot = page.addAnnot(Annot.e_annotSquare, mBBox);

            SquareEvent addEvent = new SquareEvent(EditAnnotEvent.EVENTTYPE_ADD, this, (Square) annot, mPdfViewCtrl);
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

class SquareModifyUndoItem extends SquareUndoItem {
    public int 		mUndoColor;
    public float 	mUndoOpacity;
    public float    mUndoLineWidth;
    public RectF    mUndoBbox;
    public String   mUndoContent;


    public int		mRedoColor;
    public float	mRedoOpacity;
    public float    mRedoLineWidth;
    public RectF    mRedoBbox;
    public String   mRedoContent;

    public SquareModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        return modifyAnnot(mUndoColor, mUndoOpacity, mUndoLineWidth, mUndoBbox, mUndoContent);
    }

    @Override
    public boolean redo() {
        return modifyAnnot(mRedoColor, mRedoOpacity, mRedoLineWidth, mRedoBbox, mRedoContent);
    }

    private boolean modifyAnnot(int color, float opacity, float lineWidth, RectF bbox, String content) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof Square)) {
                return false;
            }

            final RectF oldBbox = annot.getRect();
            mColor = color;
            mOpacity = opacity;
            mBBox = new RectF(bbox);
            mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            mLineWidth = lineWidth;
            mContents = content;

            SquareEvent modifyEvent = new SquareEvent(EditAnnotEvent.EVENTTYPE_MODIFY, this, (Square) annot, mPdfViewCtrl);
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

class SquareDeleteUndoItem extends SquareUndoItem {

    public SquareDeleteUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        SquareAddUndoItem undoItem = new SquareAddUndoItem(mPdfViewCtrl);
        undoItem.mPageIndex = mPageIndex;
        undoItem.mNM = mNM;
        undoItem.mAuthor = mAuthor;
        undoItem.mFlags = mFlags;
        undoItem.mSubject = mSubject;
        undoItem.mCreationDate = mCreationDate;
        undoItem.mModifiedDate = mModifiedDate;
        undoItem.mBBox = new RectF(mBBox);
        undoItem.mColor = mColor;
        undoItem.mOpacity = mOpacity;
        undoItem.mLineWidth = mLineWidth;
        undoItem.mBorderStyle = mBorderStyle;
        undoItem.mContents = mContents;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Square annot = (Square) page.addAnnot(Annot.e_annotSquare, mBBox);
            SquareEvent addEvent = new SquareEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
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
            if (annot == null || !(annot instanceof Square)) {
                return false;
            }

            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
            }

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);

            final RectF annotRectF = annot.getRect();
            SquareEvent deleteEvent = new SquareEvent(EditAnnotEvent.EVENTTYPE_DELETE, this, (Square) annot, mPdfViewCtrl);
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