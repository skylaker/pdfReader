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
package com.foxit.uiextensions.annots.freetext.typewriter;

import android.graphics.RectF;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.ToolUtil;

public abstract class TypewriterUndoItem extends AnnotUndoItem {
    Font mFont;
    float mFontSize;
    long mTextColor;
    long mDaFlags;
    public TypewriterUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }
}

class TypewriterAddUndoItem extends TypewriterUndoItem {

    public TypewriterAddUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        TypewriterDeleteUndoItem undoItem = new TypewriterDeleteUndoItem(mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mPageIndex = mPageIndex;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof FreeText)) {
                return false;
            }

            if (((FreeText) annot).getIntent() == null
                    || !((FreeText) annot).getIntent().equals("FreeTextTypewriter")) {
                return false;
            }

            TypewriterAnnotHandler annotHandler = (TypewriterAnnotHandler) ToolUtil.getAnnotHandlerByType((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager(), Annot.e_annotFreeText);
            if (annotHandler == null) {
                return false;
            }
            annotHandler.deleteAnnot(annot, undoItem, null);
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
            final Annot annot = page.addAnnot(Annot.e_annotFreeText, mBBox);

            TypewriterEvent addEvent = new TypewriterEvent(EditAnnotEvent.EVENTTYPE_ADD, this, (FreeText) annot, mPdfViewCtrl);
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

class TypewriterModifyUndoItem extends TypewriterUndoItem {
    Font mOldFont;
    float mOldFontSize;
    long mOldTextColor;
    public TypewriterModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        TypewriterModifyUndoItem undoItem = new TypewriterModifyUndoItem(mPdfViewCtrl);
        undoItem.mPageIndex = this.mPageIndex;
        undoItem.mNM = this.mNM;
        undoItem.mTextColor = this.mOldTextColor;
        undoItem.mOpacity = this.mOldOpacity;
        undoItem.mFont = this.mOldFont;
        undoItem.mFontSize = this.mOldFontSize;
        undoItem.mContents = this.mOldContents;
        undoItem.mBBox = new RectF(this.mOldBBox);
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();

        return modifyAnnot(undoItem);
    }

    @Override
    public boolean redo() {
        return modifyAnnot(this);
    }

    private boolean modifyAnnot(TypewriterModifyUndoItem undoItem) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof FreeText)) {
                return false;
            }

            if (((FreeText) annot).getIntent() == null
                    || !((FreeText) annot).getIntent().equals("FreeTextTypewriter")) {
                return false;
            }

            final RectF oldBbox = annot.getRect();

            TypewriterEvent modifyEvent = new TypewriterEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (FreeText) annot, mPdfViewCtrl);
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
                                annotRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 3,
                                        -AppAnnotUtil.getAnnotBBoxSpace() - 3);
                                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(annotRect));

                                mPdfViewCtrl.convertPdfRectToPageViewRect(oldBbox, oldBbox, mPageIndex);
                                oldBbox.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 3,
                                        -AppAnnotUtil.getAnnotBBoxSpace() - 3);
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

class TypewriterDeleteUndoItem extends TypewriterUndoItem {

    public TypewriterDeleteUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        TypewriterAddUndoItem undoItem = new TypewriterAddUndoItem(mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mAuthor = mAuthor;
        undoItem.mBBox = new RectF(mBBox);
        undoItem.mColor = mColor;
        undoItem.mContents = mContents;
        undoItem.mModifiedDate = mModifiedDate;
        undoItem.mOpacity = mOpacity;
        undoItem.mPageIndex = mPageIndex;
        undoItem.mFlags = mFlags;
        undoItem.mFont = mFont;
        undoItem.mFontSize = mFontSize;
        undoItem.mTextColor = mTextColor;
        undoItem.mDaFlags = mDaFlags;
        undoItem.mIntent = mIntent;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final FreeText annot = (FreeText) page.addAnnot(Annot.e_annotFreeText, new RectF(mBBox));

            TypewriterEvent addEvent = new TypewriterEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
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
            if (annot == null || !(annot instanceof FreeText)) {
                return false;
            }

            if (((FreeText) annot).getIntent() == null
                    || !((FreeText) annot).getIntent().equals("FreeTextTypewriter")) {
                return false;
            }

            TypewriterAnnotHandler annotHandler = (TypewriterAnnotHandler) ToolUtil.getAnnotHandlerByType((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager(), Annot.e_annotFreeText);
            if (annotHandler == null) {
                return false;
            }
            annotHandler.deleteAnnot(annot, this, null);

            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}