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
package com.foxit.uiextensions.annots.textmarkup.squiggly;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.CommonDefines;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.PDFTextSelect;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.QuadPoints;
import com.foxit.sdk.pdf.annots.Squiggly;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupUtil;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.controls.toolbar.BaseItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.CircleItemImpl;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public class SquigglyToolHandler implements ToolHandler {

    private Context mContext;

    private Paint mPaint;
    private int mColor;
    private int mCurrentIndex;
    private int mOpacity;
    public SelectInfo mSelectInfo;
    private RectF mTmpRect;
    private RectF mTmpDesRect;
    private Path mPath;
    private BaseItem mAnnotButton;
    private PropertyBar mPropertyBar;
    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    private PDFViewCtrl mPdfViewCtrl;

    public class SelectInfo {
        public boolean mIsFromTS;
        public int mStartChar;
        public int mEndChar;
        public RectF mBBox;
        public ArrayList<RectF> mRectArray;
        public ArrayList<Boolean> mRectVert;
        public ArrayList<Integer> mRotation;

        public SelectInfo() {
            mBBox = new RectF();
            mRectArray = new ArrayList<RectF>();
            mRectVert = new ArrayList<Boolean>();
            mRotation = new ArrayList<Integer>();
        }

        public void clear() {
            mIsFromTS = false;
            mStartChar = mEndChar = -1;
            mBBox.setEmpty();
            mRectArray.clear();
        }
    }

    public SquigglyToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mSelectInfo = new SelectInfo();
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);

        mTmpRect = new RectF();
        mTmpDesRect = new RectF();
        mPath = new Path();

        init();
    }

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    public void resetLineData() {
        mSelectInfo.mStartChar = mSelectInfo.mEndChar = -1;
        mSelectInfo.mRectArray.clear();
        mSelectInfo.mBBox.setEmpty();
        mTmpRect.setEmpty();
    }

    private void init() {
        //PropertyBar
        mPropertyBar = new PropertyBarImpl(mContext, mPdfViewCtrl);

        //Annot Icon
        mAnnotButton = new CircleItemImpl(mContext);
        mAnnotButton.setImageResource(R.drawable.annot_sqg_selector);
        mAnnotButton.setTag(ToolbarItemConfig.ANNOTS_BAR_ITEM_SQG_TAG);

        mAnnotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetLineData();
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(SquigglyToolHandler.this);
            }
        });

    }

    public void unInit() {

    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_SQUIGGLY;
    }

    @Override
    public void onActivate() {
        resetLineData();
    }

    @Override
    public void onDeactivate() {
    }


    private String getContent(PDFPage page, SelectInfo selectInfo) {
        int start = selectInfo.mStartChar;
        int end = selectInfo.mEndChar;
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        String content = null;
        try {
            if (page.isParsed() != true) {
                int ret = page.startParse(PDFPage.e_parsePageNormal, null, false);
                while (ret == CommonDefines.e_progressToBeContinued) {
                    ret = page.continueParse();
                }
            }
            PDFTextSelect textPage = PDFTextSelect.create(page);

            content = textPage.getChars(start, end - start + 1);
        } catch (PDFException e) {
            if (e.getLastError() == PDFError.OOM.getCode()) {
                mPdfViewCtrl.recoverForOOM();
            }
            return null;
        }
        return content;
    }

    public void addAnnot(final int pageIndex, final boolean addUndo, final ArrayList<RectF> rectArray, final RectF rectF, final SelectInfo selectInfo, final Event.Callback result) {
        PDFPage page = null;
        Squiggly annot = null;
        try {
            page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            annot = (Squiggly) page.addAnnot(Annot.e_annotSquiggly, rectF);
            if (annot == null) {
                if (!misFromSelector) {
                    if (!mIsContinuousCreate) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                        mAnnotButton.setSelected(false);
                    }
                }
                misFromSelector = false;
                return;
            }


        } catch (PDFException e) {
            if (e.getLastError() == PDFError.OOM.getCode()) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }

        QuadPoints[] quadPoint = new QuadPoints[rectArray.size()];
        for (int i = 0; i < rectArray.size(); i++) {
            if (i < selectInfo.mRectVert.size()) {
                RectF rF = new RectF();
                mPdfViewCtrl.convertPageViewRectToPdfRect(rectArray.get(i), rF, pageIndex);

                if (selectInfo.mRectVert.get(i)) {
                    quadPoint[i] = new QuadPoints();
                    PointF point1 = new PointF(rF.left, rF.top);
                    quadPoint[i].setFirst(point1);
                    PointF point2 = new PointF(rF.left, rF.bottom);
                    quadPoint[i].setSecond(point2);
                    PointF point3 = new PointF(rF.right, rF.top);
                    quadPoint[i].setThird(point3);
                    PointF point4 = new PointF(rF.right, rF.bottom);
                    quadPoint[i].setFourth(point4);
                } else {
                    quadPoint[i] = new QuadPoints();
                    PointF point1 = new PointF(rF.left, rF.top);
                    quadPoint[i].setFirst(point1);
                    PointF point2 = new PointF(rF.right, rF.top);
                    quadPoint[i].setSecond(point2);
                    PointF point3 = new PointF(rF.left, rF.bottom);
                    quadPoint[i].setThird(point3);
                    PointF point4 = new PointF(rF.right, rF.bottom);
                    quadPoint[i].setFourth(point4);
                }
            }
        }

        final SquigglyAddUndoItem undoItem = new SquigglyAddUndoItem(mPdfViewCtrl);
        undoItem.mType = Annot.e_annotSquiggly;
        undoItem.mColor = mColor;
        undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mQuadPoints = new QuadPoints[quadPoint.length];
        System.arraycopy(quadPoint, 0, undoItem.mQuadPoints, 0, quadPoint.length);
        undoItem.mContents = getContent(page, selectInfo);
        undoItem.mNM = AppDmUtil.randomUUID(null);
        undoItem.mSubject = "Squiggly";
        undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
        undoItem.mFlags = 4;
        undoItem.mOpacity = mOpacity / 255f;
        undoItem.mPageIndex = pageIndex;

        SquigglyEvent event = new SquigglyEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
        final Squiggly finalAnnot = annot;
        final PDFPage finalPage = page;
        EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                if (success) {
                    DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(finalPage, finalAnnot);
                    if (addUndo) {
                        DocumentManager.getInstance(mPdfViewCtrl).addUndoItem(undoItem);
                    }

                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        invalidate(pageIndex, rectF, result);
                    }

                    resetLineData();

                    if (!misFromSelector) {
                        if (!mIsContinuousCreate) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                            mAnnotButton.setSelected(false);
                        }
                    }
                    misFromSelector = false;
                }
            }
        });
        mPdfViewCtrl.addTask(task);
    }

    private boolean misFromSelector = false;

    public void setFromSelector(boolean b) {
        misFromSelector = b;
    }

    private boolean mIsContinuousCreate = false;

    public boolean getIsContinuousCreate() {
        return mIsContinuousCreate;
    }

    public void setIsContinuousCreate(boolean isContinuousCreate) {
        this.mIsContinuousCreate = isContinuousCreate;
    }

    private void invalidate(int pageIndex, RectF dmRectF, final Event.Callback result) {
        if (dmRectF == null) {
            if (result != null) {
                result.result(null, true);
            }
            return;
        }
        RectF rectF = new RectF();
        mPdfViewCtrl.convertPdfRectToPageViewRect(dmRectF, rectF, pageIndex);
        Rect rect = new Rect();
        rectF.roundOut(rect);
        mPdfViewCtrl.refresh(pageIndex, rect);

        if (null != result) {
            result.result(null, false);
        }
    }

    public void SelectCountRect(int pageIndex, SelectInfo selectInfo) {
        if (selectInfo == null) return;

        int start = selectInfo.mStartChar;
        int end = selectInfo.mEndChar;
        if (start == end && start == -1) return;
        if (end < start) {
            int tmp = end;
            end = start;
            start = tmp;
        }

        selectInfo.mRectArray.clear();
        selectInfo.mRectVert.clear();

        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            if (page.isParsed() != true) {
                int ret = page.startParse(PDFPage.e_parsePageNormal, null, false);
                while (ret == CommonDefines.e_progressToBeContinued) {
                    ret = page.continueParse();
                }
            }
            PDFTextSelect textPage = PDFTextSelect.create(page);

            int count = textPage.getTextRectCount(start, end - start + 1);

            for (int i = 0; i < count; i++) {
                RectF crect = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(textPage.getTextRect(i), crect, pageIndex);
                int rotate = textPage.getBaselineRotation(i);
                boolean vert = rotate == 1 || rotate == 3;

                selectInfo.mRectVert.add(vert);
                selectInfo.mRectArray.add(crect);
                selectInfo.mRotation.add(rotate);
                if (i == 0) {
                    selectInfo.mBBox = new RectF(crect);
                } else {
                    reSizeRect(selectInfo.mBBox, crect);
                }
            }
            textPage.release();
            mPdfViewCtrl.getDoc().closePage(pageIndex);
        } catch (PDFException e) {
            if (e.getLastError() == PDFError.OOM.getCode()) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    private void reSizeRect(RectF MainRt, RectF rect) {
        if (rect.left < MainRt.left) MainRt.left = rect.left;
        if (rect.right > MainRt.right) MainRt.right = rect.right;
        if (rect.bottom > MainRt.bottom) MainRt.bottom = rect.bottom;
        if (rect.top < MainRt.top) MainRt.top = rect.top;
    }

    private boolean OnSelectDown(int pageIndex, PointF point, SelectInfo selectInfo) {
        if (selectInfo == null) return false;
        mCurrentIndex = pageIndex;
        selectInfo.mRectArray.clear();
        selectInfo.mStartChar = selectInfo.mEndChar = -1;
        PDFPage page = null;
        try {
            page = mPdfViewCtrl.getDoc().getPage(mCurrentIndex);
            if (!page.isParsed()) {
                int ret = page.startParse(PDFPage.e_parsePageNormal, null, false);
                while (ret == CommonDefines.e_progressToBeContinued) {
                    ret = page.continueParse();
                }
            }
            PDFTextSelect textPage = PDFTextSelect.create(page);

            PointF pagePt = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(point, pagePt, mCurrentIndex);

            int index = textPage.getIndexAtPos(pagePt.x, pagePt.y, 30);
            if (index >= 0) {
                selectInfo.mStartChar = selectInfo.mEndChar = index;
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFError.OOM.getCode()) {
                mPdfViewCtrl.recoverForOOM();
            }
            return false;
        }
        return true;
    }

    private boolean OnSelectMove(int pageIndex, PointF point, SelectInfo selectInfo) {
        if (selectInfo == null) return false;
        if (mCurrentIndex != pageIndex) return false;
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mCurrentIndex);
            if (!page.isParsed()) {
                int ret = page.startParse(PDFPage.e_parsePageNormal, null, false);
                while (ret == CommonDefines.e_progressToBeContinued) {
                    ret = page.continueParse();
                }
            }
            PDFTextSelect textPage = PDFTextSelect.create(page);

            PointF pagePt = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(point, pagePt, mCurrentIndex);

            int index = textPage.getIndexAtPos(pagePt.x, pagePt.y, 30);
            if (index >= 0) {
                if (selectInfo.mStartChar < 0) selectInfo.mStartChar = index;
                selectInfo.mEndChar = index;
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFError.OOM.getCode()) {
                mPdfViewCtrl.recoverForOOM();
            }
            return false;
        }
        return true;
    }

    public boolean OnSelectRelease(int pageIndex, SelectInfo selectInfo, Event.Callback result) {
        if (selectInfo == null) return false;
        int size = mSelectInfo.mRectArray.size();
        if (size == 0) return false;
        RectF rectF = new RectF();

        mPdfViewCtrl.convertPageViewRectToPdfRect(mSelectInfo.mBBox, rectF, pageIndex);

        addAnnot(pageIndex, true, mSelectInfo.mRectArray, rectF, selectInfo, result);

        return true;
    }

    private RectF calculate(RectF desRectF, RectF srcRectF) {
        if (srcRectF.isEmpty()) return desRectF;
        int count = 0;
        if (desRectF.left == srcRectF.left && desRectF.top == srcRectF.top) count++;
        if (desRectF.right == srcRectF.right && desRectF.top == srcRectF.top) count++;
        if (desRectF.left == srcRectF.left && desRectF.bottom == srcRectF.bottom) count++;
        if (desRectF.right == srcRectF.right && desRectF.bottom == srcRectF.bottom) count++;
        mTmpDesRect.set(desRectF);
        if (count == 2) {
            mTmpDesRect.union(srcRectF);
            RectF rectF = new RectF();
            rectF.set(mTmpDesRect);
            mTmpDesRect.intersect(srcRectF);
            rectF.intersect(mTmpDesRect);
            return rectF;
        } else if (count == 3 || count == 4) {
            return mTmpDesRect;
        } else {
            mTmpDesRect.union(srcRectF);
            return mTmpDesRect;
        }
    }

    private void invalidateTouch(SelectInfo selectInfo, int pageIndex) {
        if (selectInfo == null) return;
        RectF rectF = new RectF();
        rectF.set(mSelectInfo.mBBox);
        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, pageIndex);
        RectF rF = calculate(rectF, mTmpRect);
        Rect rect = new Rect();
        rF.roundOut(rect);
        rect.bottom += 4;
        rect.top -= 4;
        rect.left -= 4;
        rect.right += 4;
        mPdfViewCtrl.invalidate(rect);
        mTmpRect.set(rectF);
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        PointF point = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        int action = motionEvent.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                OnSelectDown(pageIndex, point, mSelectInfo);
                break;
            case MotionEvent.ACTION_MOVE:
                OnSelectMove(pageIndex, point, mSelectInfo);
                SelectCountRect(pageIndex, mSelectInfo);
                invalidateTouch(mSelectInfo, pageIndex);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                OnSelectRelease(pageIndex, mSelectInfo, null);
                return true;
            default:
                break;
        }
        return true;

    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (mCurrentIndex != pageIndex) return;
        Rect clipRect = canvas.getClipBounds();

        int i = 0;
        PointF startPointF = new PointF();
        PointF endPointF = new PointF();
        RectF widthRect = new RectF();

        for (RectF rect : mSelectInfo.mRectArray) {
            Rect r = new Rect();
            rect.round(r);
            if (r.intersect(clipRect)) {

                RectF tmpF = new RectF();
                tmpF.set(rect);

                if (i < mSelectInfo.mRectVert.size()) {
                    boolean vert = mSelectInfo.mRectVert.get(i);
                    mPdfViewCtrl.convertPageViewRectToPdfRect(rect, widthRect, pageIndex);

                    //reset Paint width
                    if ((widthRect.top - widthRect.bottom) > (widthRect.right - widthRect.left)) {
                        TextMarkupUtil.resetDrawLineWidth(mPdfViewCtrl, pageIndex, mPaint, widthRect.right, widthRect.left);
                    } else {
                        TextMarkupUtil.resetDrawLineWidth(mPdfViewCtrl, pageIndex, mPaint, widthRect.top, widthRect.bottom);
                    }

                    if (vert) {
                        if (mSelectInfo.mRotation.get(i) == 3) {
                            startPointF.x = tmpF.right - (tmpF.right - tmpF.left) / 8f;
                        } else {
                            startPointF.x = tmpF.left + (tmpF.right - tmpF.left) / 8f;
                        }

                        startPointF.y = tmpF.top;
                        endPointF.x = startPointF.x;
                        endPointF.y = tmpF.bottom;
                    } else {
                        startPointF.x = tmpF.left;
                        startPointF.y = tmpF.bottom + (tmpF.bottom - tmpF.top) / 8f;
                        endPointF.x = tmpF.right;
                        endPointF.y = startPointF.y;
                    }

                    canvas.save();
                    drawSquiggly(canvas, startPointF.x, startPointF.y, endPointF.x, endPointF.y, mPaint);
                    canvas.restore();
                }
            }
            i++;
        }
    }


    public void setPaint(int color, int opacity) {
        mColor = color;
        mOpacity = opacity;
        mPaint.setColor(mColor);
        mPaint.setAlpha(mOpacity);
    }

    private int[] mPBColors = new int[PropertyBar.PB_COLORS_SQUIGGLY.length];

    public int getPBCustomColor() {
        return PropertyBar.PB_COLORS_SQUIGGLY[0];
    }

    private void resetPropertyBar() {
        long supportProperty = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY;
        System.arraycopy(PropertyBar.PB_COLORS_SQUIGGLY, 0, mPBColors, 0, mPBColors.length);
        mPBColors[0] = getPBCustomColor();
        mPropertyBar.setColors(mPBColors);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, mColor);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100(mOpacity));
        mPropertyBar.reset(supportProperty);
        mPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
    }

    public int getColor() {
        return mColor;
    }

    public int getOpacity() {
        return mOpacity;
    }

    private void drawSquiggly(Canvas canvas, float x, float y, float x2,
                              float y2, Paint paint) {

        PointF step = new PointF((x2 - x) / 8, (y2 - y) / 8);
        TextMarkupUtil.stepNormalize(step, mPaint.getStrokeWidth());
        PointF step1 = new PointF();
        PointF step2 = new PointF();
        step1.set(step);
        step2.set(step);

        TextMarkupUtil.stepRotate(-TextMarkupUtil.SQGPI / 2.0, step1);
        TextMarkupUtil.stepRotate(TextMarkupUtil.SQGPI / 2.0, step2);

        mPath.moveTo(x + step1.x, y + step1.y);
        int i = 1;
        float fX = x, fY = y;
        float fMinX = (x < x2) ? x : x2;
        float fMaxX = (x > x2) ? x : x2;
        float fMinY = (y < y2) ? y : y2;
        float fMaxY = (y > y2) ? y : y2;

        if (x == x2) {
            //Vertical
            fX += step.x * 2.0f;
            while ((fX >= fMinX && fX <= fMaxX) && (fY >= fMinY && fY <= fMaxY)) {
                fY += step.y * 2.0f;
                mPath.lineTo(fX + ((i % 2) == 0 ? step1.x : step2.x), fY + ((i % 2) == 0 ? step1.y : step2.y));
                i++;
            }
        } else {
            //horizontal
            fY += step.y * 2.0f;
            while ((fX >= fMinX && fX <= fMaxX) && (fY >= fMinY && fY <= fMaxY)) {
                fX += step.x * 2.0f;
                mPath.lineTo(fX + ((i % 2) == 0 ? step1.x : step2.x), fY + ((i % 2) == 0 ? step1.y : step2.y));
                i++;
            }
        }

        canvas.drawPath(mPath, paint);
        mPath.rewind();
    }

    public void removeProbarListener() {
        mPropertyChangeListener = null;
    }

    public void onToolHandlerChanged(ToolHandler lastTool, ToolHandler currentTool) {
        if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            mAnnotButton.setEnable(false);
        } else {
            mAnnotButton.setEnable(true);
        }
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() == this) {
            mAnnotButton.setSelected(true);
            resetPropertyBar();
        } else {
            mAnnotButton.setSelected(false);
        }
    }

    public boolean onPrepareOptionsMenu() {
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() == this) {
            return false;
        }
        return true;
    }

    public void onStateChanged() {
        if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            mAnnotButton.setEnable(true);
        } else {
            mAnnotButton.setEnable(false);
        }
    }
}
