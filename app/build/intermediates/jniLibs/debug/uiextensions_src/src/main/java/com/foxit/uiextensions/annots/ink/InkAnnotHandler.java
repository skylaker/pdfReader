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

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.common.PDFPath;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Ink;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AbstractAnnotHandler;
import com.foxit.uiextensions.annots.AbstractToolHandler;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.common.IAnnotTaskResult;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;


class InkAnnotHandler extends AbstractAnnotHandler {
    protected InkToolHandler mToolHandler;
    protected InkAnnotUtil mUtil;
    protected ArrayList<Integer> mMenuText;
    protected String mSubject = "Pencil";

    protected float mBackOpacity;
    protected int mBackColor;
    protected ArrayList<ArrayList<PointF>> mOldInkLists;

    public InkAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl, InkToolHandler toolHandler, InkAnnotUtil util) {
        super(context, pdfViewCtrl, Annot.e_annotInk);
        mToolHandler = toolHandler;
        mColor = mToolHandler.getColor();
        mOpacity = mToolHandler.getOpacity();
        mThickness = mToolHandler.getThickness();
        mUtil = util;
        mMenuText = new ArrayList<Integer>();
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected AbstractToolHandler getToolHandler() {
        return mToolHandler;
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean reRender) {
        try {
            mColor = (int) annot.getBorderColor();
            mOpacity = AppDmUtil.opacity255To100((int) (((Ink) annot).getOpacity() * 255f + 0.5f));
            mThickness = annot.getBorderInfo().getWidth();

            mBackColor = mColor;
            mBackOpacity = ((Ink) annot).getOpacity();
            mOldInkLists = InkAnnotUtil.generateInkList(((Ink) annot).getInkList());
            super.onAnnotSelected(annot, reRender);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
        if (!mIsModified) {
            super.onAnnotDeselected(annot, reRender);
        } else {
            InkModifyUndoItem undoItem = new InkModifyUndoItem(this, mPdfViewCtrl);
            undoItem.setCurrentValue(mSelectedAnnot);
            try {
                undoItem.mPath = ((Ink)mSelectedAnnot).getInkList();
                undoItem.mInkLists = InkAnnotUtil.generateInkList(undoItem.mPath);
                undoItem.mOldColor = mBackColor;
                undoItem.mOldOpacity = mBackOpacity;
                undoItem.mOldBBox = new RectF(mBackRect);
                undoItem.mOldLineWidth = mBackThickness;

                undoItem.mOldInkLists = InkAnnotUtil.cloneInkList(mOldInkLists);
                undoItem.mOldPath = PDFPath.create();
                for (int li = 0; li < mOldInkLists.size(); li++) { //li: line index
                    ArrayList<PointF> line = mOldInkLists.get(li);
                    for (int pi = 0; pi < line.size(); pi++) {//pi: point index
                        if (pi == 0) {
                            undoItem.mOldPath.moveTo(line.get(pi));
                        } else {
                            undoItem.mOldPath.lineTo(line.get(pi));
                        }
                    }
                }

            } catch (PDFException e) {
                e.printStackTrace();
            }
            modifyAnnot(mSelectedAnnot, undoItem, false, true, reRender, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (mSelectedAnnot != DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                        resetStatus();
                    }
                }
            });
            dismissPopupMenu();
            hidePropertyBar();
        }
    }

    @Override
    public void addAnnot(int pageIndex, final AnnotContent content, boolean addUndo, final Event.Callback result) {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            final InkAnnotContent inkAnnotContent = (InkAnnotContent) content;
            final Ink annot = (Ink) page.addAnnot(Annot.e_annotInk, inkAnnotContent.getBBox());
            InkAddUndoItem undoItem = new InkAddUndoItem(this, mPdfViewCtrl);
            undoItem.setCurrentValue(inkAnnotContent);
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();

            ArrayList<ArrayList<PointF>> lines = ((InkAnnotContent) content).getInkLisk();
            if (lines != null) {
                undoItem.mPath = PDFPath.create();
                for (int i = 0; i < lines.size(); i++) {
                    ArrayList<PointF> line = lines.get(i);
                    for (int j = 0; j < line.size(); j++) {
                        if (j == 0) {
                            undoItem.mPath.moveTo(line.get(j));
                        } else {
                            undoItem.mPath.lineTo(line.get(j));
                        }
                    }
                }
            }
            undoItem.mInkLists = InkAnnotUtil.cloneInkList(lines);
            addAnnot(pageIndex, annot, undoItem, addUndo, new IAnnotTaskResult<PDFPage, Annot, Void>() {
                        public void onResult(boolean success, PDFPage p1, Annot p2, Void p3) {
                            if (result != null) {
                                result.result(null, true);
                            }
                        }
                    });
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected Annot addAnnot(int pageIndex, RectF bbox, final int color, final int opacity, final float thickness,
                             final ArrayList<ArrayList<PointF>> lines, IAnnotTaskResult<PDFPage, Annot, Void> result) {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            final Ink annot = (Ink) page.addAnnot(Annot.e_annotInk, bbox);

            InkAddUndoItem undoItem = new InkAddUndoItem(this, mPdfViewCtrl);
            undoItem.mPageIndex = pageIndex;
            undoItem.mNM = AppDmUtil.randomUUID(null);
            undoItem.mBBox = new RectF(bbox);
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mFlags = Annot.e_annotFlagPrint;
            undoItem.mSubject = mSubject;
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mColor = color;
            undoItem.mOpacity = opacity / 255f;
            undoItem.mLineWidth = thickness;
            undoItem.mPath = PDFPath.create();
            for (int li = 0; li < lines.size(); li++) { //li: line index
                ArrayList<PointF> line = lines.get(li);
                for (int pi = 0; pi < line.size(); pi++) {//pi: point index
                    if (pi == 0) {
                        undoItem.mPath.moveTo(line.get(pi));
                    } else {
                        undoItem.mPath.lineTo(line.get(pi));
                    }
                }
            }
            undoItem.mInkLists = InkAnnotUtil.cloneInkList(lines);
            addAnnot(pageIndex, annot, undoItem, true, result);
            return annot;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void addAnnot(int pageIndex, Annot annot, InkAddUndoItem undoItem, boolean addUndo, IAnnotTaskResult<PDFPage, Annot, Void> result) {
        InkEvent event = new InkEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, (Ink) annot, mPdfViewCtrl);

        handleAddAnnot(pageIndex, annot, event, addUndo, result);
    }

    @Override
    public Annot handleAddAnnot(final int pageIndex, final Annot annot, final EditAnnotEvent addEvent, final boolean addUndo,
                                final IAnnotTaskResult<PDFPage, Annot, Void> result) {
        try {
            final PDFPage page = annot.getPage();

            EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, annot);
                        if (addUndo) {
                            DocumentManager.getInstance(mPdfViewCtrl).addUndoItem(addEvent.mUndoItem);
                        }
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            RectF pvRect = getBBox(mPdfViewCtrl, annot);
                            final Rect tv_rect1 = new Rect();
                            pvRect.roundOut(tv_rect1);
                            mPdfViewCtrl.refresh(pageIndex, tv_rect1);
                        }

                    }

                    if (result != null) {
                        result.onResult(success, page, annot, null);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return annot;
    }

    @Override
    public void modifyAnnot(final Annot annot, final AnnotContent content, boolean addUndo, final Event.Callback result) {
        try {
            InkModifyUndoItem undoItem = new InkModifyUndoItem(this, mPdfViewCtrl);
            undoItem.setOldValue(annot);
            undoItem.mOldPath = ((Ink)annot).getInkList();
            undoItem.mOldInkLists = InkAnnotUtil.generateInkList(undoItem.mOldPath );
            undoItem.setCurrentValue(content);
            if (content instanceof InkAnnotContent) {
                ArrayList<ArrayList<PointF>> lines = ((InkAnnotContent) content).getInkLisk();
                if (lines != null) {
                    undoItem.mPath = PDFPath.create();

                    for (int i = 0; i < lines.size(); i++) {
                        ArrayList<PointF> line = lines.get(i);
                        for (int j = 0; j < line.size(); j++) {
                            if (j == 0) {
                                undoItem.mPath.moveTo(line.get(j));
                            } else {
                                undoItem.mPath.lineTo(line.get(j));
                            }
                        }
                    }
                }
                undoItem.mInkLists = InkAnnotUtil.cloneInkList(lines);
            }

            modifyAnnot(annot, undoItem, false, addUndo, true, result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected void modifyAnnot(Annot annot, InkUndoItem undoItem, boolean useOldValue, boolean addUndo, boolean reRender,
                               final Event.Callback result) {
        InkEvent event = new InkEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Ink) annot, mPdfViewCtrl);
        event.useOldValue = useOldValue;
        handleModifyAnnot(annot, event, addUndo, reRender,
                new IAnnotTaskResult<PDFPage, Annot, Void>() {
                    @Override
                    public void onResult(boolean success, PDFPage p1, Annot p2, Void p3) {
                        if (result != null) {
                            result.result(null, success);
                        }
                    }
                });
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, final Event.Callback result) {
        InkDeleteUndoItem undoItem = new InkDeleteUndoItem(this, mPdfViewCtrl);
        undoItem.setCurrentValue(annot);
        try {
            undoItem.mPath = ((Ink)annot).getInkList();
            undoItem.mInkLists = InkAnnotUtil.generateInkList(undoItem.mPath);
        } catch (PDFException e) {
            e.printStackTrace();
        }

        removeAnnot(annot, undoItem, addUndo, result);
    }

    protected void removeAnnot(Annot annot, InkDeleteUndoItem undoItem, boolean addUndo, final Event.Callback result) {
        InkEvent event = new InkEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Ink) annot, mPdfViewCtrl);
        handleRemoveAnnot(annot, event, addUndo,
                new IAnnotTaskResult<PDFPage, Void, Void>() {
                    @Override
                    public void onResult(boolean success, PDFPage p1, Void p2, Void p3) {
                        if (result != null) {
                            result.result(null, success);
                        }
                    }
                });
    }

    @Override
    protected ArrayList<Path> generatePathData(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot) {
        return InkAnnotUtil.generatePathData(mPdfViewCtrl, pageIndex, (Ink) annot);
    }

    @Override
    protected void transformAnnot(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot, Matrix matrix) {
        RectF bbox = getBBox(pdfViewCtrl, annot);
        matrix.mapRect(bbox);
        pdfViewCtrl.convertPageViewRectToPdfRect(bbox, bbox, pageIndex);

        transformLines(pdfViewCtrl, pageIndex, (Ink) annot, matrix);

        try {
            annot.move(bbox);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void resetStatus() {
        mBackRect = null;
        mBackThickness = 0.0f;
        mSelectedAnnot = null;
        mIsModified = false;
    }

    @Override
    protected void showPopupMenu() {
        Annot curAnnot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (curAnnot == null) return;
        try {
            if (curAnnot.getType() != Annot.e_annotInk)
                return;

            reloadPopupMenuString();
            mAnnotMenu.setMenuItems(mMenuText);
            RectF bbox = curAnnot.getRect();
            int pageIndex = curAnnot.getPage().getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
            mAnnotMenu.show(bbox);
            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
                @Override
                public void onAMClick(int flag) {
                    if (mSelectedAnnot == null) return;
                    if (flag == AnnotMenu.AM_BT_COMMENT) { // comment
                        DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                        UIAnnotReply.showComments(mPdfViewCtrl, ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mSelectedAnnot);
                    } else if (flag == AnnotMenu.AM_BT_REPLY) { // reply
                        DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                        UIAnnotReply.replyToAnnot(mPdfViewCtrl, ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mSelectedAnnot);
                    } else if (flag == AnnotMenu.AM_BT_DELETE) { // delete
                        if (mSelectedAnnot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                            removeAnnot(mSelectedAnnot, true, null);
                        }
                    } else if (flag == AnnotMenu.AM_BT_STYLE) { // line color
                        dismissPopupMenu();
                        showPropertyBar(PropertyBar.PROPERTY_COLOR);
                    }
                }
            });
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void dismissPopupMenu() {
        mAnnotMenu.setListener(null);
        mAnnotMenu.dismiss();
    }

    @Override
    protected void showPropertyBar(long curProperty) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null) return;
        if (!(annot instanceof Ink)) return;
        long properties = getSupportedProperties();

        mPropertyBar.setPropertyChangeListener(this);
        setPropertyBarProperties(mPropertyBar);
        mPropertyBar.reset(properties);

        try {
            RectF bbox = annot.getRect();
            int pageIndex = annot.getPage().getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
            mPropertyBar.show(bbox, false);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPaintProperty(PDFViewCtrl pdfViewCtrl, int pageIndex, Paint paint, Annot annot) {
        super.setPaintProperty(pdfViewCtrl, pageIndex, paint, annot);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Style.STROKE);

    }

    @Override
    protected long getSupportedProperties() {
        return mUtil.getSupportedProperties();
    }

    @Override
    protected void setPropertyBarProperties(PropertyBar propertyBar) {
        int[] colors = new int[PropertyBar.PB_COLORS_PENCIL.length];
        System.arraycopy(PropertyBar.PB_COLORS_PENCIL, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_PENCIL[0];
        propertyBar.setColors(colors);
        super.setPropertyBarProperties(propertyBar);
    }

    protected void reloadPopupMenuString() {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null) return;
        mMenuText.clear();

        if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            mMenuText.add(AnnotMenu.AM_BT_STYLE);
            mMenuText.add(AnnotMenu.AM_BT_COMMENT);
            mMenuText.add(AnnotMenu.AM_BT_REPLY);
            mMenuText.add(AnnotMenu.AM_BT_DELETE);
        } else {
            mMenuText.add(AnnotMenu.AM_BT_COMMENT);
        }
    }

    private void transformLines(PDFViewCtrl pdfViewCtrl, int pageIndex, Ink annot, Matrix matrix) {
        try {
            float[] tmp = {0, 0};
            PDFPath path = annot.getInkList();
            for (int i = 0; i < path.getPointCount(); i++) {
                PointF pt = path.getPoint(i);
                pdfViewCtrl.convertPdfPtToPageViewPt(pt, pt, pageIndex);
                tmp[0] = pt.x;
                tmp[1] = pt.y;
                matrix.mapPoints(tmp);
                pt.set(tmp[0], tmp[1]);
                pdfViewCtrl.convertPageViewPtToPdfPt(pt, pt, pageIndex);

                path.setPoint(i, pt, path.getPointType(i));
            }

            annot.setInkList(path);
            annot.resetAppearanceStream();
        } catch (PDFException e) {

        }

    }

}