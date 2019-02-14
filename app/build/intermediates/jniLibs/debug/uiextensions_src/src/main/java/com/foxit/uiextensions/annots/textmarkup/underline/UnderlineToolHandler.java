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
package com.foxit.uiextensions.annots.textmarkup.underline;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.CommonDefines;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.PDFTextSelect;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.QuadPoints;
import com.foxit.sdk.pdf.annots.Underline;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupUtil;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public class UnderlineToolHandler implements ToolHandler {

    private Context mContext;
    private Paint mPaint;
    private int mColor;
    private int mCurrentIndex;
    private int mOpacity;
    public SelectInfo mSelectInfo;
    private RectF mTmpRect;
    private RectF mTmpDesRect;

    private PropertyBar mPropertyBar;

    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    private PDFViewCtrl mPdfViewCtrl;

    public UnderlineToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mSelectInfo = new SelectInfo();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        mTmpRect = new RectF();
        mTmpDesRect = new RectF();

        init();
    }

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    private void init() {
        //PropertyBar
        mPropertyBar = new PropertyBarImpl(mContext, mPdfViewCtrl);
    }

    public void unInit() {
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_UNDERLINE;
    }

    @Override
    public void onActivate() {
        resetLineData();
    }

    @Override
    public void onDeactivate() {
    }

    private boolean OnSelectDown(int pageIndex, PointF point, SelectInfo selectInfo) {
        if (selectInfo == null) return false;
        try {
            mCurrentIndex = pageIndex;
            selectInfo.mRectArray.clear();
            selectInfo.mStartChar = selectInfo.mEndChar = -1;
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
                selectInfo.mStartChar = selectInfo.mEndChar = index;
            }

            mPdfViewCtrl.getDoc().closePage(mCurrentIndex);
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

            mPdfViewCtrl.getDoc().closePage(mCurrentIndex);
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
        rectF.set(mSelectInfo.mBBox);
        rectF.bottom += 2;
        rectF.left -= 2;
        rectF.right += 2;
        rectF.top -= 2;
        RectF pageRt = new RectF();
        mPdfViewCtrl.convertPageViewRectToPdfRect(rectF, pageRt, pageIndex);
        addAnnot(pageIndex, true, mSelectInfo.mRectArray, pageRt, selectInfo, result);

        return true;
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
            if (!page.isParsed()) {
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
                mSelectInfo.mRectArray.add(crect);
                mSelectInfo.mRectVert.add(vert);
                mSelectInfo.mRotaton.add(rotate);
                if(i == 0){
                    selectInfo.mBBox = new RectF(crect);
                } else{
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

    public void onToolHandlerChanged (ToolHandler lastTool, ToolHandler currentTool) {

    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        int action = motionEvent.getActionMasked();
        PointF point = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
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
                        if (mSelectInfo.mRotaton.get(i) == 3) {
                            startPointF.x = tmpF.right - (tmpF.right - tmpF.left) / 8f;
                        } else {
                            startPointF.x = tmpF.left + (tmpF.right - tmpF.left) / 8f;
                        }

                        startPointF.y = tmpF.top;
                        endPointF.x = startPointF.x;
                        endPointF.y = tmpF.bottom;
                    } else {
                        startPointF.x = tmpF.left;
                        startPointF.y = tmpF.bottom + (tmpF.top - tmpF.bottom) / 8f;
                        endPointF.x = tmpF.right;
                        endPointF.y = startPointF.y;
                    }

                    canvas.save();
                    canvas.drawLine(startPointF.x, startPointF.y, endPointF.x, endPointF.y, mPaint);
                    canvas.restore();
                }
            }
            i++;
        }
    }

    public class SelectInfo {
        public boolean mIsFromTS;
        public int mStartChar;
        public int mEndChar;
        public RectF mBBox;
        public ArrayList<RectF> mRectArray;
        public ArrayList<Boolean> mRectVert;
        public ArrayList<Integer> mRotaton;

        public SelectInfo() {
            mBBox = new RectF();
            mRectArray = new ArrayList<RectF>();
            mRectVert = new ArrayList<Boolean>();
            mRotaton = new ArrayList<Integer>();
        }

        public void clear() {
            mIsFromTS = false;
            mStartChar = mEndChar = -1;
            mBBox.setEmpty();
            mRectArray.clear();
        }
    }

    private void addAnnot(final int pageIndex, final boolean addUndo, final ArrayList<RectF> rectArray, final RectF rectF, final SelectInfo selectInfo, final Event.Callback result) {
        Underline annot = null;
        PDFPage page = null;
        try {
            page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            annot = (Underline) page.addAnnot(Annot.e_annotUnderline, rectF);
            if (annot == null) {
                if (!misFromSelector) {
                    if (!mIsContinuousCreate) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
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

        final UnderlineAddUndoItem undoItem = new UnderlineAddUndoItem(mPdfViewCtrl);
        undoItem.mType = Annot.e_annotUnderline;
        undoItem.mColor = mColor;
        undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mQuadPoints = new QuadPoints[quadPoint.length];
        System.arraycopy(quadPoint, 0, undoItem.mQuadPoints, 0, quadPoint.length);
        undoItem.mContents = getContent(page, selectInfo);
        undoItem.mNM = AppDmUtil.randomUUID(null);
        undoItem.mSubject = "Underline";
        undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
        undoItem.mFlags = 4;
        undoItem.mOpacity = mOpacity / 255f;
        undoItem.mPageIndex = pageIndex;

        UnderlineEvent event = new UnderlineEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
        final PDFPage finalPage = page;
        final Underline finalAnnot = annot;
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
                        }
                    }
                    misFromSelector = false;
                }
            }
        });
        mPdfViewCtrl.addTask(task);
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

    private void invalidate(int pageIndex, RectF dmrectf, final Event.Callback result) {
        if (dmrectf == null) {
            if (result != null) {
                result.result(null, true);
            }
            return;
        }
        RectF rectF = new RectF();

        mPdfViewCtrl.convertPdfRectToPageViewRect(dmrectf, rectF, pageIndex);
        Rect rect = new Rect();
        rectF.roundOut(rect);
        mPdfViewCtrl.refresh(pageIndex, rect);

        if (null != result) {
            result.result(null, false);
        }
    }

    public void setPaint(int color, int opacity) {
        mColor = color;
        mOpacity = opacity;
        mPaint.setColor(mColor);
        mPaint.setAlpha(mOpacity);
    }

    private int[] mPBColors = new int[PropertyBar.PB_COLORS_UNDERLINE.length];

    public int getPBCustomColor() {
        return PropertyBar.PB_COLORS_UNDERLINE[0];
    }

    public int getColor() {
        return mColor;
    }

    public int getOpacity() {
        return mOpacity;
    }

    private void resetLineData() {
        mSelectInfo.mStartChar = mSelectInfo.mEndChar = -1;
        mSelectInfo.mRectArray.clear();
        mSelectInfo.mBBox.setEmpty();
        mTmpRect.setEmpty();
    }

    public void AddAnnot(final int pageIndex, final boolean addUndo, AnnotContent contentSupplier, ArrayList<RectF> rectFs, final RectF dmRectf,
                         SelectInfo selectInfo, final Event.Callback result) {
        try {
            final Underline annot = (Underline) mPdfViewCtrl.getDoc().getPage(pageIndex).addAnnot(Annot.e_annotUnderline, dmRectf);

            final UnderlineAddUndoItem undoItem = new UnderlineAddUndoItem(mPdfViewCtrl);
            undoItem.mPageIndex = pageIndex;

            int count = annot.getQuadPointsCount();
            undoItem.mQuadPoints = new QuadPoints[count];
            for (int i = 0; i < count; i ++) {
                undoItem.mQuadPoints[i] = annot.getQuadPoints(i);
            }
            undoItem.mColor = contentSupplier.getColor();
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mNM = AppDmUtil.randomUUID(null);
            undoItem.mSubject = "Underline";
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mFlags = 4;

            UnderlineEvent event = new UnderlineEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, annot);
                        if (addUndo) {
                            DocumentManager.getInstance(mPdfViewCtrl).addUndoItem(undoItem);
                        }
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            invalidate(pageIndex, dmRectf, result);
                        } else {
                            if (result != null) {
                                result.result(event, success);
                            }
                        }
                    } else {
                        if (result != null) {
                            result.result(event, success);
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            if (e.getLastError() == PDFError.OOM.getCode()) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() == this) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                return true;
            }
        }
        return false;
    }

    public void onConfigurationChanged(Configuration newConfig) {

    }

    private boolean mIsContinuousCreate = false;

    public boolean getIsContinuousCreate() {
        return mIsContinuousCreate;
    }

    public void setIsContinuousCreate(boolean isContinuousCreate) {
        this.mIsContinuousCreate = isContinuousCreate;
    }

    public void removeProbarListener() {
        mPropertyChangeListener = null;
    }

    private boolean misFromSelector = false;

    public void setFromSelector(boolean b) {
        misFromSelector = b;
    }
}