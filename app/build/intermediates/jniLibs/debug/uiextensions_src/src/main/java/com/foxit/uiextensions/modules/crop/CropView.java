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
package com.foxit.uiextensions.modules.crop;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.CommonDefines;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.Renderer;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.propertybar.MultiLineBar;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;

public class CropView {
    private Context mContext = null;
    private ViewGroup mParent = null;
    private PDFViewCtrl mPdfViewCtrl = null;

    private View mCropView = null;
    private LinearLayout mCrop_ll_top;
    private Button mBtnNoCrop;
    private Button mBtnCrop;

    private LinearLayout mCrop_ll_center;

    private LinearLayout mCrop_ll_bottom;
    private Button mBtnSmartCrop;
    private Button mBtnDetect;

    private MultiLineBar mSettingBar;
    private boolean mEnableDetect = false;

    public CropView(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;

        mCropView = LayoutInflater.from(context).inflate(R.layout.crop_layout, null, false);
        mCropView.setVisibility(View.GONE);
        mParent.addView(mCropView);

        initView();
        bindEvent();
    }

    private void initView() {
        mCrop_ll_top = (LinearLayout) mCropView.findViewById(R.id.rd_crop_ll_top);
        mBtnNoCrop = (Button) mCropView.findViewById(R.id.top_bt_nocrop);
        mBtnCrop = (Button) mCropView.findViewById(R.id.top_bt_crop);

        mCrop_ll_center = (LinearLayout) mCropView.findViewById(R.id.rd_crop_ll_center);

        mCrop_ll_bottom = (LinearLayout) mCropView.findViewById(R.id.rd_crop_ll_bottom);
        mBtnSmartCrop = (Button) mCropView.findViewById(R.id.bottom_bt_smartcrop);
        mBtnDetect = (Button) mCropView.findViewById(R.id.bottom_bt_detect);

        RelativeLayout.LayoutParams topParams = (RelativeLayout.LayoutParams) mCrop_ll_top.getLayoutParams();
        RelativeLayout.LayoutParams bottomParams = (RelativeLayout.LayoutParams) mCrop_ll_bottom.getLayoutParams();
        if (AppDisplay.getInstance(mContext).isPad()) {
            topParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
            bottomParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
        } else {
            topParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_phone);
            bottomParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_phone);
        }
        mCrop_ll_top.setLayoutParams(topParams);
        mCrop_ll_bottom.setLayoutParams(bottomParams);
    }

    private void bindEvent() {
        mBtnNoCrop.setOnClickListener(cropListener);
        mBtnCrop.setOnClickListener(cropListener);
        mBtnSmartCrop.setOnClickListener(cropListener);
        mBtnDetect.setOnClickListener(cropListener);
    }

    private View.OnClickListener cropListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.top_bt_nocrop) {
                removeCropRect();
            } else if (v.getId() == R.id.top_bt_crop) {
                manualCropRect();
            } else if (v.getId() == R.id.bottom_bt_smartcrop) {
                smartCrop();
            } else if (v.getId() == R.id.bottom_bt_detect) {
                resetCropRect();
                enableBtnDetect(false);
            }
        }
    };

    private void removeCropRect() {
        mPdfViewCtrl.setCropMode(PDFViewCtrl.CROPMODE_NONE);
        changeState(false);
        closeCropView();
    }

    private void manualCropRect() {
        RectF realRect = mOverlayView.getRealContentRect(OverlayView.COORDINATE_PDF);
        mPdfViewCtrl.setCropRect(-1, realRect);
        mPdfViewCtrl.setCropMode(PDFViewCtrl.CROPMODE_CUSTOMIZE);
        changeState(true);
        closeCropView();
    }

    private void smartCrop() {
        mPdfViewCtrl.setCropMode(PDFViewCtrl.CROPMODE_CONTENTSBOX);
        changeState(true);
        closeCropView();
    }

    private void resetCropRect() {
        RectF DefaultRect = mOverlayView.getDefaultContentRect();
        RectF realRect = mOverlayView.getRealContentRect(OverlayView.COORDINATE_DEVICE);
        mOverlayView.setRealContentRect(DefaultRect);
        realRect.union(DefaultRect);
        mOverlayView.invalidate(AppDmUtil.rectFToRect(realRect));
    }

    private void enableBtnDetect(boolean enable){
        mEnableDetect = enable;
        mBtnDetect.setEnabled(enable);
    }

    protected void show() {
        if (mCropView != null) {
            mCropView.setVisibility(View.VISIBLE);
        }
    }

    protected void dismiss() {
        if (mCropView != null) {
            mCropView.setVisibility(View.GONE);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mCropView.getVisibility() == View.VISIBLE) {
                closeCropView();
                return true;
            }
        }
        return false;
    }

    private OverlayView mOverlayView;
    protected void openCropView() {
        mOverlayView = new OverlayView(mContext, mPdfViewCtrl);
        mCrop_ll_center.removeAllViews();
        mCrop_ll_center.addView(mOverlayView);
        mCropView.setVisibility(View.VISIBLE);
        enableBtnDetect(false);

        Activity activity = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    protected void closeCropView() {
        mOverlayView.releaseResources();
        mOverlayView = null;
        dismiss();

        if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() != null) {
            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
        }

        Activity activity = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    protected void setSettingBar(MultiLineBar settingBar) {
        mSettingBar = settingBar;
    }

    private void changeState(boolean state) {
        if (mSettingBar != null) {
            mSettingBar.setProperty(MultiLineBar.TYPE_CROP, state);
        }
    }


    class OverlayView extends View implements Runnable{
        private final String TAG = OverlayView.class.getName();

        private PDFViewCtrl mCropViewCtrl;
        private Bitmap mBitmap = null;

        public static final int CTR_NONE = -1;
        public static final int CTR_LT = 1;
        public static final int CTR_T = 2;
        public static final int CTR_RT = 3;
        public static final int CTR_R = 4;
        public static final int CTR_RB = 5;
        public static final int CTR_B = 6;
        public static final int CTR_LB = 7;
        public static final int CTR_L = 8;
        private int mCurrentCtr = CTR_NONE;

        public static final int OPER_DEFAULT = -1;
        public static final int OPER_SCALE_LT = 1;// old:start at 0
        public static final int OPER_SCALE_T = 2;
        public static final int OPER_SCALE_RT = 3;
        public static final int OPER_SCALE_R = 4;
        public static final int OPER_SCALE_RB = 5;
        public static final int OPER_SCALE_B = 6;
        public static final int OPER_SCALE_LB = 7;
        public static final int OPER_SCALE_L = 8;
        public static final int OPER_TRANSLATE = 9;
        private int mLastOper = OPER_DEFAULT;

        private float mCtlPtLineWidth = 4;//2;
        private float mCtlPtRadius = 10;//5;
        private float mCtlPtTouchExt = 20;
        private float mCtlPtDeltyXY = 20;// Additional refresh range

        private Paint mBitmapPaint;
        private Paint mPaint;// outline
        private Paint mCtlPtPaint;
        private Paint mMaskPaint;

        private boolean mTouchCaptured = false;
        private PointF mDownPoint;
        private PointF mLastPoint;

        private RectF mBBoxInOnDraw = new RectF();
        private RectF mViewDrawRectInOnDraw = new RectF();
        private RectF mPageViewRect = new RectF(0, 0, 0, 0);
        private RectF mInvalidateRect = new RectF(0, 0, 0, 0);
        private RectF mAdjustRect = new RectF(0, 0, 0, 0);
        private RectF mPageDrawRect = new RectF();

        private RectF mPdfCropRect = new RectF();
        private RectF mDefaultRect = new RectF();
        private RectF mRealContentRect = new RectF(0, 0, 0, 0);

        private Matrix page2device = null;
        public OverlayView(Context context, PDFViewCtrl pdfViewCtrl) {
            super(context);
            mCropViewCtrl = pdfViewCtrl;
            initialize();

            if (Build.VERSION.SDK_INT < 24) {
                post(this);
            }
        }

        private void initialize() {
            mBitmapPaint = new Paint();
            mBitmapPaint.setAntiAlias(true);
            mBitmapPaint.setFilterBitmap(true);

            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setAntiAlias(true);
            mPaint.setDither(true);
            mPaint.setColor(Color.parseColor("#179CD8"));
            PathEffect effect = AppAnnotUtil.getAnnotBBoxPathEffect();
            mPaint.setPathEffect(effect);


            mCtlPtPaint = new Paint();

            mMaskPaint = new Paint();
            mMaskPaint.setStyle(Paint.Style.FILL);
            mMaskPaint.setColor(Color.argb(51, 0, 0, 0));

            mDownPoint = new PointF();
            mLastPoint = new PointF();

            setWillNotDraw(false);
            setBackgroundColor(Color.argb(0xff, 0xe1, 0xe1, 0xe1));
            setDrawingCacheEnabled(true);
        }

        public boolean isHit(RectF rectF, PointF point) {
            return rectF.contains(point.x, point.y);
        }

        private RectF getPageContentRect(PDFPage pdfPage) {
            RectF rect = new RectF();
            try {
                rect.set(0, 0, pdfPage.getWidth(), pdfPage.getHeight());
                if (!pdfPage.isParsed()) {
                    int progress = pdfPage.startParse(PDFPage.e_parsePageNormal, null, false);
                    while (progress == CommonDefines.e_progressToBeContinued) {
                        progress = pdfPage.continueParse();
                    }
                    if (progress != CommonDefines.e_progressFinished) {
                        return rect;
                    }
                }

                RectF contentBbox = pdfPage.calcContentBBox(PDFPage.e_calcContentsBox);
                if (contentBbox.left != 0 || contentBbox.right != 0 || contentBbox.top != 0 || contentBbox.bottom != 0) {
                    RectF newRect = new RectF(contentBbox);
                    rect.set(newRect);
                }

            } catch (PDFException e) {
                e.printStackTrace();
            }
            return rect;
        }

        RectF mMapBounds = new RectF();
        private PointF[] calculateControlPoints(RectF rect) {
            rect.sort();
            mMapBounds.set(rect);
            mMapBounds.inset(-mCtlPtRadius - mCtlPtLineWidth / 2f, -mCtlPtRadius - mCtlPtLineWidth / 2f);// control rect

            mMapBounds.left = mMapBounds.left < 0 ? 0 : mMapBounds.left;
            mMapBounds.top = mMapBounds.top < 0 ? 0 : mMapBounds.top;
            mMapBounds.right = mMapBounds.right > getWidth() ? getWidth() : mMapBounds.right;
            mMapBounds.bottom = mMapBounds.bottom > getHeight() ? getHeight() : mMapBounds.bottom;

            PointF p1 = new PointF(mMapBounds.left, mMapBounds.top);
            PointF p2 = new PointF((mMapBounds.right + mMapBounds.left) / 2, mMapBounds.top);
            PointF p3 = new PointF(mMapBounds.right, mMapBounds.top);
            PointF p4 = new PointF(mMapBounds.right, (mMapBounds.bottom + mMapBounds.top) / 2);
            PointF p5 = new PointF(mMapBounds.right, mMapBounds.bottom);
            PointF p6 = new PointF((mMapBounds.right + mMapBounds.left) / 2, mMapBounds.bottom);
            PointF p7 = new PointF(mMapBounds.left, mMapBounds.bottom);
            PointF p8 = new PointF(mMapBounds.left, (mMapBounds.bottom + mMapBounds.top) / 2);

            return new PointF[]{p1, p2, p3, p4, p5, p6, p7, p8};
        }

        private int isTouchControlPoint(RectF rect, float x, float y) {
            PointF[] ctlPts = calculateControlPoints(rect);
            RectF area = new RectF();
            int ret = -1;
            for (int i = 0; i < ctlPts.length; i++) {
                area.set(ctlPts[i].x, ctlPts[i].y, ctlPts[i].x, ctlPts[i].y);
                area.inset(-mCtlPtTouchExt, -mCtlPtTouchExt);
                if (area.contains(x, y)) {
                    ret = i + 1;
                }
            }
            return ret;
        }

        Path mImaginaryPath = new Path();
        private void drawControlImaginary(Canvas canvas, RectF rectBBox, int color) {
            PointF[] ctlPts = calculateControlPoints(rectBBox);
            mPaint.setStrokeWidth(mCtlPtLineWidth);
            mPaint.setColor(Color.parseColor("#179CD8"));
            mImaginaryPath.reset();
            // set path
            pathAddLine(mImaginaryPath, ctlPts[0].x + mCtlPtRadius, ctlPts[0].y, ctlPts[1].x - mCtlPtRadius, ctlPts[1].y);
            pathAddLine(mImaginaryPath, ctlPts[1].x + mCtlPtRadius, ctlPts[1].y, ctlPts[2].x - mCtlPtRadius, ctlPts[2].y);
            pathAddLine(mImaginaryPath, ctlPts[2].x, ctlPts[2].y + mCtlPtRadius, ctlPts[3].x, ctlPts[3].y - mCtlPtRadius);
            pathAddLine(mImaginaryPath, ctlPts[3].x, ctlPts[3].y + mCtlPtRadius, ctlPts[4].x, ctlPts[4].y - mCtlPtRadius);
            pathAddLine(mImaginaryPath, ctlPts[4].x - mCtlPtRadius, ctlPts[4].y, ctlPts[5].x + mCtlPtRadius, ctlPts[5].y);
            pathAddLine(mImaginaryPath, ctlPts[5].x - mCtlPtRadius, ctlPts[5].y, ctlPts[6].x + mCtlPtRadius, ctlPts[6].y);
            pathAddLine(mImaginaryPath, ctlPts[6].x, ctlPts[6].y - mCtlPtRadius, ctlPts[7].x, ctlPts[7].y + mCtlPtRadius);
            pathAddLine(mImaginaryPath, ctlPts[7].x, ctlPts[7].y - mCtlPtRadius, ctlPts[0].x, ctlPts[0].y + mCtlPtRadius);

            canvas.drawPath(mImaginaryPath, mPaint);
        }

        private void pathAddLine(Path path, float start_x, float start_y, float end_x, float end_y) {
            path.moveTo(start_x, start_y);
            path.lineTo(end_x, end_y);

        }

        private void drawControlPoints(Canvas canvas, RectF rectBBox, int color) {
            PointF[] ctlPts = calculateControlPoints(rectBBox);
            mCtlPtPaint.setStrokeWidth(mCtlPtLineWidth);
            for (PointF ctlPt : ctlPts) {
                mCtlPtPaint.setColor(Color.WHITE);
                mCtlPtPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
                mCtlPtPaint.setColor(Color.parseColor("#179CD8"));
                mCtlPtPaint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
            }
        }

        private void drawMask(Canvas canvas, RectF cropRect) {
            canvas.drawRect(0, 0, getWidth(), cropRect.top, mMaskPaint); // top
            canvas.drawRect(0, cropRect.bottom, getWidth(), getHeight(), mMaskPaint); //bottom
            canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, mMaskPaint); //left
            canvas.drawRect(cropRect.right, cropRect.top, getWidth(), cropRect.bottom, mMaskPaint); //right
        }

        private PointF mAdjustPointF = new PointF(0, 0);

        private PointF adjustScalePointF(RectF rectF, float dxy) {
            float adjustx = 0;
            float adjusty = 0;
            if (mLastOper != OPER_TRANSLATE) {
                rectF.inset(-mCtlPtLineWidth / 2f, -mCtlPtLineWidth / 2f);
            }

            if ((int) rectF.left < dxy) {
                adjustx = -rectF.left + dxy;
                rectF.left = dxy;
            }
            if ((int) rectF.top < dxy) {
                adjusty = -rectF.top + dxy;
                rectF.top = dxy;
            }

            if ((int) rectF.right > getWidth() - dxy) {
                adjustx = getWidth() - rectF.right - dxy;
                rectF.right = getWidth() - dxy;
            }
            if ((int) rectF.bottom > getHeight() - dxy) {
                adjusty = getHeight() - rectF.bottom - dxy;
                rectF.bottom = getHeight() - dxy;
            }
            mAdjustPointF.set(adjustx, adjusty);
            return mAdjustPointF;
        }

        //device Rect (display rect)
        public RectF getDefaultContentRect() {
            return mDefaultRect;
        }

        //device Rect (display rect)
        public static final int COORDINATE_DEVICE = 0;
        public static final int COORDINATE_PDF = 1;
        /**
         *
         * @param mode 0: display Rect(device coordinate), 1: pdf Rect (PDF coordinate)
         * @return
         */
        public RectF getRealContentRect(int mode) {
            if (mode == COORDINATE_DEVICE) {
                return mRealContentRect;
            } else if (mode == COORDINATE_PDF) {
                RectF rect = new RectF(mRealContentRect);

                Matrix device2page = new Matrix();
                page2device.invert(device2page);

                device2page.mapRect(rect);
                float tmp = rect.top;
                rect.top = rect.bottom;
                rect.bottom = tmp;

                return rect;
            }
            return null;
        }

        //device Rect (display rect)
        public void setRealContentRect(RectF rect) {
            mRealContentRect.set(rect);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            PointF point = new PointF(event.getX(), event.getY());
            float evX = point.x;
            float evY = point.y;

            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    RectF pageViewBBox =  new RectF(mRealContentRect);
                    RectF pdfRect =  new RectF(mRealContentRect);
                    mPageViewRect.set(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                    mPageViewRect.inset(mCtlPtLineWidth / 2f, mCtlPtLineWidth / 2f);

                    mCurrentCtr = isTouchControlPoint(pageViewBBox, evX, evY);

                    mDownPoint.set(evX, evY);
                    mLastPoint.set(evX, evY);

                    if (mCurrentCtr == CTR_LT) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_LT;
                    } else if (mCurrentCtr == CTR_T) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_T;
                    } else if (mCurrentCtr == CTR_RT) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_RT;
                    } else if (mCurrentCtr == CTR_R) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_R;
                    } else if (mCurrentCtr == CTR_RB) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_RB;
                    } else if (mCurrentCtr == CTR_B) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_B;
                    } else if (mCurrentCtr == CTR_LB) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_LB;
                    } else if (mCurrentCtr == CTR_L) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_L;
                    } else if (isHit(mRealContentRect, point)) {
                        mTouchCaptured = true;
                        mLastOper = OPER_TRANSLATE;
                    }
                }
                return true;
                case MotionEvent.ACTION_MOVE:
                    if (mTouchCaptured) {
                        if (evX != mLastPoint.x && evY != mLastPoint.y) {
                            if (!mEnableDetect){
                                enableBtnDetect(true);
                            }

                            RectF pageViewBBox = new RectF(mRealContentRect);
                            float deltaXY = mCtlPtLineWidth + mCtlPtRadius + 2;// Judging border value
                            switch (mLastOper) {
                                case OPER_TRANSLATE: {
                                    mInvalidateRect.set(pageViewBBox);
                                    mAdjustRect.set(pageViewBBox);
                                    mInvalidateRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    mAdjustRect.offset(evX - mDownPoint.x, evY - mDownPoint.y);
                                    PointF adjustXY = adjustScalePointF(mAdjustRect, deltaXY);

                                    mInvalidateRect.union(mAdjustRect);

                                    mInvalidateRect.inset(-deltaXY - mCtlPtDeltyXY, -deltaXY - mCtlPtDeltyXY);
                                    invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    break;
                                }
                                case OPER_SCALE_LT: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
                                        mAdjustRect.set(evX, evY, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAdjustRect.sort();
                                        mInvalidateRect.union(mAdjustRect);
                                        mInvalidateRect.inset(-mCtlPtLineWidth - mCtlPtDeltyXY, -mCtlPtLineWidth - mCtlPtDeltyXY);
                                        invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(mAdjustRect, deltaXY);

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_T: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
                                        mAdjustRect.set(mPageViewRect.left, evY, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAdjustRect.sort();
                                        mInvalidateRect.union(mAdjustRect);
                                        mInvalidateRect.inset(-mCtlPtLineWidth - mCtlPtDeltyXY, -mCtlPtLineWidth - mCtlPtDeltyXY);
                                        invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(mAdjustRect, deltaXY);

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RT: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {

                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mLastPoint.x, mPageViewRect.bottom);
                                        mAdjustRect.set(mPageViewRect.left, evY, evX, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAdjustRect.sort();
                                        mInvalidateRect.union(mAdjustRect);
                                        mInvalidateRect.inset(-mCtlPtLineWidth - mCtlPtDeltyXY, -mCtlPtLineWidth - mCtlPtDeltyXY);
                                        invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(mAdjustRect, deltaXY);

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_R: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mPageViewRect.bottom);
                                        mAdjustRect.set(mPageViewRect.left, mPageViewRect.top, evX, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAdjustRect.sort();
                                        mInvalidateRect.union(mAdjustRect);
                                        mInvalidateRect.inset(-mCtlPtLineWidth - mCtlPtDeltyXY, -mCtlPtLineWidth - mCtlPtDeltyXY);
                                        invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(mAdjustRect, deltaXY);

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RB: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mLastPoint.y);
                                        mAdjustRect.set(mPageViewRect.left, mPageViewRect.top, evX, evY);
                                        mInvalidateRect.sort();
                                        mAdjustRect.sort();
                                        mInvalidateRect.union(mAdjustRect);
                                        mInvalidateRect.inset(-mCtlPtLineWidth - mCtlPtDeltyXY, -mCtlPtLineWidth - mCtlPtDeltyXY);
                                        invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(mAdjustRect, deltaXY);
                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_B: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
                                        mAdjustRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, evY);
                                        mInvalidateRect.sort();
                                        mAdjustRect.sort();
                                        mInvalidateRect.union(mAdjustRect);mInvalidateRect.inset(-mCtlPtLineWidth - mCtlPtDeltyXY, -mCtlPtLineWidth - mCtlPtDeltyXY);
                                        invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(mAdjustRect, deltaXY);

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_LB: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
                                        mAdjustRect.set(evX, mPageViewRect.top, mPageViewRect.right, evY);
                                        mInvalidateRect.sort();
                                        mAdjustRect.sort();
                                        mInvalidateRect.union(mAdjustRect);
                                        mInvalidateRect.inset(-mCtlPtLineWidth - mCtlPtDeltyXY, -mCtlPtLineWidth - mCtlPtDeltyXY);
                                        invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(mAdjustRect, deltaXY);

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_L: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
                                        mAdjustRect.set(evX, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAdjustRect.sort();
                                        mInvalidateRect.union(mAdjustRect);
                                        mInvalidateRect.inset(-mCtlPtLineWidth - mCtlPtDeltyXY, -mCtlPtLineWidth - mCtlPtDeltyXY);
                                        invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(mAdjustRect, deltaXY);

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;

                                }
                                default:
                                    break;
                            }
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (mTouchCaptured) {
                        RectF pageViewRect = new RectF(mRealContentRect);
                        pageViewRect.inset(mCtlPtLineWidth / 2, mCtlPtLineWidth / 2);

                        switch (mLastOper) {
                            case OPER_TRANSLATE: {
                                mPageDrawRect.set(pageViewRect);
                                mPageDrawRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                break;
                            }
                            case OPER_SCALE_LT: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, mLastPoint.y, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_T: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, mLastPoint.y, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_RT: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, mLastPoint.y, mLastPoint.x, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_R: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_RB: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, mLastPoint.y);
                                }
                                break;
                            }
                            case OPER_SCALE_B: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, pageViewRect.right, mLastPoint.y);
                                }
                                break;
                            }
                            case OPER_SCALE_LB: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, mLastPoint.y);
                                }
                                break;
                            }
                            case OPER_SCALE_L: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            default:
                                break;
                        }
                        mRealContentRect.set(mPageDrawRect);
                        mRealContentRect.sort();
                        mRealContentRect.inset(-mCtlPtLineWidth / 2, -mCtlPtLineWidth / 2);

                        mTouchCaptured = false;
                        mDownPoint.set(0, 0);
                        mLastPoint.set(0, 0);
                        mLastOper = OPER_DEFAULT;
                        mCurrentCtr = CTR_NONE;
                        return true;
                    }

                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    mLastOper = OPER_DEFAULT;
                    mCurrentCtr = CTR_NONE;
                    mTouchCaptured = false;
                    return true;
            }
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (mBitmap == null) {
                if (Build.VERSION.SDK_INT >= 24) {
                    post(this);
                }
                return;
            }
            Rect clipRect = canvas.getClipBounds();
            canvas.drawBitmap(mBitmap, clipRect.left, clipRect.top, mBitmapPaint);

            if (mRealContentRect.equals(new RectF(0, 0, 0, 0))) {
                page2device.mapRect(mRealContentRect, mPdfCropRect);
                mDefaultRect.set(mRealContentRect);
            }
            RectF _rect = new RectF(mRealContentRect);
            mViewDrawRectInOnDraw.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
            mViewDrawRectInOnDraw.inset(mCtlPtLineWidth / 2f, mCtlPtLineWidth / 2f);
            if (mLastOper == OPER_SCALE_LT) {// SCALE
                mBBoxInOnDraw.set(mLastPoint.x, mLastPoint.y, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
            } else if (mLastOper == OPER_SCALE_T) {
                mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mLastPoint.y, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
            } else if (mLastOper == OPER_SCALE_RT) {
                mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mLastPoint.y, mLastPoint.x, mViewDrawRectInOnDraw.bottom);
            } else if (mLastOper == OPER_SCALE_R) {
                mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mLastPoint.x, mViewDrawRectInOnDraw.bottom);
            } else if (mLastOper == OPER_SCALE_RB) {
                mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mLastPoint.x, mLastPoint.y);
            } else if (mLastOper == OPER_SCALE_B) {
                mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mLastPoint.y);
            } else if (mLastOper == OPER_SCALE_LB) {
                mBBoxInOnDraw.set(mLastPoint.x, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mLastPoint.y);
            } else if (mLastOper == OPER_SCALE_L) {
                mBBoxInOnDraw.set(mLastPoint.x, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
            }
            mBBoxInOnDraw.inset(-mCtlPtLineWidth / 2f, -mCtlPtLineWidth / 2f);
            if (mLastOper == OPER_TRANSLATE || mLastOper == OPER_DEFAULT) {// TRANSLATE or DEFAULT
                mBBoxInOnDraw = new RectF(mRealContentRect);
                float dx = mLastPoint.x - mDownPoint.x;
                float dy = mLastPoint.y - mDownPoint.y;

                mBBoxInOnDraw.offset(dx, dy);
            }

            canvas.save();
            RectF maskBboxOndraw = new RectF(mBBoxInOnDraw);
            maskBboxOndraw.sort();
            maskBboxOndraw.inset(-mCtlPtRadius, -mCtlPtRadius);
            drawMask(canvas, maskBboxOndraw);
            canvas.restore();

            canvas.save();
            drawControlPoints(canvas, mBBoxInOnDraw, 0);
            // add Control Imaginary
            drawControlImaginary(canvas, mBBoxInOnDraw, 0);
            canvas.restore();
        }

        void releaseResources() {
            if (mBitmap != null) {
                mBitmap.recycle();
                mBitmap = null;
            }
        }

        @Override
        public void run() {
            PDFDoc pdfDoc = mCropViewCtrl.getDoc();
            try {
                PDFPage pdfPage = pdfDoc.getPage(mCropViewCtrl.getCurrentPage());
                synchronized (pdfPage) {
                    if (!pdfPage.isParsed()) {
                        int progress = pdfPage.startParse(PDFPage.e_parsePageNormal, null, false);
                        while (progress == CommonDefines.e_progressToBeContinued) {
                            progress = pdfPage.continueParse();
                        }
                        if (progress != CommonDefines.e_progressFinished) {
                            return;
                        }
                    }
                    PointF pageSize = new PointF(pdfPage.getWidth(), pdfPage.getHeight());
                    float wScale = getWidth() / pageSize.x;
                    float hScale = getHeight() / pageSize.y;
                    float scale = Math.min(wScale, hScale);

                    int w = (int) (pageSize.x * scale);
                    int h = (int) (pageSize.y * scale);
                    page2device = pdfPage.getDisplayMatrix(0, 0, w, h, 0);
                    mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    mBitmap.eraseColor(Color.WHITE);
                    Renderer renderer = Renderer.create(mBitmap);
                    int progress = renderer.startRender(pdfPage, page2device, null);
                    while (progress == CommonDefines.e_progressToBeContinued) {
                        progress = renderer.continueRender();
                    }

                    renderer.release();

                    mPdfCropRect = getPageContentRect(pdfPage);

                    pdfDoc.closePage(mCropViewCtrl.getCurrentPage());
                    invalidate();
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }
}
