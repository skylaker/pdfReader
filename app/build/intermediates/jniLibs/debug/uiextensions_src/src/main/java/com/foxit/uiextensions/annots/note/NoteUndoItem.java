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
package com.foxit.uiextensions.annots.note;

import android.graphics.RectF;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.Note;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;

public abstract class NoteUndoItem extends AnnotUndoItem {
    String mIconName;
    boolean mOpenStatus;
    boolean mIsFromReplyModule = false;
    String mParentNM;// use nm get annot, and addReply
}

class NoteAddUndoItem extends NoteUndoItem {

    public NoteAddUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }
    @Override
    public boolean undo() {
        NoteDeleteUndoItem undoItem = new NoteDeleteUndoItem(mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mPageIndex = mPageIndex;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof Note)) {
                return false;
            }

            final RectF annotRectF = annot.getRect();

            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
            }

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);

            NoteEvent deleteEvent = new NoteEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Note) annot, mPdfViewCtrl);
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
            Annot annot = null;
            if (mIsFromReplyModule) {
                Annot note = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mParentNM);
                if (note == null) return false;
                annot = ((Markup) note).addReply();
            } else {
                annot = page.addAnnot(Annot.e_annotNote, this.mBBox);
            }

            NoteEvent addEvent = new NoteEvent(EditAnnotEvent.EVENTTYPE_ADD, this, (Note) annot, mPdfViewCtrl);
            final Annot finalAnnot = annot;
            EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, finalAnnot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            try {
                                RectF annotRect = finalAnnot.getRect();
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

class NoteModifyUndoItem extends NoteUndoItem {
    public int 		mUndoColor;
    public float 	mUndoOpacity;
    public String   mUndoIconName;
    public RectF    mUndoBbox;
    public String   mUndoContent;


    public int		mRedoColor;
    public float	mRedoOpacity;
    public String   mRedoIconName;
    public RectF    mRedoBbox;
    public String   mRedoContent;

    public NoteModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        return modifyAnnot(mUndoColor, mUndoOpacity, mUndoContent, mUndoIconName, mUndoBbox);
    }

    @Override
    public boolean redo() {
        return modifyAnnot(mRedoColor, mRedoOpacity, mRedoContent, mRedoIconName, mRedoBbox);
    }

    private boolean modifyAnnot(int color, float opacity, String content, String iconName, RectF bbox) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof Note)) {
                return false;
            }

            final RectF oldBbox = annot.getRect();
            mBBox = new RectF(bbox);
            mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            mColor = color;
            mOpacity = opacity;
            mIconName = iconName;
            mContents = content;

            NoteEvent modifyEvent = new NoteEvent(EditAnnotEvent.EVENTTYPE_MODIFY, this, (Note) annot, mPdfViewCtrl);
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
                                annotRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 3, -AppAnnotUtil.getAnnotBBoxSpace() - 3);
                                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(annotRect));

                                mPdfViewCtrl.convertPdfRectToPageViewRect(oldBbox, oldBbox, mPageIndex);
                                oldBbox.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 3, -AppAnnotUtil.getAnnotBBoxSpace() - 3);
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

class NoteDeleteUndoItem extends NoteUndoItem {

    public NoteDeleteUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        NoteAddUndoItem undoItem = new NoteAddUndoItem(mPdfViewCtrl);
        undoItem.mPageIndex = mPageIndex;
        undoItem.mNM = mNM;
        undoItem.mAuthor = mAuthor;
        undoItem.mFlags = mFlags;
        undoItem.mSubject = mSubject;
        undoItem.mIconName = mIconName;
        undoItem.mCreationDate = mCreationDate;
        undoItem.mModifiedDate = mModifiedDate;
        undoItem.mBBox = new RectF(mBBox);
        undoItem.mContents = mContents;
        undoItem.mColor = mColor;
        undoItem.mOpacity = mOpacity;
        undoItem.mIsFromReplyModule = mIsFromReplyModule;
        undoItem.mParentNM = mParentNM;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Note annot = null;
            if (mIsFromReplyModule) {
                Annot note = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mParentNM);
                if (note == null) return false;
                annot = ((Markup) note).addReply();
            } else {
                annot = (Note) page.addAnnot(Annot.e_annotNote, this.mBBox);
            }
            NoteEvent addEvent = new NoteEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
            final Note finalAnnot = annot;
            EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, finalAnnot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            try {
                                RectF annotRectF = finalAnnot.getRect();
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
            if (annot == null || !(annot instanceof Note)) {
                return false;
            }

            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
            }

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);

            final RectF annotRectF = annot.getRect();
            NoteEvent deleteEvent = new NoteEvent(EditAnnotEvent.EVENTTYPE_DELETE, this, (Note) annot, mPdfViewCtrl);
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