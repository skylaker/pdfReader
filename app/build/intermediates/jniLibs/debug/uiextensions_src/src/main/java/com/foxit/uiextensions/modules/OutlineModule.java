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
package com.foxit.uiextensions.modules;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.pdf.Bookmark;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.panel.PanelHost;
import com.foxit.uiextensions.controls.panel.PanelSpec;
import com.foxit.uiextensions.controls.panel.impl.PanelHostImpl;
import com.foxit.uiextensions.utils.AppDisplay;

import java.util.ArrayList;

public class OutlineModule implements Module, PanelSpec {
    public static final float RD_PANEL_WIDTH_SCALE_H = 0.338f;
    public static final float RD_PANEL_WIDTH_SCALE_V = 0.535f;
    private Context mContext;
    private PanelHost mPanelHost;
    private FrameLayout mOutlineTopBar;
    private RelativeLayout mPanel_outline_topbar;
    private ImageView mPanel_outline_topbar_close;
    private TextView mPanel_outline_topbar_title;
    private RelativeLayout mContentView;
    private LinearLayout mContent;
    private LinearLayout mLlBack;
    private ImageView mBack;
    private ImageView mSeparate;
    private ListView mListView;
    private TextView mNoInfoView;
    private OutlineSupport mOutlineSupport;
    private int mLevel = 0;
    private PDFViewCtrl mPdfViewCtrl;
    private ArrayList<OutlineItem> mOutlineArr = new ArrayList<OutlineItem>();
    private PopupWindow mPanelPopupWindow = null;
    private AppDisplay mDisplay;
    private ViewGroup mParent;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    private PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {
        }

        @Override
        public void onDocOpened(PDFDoc pdfDoc, int errCode) {
            if (pdfDoc == null || errCode != PDFError.NO_ERROR.getCode())
                return;
            mOutlineSupport.init();

        }

        @Override
        public void onDocWillClose(PDFDoc pdfDoc) {
        }

        @Override
        public void onDocClosed(PDFDoc pdfDoc, int i) {
            mOutlineSupport = null;
        }

        @Override
        public void onDocWillSave(PDFDoc pdfDoc) {
        }

        @Override
        public void onDocSaved(PDFDoc pdfDoc, int i) {
        }
    };

    public OutlineModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mPdfViewCtrl = pdfViewCtrl;
        mContext = context;
        mUiExtensionsManager = uiExtensionsManager;
        mParent = parent;
        mPanelHost = null;
    }

    public void setPanelHost(PanelHost panelHost) {
        mPanelHost = panelHost;
    }

    public PanelHost getPanelHost(){
        return mPanelHost;
    }

    public PopupWindow getPopupWindow(){
        return mPanelPopupWindow;
    }

    public void setPopupWindow(PopupWindow window) {
        mPanelPopupWindow = window;
    }

    public void show() {
        int viewWidth = mParent.getWidth();
        int viewHeight = mParent.getHeight();
        int height;
        int width;
        boolean bVertical = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (bVertical) {
            height = Math.max(viewWidth, viewHeight);
            width = Math.min(viewWidth, viewHeight);
        } else {
            height = Math.min(viewWidth, viewHeight);
            width = Math.max(viewWidth, viewHeight);
        }
        if (mDisplay.isPad()) {
            float scale = RD_PANEL_WIDTH_SCALE_V;
            if (width > height) {
                scale = RD_PANEL_WIDTH_SCALE_H;
            }
            width = (int) (mDisplay.getScreenWidth() * scale);
        }
        mPanelPopupWindow.setWidth(width);
        mPanelPopupWindow.setHeight(height);
        mPanelPopupWindow.setSoftInputMode(PopupWindow.INPUT_METHOD_NEEDED);
        // need this, else lock screen back will show input keyboard
        mPanelPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        mPanelHost.setCurrentSpec(PANELSPEC_TAG_OUTLINE);
        mPanelPopupWindow.showAtLocation(mPdfViewCtrl, Gravity.LEFT | Gravity.TOP, 0, 0);
    }

    private UIExtensionsManager.ConfigurationChangedListener mConfigurationChangedListener = new UIExtensionsManager.ConfigurationChangedListener() {
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            if(mPanelPopupWindow != null && mPanelPopupWindow.isShowing() && mPanelHost != null && mPanelHost.getCurrentSpec() == OutlineModule.this){
                mPanelPopupWindow.dismiss();
                show();
            }
        }
    };

    @Override
    public String getName() {
        return Module.MODULE_NAME_OUTLINE;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            mPanelHost = ((UIExtensionsManager) mUiExtensionsManager).getPanelManager().getPanel();
            mPanelPopupWindow = ((UIExtensionsManager) mUiExtensionsManager).getPanelManager().getPanelWindow();
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        if (mPanelHost == null)
            mPanelHost = new PanelHostImpl(mContext);

        mDisplay = new AppDisplay(mContext);
        initView();
        if (mPanelPopupWindow == null) {
            mPanelPopupWindow = new PopupWindow(mPanelHost.getContentView(),
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, true);
            mPanelPopupWindow.setBackgroundDrawable(new ColorDrawable(0x00FFFFFF));
            mPanelPopupWindow.setAnimationStyle(R.style.View_Animation_LtoR);
            mPanelPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                }
            });
        }
        mOutlineSupport = new OutlineSupport(mContext, mPdfViewCtrl, mDisplay, mPanelPopupWindow, mBack) {

            @Override
            public void OutlineBindingListView(BaseAdapter adapter) {
                mListView.setAdapter(adapter);
            }

            @Override
            public void getShowOutline(ArrayList<OutlineItem> mOutlineList) {
                mOutlineArr.clear();
                mOutlineArr.addAll(mOutlineList);
            }

            @Override
            public void updateUI(int level, int state) {
                mLevel = level;

                if (state == OutlineSupport.STATE_LOAD_FINISH) {
                    mNoInfoView.setText(mContext.getResources().getString(R.string.rv_panel_outline_noinfo));
                } else if (state == OutlineSupport.STATE_NORMAL) {
                    mNoInfoView.setText(mContext.getResources().getString(R.string.rv_panel_outline_noinfo));
                } else if (state == OutlineSupport.STATE_LOADING) {
                    mNoInfoView.setText(mContext.getResources().getString(R.string.rv_panel_annot_loading_start));
                }

                if (mOutlineArr.size() > 0) {
                    mContent.setVisibility(View.VISIBLE);
                    if (mLevel > 0) {
                        mLlBack.setVisibility(View.VISIBLE);
                        mSeparate.setVisibility(View.VISIBLE);
                    } else {
                        mLlBack.setVisibility(View.GONE);
                        mSeparate.setVisibility(View.GONE);
                    }

                    mNoInfoView.setVisibility(View.GONE);
                } else {
                    mContent.setVisibility(View.GONE);
                    mLlBack.setVisibility(View.GONE);
                    mSeparate.setVisibility(View.GONE);

                    mNoInfoView.setVisibility(View.VISIBLE);
                }
            }
        };
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerConfigurationChangedListener(mConfigurationChangedListener);
        }
        return true;
    }

    @Override
    public boolean unloadModule() {
        mPanelHost.removeSpec(this);
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mDocEventListener = null;
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterConfigurationChangedListener(mConfigurationChangedListener);
        }
        recycleImageViewResource(mBack);
        recycleImageViewResource(mSeparate);
        return true;
    }

    public void recycleImageViewResource(ImageView imageView) {
        if (imageView == null) return;
        Drawable drawable = imageView.getDrawable();
        if (drawable != null && drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                imageView.setImageBitmap(null);
                bitmap.recycle();
                bitmap = null;
            }
        }
    }

    private void initView() {
        mOutlineTopBar = (FrameLayout) LayoutInflater.from(mContext).inflate(R.layout.panel_outline_topbar, null, false);
        mPanel_outline_topbar = (RelativeLayout) mOutlineTopBar.findViewById(R.id.panel_outline_topbar);
        mPanel_outline_topbar_close = (ImageView) mOutlineTopBar.findViewById(R.id.panel_outline_topbar_close);
        mPanel_outline_topbar_title = (TextView) mOutlineTopBar.findViewById(R.id.panel_outline_topbar_title);

        // set top side offset
        if (mDisplay.isPad()) {
            mPanel_outline_topbar_close.setVisibility(View.GONE);

            FrameLayout.LayoutParams rl_topLayoutParams = (FrameLayout.LayoutParams) mPanel_outline_topbar.getLayoutParams();
            rl_topLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
            mPanel_outline_topbar.setLayoutParams(rl_topLayoutParams);

            RelativeLayout.LayoutParams topCloseLayoutParams = (RelativeLayout.LayoutParams) mPanel_outline_topbar_close.getLayoutParams();
            topCloseLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
            mPanel_outline_topbar_close.setLayoutParams(topCloseLayoutParams);
        } else {
            mPanel_outline_topbar_close.setVisibility(View.VISIBLE);

            RelativeLayout.LayoutParams topTitleLayoutParams = (RelativeLayout.LayoutParams) mPanel_outline_topbar_title.getLayoutParams();
            topTitleLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
            topTitleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
            topTitleLayoutParams.leftMargin = mDisplay.dp2px(70.0f);
            mPanel_outline_topbar_title.setLayoutParams(topTitleLayoutParams);
        }

        mPanel_outline_topbar_close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPanelPopupWindow.isShowing()) {
                    mPanelPopupWindow.dismiss();
                }
            }
        });

        //content layout
        mContentView = new RelativeLayout(mContext);
        mContentView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        mContentView.setBackgroundResource(R.color.ux_color_white);

        mContent = new LinearLayout(mContext);
        mContent.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        mContent.setOrientation(LinearLayout.VERTICAL);

        mLlBack = new LinearLayout(mContext);
        mLlBack.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mLlBack.setBackgroundResource(R.drawable.panel_outline_item_bg);
        mLlBack.setGravity(Gravity.CENTER_VERTICAL);
        mContent.addView(mLlBack);

        mBack = new ImageView(mContext);
        LinearLayout.LayoutParams backLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams backLlLayoutParams = (LinearLayout.LayoutParams) mLlBack.getLayoutParams();
        if (mDisplay.isPad()) {
            backLlLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_pad);

            backLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
            backLayoutParams.rightMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
        } else {
            backLlLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_phone);

            backLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
            backLayoutParams.rightMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
        }
        mLlBack.setLayoutParams(backLlLayoutParams);
        mBack.setLayoutParams(backLayoutParams);
        mBack.setImageResource(R.drawable.panel_outline_back_selector);
        mBack.setPadding(0, mDisplay.dp2px(5.0f), 0, mDisplay.dp2px(5.0f));
        mBack.setScaleType(ImageView.ScaleType.FIT_START);
        mBack.setFocusable(false);
        mLlBack.addView(mBack);

        mSeparate = new ImageView(mContext);
        mSeparate.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        mSeparate.setImageResource(R.color.ux_color_seperator_gray);
        mContent.addView(mSeparate);

        mListView = new ListView(mContext);
        mListView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        mListView.setCacheColorHint(mContext.getResources().getColor(R.color.ux_color_translucent));
        mListView.setDivider(mContext.getResources().getDrawable(R.color.ux_color_seperator_gray));
        mListView.setDividerHeight(1);
        mListView.setFastScrollEnabled(false);
        mContent.addView(mListView);

        mNoInfoView = new TextView(mContext);
        RelativeLayout.LayoutParams textViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        textViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mNoInfoView.setLayoutParams(textViewParams);
        mNoInfoView.setGravity(Gravity.CENTER);
        mNoInfoView.setText(mContext.getResources().getString(R.string.rv_panel_outline_noinfo));
        mNoInfoView.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_body2_dark));
        mNoInfoView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mContext.getResources().getDimension(R.dimen.ux_text_height_body2));
        mContentView.addView(mContent);
        mContentView.addView(mNoInfoView);

        mPanelHost.addSpec(this);
    }



    @Override
    public int getTag() {
        return PanelSpec.PANELSPEC_TAG_OUTLINE;
    }

    @Override
    public int getIcon() {
        return R.drawable.panel_tabimg_outline_selector;
    }

    @Override
    public View getTopToolbar() {
        return mOutlineTopBar;
    }

    @Override
    public View getContentView() {
        return mContentView;
    }

    @Override
    public void onActivated() {
        mOutlineSupport.updateUI(mLevel, mOutlineSupport.getCurrentState());
    }

    @Override
    public void onDeactivated() {
    }

    public static class OutlineItem {
        public String mTitle;
        public int mPageIndex;
        public float mX;
        public float mY;
        public int mLevel;
        public int mParentPos;
        public boolean mHaveChild;
        public Bookmark mBookmark;
        public ArrayList<OutlineItem> mChildren;
        public boolean mIsExpanded;
        public long mNdkAddr;

        public OutlineItem() {
            mTitle = null;
            mPageIndex = -1;
            mX = 0;
            mY = 0;
            mLevel = -1;
            mHaveChild = true;
            mChildren = new ArrayList<OutlineModule.OutlineItem>();
            mIsExpanded = false;
            mNdkAddr = 0;
        }
    }
}
