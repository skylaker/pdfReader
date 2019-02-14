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

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.panel.PanelHost;
import com.foxit.uiextensions.controls.panel.PanelSpec;
import com.foxit.uiextensions.controls.panel.impl.PanelHostImpl;
import com.foxit.uiextensions.controls.toolbar.BaseItem;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.OnPageEventListener;

import java.util.ArrayList;

public class ReadingBookmarkModule implements Module, PanelSpec {
    private static final float RD_PANEL_WIDTH_SCALE_H = 0.338f;
    private static final float RD_PANEL_WIDTH_SCALE_V = 0.535f;

    private boolean mIsReadingBookmark = false;
    protected PDFViewCtrl mPdfViewCtrl;
    private Context mContext;
    private AppDisplay mDisplay;
    private View mTopBarView;
    private Boolean mIsPad;
    private View mClearView;
    private UITextEditDialog mDialog;
    private ArrayList<BaseItem> mMarkItemList;
    protected View mContentView;
    private RelativeLayout mReadingMarkContent;

    private ListView mReadingBookmarkListView;
    private TextView mReadingBookmarkNoInfoTv;
    private ReadingBookmarkSupport mSupport;

    private boolean isTouchHold;
    protected ArrayList<Boolean> mItemMoreViewShow;
    private PanelHost mPanelHost;
    private PopupWindow mPanelPopupWindow = null;
    private ViewGroup mParent;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    public ReadingBookmarkModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        if (context == null || pdfViewCtrl == null) {
            throw new NullPointerException();
        }
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
        this.mItemMoreViewShow = new ArrayList<Boolean>();
        mMarkItemList = new ArrayList<BaseItem>();
        mDisplay = new AppDisplay(mContext);
        mIsPad = mDisplay.isPad();
        mParent = parent;
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

    public void changeMarkItemState(boolean mark) {
        mIsReadingBookmark = mark;
        for(BaseItem item:mMarkItemList){
            item.setSelected(mIsReadingBookmark);
        }
    }

    public void addMarkedItem(BaseItem item) {
        if(mMarkItemList.contains(item))
            return;
        mMarkItemList.add(item);
        View.OnClickListener  listener = new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mIsReadingBookmark = !isMarked(mPdfViewCtrl.getCurrentPage());
                if (mIsReadingBookmark) {
                    addMark(mPdfViewCtrl.getCurrentPage());
                } else {
                    removeMark(mPdfViewCtrl.getCurrentPage());
                }
                changeMarkItemState(mIsReadingBookmark);
            }
        };

       item.setOnClickListener(listener);
    }

    public void removeMarkedItem(BaseItem item){
        if(!mMarkItemList.contains(item))
            return;
        item.setOnClickListener(null);
        mMarkItemList.remove(item);
    }

    private void prepareSupport(){
        if(mSupport == null){
            mSupport = new ReadingBookmarkSupport(ReadingBookmarkModule.this);
            mReadingBookmarkListView.setAdapter(mSupport.getAdapter());
        }
    }

    private final PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc pdfDoc, int errCode) {
            if (errCode != PDFError.NO_ERROR.getCode()) {
                return;
            }
            prepareSupport();
            remarkItemState(mPdfViewCtrl.getCurrentPage());
        }

        @Override
        public void onDocWillClose(PDFDoc pdfDoc) {

        }

        @Override
        public void onDocClosed(PDFDoc pdfDoc, int i) {

        }

        @Override
        public void onDocWillSave(PDFDoc pdfDoc) {

        }

        @Override
        public void onDocSaved(PDFDoc pdfDoc, int i) {

        }
    };

    private final PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener(){

        @Override
        public void onPageChanged(int oldPageIndex, int curPageIndex) {
            remarkItemState(curPageIndex);
        }

        @Override
        public void onPageMoved(boolean success, int index, int dstIndex) {
            mSupport.getAdapter().onPageMoved(success,index,dstIndex);

            remarkItemState(mPdfViewCtrl.getCurrentPage());
        }

        @Override
        public void onPagesRemoved(boolean success, int[] pageIndexes) {
            for(int i = 0; i < pageIndexes.length; i++) {
                mSupport.getAdapter().onPageRemoved(success,pageIndexes[i] - i);
            }

            remarkItemState(mPdfViewCtrl.getCurrentPage());
        }

        @Override
        public void onPagesInserted(boolean success, int dstIndex, int[] range) {
            mSupport.getAdapter().onPagesInsert(success, dstIndex, range);
            remarkItemState(mPdfViewCtrl.getCurrentPage());
        }
    };

    private void remarkItemState(final int index) {
        ((Activity)((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for(BaseItem item:mMarkItemList){
                    item.setSelected(isMarked(index));
                }
            }
        });
    }

    private UIExtensionsManager.ConfigurationChangedListener mConfigurationChangedListener = new UIExtensionsManager.ConfigurationChangedListener() {
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            if(mPanelPopupWindow != null && mPanelPopupWindow.isShowing() && mPanelHost != null && mPanelHost.getCurrentSpec() == ReadingBookmarkModule.this){
                mPanelPopupWindow.dismiss();
                show();
            }
        }
    };

    public void changeViewState(boolean enable) {
        mClearView.setEnabled(enable);
        if (!enable) {
            mReadingBookmarkNoInfoTv.setVisibility(View.VISIBLE);
        } else {
            mReadingBookmarkNoInfoTv.setVisibility(View.GONE);
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mPanelPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
        mPanelHost.setCurrentSpec(PANELSPEC_TAG_BOOKMARKS);
        mPanelPopupWindow.showAtLocation(mPdfViewCtrl, Gravity.LEFT | Gravity.TOP, 0, 0);
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
        mTopBarView = View.inflate(mContext, R.layout.panel_bookmark_topbar, null);
        View closeView = mTopBarView.findViewById(R.id.panel_bookmark_close);
        TextView topTitle = (TextView) mTopBarView.findViewById(R.id.panel_bookmark_title);
        mClearView = mTopBarView.findViewById(R.id.panel_bookmark_clear);
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
        View topNormalView = mTopBarView.findViewById(R.id.panel_bookmark_rl_top);
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
                mDialog.getPromptTextView().setText(mContext.getResources().getString(R.string.rd_panel_clear_readingbookmarks));
                mDialog.getInputEditText().setVisibility(View.GONE);
                mDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSupport.clearAllNodes();
                        changeViewState(false);
                        changeMarkItemState(false);
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

        mContentView = LayoutInflater.from(mContext).inflate(R.layout.panel_bookmark_main, null);
        mReadingMarkContent = (RelativeLayout) mContentView.findViewById(R.id.panel_bookmark_content_root);

        mReadingBookmarkListView = (ListView) mReadingMarkContent.findViewById(R.id.panel_bookmark_lv);
        mReadingBookmarkNoInfoTv = (TextView) mReadingMarkContent.findViewById(R.id.panel_nobookmark_tv);

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

        mReadingBookmarkListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (AppUtil.isFastDoubleClick()) return;
                ReadingBookmarkSupport.ReadingBookmarkNode bookmarkNode = (ReadingBookmarkSupport.ReadingBookmarkNode) mSupport.getAdapter().getItem(position);
                mPdfViewCtrl.gotoPage(bookmarkNode.getIndex());
                if (mPanelPopupWindow.isShowing())
                    mPanelPopupWindow.dismiss();

            }
        });
        mReadingBookmarkListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        boolean show = false;
                        int position = 0;
                        for (int i = 0; i < mSupport.getAdapter().getCount(); i++) {
                            if (mItemMoreViewShow.get(i)) {
                                show = true;
                                position = i;
                                break;
                            }
                        }
                        if (show) {
                            mItemMoreViewShow.set(position, false);
                            mSupport.getAdapter().notifyDataSetChanged();
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

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerConfigurationChangedListener(mConfigurationChangedListener);
        }
        return true;
    }

    @Override
    public boolean unloadModule() {
        mPanelHost.removeSpec(this);
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterConfigurationChangedListener(mConfigurationChangedListener);
        }
        return true;
    }

    @Override
    public String getName() {
        return MODULE_NAME_BOOKMARK;
    }

    @Override
    public void onActivated() {
        changeViewState(mSupport.getAdapter().getCount() != 0);
        if(mSupport.needRelayout()){
            mSupport.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onDeactivated() {

    }

    @Override
    public View getTopToolbar() {
        return mTopBarView;
    }

    @Override
    public int getIcon() {
        return R.drawable.panel_tabing_readingmark_selector;
    }

    @Override
    public int getTag() {
        return PanelSpec.PANELSPEC_TAG_BOOKMARKS;
    }

    @Override
    public View getContentView() {
        return mContentView;
    }

    public boolean isMarked(int pageIndex){
        return mSupport.getAdapter().isMarked(pageIndex);
    }

    public void addMark(int index){
        mSupport.addReadingBookmarkNode(index, String.format("Page %d", index + 1));
    }

    public void removeMark(int index){
        mSupport.removeReadingBookmarkNode(index);
    }

}
