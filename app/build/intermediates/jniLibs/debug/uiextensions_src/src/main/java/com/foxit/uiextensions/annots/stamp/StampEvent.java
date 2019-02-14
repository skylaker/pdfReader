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

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.Stamp;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StampEvent extends EditAnnotEvent {

    public StampEvent(int eventType, StampUndoItem undoItem, Stamp stamp, PDFViewCtrl pdfViewCtrl) {
        mType = eventType;
        mUndoItem = undoItem;
        mAnnot = stamp;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean add() {
        if (mAnnot == null || !(mAnnot instanceof Stamp)) {
            return false;
        }
        Stamp annot = (Stamp) mAnnot;
        StampAddUndoItem undoItem = (StampAddUndoItem) mUndoItem;
        ByteArrayOutputStream baos = null;
        try {
            annot.setUniqueID(mUndoItem.mNM);
            annot.setFlags(Annot.e_annotFlagPrint);

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

            if (mUndoItem.mSubject != null) {
                annot.setSubject(mUndoItem.mSubject);
            }

            if (undoItem.mIconName != null) {
                annot.setIconName(undoItem.mIconName);
            }
            if (mUndoItem.mContents == null) {
                mUndoItem.mContents = "";
            }
            annot.setContent(mUndoItem.mContents);
            if (undoItem.mStampType >= 17 && undoItem.mStampType <= 21) {
                String filename = "DynamicStamps/" + undoItem.mSubject.substring(4, undoItem.mSubject.length()) + ".pdf";
                InputStream is = mPdfViewCtrl.getContext().getAssets().open(filename);
                if (is == null) {
                    return  false;
                }
                byte[] buffer = new byte[1 << 13];

                baos = new ByteArrayOutputStream();
                int n = 0;
                while(-1 != (n = is.read(buffer))) {
                    baos.write(buffer, 0, n);
                }

                PDFDoc pdfDoc = PDFDoc.createFromMemory(baos.toByteArray());
                pdfDoc.load(null);
                undoItem.mDsip.addDocMap(undoItem.mSubject + Annot.e_annotStamp, pdfDoc);

                is.close();
            } else {
                annot.setBitmap(undoItem.mBitmap);
            }

            annot.resetAppearanceStream();
            return true;
        } catch (PDFException e) {
            if (e.getLastError() == PDFError.OOM.getCode()) {
                mPdfViewCtrl.recoverForOOM();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (baos != null) {
                try {
                    baos.flush();
                    baos.close();
                } catch (IOException e) {
                }
            }
        }
        return false;
    }

    @Override
    public boolean modify() {
        if (mAnnot == null || !(mAnnot instanceof Stamp)) {
            return false;
        }
        Stamp annot = (Stamp) mAnnot;
        try {
            if (mUndoItem.mModifiedDate != null) {
                annot.setModifiedDateTime(mUndoItem.mModifiedDate);
            }
            if (mUndoItem.mContents == null) {
                mUndoItem.mContents = "";
            }
            annot.setContent(mUndoItem.mContents);
            annot.move(mUndoItem.mBBox);
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
        if (mAnnot == null || !(mAnnot instanceof Stamp)) {
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
