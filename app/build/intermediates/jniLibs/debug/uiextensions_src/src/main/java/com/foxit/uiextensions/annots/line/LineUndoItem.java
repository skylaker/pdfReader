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
package com.foxit.uiextensions.annots.line;

import android.graphics.PointF;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Line;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.annots.AnnotUndoItem;

public abstract class LineUndoItem extends AnnotUndoItem {
    PointF mStartPt = new PointF();
    PointF mEndPt = new PointF();

    String mStartingStyle;
    String mEndingStyle;

    PointF mOldStartPt = new PointF();
    PointF mOldEndPt = new PointF();

    String mOldStartingStyle;
    String mOldEndingStyle;

    LineRealAnnotHandler mAnnotHandler;
    public LineUndoItem(LineRealAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
        mAnnotHandler = annotHandler;
        mPdfViewCtrl = pdfViewCtrl;
    }
}

class LineAddUndoItem extends LineUndoItem {

    public LineAddUndoItem(LineRealAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
        super(annotHandler, pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        LineDeleteUndoItem undoItem = new LineDeleteUndoItem(mAnnotHandler, mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mPageIndex = mPageIndex;
        undoItem.mStartPt.set(mStartPt);
        undoItem.mEndPt.set(mEndPt);
        undoItem.mStartingStyle = mStartingStyle;
        undoItem.mEndingStyle = mEndingStyle;

        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof Line)) {
                return false;
            }

            mAnnotHandler.removeAnnot(annot, undoItem, false, null);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean redo() {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Annot annot = page.addAnnot(Annot.e_annotLine, mBBox);

            mAnnotHandler.addAnnot(mPageIndex, (Line) annot, this, false, null);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return false;
    }
}

class LineModifyUndoItem extends LineUndoItem {

    public LineModifyUndoItem(LineRealAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
        super(annotHandler, pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        return modifyAnnot(true);
    }

    @Override
    public boolean redo() {
        return modifyAnnot(false);
    }

    private boolean modifyAnnot(boolean userOldValue) {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof Line)) {
                return false;
            }

            mAnnotHandler.modifyAnnot((Line) annot, this, userOldValue, false, true, null);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}

class LineDeleteUndoItem extends LineUndoItem {

    public LineDeleteUndoItem(LineRealAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
        super(annotHandler, pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Line annot = (Line) page.addAnnot(Annot.e_annotLine, mBBox);
            LineAddUndoItem undoItem = new LineAddUndoItem(mAnnotHandler, mPdfViewCtrl);
            undoItem.mNM = mNM;
            undoItem.mPageIndex = mPageIndex;
            undoItem.mStartPt.set(mStartPt);
            undoItem.mEndPt.set(mEndPt);
            undoItem.mStartingStyle = mStartingStyle;
            undoItem.mEndingStyle = mEndingStyle;
            undoItem.mAuthor = mAuthor;
            undoItem.mFlags = mFlags;
            undoItem.mSubject = mSubject;
            undoItem.mCreationDate = mCreationDate;
            undoItem.mModifiedDate = mModifiedDate;
            undoItem.mColor = mColor;
            undoItem.mOpacity = mOpacity;
            undoItem.mLineWidth = mLineWidth;
            undoItem.mIntent = mIntent;
            undoItem.mContents = mContents;

            mAnnotHandler.addAnnot(mPageIndex, annot, undoItem, false, null);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean redo() {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof Line)) {
                return false;
            }

            mAnnotHandler.removeAnnot(annot, this, false, null);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}