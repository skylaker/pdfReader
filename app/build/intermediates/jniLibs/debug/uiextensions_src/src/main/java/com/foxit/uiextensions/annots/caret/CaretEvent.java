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
package com.foxit.uiextensions.annots.caret;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.CommonDefines;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Caret;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.objects.PDFObject;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.utils.AppAnnotUtil;

public class CaretEvent extends EditAnnotEvent {
    public CaretEvent(int eventType, CaretUndoItem undoItem, Caret caret, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnot = caret;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        if (mAnnot == null || !(mAnnot instanceof Caret)) {
            return false;
        }

        try {
            Caret annot = (Caret) mAnnot;
            annot.setBorderColor(mUndoItem.mColor);
            annot.setOpacity(mUndoItem.mOpacity);
            if (mUndoItem.mContents != null) {
                annot.setContent(mUndoItem.mContents);
            }

            if (mUndoItem.mCreationDate != null) {
                annot.setCreationDateTime(mUndoItem.mCreationDate);
            }

            if (mUndoItem.mModifiedDate != null) {
                annot.setModifiedDateTime(mUndoItem.mModifiedDate);
            }

            if (mUndoItem.mAuthor != null) {
                annot.setTitle(mUndoItem.mAuthor);
            }
            if (mUndoItem.mSubject != null) {
                annot.setSubject(mUndoItem.mSubject);
            }
            annot.setIntent(mUndoItem.mIntent);
            annot.setUniqueID(mUndoItem.mNM);
            int rotate = ((CaretAddUndoItem) mUndoItem).mRotate;
            if (rotate < CommonDefines.e_rotation0 || rotate >CommonDefines.e_rotationUnknown) {
                rotate = 0;
            }
            annot.getDict().setAt("Rotate", PDFObject.createFromInteger(360 - rotate * 90));
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
        if (mAnnot == null || !(mAnnot instanceof Caret)) {
            return false;
        }
        try {
            Caret annot = (Caret) mAnnot;
            annot.setBorderColor(mUndoItem.mColor);
            annot.setOpacity(mUndoItem.mOpacity);
            if (mUndoItem.mContents != null) {
                annot.setContent(mUndoItem.mContents);
            }
            annot.resetAppearanceStream();
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete() {
        if (mAnnot == null || !(mAnnot instanceof Caret)) {
            return false;
        }
        try {
            PDFPage page = mAnnot.getPage();
            //delete strikeout
            if (AppAnnotUtil.isReplaceCaret(mAnnot)) {
                Caret caret = (Caret) mAnnot;
                int nCount = caret.getGroupElementCount();
                for (int i = nCount - 1; i >= 0; i --) {
                    Markup groupAnnot = caret.getGroupElement(i);
                    if (groupAnnot.getType() == Annot.e_annotStrikeOut) {
                        page.removeAnnot(groupAnnot);
                        break;
                    }
                }
            }
            page.removeAnnot(mAnnot);// delete caret
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}
