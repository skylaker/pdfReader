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

import android.graphics.PointF;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.common.PDFPath;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Ink;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.annots.AnnotUndoItem;

import java.util.ArrayList;

public abstract class InkUndoItem extends AnnotUndoItem {
    ArrayList<ArrayList<PointF>> mInkLists;
    ArrayList<ArrayList<PointF>> mOldInkLists;
    PDFPath mPath;
    PDFPath mOldPath;
    InkAnnotHandler mAnnotHandler;
    public InkUndoItem(InkAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
        mAnnotHandler = annotHandler;
        mPdfViewCtrl = pdfViewCtrl;
    }
}

class InkAddUndoItem extends InkUndoItem {

    public InkAddUndoItem(InkAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
        super(annotHandler, pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        InkDeleteUndoItem undoItem = new InkDeleteUndoItem(mAnnotHandler, mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mPageIndex = mPageIndex;
        undoItem.mInkLists = InkAnnotUtil.cloneInkList(mInkLists);
        try {
            undoItem.mPath = PDFPath.create();
            for (int li = 0; li < mInkLists.size(); li++) { //li: line index
                ArrayList<PointF> line = mInkLists.get(li);
                for (int pi = 0; pi < line.size(); pi++) {//pi: point index
                    if (pi == 0) {
                        undoItem.mPath.moveTo(line.get(pi));
                    } else {
                        undoItem.mPath.lineTo(line.get(pi));
                    }
                }
            }
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, mNM);
            if (annot == null || !(annot instanceof Ink)) {
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
            Annot annot = page.addAnnot(Annot.e_annotInk, mBBox);

            if (mInkLists == null) return false;
            mPath = PDFPath.create();
            for (int li = 0; li < mInkLists.size(); li++) { //li: line index
                ArrayList<PointF> line = mInkLists.get(li);
                for (int pi = 0; pi < line.size(); pi++) {//pi: point index
                    if (pi == 0) {
                        mPath.moveTo(line.get(pi));
                    } else {
                        mPath.lineTo(line.get(pi));
                    }
                }
            }
            mAnnotHandler.addAnnot(mPageIndex, (Ink) annot, this, false, null);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}

class InkModifyUndoItem extends InkUndoItem {

    public InkModifyUndoItem(InkAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
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
            if (annot == null || !(annot instanceof Ink)) {
                return false;
            }
            if (userOldValue) {
                if (mOldInkLists != null) {
                    mOldPath = PDFPath.create();
                    for (int li = 0; li < mOldInkLists.size(); li++) { //li: line index
                        ArrayList<PointF> line = mOldInkLists.get(li);
                        for (int pi = 0; pi < line.size(); pi++) {//pi: point index
                            if (pi == 0) {
                                mOldPath.moveTo(line.get(pi));
                            } else {
                                mOldPath.lineTo(line.get(pi));
                            }
                        }
                    }
                }
            } else {
                if (mInkLists != null) {
                    mPath = PDFPath.create();
                    for (int li = 0; li < mInkLists.size(); li++) { //li: line index
                        ArrayList<PointF> line = mInkLists.get(li);
                        for (int pi = 0; pi < line.size(); pi++) {//pi: point index
                            if (pi == 0) {
                                mPath.moveTo(line.get(pi));
                            } else {
                                mPath.lineTo(line.get(pi));
                            }
                        }
                    }
                }
            }
            mAnnotHandler.modifyAnnot((Ink) annot, this, userOldValue, false, true, null);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}

class InkDeleteUndoItem extends InkUndoItem {

    public InkDeleteUndoItem(InkAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
        super(annotHandler, pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Ink annot = (Ink) page.addAnnot(Annot.e_annotInk, mBBox);
            InkAddUndoItem undoItem = new InkAddUndoItem(mAnnotHandler, mPdfViewCtrl);
            undoItem.mNM = mNM;
            undoItem.mPageIndex = mPageIndex;
            undoItem.mAuthor = mAuthor;
            undoItem.mFlags = mFlags;
            undoItem.mSubject = mSubject;
            undoItem.mCreationDate = mCreationDate;
            undoItem.mModifiedDate = mModifiedDate;
            undoItem.mColor = mColor;
            undoItem.mOpacity = mOpacity;
            undoItem.mLineWidth = mLineWidth;
            undoItem.mIntent = mIntent;
            undoItem.mBBox = mBBox;
            undoItem.mContents = mContents;

            undoItem.mPath = PDFPath.create();
            if (mInkLists != null) {
                for (int li = 0; li < mInkLists.size(); li++) { //li: line index
                    ArrayList<PointF> line = mInkLists.get(li);
                    for (int pi = 0; pi < line.size(); pi++) {//pi: point index
                        if (pi == 0) {
                            undoItem.mPath.moveTo(line.get(pi));
                        } else {
                            undoItem.mPath.lineTo(line.get(pi));
                        }
                    }
                }
            }
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
            if (annot == null || !(annot instanceof Ink)) {
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