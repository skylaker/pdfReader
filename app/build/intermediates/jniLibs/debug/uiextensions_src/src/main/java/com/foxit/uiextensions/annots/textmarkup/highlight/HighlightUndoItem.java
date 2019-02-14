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
package com.foxit.uiextensions.annots.textmarkup.highlight;

import android.graphics.Paint;
import android.graphics.RectF;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Highlight;
import com.foxit.sdk.pdf.annots.QuadPoints;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;

public abstract class HighlightUndoItem extends AnnotUndoItem {
    QuadPoints[] quadPointsArray;
}

class HighlightAddUndoItem extends HighlightUndoItem {

    public HighlightAddUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        HighlightDeleteUndoItem undoItem = new HighlightDeleteUndoItem(mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mPageIndex = mPageIndex;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof Highlight)) {
                return false;
            }

            final RectF annotRectF = annot.getRect();

            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
            }

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);

            HighlightEvent deleteEvent = new HighlightEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Highlight) annot, mPdfViewCtrl);
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
            final Annot annot = page.addAnnot(Annot.e_annotHighlight, new RectF(0, 0, 0, 0));

            HighlightEvent addEvent = new HighlightEvent(EditAnnotEvent.EVENTTYPE_ADD, this, (Highlight) annot, mPdfViewCtrl);
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

class HighlightModifyUndoItem extends HighlightUndoItem {
    public int 		mUndoColor;
    public float 	mUndoOpacity;
    public String	mUndoContents;

    public int		mRedoColor;
    public float	mRedoOpacity;
    public String	mRedoContents;

    public Paint mPaintBbox;

    public HighlightModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof Highlight)) {
                return false;
            }

            mColor = mUndoColor;
            mOpacity = mUndoOpacity;
            mContents = mUndoContents;

            mPaintBbox.setColor(mUndoColor | 0xFF000000);

            HighlightEvent modifyEvent = new HighlightEvent(EditAnnotEvent.EVENTTYPE_MODIFY, this, (Highlight) annot, mPdfViewCtrl);
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
            if (annot == null || !(annot instanceof Highlight)) {
                return false;
            }

            mColor = mRedoColor;
            mOpacity = mRedoOpacity;
            mContents = mRedoContents;
            mPaintBbox.setColor(mRedoColor | 0xFF000000);
            HighlightEvent modifyEvent = new HighlightEvent(EditAnnotEvent.EVENTTYPE_MODIFY, this, (Highlight) annot, mPdfViewCtrl);
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

class HighlightDeleteUndoItem extends HighlightUndoItem {

    public HighlightDeleteUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        HighlightAddUndoItem undoItem = new HighlightAddUndoItem(mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mAuthor = mAuthor;
        undoItem.mBBox = new RectF(mBBox);
        undoItem.mColor = mColor;
        undoItem.mContents = mContents;
        undoItem.mModifiedDate = mModifiedDate;
        undoItem.mOpacity = mOpacity;
        undoItem.mPageIndex = mPageIndex;
        undoItem.mType = mType;
        undoItem.mFlags = mFlags;
        undoItem.quadPointsArray = new QuadPoints[quadPointsArray.length];
        System.arraycopy(quadPointsArray, 0, undoItem.quadPointsArray, 0, quadPointsArray.length);

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Highlight highlight = (Highlight) page.addAnnot(Annot.e_annotHighlight, new RectF(0, 0, 0, 0));

            HighlightEvent addEvent = new HighlightEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, highlight, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, highlight);

                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            try {
                                RectF annotRectF = highlight.getRect();
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

        }
        return false;
    }

    @Override
    public boolean redo() {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof Highlight)) {
                return false;
            }

            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
            }

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);

            final RectF annotRectF = annot.getRect();
            HighlightEvent deleteEvent = new HighlightEvent(EditAnnotEvent.EVENTTYPE_DELETE, this, (Highlight) annot, mPdfViewCtrl);
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