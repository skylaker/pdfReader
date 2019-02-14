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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Library;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Stamp;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotActionHandler;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.toolbar.PropertyCircleItem;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;


public class StampToolHandler implements ToolHandler {
    private Context mContext;

    private PropertyCircleItem mProItem;

    private boolean mIsContinuousCreate;

    private PropertyBar mPropertyBar;
    private View mStampSelectViewForStandard;
    private View mStampSelectViewForForSignHere;
    private View mStampSelectViewForDynamic;
    private GridView mGridViewForStandard;
    private GridView mGridViewForForSignHere;
    private GridView mGridViewForDynamic;
    private int mStampType = 0;

    private long itemStandard = 0x100000000L;
    private long itemSignHere = 0x200000000L;
    private long itemDynamic = 0x400000000L;

    Integer[] mStampIds = {
            R.drawable._feature_annot_stamp_style0,
            R.drawable._feature_annot_stamp_style1,
            R.drawable._feature_annot_stamp_style2,
            R.drawable._feature_annot_stamp_style3,
            R.drawable._feature_annot_stamp_style4,
            R.drawable._feature_annot_stamp_style5,
            R.drawable._feature_annot_stamp_style6,
            R.drawable._feature_annot_stamp_style7,
            R.drawable._feature_annot_stamp_style8,
            R.drawable._feature_annot_stamp_style9,
            R.drawable._feature_annot_stamp_style10,
            R.drawable._feature_annot_stamp_style11,
            R.drawable._feature_annot_stamp_style12,
            R.drawable._feature_annot_stamp_style13,
            R.drawable._feature_annot_stamp_style14,
            R.drawable._feature_annot_stamp_style15,
            R.drawable._feature_annot_stamp_style16,
            R.drawable._feature_annot_stamp_style17,
            R.drawable._feature_annot_stamp_style18,
            R.drawable._feature_annot_stamp_style19,
            R.drawable._feature_annot_stamp_style20,
            R.drawable._feature_annot_stamp_style21,
    };

    private PDFViewCtrl mPdfViewCtrl;
    protected DynamicStampIconProvider mDsip;

    private AnnotActionHandler mActionHandler;

    public StampToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        mContext = context;

        initAnnotIconProvider();
    }

    protected void initAnnotIconProvider() {
        if (mDsip == null) {
            mDsip = new DynamicStampIconProvider();
        }

        try {
            Library.setAnnotIconProvider(mDsip);

            //IdentityProperties
            mActionHandler = (AnnotActionHandler) DocumentManager.getInstance(mPdfViewCtrl).getActionHandler();
            if (mActionHandler == null) {
                mActionHandler = new AnnotActionHandler(mContext);
            }
            DocumentManager.getInstance(mPdfViewCtrl).setActionHandler(mActionHandler);
        } catch (PDFException e) {

        }

    }

    public void setPropertyBar(PropertyBar propertyBar) {
        mPropertyBar = propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mPropertyBar;
    }

    public void initDisplayItems(final PropertyBar propertyBar, final PropertyCircleItem propertyItem) {

        mStampSelectViewForStandard = View.inflate(mContext, R.layout._future_rd_annot_stamp_gridview, null);
        mStampSelectViewForForSignHere = View.inflate(mContext, R.layout._future_rd_annot_stamp_gridview, null);
        mStampSelectViewForDynamic = View.inflate(mContext, R.layout._future_rd_annot_stamp_gridview, null);
        int t = AppDisplay.getInstance(mContext).dp2px(16);
        mStampSelectViewForStandard.setPadding(0, t, 0, 0);
        mStampSelectViewForForSignHere.setPadding(0, t, 0, 0);
        mStampSelectViewForDynamic.setPadding(0, t, 0, 0);
        int gvHeight;
        if (AppDisplay.getInstance(mContext).isPad()) {
            gvHeight = AppDisplay.getInstance(mContext).dp2px(300);
        } else {
            gvHeight = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        LinearLayout.LayoutParams gridViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, gvHeight);
        mGridViewForStandard = (GridView) mStampSelectViewForStandard.findViewById(R.id.rd_annot_item_stamp_gridview);
        mGridViewForStandard.setLayoutParams(gridViewParams);
        mGridViewForForSignHere = (GridView) mStampSelectViewForForSignHere.findViewById(R.id.rd_annot_item_stamp_gridview);
        mGridViewForForSignHere.setLayoutParams(gridViewParams);
        mGridViewForDynamic = (GridView) mStampSelectViewForDynamic.findViewById(R.id.rd_annot_item_stamp_gridview);
        mGridViewForDynamic.setLayoutParams(gridViewParams);
        final BaseAdapter adapterForStandard = new BaseAdapter() {
            @Override
            public int getCount() {
                return 12;
            }

            @Override
            public Object getItem(int position) {
                return position;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                RelativeLayout relativeLayout = new RelativeLayout(mContext);
                int w = AppDisplay.getInstance(mContext).dp2px(150);
                int h = AppDisplay.getInstance(mContext).dp2px(50);
                relativeLayout.setLayoutParams(new GridView.LayoutParams(w, h));
                relativeLayout.setGravity(Gravity.CENTER);
                IconView iconView;
                iconView = new IconView(mContext);
                RelativeLayout.LayoutParams iconParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                int left = AppDisplay.getInstance(mContext).dp2px(7);
                int top = AppDisplay.getInstance(mContext).dp2px(7);
                int right = AppDisplay.getInstance(mContext).dp2px(7);
                int bottom = AppDisplay.getInstance(mContext).dp2px(7);
                iconParams.setMargins(left, top, right, bottom);
                iconView.setLayoutParams(iconParams);
                iconView.setBackgroundResource(mStampIds[position]);
                if (position == mStampType) {
                    relativeLayout.setBackgroundResource(R.drawable._feature_annot_stamp_selectrect);
                } else {
                    relativeLayout.setBackgroundResource(0);
                }
                relativeLayout.addView(iconView);
                return relativeLayout;
            }
        };
        final BaseAdapter adapterForSignHere = new BaseAdapter() {
            @Override
            public int getCount() {
                return 5;
            }

            @Override
            public Object getItem(int position) {
                return position;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                RelativeLayout relativeLayout = new RelativeLayout(mContext);
                int w = AppDisplay.getInstance(mContext).dp2px(150);
                int h = AppDisplay.getInstance(mContext).dp2px(50);
                relativeLayout.setLayoutParams(new GridView.LayoutParams(w, h));
                relativeLayout.setGravity(Gravity.CENTER);
                IconView iconView;
                iconView = new IconView(mContext);
                RelativeLayout.LayoutParams iconParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                int left = AppDisplay.getInstance(mContext).dp2px(7);
                int top = AppDisplay.getInstance(mContext).dp2px(7);
                int right = AppDisplay.getInstance(mContext).dp2px(7);
                int bottom = AppDisplay.getInstance(mContext).dp2px(7);
                iconParams.setMargins(left, top, right, bottom);
                iconView.setLayoutParams(iconParams);
                iconView.setBackgroundResource(mStampIds[position + 12]);
                if (position + 12 == mStampType) {
                    relativeLayout.setBackgroundResource(R.drawable._feature_annot_stamp_selectrect);
                } else {
                    relativeLayout.setBackgroundResource(0);
                }
                relativeLayout.addView(iconView);
                return relativeLayout;
            }
        };
        final BaseAdapter adapterForDynamic = new BaseAdapter() {
            @Override
            public int getCount() {
                return 5;
            }

            @Override
            public Object getItem(int position) {
                return position;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                RelativeLayout relativeLayout = new RelativeLayout(mContext);
                int w = AppDisplay.getInstance(mContext).dp2px(150);
                int h = AppDisplay.getInstance(mContext).dp2px(50);
                relativeLayout.setLayoutParams(new GridView.LayoutParams(w, h));
                relativeLayout.setGravity(Gravity.CENTER);
                IconView iconView;
                iconView = new IconView(mContext);
                RelativeLayout.LayoutParams iconParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                int left = AppDisplay.getInstance(mContext).dp2px(7);
                int top = AppDisplay.getInstance(mContext).dp2px(7);
                int right = AppDisplay.getInstance(mContext).dp2px(7);
                int bottom = AppDisplay.getInstance(mContext).dp2px(7);
                iconParams.setMargins(left, top, right, bottom);
                iconView.setLayoutParams(iconParams);
                iconView.setBackgroundResource(mStampIds[position + 17]);
                if (position + 17 == mStampType) {
                    relativeLayout.setBackgroundResource(R.drawable._feature_annot_stamp_selectrect);
                } else {
                    relativeLayout.setBackgroundResource(0);
                }
                relativeLayout.addView(iconView);
                return relativeLayout;
            }
        };
        mGridViewForStandard.setAdapter(adapterForStandard);
        mGridViewForStandard.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mStampType = position;
                adapterForStandard.notifyDataSetChanged();
                adapterForSignHere.notifyDataSetChanged();
                adapterForDynamic.notifyDataSetChanged();
                if (propertyBar != null) {
                    propertyBar.dismiss();
                }
            }
        });
        mGridViewForForSignHere.setAdapter(adapterForSignHere);
        mGridViewForForSignHere.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mStampType = position + 12;
                adapterForStandard.notifyDataSetChanged();
                adapterForSignHere.notifyDataSetChanged();
                adapterForDynamic.notifyDataSetChanged();
                if (propertyBar != null) {
                    propertyBar.dismiss();
                }
            }
        });
        mGridViewForDynamic.setAdapter(adapterForDynamic);
        mGridViewForDynamic.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mStampType = position + 17;
                adapterForStandard.notifyDataSetChanged();
                adapterForSignHere.notifyDataSetChanged();
                adapterForDynamic.notifyDataSetChanged();

                if (propertyBar != null) {
                    propertyBar.dismiss();
                }
            }
        });

        resetPropertyBar(propertyBar);

        if (propertyItem != null) {
            mProItem = propertyItem;
            Rect rect = new Rect();
            mProItem.getContentView().getGlobalVisibleRect(rect);
            RectF rectF = new RectF(rect);
            mPropertyBar.show(rectF, true);
        }
    }

    public void resetPropertyBar(PropertyBar propertyBar) {
        if (propertyBar == null) return;
        mPropertyBar = propertyBar;
        mPropertyBar.setArrowVisible(true);
        mPropertyBar.setPhoneFullScreen(true);
        mPropertyBar.reset(0);
        mPropertyBar.setTopTitleVisible(true);
        mPropertyBar.addTab("Standard Stamps", R.drawable._feature_annot_stamp_standardstamps_selector, "", 0);
        mPropertyBar.addCustomItem(itemStandard, mStampSelectViewForStandard, 0, 0);
        mPropertyBar.addTab("Sign Here", R.drawable._feature_annot_stamp_signherestamps_selector, "", 1);
        mPropertyBar.addCustomItem(itemSignHere, mStampSelectViewForForSignHere, 1, 0);
        mPropertyBar.addTab("Dynamic Stamps", R.drawable._feature_annot_stamp_dynamicstamps_selector, "", 2);
        mPropertyBar.addCustomItem(itemDynamic, mStampSelectViewForDynamic, 2, 0);

        if (mStampType >= 0 && mStampType <= 11) {
            mPropertyBar.setCurrentTab(0);
        } else if (mStampType >= 12 && mStampType <= 16) {
            mPropertyBar.setCurrentTab(1);
        } else if (mStampType >= 17 && mStampType <= 21) {
            mPropertyBar.setCurrentTab(2);
        }
    }

    private String getSubject(int mStampType) {
        if (mStampType == 0) return "Approved";
        if (mStampType == 1) return "Completed";
        if (mStampType == 2) return "Confidential";
        if (mStampType == 3) return "Draft";
        if (mStampType == 4) return "Emergency";
        if (mStampType == 5) return "Expired";
        if (mStampType == 6) return "Final";
        if (mStampType == 7) return "Received";
        if (mStampType == 8) return "Reviewed";
        if (mStampType == 9) return "Revised";
        if (mStampType == 10) return "Verified";
        if (mStampType == 11) return "Void";
        if (mStampType == 12) return "Accepted";
        if (mStampType == 13) return "Initial";
        if (mStampType == 14) return "Rejected";
        if (mStampType == 15) return "Sign Here";
        if (mStampType == 16) return "Witness";
        if (mStampType == 17) return "DynaApproved";
        if (mStampType == 18) return "DynaConfidential";
        if (mStampType == 19) return "DynaReceived";
        if (mStampType == 20) return "DynaReviewed";
        if (mStampType == 21) return "DynaRevised";
        return null;
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_STAMP;
    }

    @Override
    public void onActivate() {

    }

    @Override
    public void onDeactivate() {

    }

    private RectF getStampRectOnPageView(PointF point, int pageIndex) {
        PointF pageViewPt = new PointF(point.x, point.y);
        float offsetX = 49.5f;
        float offsetY = 15.5f;

        offsetX = thicknessOnPageView(pageIndex, offsetX);
        offsetY = thicknessOnPageView(pageIndex, offsetY);
        RectF pageViewRect = new RectF(pageViewPt.x - offsetX, pageViewPt.y - offsetY, pageViewPt.x + offsetX, pageViewPt.y + offsetY);
        if (pageViewRect.left < 0) {
            pageViewRect.offset(-pageViewRect.left, 0);
        }
        if (pageViewRect.right > mPdfViewCtrl.getPageViewWidth(pageIndex)) {
            pageViewRect.offset(mPdfViewCtrl.getPageViewWidth(pageIndex) - pageViewRect.right, 0);
        }
        if (pageViewRect.top < 0) {
            pageViewRect.offset(0, -pageViewRect.top);
        }
        if (pageViewRect.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex)) {
            pageViewRect.offset(0, mPdfViewCtrl.getPageViewHeight(pageIndex) - pageViewRect.bottom);
        }
        return pageViewRect;
    }

    private RectF mLastStampRect = new RectF(0, 0, 0, 0);
    private RectF mStampRect = new RectF(0, 0, 0, 0);
    private boolean mTouchCaptured = false;
    private int mLastPageIndex = -1;

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e) {
        PointF point = new PointF(e.getX(), e.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        int action = e.getAction();
        mStampRect = getStampRectOnPageView(point, pageIndex);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mTouchCaptured && mLastPageIndex == -1 || mLastPageIndex == pageIndex) {
                    mTouchCaptured = true;
                    mLastStampRect = new RectF(mStampRect);
                    if (mLastPageIndex == -1) {
                        mLastPageIndex = pageIndex;
                    }
                }
            case MotionEvent.ACTION_MOVE:
                if (!mTouchCaptured || mLastPageIndex != pageIndex)
                    break;
                RectF rect = new RectF(mLastStampRect);
                rect.union(mStampRect);
                rect.inset(-10, -10);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rect, rect, pageIndex);
                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rect));
                mLastStampRect = new RectF(mStampRect);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!mIsContinuousCreate) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                }
                RectF pdfRect = new RectF();
                mPdfViewCtrl.convertPageViewRectToPdfRect(mStampRect, pdfRect, pageIndex);
                createAnnot(pdfRect, pageIndex);
                return true;
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
        if (!mTouchCaptured || pageIndex != mLastPageIndex)
            return;
        Paint paint = new Paint();
        paint.setAlpha(100);

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), mStampIds[mStampType]);
        if (bitmap == null || mStampRect == null)
            return;
        canvas.drawBitmap(bitmap, null, mStampRect, paint);
    }

    private RectF mPageViewThickness = new RectF(0, 0, 0, 0);

    private float thicknessOnPageView(int pageIndex, float thickness) {
        mPageViewThickness.set(0, 0, thickness, thickness);
        mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewThickness, mPageViewThickness, pageIndex);
        float rectF;
        rectF = Math.abs(mPageViewThickness.width());

        return rectF;
    }

    private void createAnnot(final RectF rectF, final int pageIndex) {
        if (mPdfViewCtrl.isPageVisible(pageIndex)) {

            try {
                final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
                final Stamp annot = (Stamp) page.addAnnot(Annot.e_annotStamp, rectF);

                final StampAddUndoItem undoItem = new StampAddUndoItem(mPdfViewCtrl);
                undoItem.mPageIndex = pageIndex;
                undoItem.mStampType = mStampType;
                undoItem.mDsip = mDsip;
                undoItem.mNM = AppDmUtil.randomUUID(null);
                undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
                undoItem.mFlags = Annot.e_annotFlagPrint;
                undoItem.mSubject = getSubject(mStampType);
                undoItem.mIconName = undoItem.mSubject;
                undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
                undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
                undoItem.mBBox = new RectF(rectF);

                if (mStampType <= 17) {
                    undoItem.mBitmap = BitmapFactory.decodeResource(mContext.getResources(), mStampIds[mStampType]);
                }

                StampEvent event = new StampEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
                EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, annot);
                            DocumentManager.getInstance(mPdfViewCtrl).addUndoItem(undoItem);
                            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                                try {
                                    RectF viewRect = annot.getRect();
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                                    Rect rect = new Rect();
                                    viewRect.roundOut(rect);
                                    viewRect.union(mLastStampRect);
                                    rect.inset(-10, -10);
                                    mPdfViewCtrl.refresh(pageIndex, rect);
                                    mLastStampRect.setEmpty();
                                } catch (PDFException e) {
                                    e.printStackTrace();
                                }

                            }
                        }

                        mTouchCaptured = false;
                        mLastPageIndex = -1;
                    }
                });
                mPdfViewCtrl.addTask(task);
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean getIsContinuousCreate() {
        return mIsContinuousCreate;
    }

    public void setIsContinuousCreate(boolean isContinuousCreate) {
        this.mIsContinuousCreate = isContinuousCreate;
    }

    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, final Event.Callback result) {
        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
            RectF bboxRect = content.getBBox();

            try {
                PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
                final Stamp annot = (Stamp) page.addAnnot(Annot.e_annotStamp, bboxRect);

                annot.setUniqueID(content.getNM());
                annot.setCreationDateTime(AppDmUtil.currentDateToDocumentDate());
                annot.setModifiedDateTime(content.getModifiedDate());
                annot.setFlags(Annot.e_annotFlagPrint);

                annot.resetAppearanceStream();

                DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, annot);

                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    RectF viewRect = annot.getRect();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                    Rect rect = new Rect();
                    viewRect.roundOut(rect);
                    rect.inset(-10, -10);
                    mPdfViewCtrl.refresh(pageIndex, rect);
                    if (result != null) {
                        result.result(null, true);
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

    class IconView extends View {
        private Paint mPaint = new Paint();
        private int selectRect;
        private RectF mIconRectF;

        public IconView(Context context) {
            super(context);
            mPaint.setColor(selectRect);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(5);
            selectRect = Color.parseColor("#00000000");
            mIconRectF = new RectF(0, 0, 0, 0);
        }

        public IconView(Context context, int type) {
            super(context);
            mPaint.setColor(selectRect);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(5);
            selectRect = Color.parseColor("#179CD8");
            mIconRectF = new RectF(0, 0, 300, 90);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.save();
            canvas.drawRoundRect(mIconRectF, 6, 6, mPaint);
            canvas.restore();
        }

    }

}
