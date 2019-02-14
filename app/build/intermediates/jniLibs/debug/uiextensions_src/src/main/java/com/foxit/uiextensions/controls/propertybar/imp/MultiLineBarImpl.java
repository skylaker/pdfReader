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
package com.foxit.uiextensions.controls.propertybar.imp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.propertybar.MultiLineBar;
import com.foxit.uiextensions.utils.AppDisplay;

import java.util.HashMap;
import java.util.Map;


public class MultiLineBarImpl extends ViewGroup implements MultiLineBar {
    private Context mContext;
    private ViewGroup mRootView;
    private UIExtensionsManager.Config mModulesConfig;
    private int mLight = 50;
    private boolean mDay = true;
    private boolean mSysLight = true;
    private boolean mSinglePage = true;
    private boolean mLockScreen = false;
    private boolean mIsCrop = false;
    private Map<Integer, IML_ValueChangeListener> mListeners;
    private PopupWindow mPopupWindow;

    private View mLl_root;
    private ImageView mTablet_iv_singlepage;
    private ImageView mTablet_iv_conpage;
    private ImageView mTablet_iv_thumbs;
    private ImageView mTablet_iv_reflow;
    private ImageView mTablet_iv_crop;
    private ImageView mTablet_iv_lockscreen;

    private ImageView mTablet_iv_light_small;
    private ImageView mTablet_iv_light_big;
    private SeekBar mTablet_sb_light;
    private ImageView mTablet_iv_daynight;
    private ImageView mTablet_iv_syslight;

    private TextView mTv_singlepage;
    private TextView mTv_continuepage;
    private TextView mTv_thumbs;
    private ImageView mIv_setlockscreen;

    private ImageView mIv_light_small;
    private ImageView mIv_light_big;
    private ImageView mIv_setreflow;
    private ImageView mIv_setcrop;
    private SeekBar mSb_light;
    private ImageView mIv_daynight;
    private ImageView mIv_syslight;

    public MultiLineBarImpl(Context context) {
        this(context, null);
        this.mContext = context;
        mListeners = new HashMap<Integer, IML_ValueChangeListener>();

    }

    public void init(ViewGroup viewGroup, UIExtensionsManager.Config config) {
        mRootView = viewGroup;
        mModulesConfig = config;
        initView();
    }

    public MultiLineBarImpl(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiLineBarImpl(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setProperty(int property, Object value) {
        if (AppDisplay.getInstance(mContext).isPad()) {
            if (property == MultiLineBar.TYPE_LIGHT) {
                this.mLight = (Integer) value;
                mTablet_sb_light.setProgress(this.mLight);
            } else if (property == MultiLineBar.TYPE_DAYNIGHT) {
                this.mDay = (Boolean) value;
                if (mDay) {
                    mTablet_iv_daynight.setImageResource(R.drawable.ml_daynight_day_selector);
                } else {
                    mTablet_iv_daynight.setImageResource(R.drawable.ml_daynight_night_selector);
                }
            } else if (property == MultiLineBar.TYPE_SYSLIGHT) {
                this.mSysLight = (Boolean) value;
                if (mSysLight) {
                    mTablet_iv_syslight.setImageResource(R.drawable.setting_on);
                    Rect bounds = mTablet_sb_light.getProgressDrawable().getBounds();
                    mTablet_sb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_unenable_bg));
                    mTablet_sb_light.getProgressDrawable().setBounds(bounds);
                    mTablet_sb_light.setEnabled(false);
                    mTablet_iv_light_small.setImageResource(R.drawable.ml_light_small_pressed);
                    mTablet_iv_light_big.setImageResource(R.drawable.ml_light_big_pressed);
                } else {
                    mTablet_iv_syslight.setImageResource(R.drawable.setting_off);
                    Rect bounds = mTablet_sb_light.getProgressDrawable().getBounds();
                    mTablet_sb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_bg));
                    mTablet_sb_light.getProgressDrawable().setBounds(bounds);
                    mTablet_sb_light.setEnabled(true);
                    if (mTablet_sb_light.getProgress() >= 1) {
                        mTablet_sb_light.setProgress(mTablet_sb_light.getProgress() - 1);
                        mTablet_sb_light.setProgress(mTablet_sb_light.getProgress() + 1);
                    }

                    mTablet_iv_light_small.setImageResource(R.drawable.ml_light_small);
                    mTablet_iv_light_big.setImageResource(R.drawable.ml_light_big);
                }
            } else if (property == MultiLineBar.TYPE_SINGLEPAGE) {
                this.mSinglePage = (Boolean) value;
                if (mSinglePage) {
                    mTablet_iv_singlepage.setImageResource(R.drawable.ml_iv_singlepage_pad_checked_selector);
                    mTablet_iv_singlepage.setBackgroundResource(R.drawable.ml_iv_circle_bg_checked_selector);
                    mTablet_iv_conpage.setImageResource(R.drawable.ml_iv_conpage_pad_selector);
                    mTablet_iv_conpage.setBackgroundResource(R.drawable.ml_iv_circle_bg_selector);
                } else {
                    mTablet_iv_singlepage.setImageResource(R.drawable.ml_iv_singlepage_pad_selector);
                    mTablet_iv_singlepage.setBackgroundResource(R.drawable.ml_iv_circle_bg_selector);
                    mTablet_iv_conpage.setImageResource(R.drawable.ml_iv_conpage_pad_checked_selector);
                    mTablet_iv_conpage.setBackgroundResource(R.drawable.ml_iv_circle_bg_checked_selector);
                }
            } else if (property == MultiLineBar.TYPE_LOCKSCREEN) {
                this.mLockScreen = (Boolean) value;
                if (mLockScreen) {
                    mTablet_iv_lockscreen.setImageResource(R.drawable.ml_iv_lockscreen_checked_selector);
                    mTablet_iv_lockscreen.setBackgroundResource(R.drawable.ml_iv_circle_bg_checked_selector);
                } else {
                    mTablet_iv_lockscreen.setImageResource(R.drawable.ml_iv_lockscreen_selector);
                    mTablet_iv_lockscreen.setBackgroundResource(R.drawable.ml_iv_circle_bg_selector);
                }
            } else if (property == MultiLineBar.TYPE_CROP) {
                this.mIsCrop = (Boolean) value;
                if (mIsCrop) {
                    mTablet_iv_crop.setImageResource(R.drawable.ml_iv_crop_checked_selector);
                    mTablet_iv_crop.setBackgroundResource(R.drawable.ml_iv_circle_bg_checked);

                    mTablet_iv_reflow.setEnabled(false);
                } else {
                    mTablet_iv_crop.setImageResource(R.drawable.ml_iv_crop_selector);
                    mTablet_iv_crop.setBackgroundResource(R.drawable.ml_iv_circle_bg_selector);

                    mTablet_iv_reflow.setEnabled(true);
                }
            }

        } else {
            if (property == MultiLineBar.TYPE_LIGHT) {
                this.mLight = (Integer) value;
                mSb_light.setProgress(this.mLight);
            } else if (property == MultiLineBar.TYPE_DAYNIGHT) {
                this.mDay = (Boolean) value;
                if (mDay) {
                    mIv_daynight.setImageResource(R.drawable.ml_daynight_day_selector);
                } else {
                    mIv_daynight.setImageResource(R.drawable.ml_daynight_night_selector);
                }
            } else if (property == MultiLineBar.TYPE_SYSLIGHT) {
                this.mSysLight = (Boolean) value;
                if (mSysLight) {
                    mIv_syslight.setImageResource(R.drawable.setting_on);
                    Rect bounds = mSb_light.getProgressDrawable().getBounds();
                    mSb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_unenable_bg));
                    mSb_light.getProgressDrawable().setBounds(bounds);
                    mSb_light.setEnabled(false);
                    mIv_light_small.setImageResource(R.drawable.ml_light_small_pressed);
                    mIv_light_big.setImageResource(R.drawable.ml_light_big_pressed);
                } else {
                    mIv_syslight.setImageResource(R.drawable.setting_off);
                    Rect bounds = mSb_light.getProgressDrawable().getBounds();
                    mSb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_bg));
                    mSb_light.getProgressDrawable().setBounds(bounds);
                    mSb_light.setEnabled(true);
                    if (mSb_light.getProgress() >= 1) {
                        mSb_light.setProgress(mSb_light.getProgress() - 1);
                        mSb_light.setProgress(mSb_light.getProgress() + 1);
                    }

                    mIv_light_small.setImageResource(R.drawable.ml_light_small);
                    mIv_light_big.setImageResource(R.drawable.ml_light_big);

                }
            } else if (property == MultiLineBar.TYPE_SINGLEPAGE) {
                this.mSinglePage = (Boolean) value;
                if (mSinglePage) {
                    ((LinearLayout) mTv_singlepage.getParent()).setBackgroundResource(R.drawable.ml_top_tv_bg_checked);
                    mTv_singlepage.setTextColor(Color.WHITE);
                    ((LinearLayout) mTv_continuepage.getParent()).setBackgroundResource(R.drawable.ml_top_tv_bg);
                    mTv_continuepage.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_body2_dark));

                } else {
                    ((LinearLayout) mTv_singlepage.getParent()).setBackgroundResource(R.drawable.ml_top_tv_bg);
                    mTv_singlepage.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_body2_dark));
                    ((LinearLayout) mTv_continuepage.getParent()).setBackgroundResource(R.drawable.ml_top_tv_bg_checked);
                    mTv_continuepage.setTextColor(Color.WHITE);

                }
            } else if (property == MultiLineBar.TYPE_LOCKSCREEN) {
                this.mLockScreen = (Boolean) value;
                if (mLockScreen) {
                    mIv_setlockscreen.setImageResource(R.drawable.ml_iv_lockscreen_checked_selector);
                    mIv_setlockscreen.setBackgroundResource(R.drawable.ml_iv_circle_bg_checked_selector);
                } else {
                    mIv_setlockscreen.setImageResource(R.drawable.ml_iv_lockscreen_selector);
                    mIv_setlockscreen.setBackgroundResource(R.drawable.ml_iv_circle_bg_selector);
                }
            } else if (property == MultiLineBar.TYPE_CROP) {
                this.mIsCrop = (Boolean) value;
                if (mIsCrop) {
                    mIv_setcrop.setImageResource(R.drawable.ml_iv_crop_checked_selector);
                    mIv_setcrop.setBackgroundResource(R.drawable.ml_iv_circle_bg_checked);

                    mIv_setreflow.setEnabled(false);
                } else {
                    mIv_setcrop.setImageResource(R.drawable.ml_iv_crop_selector);
                    mIv_setcrop.setBackgroundResource(R.drawable.ml_iv_circle_bg_selector);

                    mIv_setreflow.setEnabled(true);
                }
            }
        }
    }

    @Override
    public View getContentView() {
        return mLl_root;
    }

    @Override
    public void registerListener(IML_ValueChangeListener listener) {
        int type = listener.getType();
        if (!mListeners.containsKey(type)) {
            this.mListeners.put(type, listener);
        }
    }

    @Override
    public void unRegisterListener(IML_ValueChangeListener listener) {
        if (mListeners.containsKey(listener.getType())) {
            this.mListeners.remove(listener.getType());
        }
    }

    @Override
    public boolean isShowing() {
        if (mPopupWindow != null) {
            return mPopupWindow.isShowing();
        } else {
            return false;
        }
    }

    @Override
    public void show() {
        if (mPopupWindow != null && !isShowing()) {
            mPopupWindow.setFocusable(true);
            mPopupWindow.showAtLocation(mRootView, Gravity.BOTTOM, 0, 0);
        }
    }

    @Override
    public void dismiss() {
        if (mPopupWindow != null && isShowing()) {
            mPopupWindow.setFocusable(false);
            mPopupWindow.dismiss();
        }
    }


    OnClickListener mPadClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();

            if (id == R.id.ml_tablet_iv_singlepage) {
                if (mListeners.get(MultiLineBar.TYPE_SINGLEPAGE) != null && !mSinglePage) {
                    mSinglePage = true;
                    mListeners.get(MultiLineBar.TYPE_SINGLEPAGE).onValueChanged(MultiLineBar.TYPE_SINGLEPAGE, mSinglePage);
                    if (mSinglePage) {
                        mTablet_iv_singlepage.setImageResource(R.drawable.ml_iv_singlepage_pad_checked_selector);
                        mTablet_iv_singlepage.setBackgroundResource(R.drawable.ml_iv_circle_bg_checked_selector);
                        mTablet_iv_conpage.setImageResource(R.drawable.ml_iv_conpage_pad_selector);
                        mTablet_iv_conpage.setBackgroundResource(R.drawable.ml_iv_circle_bg_selector);
                    } else {
                        mTablet_iv_singlepage.setImageResource(R.drawable.ml_iv_singlepage_pad_selector);
                        mTablet_iv_singlepage.setBackgroundResource(R.drawable.ml_iv_circle_bg_selector);
                        mTablet_iv_conpage.setImageResource(R.drawable.ml_iv_conpage_pad_checked_selector);
                        mTablet_iv_conpage.setBackgroundResource(R.drawable.ml_iv_circle_bg_checked_selector);
                    }
                }
            } else if (id == R.id.ml_tablet_iv_conpage) {
                if (mListeners.get(MultiLineBar.TYPE_SINGLEPAGE) != null && mSinglePage) {
                    mSinglePage = false;
                    mListeners.get(MultiLineBar.TYPE_SINGLEPAGE).onValueChanged(MultiLineBar.TYPE_SINGLEPAGE, mSinglePage);
                    if (mSinglePage) {
                        mTablet_iv_singlepage.setImageResource(R.drawable.ml_iv_singlepage_pad_checked_selector);
                        mTablet_iv_singlepage.setBackgroundResource(R.drawable.ml_iv_circle_bg_checked_selector);
                        mTablet_iv_conpage.setImageResource(R.drawable.ml_iv_conpage_pad_selector);
                        mTablet_iv_conpage.setBackgroundResource(R.drawable.ml_iv_circle_bg_selector);
                    } else {
                        mTablet_iv_singlepage.setImageResource(R.drawable.ml_iv_singlepage_pad_selector);
                        mTablet_iv_singlepage.setBackgroundResource(R.drawable.ml_iv_circle_bg_selector);
                        mTablet_iv_conpage.setImageResource(R.drawable.ml_iv_conpage_pad_checked_selector);
                        mTablet_iv_conpage.setBackgroundResource(R.drawable.ml_iv_circle_bg_checked_selector);
                    }
                }
            } else if (id == R.id.ml_tablet_iv_thumbs) {
                if (mListeners.get(MultiLineBar.TYPE_THUMBNAIL) != null) {
                    mListeners.get(MultiLineBar.TYPE_THUMBNAIL).onValueChanged(MultiLineBar.TYPE_THUMBNAIL, 0);
                }
            } else if (id == R.id.ml_tablet_iv_lockscreen) {
                mLockScreen = false;
                if (mListeners.get(MultiLineBar.TYPE_LOCKSCREEN) != null) {
                    mListeners.get(MultiLineBar.TYPE_LOCKSCREEN).onValueChanged(MultiLineBar.TYPE_LOCKSCREEN, mLockScreen);
                    if (mLockScreen) {
                        mTablet_iv_lockscreen.setImageResource(R.drawable.ml_iv_lockscreen_checked_selector);
                        mTablet_iv_lockscreen.setBackgroundResource(R.drawable.ml_iv_circle_bg_checked_selector);
                    } else {
                        mTablet_iv_lockscreen.setImageResource(R.drawable.ml_iv_lockscreen_selector);
                        mTablet_iv_lockscreen.setBackgroundResource(R.drawable.ml_iv_circle_bg_selector);
                    }
                }
            } else if (id == R.id.ml_tablet_iv_daynight) {
                if (mListeners.get(MultiLineBar.TYPE_DAYNIGHT) != null) {
                    ImageView imageView = (ImageView) v;
                    mDay = !mDay;
                    mListeners.get(MultiLineBar.TYPE_DAYNIGHT).onValueChanged(MultiLineBar.TYPE_DAYNIGHT, mDay);
                    if (mDay) {
                        imageView.setImageResource(R.drawable.ml_daynight_day_selector);
                    } else {
                        imageView.setImageResource(R.drawable.ml_daynight_night_selector);
                    }
                }
            } else if (id == R.id.ml_tablet_iv_syslight) {
                if (mListeners.get(MultiLineBar.TYPE_SYSLIGHT) != null) {
                    mSysLight = !mSysLight;
                    mListeners.get(MultiLineBar.TYPE_SYSLIGHT).onValueChanged(MultiLineBar.TYPE_SYSLIGHT, mSysLight);
                    if (mSysLight) {
                        ((ImageView) v).setImageResource(R.drawable.setting_on);
                        Rect bounds = mTablet_sb_light.getProgressDrawable().getBounds();
                        mTablet_sb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_unenable_bg));
                        mTablet_sb_light.getProgressDrawable().setBounds(bounds);
                        mTablet_sb_light.setEnabled(false);
                        mTablet_iv_light_small.setImageResource(R.drawable.ml_light_small_pressed);
                        mTablet_iv_light_big.setImageResource(R.drawable.ml_light_big_pressed);
                    } else {
                        ((ImageView) v).setImageResource(R.drawable.setting_off);
                        Rect bounds = mTablet_sb_light.getProgressDrawable().getBounds();
                        mTablet_sb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_bg));
                        mTablet_sb_light.getProgressDrawable().setBounds(bounds);
                        mTablet_sb_light.setEnabled(true);
                        if (mTablet_sb_light.getProgress() >= 1) {
                            mTablet_sb_light.setProgress(mTablet_sb_light.getProgress() - 1);
                            mTablet_sb_light.setProgress(mTablet_sb_light.getProgress() + 1);
                        }

                        mTablet_iv_light_small.setImageResource(R.drawable.ml_light_small);
                        mTablet_iv_light_big.setImageResource(R.drawable.ml_light_big);
                    }
                }
            } else if (id == R.id.ml_tablet_iv_reflow) {
                if (mListeners.get(MultiLineBar.TYPE_REFLOW) != null) {
                    mListeners.get(MultiLineBar.TYPE_REFLOW).onValueChanged(MultiLineBar.TYPE_REFLOW, true);
                    mTablet_iv_reflow.setImageResource(R.drawable.ml_iv_reflow_selector);
                    mTablet_iv_reflow.setBackgroundResource(R.drawable.ml_iv_circle_bg_selector);
                }
            } else if (id == R.id.ml_tablet_iv_crop) {
                if (mListeners.get(MultiLineBar.TYPE_CROP) != null) {
                    mListeners.get(MultiLineBar.TYPE_CROP).onValueChanged(MultiLineBar.TYPE_CROP, true);
                }
            }
        }
    };

    OnClickListener mPhoneClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();

            if (id == R.id.ml_tv_singlepage) {
                if (mListeners.get(MultiLineBar.TYPE_SINGLEPAGE) != null && !mSinglePage) {
                    mSinglePage = true;
                    mListeners.get(MultiLineBar.TYPE_SINGLEPAGE).onValueChanged(MultiLineBar.TYPE_SINGLEPAGE, mSinglePage);
                    mTv_singlepage.setTextColor(Color.WHITE);
                    ((LinearLayout) mTv_singlepage.getParent()).setBackgroundResource(R.drawable.ml_top_tv_bg_checked);
                    mTv_continuepage.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_body2_dark));
                    ((LinearLayout) mTv_continuepage.getParent()).setBackgroundResource(R.drawable.ml_top_tv_bg);
                }
            } else if (id == R.id.ml_tv_conpage) {
                if (mListeners.get(MultiLineBar.TYPE_SINGLEPAGE) != null && mSinglePage) {
                    mSinglePage = false;
                    mListeners.get(MultiLineBar.TYPE_SINGLEPAGE).onValueChanged(MultiLineBar.TYPE_SINGLEPAGE, mSinglePage);
                    mTv_singlepage.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_body2_dark));
                    ((LinearLayout) mTv_singlepage.getParent()).setBackgroundResource(R.drawable.ml_top_tv_bg);
                    mTv_continuepage.setTextColor(Color.WHITE);
                    ((LinearLayout) mTv_continuepage.getParent()).setBackgroundResource(R.drawable.ml_top_tv_bg_checked);
                }
            } else if (id == R.id.ml_tv_thumbs) {
                if (mListeners.get(MultiLineBar.TYPE_THUMBNAIL) != null) {
                    mListeners.get(MultiLineBar.TYPE_THUMBNAIL).onValueChanged(MultiLineBar.TYPE_THUMBNAIL, 0);
                }
            } else if (id == R.id.ml_iv_lockscreen) {

                mLockScreen = false;
                if (mListeners.get(MultiLineBar.TYPE_LOCKSCREEN) != null) {
                    mListeners.get(MultiLineBar.TYPE_LOCKSCREEN).onValueChanged(MultiLineBar.TYPE_LOCKSCREEN, mLockScreen);
                    if (mLockScreen) {
                        mIv_setlockscreen.setImageResource(R.drawable.ml_iv_lockscreen_checked_selector);
                        mIv_setlockscreen.setBackgroundResource(R.drawable.ml_iv_circle_bg_checked_selector);
                    } else {
                        mIv_setlockscreen.setImageResource(R.drawable.ml_iv_lockscreen_selector);
                        mIv_setlockscreen.setBackgroundResource(R.drawable.ml_iv_circle_bg_selector);
                    }
                }
            } else if (id == R.id.ml_iv_daynight) {
                if (mListeners.get(MultiLineBar.TYPE_DAYNIGHT) != null) {
                    mDay = !mDay;
                    mListeners.get(MultiLineBar.TYPE_DAYNIGHT).onValueChanged(MultiLineBar.TYPE_DAYNIGHT, mDay);
                    ImageView imageView = (ImageView) v;
                    if (mDay) {
                        imageView.setImageResource(R.drawable.ml_daynight_day_selector);
                    } else {
                        imageView.setImageResource(R.drawable.ml_daynight_night_selector);
                    }
                }
            } else if (id == R.id.ml_iv_syslight) {
                if (mListeners.get(MultiLineBar.TYPE_SYSLIGHT) != null) {
                    mSysLight = !mSysLight;
                    mListeners.get(MultiLineBar.TYPE_SYSLIGHT).onValueChanged(MultiLineBar.TYPE_SYSLIGHT, mSysLight);
                    if (mSysLight) {
                        ((ImageView) v).setImageResource(R.drawable.setting_on);
                        Rect bounds = mSb_light.getProgressDrawable().getBounds();
                        mSb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_unenable_bg));
                        mSb_light.getProgressDrawable().setBounds(bounds);
                        mSb_light.setEnabled(false);
                        mIv_light_small.setImageResource(R.drawable.ml_light_small_pressed);
                        mIv_light_big.setImageResource(R.drawable.ml_light_big_pressed);
                    } else {
                        ((ImageView) v).setImageResource(R.drawable.setting_off);
                        Rect bounds = mSb_light.getProgressDrawable().getBounds();
                        mSb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_bg));
                        mSb_light.getProgressDrawable().setBounds(bounds);
                        mSb_light.setEnabled(true);
                        if (mSb_light.getProgress() >= 1) {
                            mSb_light.setProgress(mSb_light.getProgress() - 1);
                            mSb_light.setProgress(mSb_light.getProgress() + 1);
                        }

                        mIv_light_small.setImageResource(R.drawable.ml_light_small);
                        mIv_light_big.setImageResource(R.drawable.ml_light_big);
                    }
                }
            } else if (id == R.id.ml_iv_reflow) {
                if (mListeners.get(MultiLineBar.TYPE_REFLOW) != null) {
                    mListeners.get(MultiLineBar.TYPE_REFLOW).onValueChanged(MultiLineBar.TYPE_REFLOW, true);
                    mIv_setreflow.setImageResource(R.drawable.ml_iv_reflow_selector);
                    mIv_setreflow.setBackgroundResource(R.drawable.ml_iv_circle_bg_selector);
                }
            } else if (id == R.id.ml_iv_crop) {
                if (mListeners.get(MultiLineBar.TYPE_CROP) != null) {
                    mListeners.get(MultiLineBar.TYPE_CROP).onValueChanged(MultiLineBar.TYPE_CROP, true);
                }
            }
        }
    };

    @SuppressLint("NewApi")
    private void initView() {
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setBackgroundColor(Color.WHITE);

        if (AppDisplay.getInstance(mContext).isPad()) {
            mLl_root = LayoutInflater.from(mContext).inflate(R.layout.ml_setbar_tablet, null, false);
            mLl_root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addView(mLl_root);

            HorizontalScrollView ml_tablet_hsv_all = (HorizontalScrollView) mLl_root.findViewById(R.id.ml_tablet_hsv_all);
            ml_tablet_hsv_all.setHorizontalScrollBarEnabled(false);

            mTablet_iv_singlepage = (ImageView) mLl_root.findViewById(R.id.ml_tablet_iv_singlepage);
            mTablet_iv_conpage = (ImageView) mLl_root.findViewById(R.id.ml_tablet_iv_conpage);
            mTablet_iv_singlepage.setOnClickListener(mPadClickListener);
            mTablet_iv_conpage.setOnClickListener(mPadClickListener);

            if (mModulesConfig.isLoadThumbnail()) {
                mTablet_iv_thumbs = (ImageView) mLl_root.findViewById(R.id.ml_tablet_iv_thumbs);
                mTablet_iv_thumbs.setOnClickListener(mPadClickListener);
            } else {
                mLl_root.findViewById(R.id.ml_tablet_ll_thumbs).setVisibility(GONE);
            }

            mTablet_iv_reflow = (ImageView) mLl_root.findViewById(R.id.ml_tablet_iv_reflow);
            mTablet_iv_reflow.setOnClickListener(mPadClickListener);

            mTablet_iv_crop = (ImageView) mLl_root.findViewById(R.id.ml_tablet_iv_crop);
            mTablet_iv_crop.setOnClickListener(mPadClickListener);

            mTablet_iv_lockscreen = (ImageView) mLl_root.findViewById(R.id.ml_tablet_iv_lockscreen);
            mTablet_iv_lockscreen.setOnClickListener(mPadClickListener);

            mTablet_iv_daynight = (ImageView) mLl_root.findViewById(R.id.ml_tablet_iv_daynight);
            mTablet_iv_syslight = (ImageView) mLl_root.findViewById(R.id.ml_tablet_iv_syslight);
            mTablet_sb_light = (SeekBar) mLl_root.findViewById(R.id.ml_tablet_sb_light);
            mTablet_iv_light_small = (ImageView) mLl_root.findViewById(R.id.ml_tablet_iv_light_small);
            mTablet_iv_light_big = (ImageView) mLl_root.findViewById(R.id.ml_tablet_iv_light_big);
            mTablet_iv_daynight.setOnClickListener(mPadClickListener);
            mTablet_iv_syslight.setOnClickListener(mPadClickListener);

            if (mDay) {
                mTablet_iv_daynight.setImageResource(R.drawable.ml_daynight_day_selector);
            } else {
                mTablet_iv_daynight.setImageResource(R.drawable.ml_daynight_night_selector);
            }
            if (mSysLight) {
                mTablet_iv_syslight.setImageResource(R.drawable.setting_on);
                Rect bounds = mTablet_sb_light.getProgressDrawable().getBounds();
                mTablet_sb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_unenable_bg));
                mTablet_sb_light.getProgressDrawable().setBounds(bounds);
                mTablet_sb_light.setEnabled(false);
                mTablet_iv_light_small.setImageResource(R.drawable.ml_light_small_pressed);
                mTablet_iv_light_big.setImageResource(R.drawable.ml_light_big_pressed);
            } else {
                mTablet_iv_syslight.setImageResource(R.drawable.setting_off);
                Rect bounds = mTablet_sb_light.getProgressDrawable().getBounds();
                mTablet_sb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_bg));
                mTablet_sb_light.getProgressDrawable().setBounds(bounds);
                mTablet_sb_light.setEnabled(true);
                if (mTablet_sb_light.getProgress() >= 1) {
                    mTablet_sb_light.setProgress(mTablet_sb_light.getProgress() - 1);
                    mTablet_sb_light.setProgress(mTablet_sb_light.getProgress() + 1);
                }

                mTablet_iv_light_small.setImageResource(R.drawable.ml_light_small);
                mTablet_iv_light_big.setImageResource(R.drawable.ml_light_big);
            }

            mTablet_sb_light.setProgress(mLight);
            mTablet_sb_light.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mLight = progress;
                    if (mListeners.get(MultiLineBar.TYPE_LIGHT) != null) {
                        mListeners.get(MultiLineBar.TYPE_LIGHT).onValueChanged(MultiLineBar.TYPE_LIGHT, progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            mTablet_sb_light.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return event.getAction() == MotionEvent.ACTION_DOWN;
                }
            });

        } else {
            mLl_root = LayoutInflater.from(mContext).inflate(R.layout.ml_setbar, null, false);
            mLl_root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addView(mLl_root);

            mTv_singlepage = (TextView) mLl_root.findViewById(R.id.ml_tv_singlepage);
            mTv_continuepage = (TextView) mLl_root.findViewById(R.id.ml_tv_conpage);
            mTv_singlepage.setOnClickListener(mPhoneClickListener);
            mTv_continuepage.setOnClickListener(mPhoneClickListener);


            if (mModulesConfig.isLoadThumbnail()) {
                mTv_thumbs = (TextView) mLl_root.findViewById(R.id.ml_tv_thumbs);
                mTv_thumbs.setOnClickListener(mPhoneClickListener);
            } else {
                mLl_root.findViewById(R.id.ml_ll_thumbs).setVisibility(GONE);
            }

            mIv_setreflow = (ImageView) mLl_root.findViewById(R.id.ml_iv_reflow);
            mIv_setreflow.setOnClickListener(mPhoneClickListener);

            mIv_setcrop = (ImageView) mLl_root.findViewById(R.id.ml_iv_crop);
            mIv_setcrop.setOnClickListener(mPhoneClickListener);

            mIv_setlockscreen = (ImageView) mLl_root.findViewById(R.id.ml_iv_lockscreen);
            mIv_setlockscreen.setOnClickListener(mPhoneClickListener);

            mIv_daynight = (ImageView) mLl_root.findViewById(R.id.ml_iv_daynight);
            mIv_syslight = (ImageView) mLl_root.findViewById(R.id.ml_iv_syslight);
            mSb_light = (SeekBar) mLl_root.findViewById(R.id.ml_sb_light);
            mIv_light_small = (ImageView) mLl_root.findViewById(R.id.ml_iv_light_small);
            mIv_light_big = (ImageView) mLl_root.findViewById(R.id.ml_iv_light_big);

            mIv_daynight.setOnClickListener(mPhoneClickListener);
            mIv_syslight.setOnClickListener(mPhoneClickListener);

            if (mDay) {
                mIv_daynight.setImageResource(R.drawable.ml_daynight_day_selector);
            } else {
                mIv_daynight.setImageResource(R.drawable.ml_daynight_night_selector);
            }
            if (mSysLight) {
                mIv_syslight.setImageResource(R.drawable.setting_on);
                Rect bounds = mSb_light.getProgressDrawable().getBounds();
                mSb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_unenable_bg));
                mSb_light.getProgressDrawable().setBounds(bounds);
                mSb_light.setEnabled(false);
                mIv_light_small.setImageResource(R.drawable.ml_light_small_pressed);
                mIv_light_big.setImageResource(R.drawable.ml_light_big_pressed);
            } else {
                mIv_syslight.setImageResource(R.drawable.setting_off);
                Rect bounds = mSb_light.getProgressDrawable().getBounds();
                mSb_light.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.ml_seekbar_bg));
                mSb_light.getProgressDrawable().setBounds(bounds);
                mSb_light.setEnabled(true);
                if (mSb_light.getProgress() >= 1) {
                    mSb_light.setProgress(mSb_light.getProgress() - 1);
                    mSb_light.setProgress(mSb_light.getProgress() + 1);
                }

                mIv_light_small.setImageResource(R.drawable.ml_light_small);
                mIv_light_big.setImageResource(R.drawable.ml_light_big);
            }
            mSb_light.setProgress(mLight);
            mSb_light.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mLight = progress;
                    if (mListeners.get(MultiLineBar.TYPE_LIGHT) != null) {
                        mListeners.get(MultiLineBar.TYPE_LIGHT).onValueChanged(MultiLineBar.TYPE_LIGHT, progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }

        if (mPopupWindow == null) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            int heightPixels = displayMetrics.heightPixels;
            if (heightPixels < 480) {
                mPopupWindow = new PopupWindow(this, LayoutParams.MATCH_PARENT, heightPixels);
            } else {
                mPopupWindow = new PopupWindow(this, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            }

            mPopupWindow.setTouchable(true);
            mPopupWindow.setOutsideTouchable(true);
            mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    mListeners.get(MultiLineBar.TYPE_LIGHT).onDismiss();
                }
            });
        } else {
            mPopupWindow.setContentView(this);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int measureWidth = 0;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        switch (widthMode) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                measureWidth = widthSize;
                break;
        }

        int measureHeight = 0;
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int childCount = getChildCount();
        int totalHeight = 0;
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            int childHeight = childView.getMeasuredHeight();
            totalHeight += childHeight;
        }
        switch (heightMode) {
            case MeasureSpec.AT_MOST:
                measureHeight = totalHeight;
                break;
            case MeasureSpec.EXACTLY:
                measureHeight = heightSize;
                break;
        }

        setMeasuredDimension(measureWidth, measureHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int mTotalHeight = 0;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            int measureHeight = childView.getMeasuredHeight();
            int measuredWidth = childView.getMeasuredWidth();

            childView.layout(l, mTotalHeight, l + measuredWidth, mTotalHeight + measureHeight);
            mTotalHeight += measureHeight;
        }
    }
}