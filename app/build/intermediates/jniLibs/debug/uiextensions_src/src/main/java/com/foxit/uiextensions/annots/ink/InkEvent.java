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
package com.foxit.uiextensions.annots.ink;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.BorderInfo;
import com.foxit.sdk.pdf.annots.Ink;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;

public class InkEvent extends EditAnnotEvent {
    public InkEvent(int eventType, InkUndoItem undoItem, Ink ink, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnot = ink;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        if (mAnnot == null || !(mAnnot instanceof Ink)) {
            return false;
        }
        Ink annot = (Ink) mAnnot;
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

            if (mUndoItem.mIntent != null) {
                annot.setIntent(mUndoItem.mIntent);
            }

            if (mUndoItem.mSubject != null) {
                annot.setSubject(mUndoItem.mSubject);
            }

            if (((InkAddUndoItem)mUndoItem).mPath != null) {
                annot.setInkList(((InkAddUndoItem)mUndoItem).mPath);
            }

            BorderInfo borderInfo = new BorderInfo();
            borderInfo.setWidth(mUndoItem.mLineWidth);
            annot.setBorderInfo(borderInfo);

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
        if (mAnnot == null || !(mAnnot instanceof Ink)) {
            return false;
        }
        Ink annot = (Ink) mAnnot;
        try {
            if (mUndoItem.mModifiedDate != null) {
                annot.setModifiedDateTime(mUndoItem.mModifiedDate);
            }
            if (!useOldValue) {
                annot.setBorderColor(mUndoItem.mColor);
                annot.setOpacity(mUndoItem.mOpacity);
                BorderInfo borderInfo = new BorderInfo();
                borderInfo.setWidth(mUndoItem.mLineWidth);
                annot.setBorderInfo(borderInfo);
                if (((InkModifyUndoItem)mUndoItem).mPath != null) {
                    annot.setInkList(((InkModifyUndoItem)mUndoItem).mPath);
                }
                if (mUndoItem.mContents != null) {
                    annot.setContent(mUndoItem.mContents);
                } else {
                    annot.setContent("");
                }
            } else {
                annot.setBorderColor(mUndoItem.mOldColor);
                annot.setOpacity(mUndoItem.mOldOpacity);
                BorderInfo borderInfo = new BorderInfo();
                borderInfo.setWidth(mUndoItem.mOldLineWidth);
                annot.setBorderInfo(borderInfo);
                if (((InkModifyUndoItem)mUndoItem).mOldPath != null) {
                    annot.setInkList(((InkModifyUndoItem)mUndoItem).mOldPath);
                }
                if (mUndoItem.mOldContents != null) {
                    annot.setContent(mUndoItem.mOldContents);
                } else {
                    annot.setContent("");
                }
            }

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
    public boolean delete() {
        if (mAnnot == null || !(mAnnot instanceof Ink)) {
            return false;
        }

        try {
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
