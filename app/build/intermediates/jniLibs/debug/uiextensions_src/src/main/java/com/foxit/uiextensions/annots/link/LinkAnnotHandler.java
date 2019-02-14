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
package com.foxit.uiextensions.annots.link;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.CommonDefines;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.action.Action;
import com.foxit.sdk.pdf.action.Destination;
import com.foxit.sdk.pdf.action.GotoAction;
import com.foxit.sdk.pdf.action.URIAction;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Link;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.OnPageEventListener;

import java.util.ArrayList;

class LinkAnnotHandler implements AnnotHandler {
    protected Context mContext;
    protected boolean isDocClosed = false;
    private Paint mPaint;
    private final int mType;
    private PDFViewCtrl mPdfViewCtrl;

    class LinkInfo {
        int pageIndex;
        ArrayList<Link> links;
    }

    protected LinkInfo mLinkInfo;

    LinkAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mPaint = new Paint();
//        mPaint.setARGB(0x16, 0x0, 0x7F, 0xFF);
        mType = Annot.e_annotLink;
        mLinkInfo = new LinkInfo();
        mLinkInfo.pageIndex = -1;
        mLinkInfo.links = new ArrayList<>();
    }

    private boolean isLoadLink(int pageIndex) {
        return mLinkInfo.pageIndex == pageIndex;
    }

    protected PDFViewCtrl.IPageEventListener getPageEventListener(){
        return mPageEventListener;
    }

    private OnPageEventListener mPageEventListener = new OnPageEventListener() {
        @Override
        public void onPageMoved(boolean success, int index, int dstIndex) {
            if (!success || mLinkInfo.pageIndex == -1 || index == dstIndex)
                return;

            if (mLinkInfo.pageIndex < Math.min(index, dstIndex) || mLinkInfo.pageIndex > Math.max(index, dstIndex))
                return;

            if (mLinkInfo.pageIndex == index) {
                mLinkInfo.pageIndex = dstIndex;
                return;
            }

            if (index > dstIndex) {
                mLinkInfo.pageIndex += 1;
            } else {
                mLinkInfo.pageIndex -= 1;
            }

        }

        @Override
        public void onPagesRemoved(boolean success, int[] pageIndexes) {
            if (!success || mLinkInfo.pageIndex == -1)
                return;

            int count = pageIndexes.length;
            for(int i = 0; i < count; i++) {
                if (mLinkInfo.pageIndex == pageIndexes[i]) {
                    mLinkInfo.pageIndex = -1;
                    break;
                }
            }
            if (mLinkInfo.pageIndex == -1) {
                mLinkInfo.links.clear();
            }
            else
                mLinkInfo.pageIndex -= count;
        }

        @Override
        public void onPagesInserted(boolean success, int dstIndex, int[] range) {
            if (!success || mLinkInfo.pageIndex == -1)
                return;
            if (mLinkInfo.pageIndex > dstIndex) {
                for (int i = 0; i < range.length / 2; i++) {
                    mLinkInfo.pageIndex += range[2 * i + 1];
                }
            }
        }
    };

    private void loadLinks(int pageIndex) {
        try {
            if (mPdfViewCtrl.getDoc() == null) return;
            clear();
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            int count = page.getAnnotCount();
            mLinkInfo.pageIndex = pageIndex;
            Annot annot = null;
            for (int i = 0; i < count; i++) {
                annot = page.getAnnot(i);
                if (annot == null) continue;
                if (annot.getType() == Annot.e_annotLink) {
                    mLinkInfo.links.add((Link) annot);
                }
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getType() {
        return mType;
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        try {
            if (annot.getType() == mType) {
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public RectF getAnnotBBox(Annot annot) {
        try {
            return annot.getRect();
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        return getAnnotBBox(annot).contains(point.x, point.y);
    }

    @Override
    public void onAnnotSelected(Annot annot, boolean reRender) {

    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, Event.Callback result) {

    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {

    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return false;
    }

    private PointF getDestinationPoint(Destination destination) {
        if (destination == null) {
            return null;
        }

        PointF pt = new PointF(0, 0);
        try {
            switch (destination.getZoomMode()) {
                case CommonDefines.e_zoomXYZ:
                    pt.x = destination.getLeft();
                    pt.y = destination.getTop();
                    break;
                case CommonDefines.e_zoomFitHorz:
                case CommonDefines.e_zoomFitBHorz:
                    pt.y = destination.getTop();
                    break;
                case CommonDefines.e_zoomFitVert:
                case CommonDefines.e_zoomFitBVert:
                    pt.x = destination.getLeft();
                    break;
                case CommonDefines.e_zoomFitRect:
                    pt.x = destination.getLeft();
                    pt.y = destination.getBottom();
                    break;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return pt;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (isDocClosed) return;
        if (!((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).isLinkHighlightEnabled())return;

        if (!isLoadLink(pageIndex)) {
            loadLinks(pageIndex);
        }

        if (mLinkInfo.links.size() == 0) return;

        canvas.save();
        Rect clipRect = canvas.getClipBounds();
        Rect rect = new Rect();
        try {
            int count = mLinkInfo.links.size();
            mPaint.setColor((int) ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getLinkHighlightColor());

            for (int i = 0; i < count; i++) {
                Annot annot = mLinkInfo.links.get(i);
                RectF rectF = annot.getRect();
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                rectF.round(rect);
                if (rect.intersect(clipRect)) {
                    canvas.drawRect(rect, mPaint);
                }
            }
            canvas.restore();
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).isLinksEnabled())
            return false;

        if (pageIndex != mPdfViewCtrl.getCurrentPage()) return false;

        try {
            Action annotAction = ((Link) annot).getAction();
            if (annotAction == null) {
                return false;
            }
            int type = annotAction.getType();
            switch (type) {
                case Action.e_actionTypeGoto:
                    GotoAction gotoAction = (GotoAction) annotAction;
                    Destination destination = gotoAction.getDestination();
                    if (destination == null) return false;
                    PointF destPt = getDestinationPoint(destination);
                    PointF devicePt = new PointF();
                    if (!mPdfViewCtrl.convertPdfPtToPageViewPt(destPt, devicePt, destination.getPageIndex())) {
                        devicePt.set(0, 0);
                    }
                    mPdfViewCtrl.gotoPage(destination.getPageIndex(), devicePt.x, devicePt.y);
                    break;
                case Action.e_actionTypeURI:
                    if (mPdfViewCtrl.getUIExtensionsManager() == null) return false;
                    Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                    if (context == null) return false;
                    URIAction uriAction = (URIAction) annotAction;
                    String uri = uriAction.getURI();
                    if (uri.toLowerCase().startsWith("mailto:")) {
                        AppUtil.mailTo((Activity) context, uri);
                    } else {
                        AppUtil.openUrl((Activity) context, uri);
                    }
                    break;
                case Action.e_actionTypeUnknown:
                    return false;
            }

            mPdfViewCtrl.getDoc().closePage(pageIndex);
        } catch (PDFException e1) {
            if (e1.getLastError() == PDFError.OOM.getCode()) {
                mPdfViewCtrl.recoverForOOM();
            }
            e1.printStackTrace();
            return true;
        }

        return true;
    }

    protected void clear() {
        synchronized (mLinkInfo) {
            try {
                if(mLinkInfo.pageIndex != -1)
                    mPdfViewCtrl.getDoc().closePage(mLinkInfo.pageIndex);
            } catch (PDFException e) {
                e.printStackTrace();
            }
            mLinkInfo.pageIndex = -1;
            mLinkInfo.links.clear();
        }
    }
}
