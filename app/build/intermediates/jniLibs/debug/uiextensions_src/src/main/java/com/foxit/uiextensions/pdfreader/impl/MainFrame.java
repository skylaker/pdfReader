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
package com.foxit.uiextensions.pdfreader.impl;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.menu.MoreMenuModule;
import com.foxit.uiextensions.controls.menu.MoreMenuView;
import com.foxit.uiextensions.controls.propertybar.MoreTools;
import com.foxit.uiextensions.controls.propertybar.MultiLineBar;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.MoreToolsImpl;
import com.foxit.uiextensions.controls.propertybar.imp.MultiLineBarImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.BaseItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.AnnotBar;
import com.foxit.uiextensions.controls.toolbar.impl.AnnotsBar;
import com.foxit.uiextensions.controls.toolbar.impl.BaseBarImpl;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.BottomBarImpl;
import com.foxit.uiextensions.controls.toolbar.impl.CircleItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.EditDoneBar;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.modules.ScreenLockModule;
import com.foxit.uiextensions.modules.panel.IPanelManager;
import com.foxit.uiextensions.modules.panel.PanelManager;
import com.foxit.uiextensions.modules.signature.SignatureModule;
import com.foxit.uiextensions.modules.signature.SignatureToolHandler;
import com.foxit.uiextensions.modules.thumbnail.ThumbnailModule;
import com.foxit.uiextensions.pdfreader.IMainFrame;
import com.foxit.uiextensions.pdfreader.IPDFReader;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;

public class MainFrame implements IMainFrame {
    private int SHOW_ANIMATION_TAG = 100;
    private int HIDE_ANIMATION_TAG = 101;
    private boolean mSinglePage = true;//true:SINGLE_PAGE,false:CONTINUOUS_PAGE
    private Activity mAttachedActivity;
    private boolean mCloseAttachedActivity = true;
    private Context mContext;
    private boolean mIsFullScreen;
    private UIExtensionsManager.Config mModuleConfig;
    private int mThirdMaskCounter;

    private IPDFReader mReader;
    private ViewGroup mRootView;

    private ViewGroup mDocViewerLayout;
    private ViewGroup mTopBarLayout;
    private ViewGroup mBottomBarLayout;
    private ViewGroup mEditBarLayout;
    private ViewGroup mEditDoneBarLayout;
    private ViewGroup mToolSetBarLayout;
    private ViewGroup mAnnotCustomTopBarLayout;
    private ViewGroup mAnnotCustomBottomBarLayout;
    private ViewGroup mMaskView;
    private ImageView mToolIconView;
    private TextView mToolNameTv;

    private TopBarImpl mTopBar;
    private BaseBarImpl mBottomBar;
    private BaseBarImpl mEditDoneBar;
    private BaseBarImpl mEditBar;
    private MultiLineBarImpl mSettingBar;
    private MoreToolsImpl mMoreToolsBar;
    private BaseBarImpl mToolSetBar;
    private BaseBarImpl mAnnotCustomTopBar;
    private BaseBarImpl mAnnotCustomBottomBar;

    private AnimationSet mTopBarShowAnim;
    private AnimationSet mTopBarHideAnim;
    private AnimationSet mBottomBarShowAnim;
    private AnimationSet mBottomBarHideAnim;
    private AnimationSet mMaskShowAnim;
    private AnimationSet mMaskHideAnim;

    private PropertyBar mPropertyBar;

    private MoreMenuModule mMoreMenuModule;
    private PopupWindow mSettingPopupWindow;

    private ArrayList<View> mStateLayoutList;
    private boolean mIsShowTopToolbar = true;
    private boolean mIsShowBottomToolbar = true;

    private IPanelManager mPanelManager;
    private IPanelManager.OnShowPanelListener mShowPanelListener = null;

    public MainFrame(Context context, UIExtensionsManager.Config config) {
        mContext = context;
        mModuleConfig = config;
        mRootView = (ViewGroup) View.inflate(mContext, R.layout.rd_main_frame, null);

        mDocViewerLayout = (ViewGroup) mRootView.findViewById(R.id.read_docview_ly);

        mTopBarLayout = (ViewGroup) mRootView.findViewById(R.id.read_top_bar_ly);
        mBottomBarLayout = (ViewGroup) mRootView.findViewById(R.id.read_bottom_bar_ly);
        mEditBarLayout = (ViewGroup) mRootView.findViewById(R.id.read_edit_bar_ly);
        mEditDoneBarLayout = (ViewGroup) mRootView.findViewById(R.id.read_annot_done_bar_ly);
        mToolSetBarLayout = (ViewGroup) mRootView.findViewById(R.id.read_tool_set_bar_ly);
        mAnnotCustomTopBarLayout = (ViewGroup) mRootView.findViewById(R.id.read_annot_custom_top_bar_ly);
        mAnnotCustomBottomBarLayout = (ViewGroup) mRootView.findViewById(R.id.read_annot_custom_bottom_bar_ly);

        mMaskView = (ViewGroup) mRootView.findViewById(R.id.read_mask_ly);
        mToolIconView = (ImageView) mRootView.findViewById(R.id.read_tool_icon);
        mToolNameTv = (TextView) mRootView.findViewById(R.id.read_tool_name_tv);

        mStateLayoutList = new ArrayList<View>();
        mStateLayoutList.add(mTopBarLayout);
        mStateLayoutList.add(mBottomBarLayout);
        mStateLayoutList.add(mEditDoneBarLayout);
        mStateLayoutList.add(mEditBarLayout);
        mStateLayoutList.add(mToolSetBarLayout);
        mStateLayoutList.add(mAnnotCustomTopBarLayout);
        mStateLayoutList.add(mAnnotCustomBottomBarLayout);
    }

    public void init(PDFReader read) {
        mReader = read;
        mReader.registerStateChangeListener(mStateChangeListener);

        mTopBar = new TopBarImpl(mContext);
        mBottomBar = new BottomBarImpl(mContext);
        mEditBar = new AnnotsBar(mContext);
        mEditDoneBar = new EditDoneBar(mContext);
        mToolSetBar = new AnnotBar(mContext);
        mAnnotCustomTopBar = new TopBarImpl(mContext);
        mAnnotCustomBottomBar = new BottomBarImpl(mContext);

        mSettingBar = new MultiLineBarImpl(mContext);
        mSettingBar.init(mRootView, mModuleConfig);
        mMoreToolsBar = new MoreToolsImpl(mContext, mRootView);

        mPropertyBar = new PropertyBarImpl(mContext, mReader.getDocViewer());

        mTopBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_bg_color_toolbar_light));
        mBottomBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_bg_color_toolbar_light));
        mBottomBar.setItemSpace(mContext.getResources().getDimensionPixelSize(R.dimen.rd_bottombar_button_space));

        mEditBar.setInterceptTouch(false);

        mEditBar.setOrientation(BaseBar.HORIZONTAL);

        mEditDoneBar.setOrientation(BaseBar.HORIZONTAL);
        mEditDoneBar.setInterceptTouch(false);

        mToolSetBar.setOrientation(BaseBar.HORIZONTAL);
        mToolSetBar.setInterceptTouch(false);

        mAnnotCustomTopBar.setOrientation(BaseBar.HORIZONTAL);
        mAnnotCustomTopBar.setInterceptTouch(false);
        mAnnotCustomBottomBar.setOrientation(BaseBar.HORIZONTAL);
        mAnnotCustomBottomBar.setInterceptTouch(false);

        mTopBarLayout.addView(mTopBar.getContentView());
        mBottomBarLayout.addView(mBottomBar.getContentView());
        mEditBarLayout.addView(mEditBar.getContentView());
        mEditDoneBarLayout.addView(mEditDoneBar.getContentView());
        mToolSetBarLayout.addView(mToolSetBar.getContentView());
        mAnnotCustomTopBarLayout.addView(mAnnotCustomTopBar.getContentView());
        mAnnotCustomBottomBarLayout.addView(mAnnotCustomBottomBar.getContentView());
        setSettingView(mSettingBar.getRootView());


        mPanelManager = new PanelManager(mContext, mRootView, new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mStateChangeListener.onStateChanged(mReader.getState(), mReader.getState());
            }
        });

        mPanelManager.setOnShowPanelListener(mShowPanelListener = new IPanelManager.OnShowPanelListener() {
            @Override
            public void onShow() {
                mStateChangeListener.onStateChanged(mReader.getState(), mReader.getState());
            }
        });
        initBottomBarBtns();
        initOtherView();
        initAnimations();
        mStateChangeListener.onStateChanged(mReader.getState(), mReader.getState());
    }

    protected void release() {
        mShowPanelListener = null;
        mPanelManager = null;

        mTopBarShowAnim = null;
        mTopBarHideAnim = null;
        mBottomBarShowAnim = null;
        mBottomBarHideAnim = null;
        mMaskShowAnim = null;
        mMaskHideAnim = null;

        mMaskView = null;
        mToolIconView = null;

        mDocViewerLayout.removeAllViews();
        mDocViewerLayout = null;
        mRootView.removeAllViews();
        mRootView = null;

        mStateLayoutList.clear();
        mStateLayoutList = null;
        mStateChangeListener = null;
    }

    BaseItem mBackItem;
    CircleItemImpl mPanelBtn = null;
    CircleItemImpl mSettingBtn = null;
    CircleItemImpl mEditBtn = null;
    CircleItemImpl mReadSignItem = null;

    private boolean isLoadPanel() {
        return mModuleConfig.isLoadAnnotations() || mModuleConfig.isLoadReadingBookmark()
                || mModuleConfig.isLoadOutline() || mModuleConfig.isLoadAttachment();
    }

    private void initBottomBarBtns() {
        mPanelBtn = new CircleItemImpl(mContext);
        mSettingBtn = new CircleItemImpl(mContext);

        int circleResId = R.drawable.rd_bar_circle_bg_selector;
        int textSize = mContext.getResources().getDimensionPixelSize(R.dimen.ux_text_height_toolbar);
        int textColorResId = R.color.ux_text_color_body2_dark;
        int interval = mContext.getResources().getDimensionPixelSize(R.dimen.ux_toolbar_button_icon_text_vert_interval);

        mPanelBtn.setImageResource(R.drawable.rd_bar_panel_selector);
        mPanelBtn.setText(AppResource.getString(mReader.getMainFrame().getContext(), R.string.rd_bar_panel));
        mPanelBtn.setRelation(BaseItem.RELATION_BELOW);
        mPanelBtn.setCircleRes(circleResId);
        mPanelBtn.setInterval(interval);
        mPanelBtn.setTextSize(AppDisplay.getInstance(mContext).px2dp(textSize));
        mPanelBtn.setTextColorResource(textColorResId);
        mPanelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPanelManager != null) {
                    ((UIExtensionsManager)mReader.getUIExtensionsManager()).triggerDismissMenuEvent();
                    mPanelManager.showPanel();
                }
            }
        });
        if(isLoadPanel()) {
            mBottomBar.addView(mPanelBtn, BaseBar.TB_Position.Position_CENTER);
        }

        mSettingBtn.setImageResource(R.drawable.rd_bar_setting_selector);
        mSettingBtn.setText(AppResource.getString(mReader.getMainFrame().getContext(), R.string.rd_bar_setting));
        mSettingBtn.setRelation(BaseItemImpl.RELATION_BELOW);
        mSettingBtn.setCircleRes(circleResId);
        mSettingBtn.setInterval(interval);
        mSettingBtn.setTextSize(AppDisplay.getInstance(mContext).px2dp(textSize));
        mSettingBtn.setTextColorResource(textColorResId);
        mBottomBar.addView(mSettingBtn, BaseBar.TB_Position.Position_CENTER);
        mSettingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingBar();
            }
        });

        if(mModuleConfig.isLoadAnnotations()) {
            mEditBtn = new CircleItemImpl(mContext);
            mEditBtn.setImageResource(R.drawable.rd_bar_edit_selector);
            mEditBtn.setText(AppResource.getString(mReader.getMainFrame().getContext(), R.string.rd_bar_edit));
            mEditBtn.setRelation(BaseItemImpl.RELATION_BELOW);
            mEditBtn.setCircleRes(circleResId);
            mEditBtn.setInterval(interval);
            mEditBtn.setTextSize(AppDisplay.getInstance(mContext).px2dp(textSize));
            mEditBtn.setTextColorResource(textColorResId);
            mEditBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((UIExtensionsManager)mReader.getUIExtensionsManager()).triggerDismissMenuEvent();
                    mReader.changeState(ReadStateConfig.STATE_EDIT);
                }
            });
            mBottomBar.addView(mEditBtn, BaseBar.TB_Position.Position_CENTER);
        }

        if(mModuleConfig.isLoadSignature()){
            mReadSignItem = new CircleItemImpl(mContext);
            mReadSignItem.setImageResource(R.drawable.sg_selector);
            mReadSignItem.setText(AppResource.getString(mReader.getMainFrame().getContext(), R.string.rd_bar_sign));
            mReadSignItem.setRelation(BaseItem.RELATION_BELOW);
            mReadSignItem.setCircleRes(circleResId);
            mReadSignItem.setInterval(interval);
            mReadSignItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(textSize));
            mReadSignItem.setTextColorResource(textColorResId);
            mBottomBar.addView(mReadSignItem, BaseBar.TB_Position.Position_CENTER);
            mReadSignItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (AppUtil.isFastDoubleClick()) return;
                    ((UIExtensionsManager)mReader.getUIExtensionsManager()).triggerDismissMenuEvent();
                    Module module = ((UIExtensionsManager) mReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_PSISIGNATURE);
                    if(module instanceof SignatureModule)
                     ((UIExtensionsManager) mReader.getUIExtensionsManager()).setCurrentToolHandler(((SignatureModule) module).getToolHandler());
                    resetAnnotCustomBottomBar();
                    resetAnnotCustomTopBar();
                    mReader.changeState(ReadStateConfig.STATE_SIGNATURE);
                }
            });

        }

    }


    private BaseItem mSignListItem;
    public void resetAnnotCustomBottomBar() {
        mReader.getMainFrame().getAnnotCustomBottomBar().removeAllItems();
        mReader.getMainFrame().getAnnotCustomBottomBar().setBackgroundColor(mContext.getResources().getColor(com.foxit.uiextensions.R.color.ux_bg_color_toolbar_light));
        mReader.getMainFrame().getAnnotCustomBottomBar().setItemSpace(mContext.getResources().getDimensionPixelSize( com.foxit.uiextensions.R.dimen.rd_bottombar_button_space));

        mSignListItem = new BaseItemImpl(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (AppDisplay.getInstance(mContext).isPad()) {
                    if (mReader.getMainFrame().getPropertyBar().isShowing()) {
                        Rect rect = new Rect();
                        mSignListItem.getContentView().getGlobalVisibleRect(rect);
                        mReader.getMainFrame().getPropertyBar().update(new RectF(rect));

                    }
                }
            }
        };
        mSignListItem.setImageResource(R.drawable.sg_list_selector);
        mSignListItem.setText(AppResource.getString(mContext, com.foxit.uiextensions.R.string.rv_sign_model));
        mSignListItem.setRelation(BaseItem.RELATION_BELOW);
        mSignListItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rect rect = new Rect();
                mSignListItem.getContentView().getGlobalVisibleRect(rect);
                Module module = ((UIExtensionsManager) mReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_PSISIGNATURE);
                if(module!= null) {
                    SignatureToolHandler signature_toolHandler = (SignatureToolHandler) ((SignatureModule) module).getToolHandler();
                    signature_toolHandler.showSignList(new RectF(rect));
                }
            }
        });
        mReader.getMainFrame().getAnnotCustomBottomBar().addView(mSignListItem, BaseBar.TB_Position.Position_CENTER);
    }

    public void resetAnnotCustomTopBar() {
        BaseBar annotCustomBar = mReader.getMainFrame().getAnnotCustomTopBar();
        annotCustomBar.removeAllItems();
        annotCustomBar.setBackgroundColor(mContext.getResources().getColor(com.foxit.uiextensions.R.color.ux_bg_color_toolbar_light));

        BaseItem closeItem = new BaseItemImpl(mContext);
        closeItem.setImageResource(com.foxit.uiextensions.R.drawable.rd_reflow_back_selector);
        closeItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) return;
                ((UIExtensionsManager) mReader.getUIExtensionsManager()).setCurrentToolHandler(null);
                mReader.changeState(ReadStateConfig.STATE_NORMAL);
                mReader.getDocViewer().invalidate();
            }
        });
        BaseItem titleItem = new BaseItemImpl(mContext);
        titleItem.setText(AppResource.getString(mContext, com.foxit.uiextensions.R.string.sg_signer_title));
        titleItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(mContext.getResources().getDimension(com.foxit.uiextensions.R.dimen.ux_text_height_subhead)));
        titleItem.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_title_dark));

        annotCustomBar.addView(closeItem, BaseBar.TB_Position.Position_LT);
        annotCustomBar.addView(titleItem, BaseBar.TB_Position.Position_LT);
    }


    BaseItemImpl mMenuBtn = null;
    CircleItemImpl mAnnotDoneBtn = null;
    private BaseItem mMoreItem;
    private void initOtherView() {
        //Topbar backButton
        mBackItem = new BaseItemImpl(mContext);
        mBackItem.setImageResource(R.drawable.rd_reflow_back_selector);
        mTopBar.addView(mBackItem, BaseBar.TB_Position.Position_LT);
        mBackItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager)mReader.getUIExtensionsManager()).triggerDismissMenuEvent();
                mReader.backToPrevActivity();
            }
        });


        // Topbar Menu Button
        mMenuBtn = new BaseItemImpl(mContext);
        mMenuBtn.setImageResource(R.drawable.rd_bar_more_selector);
        mMenuBtn.setTag(ToolbarItemConfig.ITEM_TOPBAR_MORE_TAG);
        mMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreMenu();
            }
        });
        mTopBar.addView(mMenuBtn, BaseBar.TB_Position.Position_RB);

        //Annot Done Button
        mAnnotDoneBtn = new CircleItemImpl(mContext);
        mAnnotDoneBtn.setImageResource(R.drawable.cloud_back);
        mAnnotDoneBtn.setCircleRes(R.drawable.rd_back_background);
        mEditDoneBar.addView(mAnnotDoneBtn, BaseBar.TB_Position.Position_LT);
        mAnnotDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager)mReader.getUIExtensionsManager()).triggerDismissMenuEvent();
                if (((UIExtensionsManager) mReader.getUIExtensionsManager()).getCurrentToolHandler() != null) {
                    ((UIExtensionsManager) mReader.getUIExtensionsManager()).setCurrentToolHandler(null);
                }

                mReader.changeState(ReadStateConfig.STATE_NORMAL);

                if (!isToolbarsVisible()) {
                    showToolbars();
                }
            }
        });

        // add annotations more button to bottom toolbar
        mMoreItem = new CircleItemImpl(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                super.onItemLayout(l, t, r, b);

                if (getMoreToolsBar().isShowing() && mReader.getState() == ReadStateConfig.STATE_EDIT) {
                    Rect rect = new Rect();
                    mMoreItem.getContentView().getGlobalVisibleRect(rect);
                    mMoreToolsBar.update(new RectF(rect));
                }
            }
        };
        mMoreItem.setTag(ToolbarItemConfig.ITEM_ANNOTSBAR_MORE_TAG);
        mMoreItem.setImageResource(com.foxit.uiextensions.R.drawable.mt_more_selector);

        mMoreItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isEditBarShowing())
                    return;

                Rect rect = new Rect();
                mMoreItem.getContentView().getGlobalVisibleRect(rect);
                mMoreToolsBar.show(new RectF(rect), true);

            }
        });
        mEditBar.addView(mMoreItem, BaseBar.TB_Position.Position_CENTER);
    }

    void initAnimations() {
        if (mTopBarShowAnim == null) {
            mTopBarShowAnim = new AnimationSet(true);
            mTopBarHideAnim = new AnimationSet(true);
            mBottomBarShowAnim = new AnimationSet(true);
            mBottomBarHideAnim = new AnimationSet(true);
            mMaskShowAnim = new AnimationSet(true);
            mMaskHideAnim = new AnimationSet(true);
        }
        if (mTopBarShowAnim.getAnimations() != null && mTopBarShowAnim.getAnimations().size() > 0) {
            return;
        }
        if (mTopBarLayout.getHeight() == 0) {
            return;
        }
        SHOW_ANIMATION_TAG = R.id.rd_show_animation_tag;
        HIDE_ANIMATION_TAG = R.id.rd_hide_animation_tag;
        // top bar
        TranslateAnimation anim = new TranslateAnimation(0, 0, -mTopBarLayout.getHeight(), 0);
        anim.setDuration(300);
        mTopBarShowAnim.addAnimation(anim);
        anim = new TranslateAnimation(0, 0, 0, -mTopBarLayout.getHeight());
        anim.setDuration(300);
        mTopBarHideAnim.addAnimation(anim);
        // bottom bar
        anim = new TranslateAnimation(0, 0, mBottomBarLayout.getHeight(), 0);
        anim.setDuration(300);
        mBottomBarShowAnim.addAnimation(anim);
        anim = new TranslateAnimation(0, 0, 0, mTopBarLayout.getHeight());
        anim.setDuration(300);
        mBottomBarHideAnim.addAnimation(anim);
        // mask view
        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        alphaAnimation.setDuration(300);
        mMaskShowAnim.addAnimation(alphaAnimation);
        alphaAnimation = new AlphaAnimation(1, 0);
        alphaAnimation.setDuration(300);
        mMaskHideAnim.addAnimation(alphaAnimation);

        mTopBarLayout.setTag(SHOW_ANIMATION_TAG, mTopBarShowAnim);
        mTopBarLayout.setTag(HIDE_ANIMATION_TAG, mTopBarHideAnim);
        mBottomBarLayout.setTag(SHOW_ANIMATION_TAG, mBottomBarShowAnim);
        mBottomBarLayout.setTag(HIDE_ANIMATION_TAG, mBottomBarHideAnim);
        mEditDoneBarLayout.setTag(SHOW_ANIMATION_TAG, mTopBarShowAnim);
        mEditDoneBarLayout.setTag(HIDE_ANIMATION_TAG, mTopBarHideAnim);
        mEditBarLayout.setTag(SHOW_ANIMATION_TAG, mBottomBarShowAnim);
        mEditBarLayout.setTag(HIDE_ANIMATION_TAG, mBottomBarHideAnim);
        mToolSetBarLayout.setTag(SHOW_ANIMATION_TAG, mBottomBarShowAnim);
        mToolSetBarLayout.setTag(HIDE_ANIMATION_TAG, mBottomBarHideAnim);
        mAnnotCustomTopBarLayout.setTag(SHOW_ANIMATION_TAG, mTopBarShowAnim);
        mAnnotCustomTopBarLayout.setTag(HIDE_ANIMATION_TAG, mTopBarHideAnim);
        mAnnotCustomBottomBarLayout.setTag(SHOW_ANIMATION_TAG, mBottomBarShowAnim);
        mAnnotCustomBottomBarLayout.setTag(HIDE_ANIMATION_TAG, mBottomBarHideAnim);
        mMaskView.setTag(SHOW_ANIMATION_TAG, mMaskShowAnim);
        mMaskView.setTag(HIDE_ANIMATION_TAG, mMaskHideAnim);
    }

    public void addDocView(View docView) {
        ViewParent parent = docView.getParent();
        if (parent != null) {
            ((ViewGroup)parent).removeView(docView);
        }
        mDocViewerLayout.addView(docView);
    }

    @Override
    public RelativeLayout getContentView() {
        return (RelativeLayout) mRootView;
    }

    @Override
    public BaseBar getAnnotCustomTopBar() {
        return mAnnotCustomTopBar;
    }
    @Override
    public BaseBar getAnnotCustomBottomBar() {
        return mAnnotCustomBottomBar;
    }

    @Override
    public BaseBar getTopToolbar() {
        return mTopBar;
    }

    @Override
    public PropertyBar getPropertyBar() {
        return mPropertyBar;
    }

    @Override
    public BaseBar getBottomToolbar() {
        return mBottomBar;
    }

    @Override
    public IPanelManager getPanelManager() {
        return mPanelManager;
    }

    @Override
    public void showToolbars() {
        mIsFullScreen = false;
        mReader.changeState(mReader.getState());
    }

    @Override
    public void hideToolbars() {
        mIsFullScreen = true;
        mReader.changeState(mReader.getState());
    }

    @Override
    public boolean isToolbarsVisible() {
        return !mIsFullScreen;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mPanelManager != null && mPanelManager.getPanelWindow() != null
                && mPanelManager.getPanelWindow().isShowing()) {
            mPanelManager.hidePanel();
            ((UIExtensionsManager)mReader.getUIExtensionsManager()).triggerDismissMenuEvent();
            mPanelManager.showPanel();
        }
    }


    public void showMoreMenu() {
        ((UIExtensionsManager)mReader.getUIExtensionsManager()).triggerDismissMenuEvent();
        mMoreMenuModule = (MoreMenuModule) ((UIExtensionsManager) mReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_MORE_MENU);
        if (mMoreMenuModule == null) return;
        MoreMenuView view = mMoreMenuModule.getView();
        if (view != null)
            view.show();
    }

    public void hideMoreMenu() {
        mMoreMenuModule = (MoreMenuModule) ((UIExtensionsManager) mReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_MORE_MENU);
        if (mMoreMenuModule == null) return;
        MoreMenuView view = mMoreMenuModule.getView();
        if (view != null)
            view.hide();
    }

    @Override
    public MultiLineBar getSettingBar() {
        return mSettingBar;
    }

    private void setSettingView(View view) {
        mSettingPopupWindow = new PopupWindow(view,
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true);
        mSettingPopupWindow.setBackgroundDrawable(new ColorDrawable(0x00FFFFFF));
        mSettingPopupWindow.setAnimationStyle(R.style.View_Animation_BtoT);
        mSettingPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mStateChangeListener.onStateChanged(mReader.getState(), mReader.getState());
            }
        });
    }

    private void applyValue() {
        if (mSinglePage) {
            mReader.getDocViewer().setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_SINGLE);
        } else {
            mReader.getDocViewer().setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_CONTINUOUS);
        }
    }

    MultiLineBar.IML_ValueChangeListener mSingleChangeListener = new MultiLineBar.IML_ValueChangeListener() {
        @Override
        public void onValueChanged(int type, Object value) {
            if (MultiLineBar.TYPE_SINGLEPAGE == type) {
                mSinglePage = (Boolean) value;
                mSettingBar.setProperty(MultiLineBar.TYPE_SINGLEPAGE, mSinglePage);
                applyValue();
            }
        }

        @Override
        public void onDismiss() {

        }

        @Override
        public int getType() {
            return MultiLineBar.TYPE_SINGLEPAGE;
        }
    };

    private void showThumbnailDialog() {
        ThumbnailModule thumbnailModule = (ThumbnailModule)((UIExtensionsManager) mReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_THUMBNAIL);
        if(thumbnailModule != null)
            thumbnailModule.show();
    }

    MultiLineBar.IML_ValueChangeListener mThumbnailListener = new MultiLineBar.IML_ValueChangeListener() {
        @Override
        public void onValueChanged(int type, Object value) {
            if (MultiLineBar.TYPE_THUMBNAIL == type) {
                showThumbnailDialog();
                mReader.getMainFrame().hideSettingBar();
            }
        }

        @Override
        public void onDismiss() {

        }

        @Override
        public int getType() {
            return MultiLineBar.TYPE_THUMBNAIL;
        }
    };

    void showSettingBar() {
        if (mSettingBar == null) return;
        ((UIExtensionsManager)mReader.getUIExtensionsManager()).triggerDismissMenuEvent();
        mSettingBar.getContentView().measure(0, 0);
        mSettingBar.registerListener(mSingleChangeListener);
        mSettingBar.registerListener(mThumbnailListener);
        ScreenLockModule screenLock = (ScreenLockModule) ((UIExtensionsManager) mReader.getUIExtensionsManager()).getModuleByName(ScreenLockModule.MODULE_NAME_SCREENLOCK);
        if(screenLock!= null)
            mSettingBar.registerListener(screenLock.getScreenLockListener());
        int height = mSettingBar.getContentView().getMeasuredHeight();
        mSettingPopupWindow.setWidth(mRootView.getWidth());
        mSettingPopupWindow.setHeight(height);
        mSettingPopupWindow.setSoftInputMode(PopupWindow.INPUT_METHOD_NEEDED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mSettingPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
        mSettingPopupWindow.showAtLocation(mRootView, Gravity.LEFT | Gravity.BOTTOM, 0, AppDisplay.getInstance(mContext).getNavBarHeight());
        mStateChangeListener.onStateChanged(mReader.getState(), mReader.getState());
        mSettingBar.setProperty(MultiLineBar.TYPE_SINGLEPAGE, mSinglePage);
    }

    public void updateSettingBar() {
        if (mSettingBar == null) return;
        mSettingBar.getContentView().measure(0, 0);
        int barHeight = mSettingBar.getContentView().getMeasuredHeight();
        boolean bVertical = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        int width = AppDisplay.getInstance(mContext).getScreenWidth();
        int height = AppDisplay.getInstance(mContext).getScreenHeight();
        int screenHeight, screenWidth;
        if (bVertical) {
            screenHeight = Math.max(width, height);
            screenWidth = Math.min(width, height);
        } else {
            screenHeight = Math.min(width, height);
            screenWidth = Math.max(width, height);
        }

        int y = 0;
        if (Build.VERSION.SDK_INT == 24){
            y = screenHeight - barHeight;
        }
        mSettingPopupWindow.update(0, y, screenWidth, barHeight);
    }

    @Override
    public void hideSettingBar() {
        if (mSettingPopupWindow != null && mSettingPopupWindow.isShowing()) {
            mSettingPopupWindow.dismiss();
        }
    }

    @Override
    public MoreTools getMoreToolsBar() {
        return mMoreToolsBar;
    }

    @Override
    public void showMaskView() {
        mThirdMaskCounter++;
        mStateChangeListener.onStateChanged(mReader.getState(), mReader.getState());
    }

    @Override
    public void hideMaskView() {
        mThirdMaskCounter--;
        if (mThirdMaskCounter < 0)
            mThirdMaskCounter = 0;
        mStateChangeListener.onStateChanged(mReader.getState(), mReader.getState());
    }

    protected void resetMaskView() {
        if (mPanelManager != null && mPanelManager.getPanelWindow().isShowing()) {
            mPanelManager.hidePanel();
        }

        if (mMoreMenuModule != null) {
            hideMoreMenu();
        }

        if (mSettingPopupWindow.isShowing()) {
            hideSettingBar();
        }
        if (mMoreToolsBar.isShowing()) {
            mMoreToolsBar.dismiss();
        }
        if (isMaskViewShowing()) {
            hideMaskView();
        }
        mThirdMaskCounter = 0;
    }

    @Override
    public boolean isMaskViewShowing() {
        return mMaskView.getVisibility() == View.VISIBLE
                || mThirdMaskCounter > 0;
    }

    @Override
    public boolean isEditBarShowing() {
        return mEditBarLayout != null && mEditBarLayout.getVisibility() == View.VISIBLE;
    }

    @Override
    public void enableTopToolbar(boolean isEnabled) {
        if (mTopBarLayout != null) {
            if (isEnabled) {
                mIsShowTopToolbar = true;
                mTopBarLayout.setVisibility(View.VISIBLE);
            } else {
                mIsShowTopToolbar = false;
                mTopBarLayout.setVisibility(View.GONE);
            }

        }
    }

    @Override
    public void enableBottomToolbar(boolean isEnabled) {
        if (mBottomBarLayout != null) {
            if (isEnabled) {
                mIsShowBottomToolbar = true;
                mBottomBarLayout.setVisibility(View.VISIBLE);
            } else {
                mIsShowBottomToolbar = false;
                mBottomBarLayout.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public BaseBar getEditBar() {
        return mEditBar;
    }

    @Override
    public BaseBar getEditDoneBar() {
        return mEditDoneBar;
    }

    @Override
    public BaseBar getToolSetBar() {
        return mToolSetBar;
    }

    @Override
    public Activity getAttachedActivity() {
        return mAttachedActivity;
    }

    public void setAttachedActivity(Activity act) {
        mAttachedActivity = act;
    }


    public boolean getCloseAttachedActivity() {
        return mCloseAttachedActivity;
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    private IStateChangeListener mStateChangeListener = new IStateChangeListener() {
        @Override
        public void onStateChanged(int oldState, int newState) {
            initAnimations();
            if(mModuleConfig.isLoadAnnotations()) {
                if (DocumentManager.getInstance(mReader.getDocViewer()).canAddAnnot()) {
                    mEditBtn.setEnable(true);
                } else {
                    mEditBtn.setEnable(false);
                }
            }
            if(mModuleConfig.isLoadSignature()) {
                if (DocumentManager.getInstance(mReader.getDocViewer()).canAddAnnot() && DocumentManager.getInstance(mReader.getDocViewer()).canSigning()) {
                    mReadSignItem.setEnable(true);
                } else {
                    mReadSignItem.setEnable(false);
                }
            }

            ArrayList<View> currentShowViews = new ArrayList<View>();
            ArrayList<View> willShowViews = new ArrayList<View>();
            for (View view : mStateLayoutList) {
                if (view.getVisibility() == View.VISIBLE) {
                    currentShowViews.add(view);
                }
            }
            switch (newState) {
                case ReadStateConfig.STATE_NORMAL:
                    if (isToolbarsVisible()) {
                        if (mIsShowTopToolbar) {
                            willShowViews.add(mTopBarLayout);
                        }

                        if (mIsShowBottomToolbar) {
                            willShowViews.add(mBottomBarLayout);
                        }
                    }
                    break;
                case ReadStateConfig.STATE_EDIT:
                    if (isToolbarsVisible()) {
                        willShowViews.add(mEditDoneBarLayout);
                        willShowViews.add(mEditBarLayout);
                    }
                    break;
                case ReadStateConfig.STATE_SIGNATURE:
                    if (isToolbarsVisible()) {
                        willShowViews.add(mAnnotCustomBottomBarLayout);
                        willShowViews.add(mAnnotCustomTopBarLayout);
                    }
                    break;
                case ReadStateConfig.STATE_ANNOTTOOL:
                    willShowViews.add(mEditDoneBarLayout);
                    willShowViews.add(mToolSetBarLayout);
                    ToolHandler toolHandler = ((UIExtensionsManager) mReader.getUIExtensionsManager()).getCurrentToolHandler();
                    if (toolHandler != null) {
                        mToolIconView.setImageResource(getToolIcon(toolHandler));
                        mToolNameTv.setText(getToolName(toolHandler));
                    }
                    break;
                case ReadStateConfig.STATE_REFLOW:
                case ReadStateConfig.STATE_SEARCH:
                    break;
            }
            for (View view : currentShowViews) {
                if (willShowViews.contains(view))
                    continue;
                if (newState == oldState && view.getTag(HIDE_ANIMATION_TAG) != null) {
                    view.startAnimation((AnimationSet) view.getTag(HIDE_ANIMATION_TAG));
                }
                view.setVisibility(View.INVISIBLE);
            }
            for (View view : willShowViews) {
                if (currentShowViews.contains(view))
                    continue;
                if (view.getTag(SHOW_ANIMATION_TAG) != null) {
                    view.startAnimation((Animation) view.getTag(SHOW_ANIMATION_TAG));
                }
                view.setVisibility(View.VISIBLE);
            }
            if ((mPanelManager != null && mPanelManager.getPanelWindow().isShowing())
                    || mSettingPopupWindow.isShowing()
                    || mThirdMaskCounter > 0) {
                if (mMaskView.getVisibility() != View.VISIBLE) {
                    mRootView.removeView(mMaskView);
                    mRootView.addView(mMaskView, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    mMaskView.setVisibility(View.VISIBLE);
                    if (mMaskView.getTag(SHOW_ANIMATION_TAG) != null) {
                        mMaskView.startAnimation((AnimationSet) mMaskView.getTag(SHOW_ANIMATION_TAG));
                    }
                }
            } else {
                if (mMaskView.getVisibility() != View.GONE) {
                    mMaskView.setVisibility(View.GONE);
                    if (mMaskView.getTag(HIDE_ANIMATION_TAG) != null) {
                        mMaskView.startAnimation((AnimationSet) mMaskView.getTag(HIDE_ANIMATION_TAG));
                    }
                }
            }
        }
    };

    int getToolIcon(ToolHandler toolHandler) {
        int toolIcon = R.drawable.fx_item_detail;
        String type = toolHandler.getType();
        switch (type) {
            case ToolHandler.TH_TYPE_HIGHLIGHT:
                toolIcon = R.drawable.annot_tool_prompt_highlight;
                break;
            case ToolHandler.TH_TYPE_SQUIGGLY:
                toolIcon = R.drawable.annot_tool_prompt_squiggly;
                break;
            case ToolHandler.TH_TYPE_STRIKEOUT:
                toolIcon = R.drawable.annot_tool_prompt_strikeout;
                break;
            case ToolHandler.TH_TYPE_UNDERLINE:
                toolIcon = R.drawable.annot_tool_prompt_underline;
                break;
            case ToolHandler.TH_TYPE_NOTE:
                toolIcon = R.drawable.annot_tool_prompt_text;
                break;
            case ToolHandler.TH_TYPE_CIRCLE:
                toolIcon = R.drawable.annot_tool_prompt_circle;
                break;
            case ToolHandler.TH_TYPE_SQUARE:
                toolIcon = R.drawable.annot_tool_prompt_square;
                break;
            case ToolHandler.TH_TYPE_TYPEWRITER:
                toolIcon = R.drawable.annot_tool_prompt_typwriter;
                break;
            case ToolHandler.TH_TYPR_INSERTTEXT:
                toolIcon = R.drawable.annot_tool_prompt_insert;
                break;
            case ToolHandler.TH_TYPE_REPLACE:
                toolIcon = R.drawable.annot_tool_prompt_replace;
                break;
            case ToolHandler.TH_TYPE_STAMP:
                toolIcon = R.drawable.annot_tool_prompt_stamp;
                break;
            case ToolHandler.TH_TYPE_ERASER:
                toolIcon = R.drawable.annot_tool_prompt_eraser;
                break;
            case ToolHandler.TH_TYPE_INK:
                toolIcon = R.drawable.annot_tool_prompt_pencil;
                break;
            case ToolHandler.TH_TYPE_ARROW:
                toolIcon = R.drawable.annot_tool_prompt_arrow;
                break;
            case ToolHandler.TH_TYPE_LINE:
                toolIcon = R.drawable.annot_tool_prompt_line;
                break;
            case ToolHandler.TH_TYPE_FileAttachment:
                toolIcon = R.drawable.annot_tool_prompt_fileattachment;
                break;
        }

        return toolIcon;
    }

    String getToolName(ToolHandler toolHandler) {
        String toolName = "-";
        String type = toolHandler.getType();
        switch (type) {
            case ToolHandler.TH_TYPE_HIGHLIGHT:
                toolName = "Highlight";
                break;
            case ToolHandler.TH_TYPE_SQUIGGLY:
                toolName = "Squiggly";
                break;
            case ToolHandler.TH_TYPE_STRIKEOUT:
                toolName = "Strikeout";
                break;
            case ToolHandler.TH_TYPE_UNDERLINE:
                toolName = "Underline";
                break;
            case ToolHandler.TH_TYPE_NOTE:
                toolName = "Note";
                break;
            case ToolHandler.TH_TYPE_CIRCLE:
                toolName = "Oval";
                break;
            case ToolHandler.TH_TYPE_SQUARE:
                toolName = "Rectangle";
                break;
            case ToolHandler.TH_TYPE_TYPEWRITER:
                toolName = "Typewriter";
                break;
            case ToolHandler.TH_TYPR_INSERTTEXT:
                toolName = "Insert Text";
                break;
            case ToolHandler.TH_TYPE_REPLACE:
                toolName = "Replace";
                break;
            case ToolHandler.TH_TYPE_STAMP:
                toolName = "Stamp";
                break;
            case ToolHandler.TH_TYPE_ERASER:
                toolName = "Eraser";
                break;
            case ToolHandler.TH_TYPE_INK:
                toolName = "Pencil";
                break;
            case ToolHandler.TH_TYPE_ARROW:
                toolName = "Arrow";
                break;
            case ToolHandler.TH_TYPE_LINE:
                toolName = "Line";
                break;
            case ToolHandler.TH_TYPE_FileAttachment:
                toolName = "Attachment";
                break;
        }
        return toolName;
    }
}
