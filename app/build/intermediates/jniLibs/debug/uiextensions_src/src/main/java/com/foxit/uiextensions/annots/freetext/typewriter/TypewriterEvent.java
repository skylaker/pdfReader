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

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.DefaultAppearance;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.utils.AppDmUtil;

public class TypewriterEvent extends EditAnnotEvent {

    public TypewriterEvent(int eventType, TypewriterUndoItem undoItem, FreeText typewriter, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnot = typewriter;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        if (mAnnot == null || !(mAnnot instanceof FreeText)) {
            return false;
        }
        FreeText annot = (FreeText) mAnnot;
        try {
            annot.setBorderColor(mUndoItem.mColor);

            annot.setOpacity(mUndoItem.mOpacity);
            if (mUndoItem.mContents != null) {
                annot.setContent(mUndoItem.mContents);
            }

            annot.setFlags(mUndoItem.mFlags);
            if (mUndoItem.mCreationDate != null) {
                annot.setCreationDateTime(mUndoItem.mCreationDate);
            }

            if (mUndoItem.mModifiedDate != null) {
                annot.setModifiedDateTime(mUndoItem.mModifiedDate);
            }

            if (mUndoItem.mAuthor != null) {
                annot.setTitle(mUndoItem.mAuthor);
            }

            TypewriterAddUndoItem undoItem = (TypewriterAddUndoItem) mUndoItem;
            DefaultAppearance da = new DefaultAppearance();
            da.set(undoItem.mDaFlags, undoItem.mFont, undoItem.mFontSize, undoItem.mTextColor);
            annot.setDefaultAppearance(da);
            annot.setIntent(mUndoItem.mIntent);
            annot.setUniqueID(mUndoItem.mNM);
            annot.resetAppearanceStream();
            return true;
        } catch (PDFException e) {
            if (e.getLastError() == PDFError.OOM.getCode()) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
        return false;
    }

    @Override
    public boolean modify() {
        if (mAnnot == null || !(mAnnot instanceof FreeText)) {
            return false;
        }

        try {
            if (((FreeText) mAnnot).getIntent() == null
                    || !((FreeText) mAnnot).getIntent().equals("FreeTextTypewriter")) {
                return false;
            }
            FreeText annot = (FreeText) mAnnot;
            TypewriterModifyUndoItem undoItem = (TypewriterModifyUndoItem) mUndoItem;
            DefaultAppearance da = annot.getDefaultAppearance();
            da.setTextColor(undoItem.mTextColor);
            da.setFont(undoItem.mFont);
            da.setFontSize(undoItem.mFontSize);
            annot.setDefaultAppearance(da);
            annot.setOpacity(undoItem.mOpacity);
            annot.move(mUndoItem.mBBox);
            annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
            annot.setContent(undoItem.mContents);
            annot.resetAppearanceStream();
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete() {
        if (mAnnot == null || !(mAnnot instanceof FreeText)) {
            return false;
        }

        try {
            if (((FreeText) mAnnot).getIntent() == null
                    || !((FreeText) mAnnot).getIntent().equals("FreeTextTypewriter")) {
                return false;
            }
            ((Markup)mAnnot).removeAllReplies();
            PDFPage page = mAnnot.getPage();
            page.removeAnnot(mAnnot);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return false;
    }
}
