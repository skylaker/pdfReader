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
package com.foxit.uiextensions.security.digitalsignature;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.signature.Signature;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

class DigitalSignatureAnnotHandler implements AnnotHandler {

    private Context mContext;
    private ViewGroup mParent;
    private PDFViewCtrl mPdfViewCtrl;
    private DigitalSignatureSecurityHandler mSignatureHandler;
    private Paint mPaintBbox;

    private AnnotMenu mAnnotMenu;
    private ArrayList<Integer> mMenuItems;
    private int mBBoxSpace = 0;
    private final int mBBoxColor = 0xFF4e4d4d;

    public DigitalSignatureAnnotHandler(Context dmContext, ViewGroup parent, PDFViewCtrl pdfViewCtrl, DigitalSignatureSecurityHandler securityHandler) {
        mContext = dmContext;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mSignatureHandler = securityHandler;
        init();
    }

    private void init() {
        mPaintBbox = new Paint();
        mPaintBbox.setAntiAlias(true);
        mPaintBbox.setStyle(Paint.Style.STROKE);
        mPaintBbox.setStrokeWidth(AppAnnotUtil.getInstance(mContext).getAnnotBBoxStrokeWidth());
        mPaintBbox.setPathEffect(AppAnnotUtil.getInstance(mContext).getBBoxPathEffect2());
        mPaintBbox.setColor(mBBoxColor);
        mAnnotMenu = new AnnotMenuImpl(mContext, mPdfViewCtrl);
        mMenuItems = new ArrayList<Integer>();
        mMenuItems.add(0, R.string.rv_security_dsg_verify);
        mMenuItems.add(1, R.string.fx_string_cancel);
    }


    @Override
    public int getType() {
        return Annot.e_annotWidget + 101; //value 101 please refer to getAnnotHandlerType function of DocumentManager class
    }


    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
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
        RectF rectF = getAnnotBBox(annot);
        if (mPdfViewCtrl != null) {
            try {
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, annot.getPage().getIndex());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return rectF.contains(point.x, point.y);
    }

    private Annot mLastAnnot;

    @Override
    public void onAnnotSelected(final Annot annot, final boolean needInvalid) {
        if (annot == null || !(annot instanceof Signature))
            return;
        final int pageIndex;
        try {
            pageIndex = annot.getPage().getIndex();

            RectF annotRect = annot.getRect();

            mMenuItems.clear();
            mMenuItems.add(AnnotMenu.AM_BT_VERIFY_SIGNATURE);
            mMenuItems.add(AnnotMenu.AM_BT_CANCEL);
            mAnnotMenu.setMenuItems(mMenuItems);

            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {

                @Override
                public void onAMClick(int btType) {

                    if (btType == AnnotMenu.AM_BT_VERIFY_SIGNATURE) {
                        try {
                            mSignatureHandler.verifySignature(annot);
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                        DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                    } else if (btType == AnnotMenu.AM_BT_CANCEL) {
                        DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                    }
                }
            });

            mLastAnnot = annot;
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                RectF displayViewRect = new RectF();
                RectF pageViewRect = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, pageViewRect, pageIndex);

                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(pageViewRect, displayViewRect, pageIndex);
                Rect rect = rectRoundOut(pageViewRect, 0);
                mPdfViewCtrl.refresh(pageIndex, rect);
                mAnnotMenu.show(displayViewRect);
            }


        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean needInvalid) {
        if (annot == null || !(annot instanceof Signature))
            return;
        mAnnotMenu.dismiss();
        mMenuItems.clear();
        mLastAnnot = null;
        int pageIndex = 0;
        try {
            pageIndex = annot.getPage().getIndex();

            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                RectF rectF = annot.getRect();

                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                Rect rect = rectRoundOut(rectF, 10);
                mPdfViewCtrl.refresh(pageIndex, rect);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, Event.Callback result) {

    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {

    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {

    }


    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        PointF pageViewPt = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
            try {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt)) {

                } else {
                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        } else {
            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(annot);
        }
        return true;
    }


    private Rect mTmpRect = new Rect();

    private Rect rectRoundOut(RectF rectF, int roundSize) {
        rectF.roundOut(mTmpRect);
        mTmpRect.inset(-roundSize, -roundSize);
        return mTmpRect;
    }

    private RectF mTmpRectF = new RectF();

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null)
            return;
        try {
            if (mLastAnnot == annot && annot.getPage().getIndex() == pageIndex) {
                RectF rect = annot.getRect();
                mTmpRectF.set(rect.left, rect.top, rect.right, rect.bottom);
                mPdfViewCtrl.convertPdfRectToPageViewRect(mTmpRectF, mTmpRectF, pageIndex);
                Rect rectBBox = rectRoundOut(mTmpRectF, mBBoxSpace);
                canvas.save();
                canvas.drawRect(rectBBox, mPaintBbox);
                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onDrawForControls(Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        try {
            if (annot != null && (annot.getType() == Annot.e_annotWidget)) {
                int pageIndex = annot.getPage().getIndex();
                RectF rect = annot.getRect();
                mTmpRectF.set(rect.left, rect.top, rect.right, rect.bottom);
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mTmpRectF, mTmpRectF, pageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mTmpRectF, mTmpRectF, pageIndex);
                    mAnnotMenu.update(mTmpRectF);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }


}
