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
package com.foxit.uiextensions.modules.panel.annot;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.panel.PanelHost;
import com.foxit.uiextensions.controls.panel.PanelSpec;
import com.foxit.uiextensions.controls.panel.impl.PanelHostImpl;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.OnPageEventListener;

import java.util.ArrayList;

public class AnnotPanelModule implements Module, PanelSpec {
    private PDFViewCtrl mPdfViewCtrl;
    private Context mContext;
    private AppDisplay mDisplay;
    private View mTopBarView;
    private Boolean mIsPad;
    private View mClearView;
    private UITextEditDialog mDialog;

    private View mContentView;
    private AnnotPanel mAnnotPanel;

    private boolean isTouchHold;
    private ArrayList<Boolean> mItemMoreViewShow;
    private TextView mNoInfoView;
    private TextView mSearchingTextView;
    private int mPausedPageIndex = 0;
    private PanelHost mPanelHost;
    private PopupWindow mPanelPopupWindow = null;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public AnnotPanelModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        if (context == null || pdfViewCtrl == null) {
            throw new NullPointerException();
        }
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
        this.mItemMoreViewShow = new ArrayList<Boolean>();
        mDisplay = new AppDisplay(mContext);
        mIsPad = mDisplay.isPad();
    }

    public void setPanelHost(PanelHost panelHost) {
        mPanelHost = panelHost;
    }

    public PanelHost getPanelHost(){
        return mPanelHost;
    }

    public void setPopupWindow(PopupWindow window) {
        mPanelPopupWindow = window;
    }

    public PopupWindow getPopupWindow() {
        return mPanelPopupWindow;
    }

    private static final float RD_PANEL_WIDTH_SCALE_H = 0.338f;
    private static final float RD_PANEL_WIDTH_SCALE_V = 0.535f;

    public void show() {
        int viewWidth = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView().getWidth();
        int viewHeight = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView().getHeight();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mPanelPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
        mPanelHost.setCurrentSpec(PANELSPEC_TAG_ANNOTATIONS);
        mPanelPopupWindow.showAtLocation(mPdfViewCtrl, Gravity.LEFT | Gravity.TOP, 0, 0);
    }

    private UIExtensionsManager.ConfigurationChangedListener mConfigurationChangedListener = new UIExtensionsManager.ConfigurationChangedListener() {
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            if(mPanelPopupWindow != null && mPanelPopupWindow.isShowing() && mPanelHost != null && mPanelHost.getCurrentSpec() == AnnotPanelModule.this){
                mPanelPopupWindow.dismiss();
                show();
            }
        }
    };

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            mPanelHost = ((UIExtensionsManager) mUiExtensionsManager).getPanelManager().getPanel();
            mPanelPopupWindow = ((UIExtensionsManager) mUiExtensionsManager).getPanelManager().getPanelWindow();
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        if (mPanelHost == null)
            mPanelHost = new PanelHostImpl(mContext);

        mTopBarView = View.inflate(mContext, R.layout.panel_annot_topbar, null);
        View closeView = mTopBarView.findViewById(R.id.panel_annot_top_close_iv);
        TextView topTitle = (TextView) mTopBarView.findViewById(R.id.rv_panel_annot_title);
        mClearView = mTopBarView.findViewById(R.id.panel_annot_top_clear_tv);
        if (mIsPad) {
            closeView.setVisibility(View.GONE);
        } else {
            closeView.setVisibility(View.VISIBLE);
            closeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPanelPopupWindow.isShowing())
                        mPanelPopupWindow.dismiss();
                }
            });
        }
        View topNormalView = mTopBarView.findViewById(R.id.panel_annot_top_normal);
        topNormalView.setVisibility(View.VISIBLE);

        if (mIsPad) {
            FrameLayout.LayoutParams topNormalLayoutParams = (FrameLayout.LayoutParams) topNormalView.getLayoutParams();
            topNormalLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
            topNormalView.setLayoutParams(topNormalLayoutParams);

            RelativeLayout.LayoutParams topCloseLayoutParams = (RelativeLayout.LayoutParams) closeView.getLayoutParams();
            topCloseLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
            closeView.setLayoutParams(topCloseLayoutParams);
            RelativeLayout.LayoutParams topClearLayoutParams = (RelativeLayout.LayoutParams) mClearView.getLayoutParams();
            topClearLayoutParams.rightMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
            mClearView.setLayoutParams(topClearLayoutParams);
        } else {
            FrameLayout.LayoutParams topNormalLayoutParams = (FrameLayout.LayoutParams) topNormalView.getLayoutParams();
            topNormalLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_phone);
            topNormalView.setLayoutParams(topNormalLayoutParams);

            RelativeLayout.LayoutParams topTitleLayoutParams = (RelativeLayout.LayoutParams) topTitle.getLayoutParams();
            topTitleLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
            topTitleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
            topTitleLayoutParams.leftMargin = mDisplay.dp2px(70.0f);
            topTitle.setLayoutParams(topTitleLayoutParams);

            RelativeLayout.LayoutParams topCloseLayoutParams = (RelativeLayout.LayoutParams) closeView.getLayoutParams();
            topCloseLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
            closeView.setLayoutParams(topCloseLayoutParams);
            RelativeLayout.LayoutParams topClearLayoutParams = (RelativeLayout.LayoutParams) mClearView.getLayoutParams();
            topClearLayoutParams.rightMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
            mClearView.setLayoutParams(topClearLayoutParams);
        }

        mClearView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
                Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                if (context == null) return;
                mDialog = new UITextEditDialog(context);
                mDialog.setTitle(mContext.getResources().getString(R.string.hm_clear));
                mDialog.getPromptTextView().setText(mContext.getResources().getString(R.string.rd_panel_clear_comment));
                mDialog.getInputEditText().setVisibility(View.GONE);
                mDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mAnnotPanel.clearAllNodes();
                        mDialog.dismiss();
                    }
                });
                mDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDialog.dismiss();
                    }
                });
                mDialog.show();
            }
        });

        mContentView = View.inflate(mContext, R.layout.panel_annot_content, null);
        mNoInfoView = (TextView) mContentView.findViewById(R.id.rv_panel_annot_noinfo);
        mSearchingTextView = (TextView) mContentView.findViewById(R.id.rv_panel_annot_searching);
        ListView listView = (ListView) mContentView.findViewById(R.id.rv_panel_annot_list);

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
        mAnnotPanel = new AnnotPanel(this, mContext, mPdfViewCtrl, mContentView, mItemMoreViewShow);
        listView.setAdapter(mAnnotPanel.getAdapter());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (AppUtil.isFastDoubleClick()) return;
                if (mAnnotPanel.jumpToPage(position)) {
                    if (mPanelPopupWindow.isShowing())
                        mPanelPopupWindow.dismiss();
                }
            }
        });
        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        boolean show = false;
                        int position = 0;
                        for (int i = 0; i < mAnnotPanel.getAdapter().getCount(); i++) {
                            if (mItemMoreViewShow.get(i)) {
                                show = true;
                                position = i;
                                break;
                            }
                        }
                        if (show) {
                            mItemMoreViewShow.set(position, false);
                            mAnnotPanel.getAdapter().notifyDataSetChanged();
                            isTouchHold = true;
                            return true;
                        }
                    case MotionEvent.ACTION_UP:

                    case MotionEvent.ACTION_CANCEL:
                        if (isTouchHold) {
                            isTouchHold = false;
                            return true;
                        }

                }
                return false;
            }
        });

        mPanelHost.addSpec(this);
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        mPdfViewCtrl.registerPageEventListener(mPageEventListener);
        DocumentManager.getInstance(mPdfViewCtrl).registerAnnotEventListener(mAnnotPanel.getAnnotEventListener());
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerConfigurationChangedListener(mConfigurationChangedListener);
        }
        return true;
    }

    private PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener(){

        @Override
        public void onPagesRemoved(boolean success, int[] pageIndexes) {
            for(int i = 0; i < pageIndexes.length; i++)
                mAnnotPanel.getAdapter().onPageRemoved(success,pageIndexes[i] - i);
        }

        @Override
        public void onPageMoved(boolean success, int index, int dstIndex) {
            mAnnotPanel.getAdapter().onPageMoved(success,index,dstIndex);
        }

        @Override
        public void onPagesInserted(boolean success, int dstIndex, int[] range) {
            //when we inserted pages or docs, the annotlist should be reloaded.
            mAnnotPanel.onDocOpened();
            mAnnotPanel.startSearch(0);
        }
    };

    private PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if(errCode == PDFException.e_errSuccess) {
                mAnnotPanel.onDocOpened();
            }
        }

        @Override
        public void onDocWillClose(PDFDoc document) {
            mAnnotPanel.onDocWillClose();
            mSearchingTextView.setVisibility(View.GONE);
        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
            mSearchingTextView.setVisibility(View.GONE);
        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }
    };

    @Override
    public boolean unloadModule() {
        mPanelHost.removeSpec(this);
        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        mAnnotPanel = null;
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterConfigurationChangedListener(mConfigurationChangedListener);
        }
        return true;
    }

    @Override
    public String getName() {
        return MODULE_NAME_ANNOTPANEL;
    }


    private void resetClearButton() {
        if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot() || mAnnotPanel.getCurrentStatus() == AnnotPanel.STATUS_LOADING || mAnnotPanel.getCurrentStatus() == AnnotPanel.STATUS_PAUSED) {
            mClearView.setEnabled(false);
        } else {
            mClearView.setVisibility(View.VISIBLE);
            if (mAnnotPanel.getCount() > 0) {
                mClearView.setEnabled(true);
            } else {
                mClearView.setEnabled(false);
            }
        }
    }

    public void pauseSearch(int pageIndex) {
        mPausedPageIndex = pageIndex;
    }

    public void showNoAnnotsView() {
        mClearView.setEnabled(false);
        mNoInfoView.setText(AppResource.getString(mContext, R.string.rv_panel_annot_noinformation));
        mNoInfoView.setVisibility(View.VISIBLE);
    }

    public void hideNoAnnotsView() {
        if (mPdfViewCtrl.getDoc() != null && DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            mClearView.setEnabled(true);
        }
        if (mNoInfoView.getVisibility() == View.GONE) return;
        mNoInfoView.setVisibility(View.GONE);
    }

    public void updateLoadedPage(int curPageIndex, int total) {
        if (mAnnotPanel.getCurrentStatus() == AnnotPanel.STATUS_DONE) {
            if (curPageIndex == 0 && total == 0) {
                mSearchingTextView.setVisibility(View.GONE);
            }
            if (mSearchingTextView.isShown()) mSearchingTextView.setVisibility(View.GONE);

            mNoInfoView.setText(mContext.getResources().getString(R.string.rv_panel_annot_noinformation));
            if (mAnnotPanel.getCount() > 0) {
                if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
                    mClearView.setEnabled(true);
                }
                mNoInfoView.setVisibility(View.GONE);
            } else {
                mClearView.setEnabled(false);
                mNoInfoView.setVisibility(View.VISIBLE);
            }
        } else if (mAnnotPanel.getCurrentStatus() == AnnotPanel.STATUS_FAILED) {
            mSearchingTextView.setVisibility(View.GONE);
            if (mAnnotPanel.getCount() == 0) {
                mNoInfoView.setText(mContext.getResources().getString(R.string.rv_panel_annot_noinformation));
                mNoInfoView.setVisibility(View.VISIBLE);
                mClearView.setEnabled(false);
            } else {
                mClearView.setEnabled(true);
            }
        } else {
            mNoInfoView.setVisibility(View.GONE);
            if (!mSearchingTextView.isShown()) mSearchingTextView.setVisibility(View.VISIBLE);
            mSearchingTextView.setText(AppResource.getString(mContext, R.string.rv_panel_annot_item_pagenum) + ": " + curPageIndex + " / " + total);
        }
    }

    @Override
    public int getTag() {
        return PanelSpec.PANELSPEC_TAG_ANNOTATIONS;
    }

    @Override
    public int getIcon() {
        return R.drawable.panel_tabing_annotation_selector;
    }

    @Override
    public View getTopToolbar() {
        return mTopBarView;
    }

    @Override
    public View getContentView() {
        return mContentView;
    }

    @Override
    public void onActivated() {
        resetClearButton();
        switch (mAnnotPanel.getCurrentStatus()) {
            case AnnotPanel.STATUS_CANCEL:
                mNoInfoView.setText(mContext.getResources().getString(R.string.rv_panel_annot_loading_start));
                mNoInfoView.setVisibility(View.VISIBLE);
                mAnnotPanel.startSearch(0);

                break;
            case AnnotPanel.STATUS_LOADING:
                mAnnotPanel.setStatusPause(false);

                break;
            case AnnotPanel.STATUS_DONE:
                if (mSearchingTextView.getVisibility() != View.GONE)
                    mSearchingTextView.setVisibility(View.GONE);
                if (mAnnotPanel.getCount() > 0) {
                    mNoInfoView.setVisibility(View.GONE);
                } else {
                    mNoInfoView.setVisibility(View.VISIBLE);
                }
                break;
            case AnnotPanel.STATUS_PAUSED:
                mAnnotPanel.setStatusPause(false);
                mAnnotPanel.startSearch(mPausedPageIndex);
                break;
            case AnnotPanel.STATUS_FAILED:
                break;
            case AnnotPanel.STATUS_DELETING:
                break;
        }
        if(mAnnotPanel.getAdapter().hasDataChanged()){
            if(mAnnotPanel.getAdapter().isEmpty())
                showNoAnnotsView();
            mAnnotPanel.getAdapter().notifyDataSetChanged();
            mAnnotPanel.getAdapter().resetDataChanged();
        }
    }


    @Override
    public void onDeactivated() {
        if (mAnnotPanel.getCurrentStatus() == AnnotPanel.STATUS_LOADING) {
            mAnnotPanel.setStatusPause(true);
        }
    }
}
