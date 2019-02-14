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
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.propertybar.MultiLineBar;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.IPDFReader;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.pdfreader.impl.LifecycleEventListener;


public class BrightnessModule implements Module {
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private IPDFReader mPDFReader;
    private MultiLineBar mSettingBar;
    private boolean mLinkToSystem = true;
    private int mBrightnessSeekValue = 3;//0-255
    private boolean mNightMode = false;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public BrightnessModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_BRIGHTNESS;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            UIExtensionsManager.Config config = ((UIExtensionsManager) mUiExtensionsManager).getModulesConfig();
            if (config != null && config.isLoadDefaultReader()) {
                mPDFReader = ((UIExtensionsManager) mUiExtensionsManager).getPDFReader();
                mPDFReader.registerLifecycleListener(mLifecycleEventListener);
            }
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }

        return true;
    }

    @Override
    public boolean unloadModule() {
        if (mPDFReader != null) {
            mPDFReader.unregisterLifecycleListener(mLifecycleEventListener);
        }
        return true;
    }

    private void initValue() {
        // set value with the value of automatic brightness setting from system.
        mLinkToSystem = true;//true;
        mNightMode = false;
        mBrightnessSeekValue = getSavedBrightSeekValue();
    }

    private void initMLBarValue() {
        if (mPDFReader == null) return;
        mSettingBar = mPDFReader.getMainFrame().getSettingBar();
        if (!mNightMode) {
            mSettingBar.setProperty(MultiLineBar.TYPE_DAYNIGHT, true);
        } else {
            mSettingBar.setProperty(MultiLineBar.TYPE_DAYNIGHT, false);
        }
        mSettingBar.setProperty(MultiLineBar.TYPE_SYSLIGHT, mLinkToSystem);
        mSettingBar.setProperty(MultiLineBar.TYPE_LIGHT, mBrightnessSeekValue);
    }

    private void applyValue() {
        if (mLinkToSystem) {
            setSystemBrightness();
        } else {
            setManualBrightness();
        }
    }

    private void registerMLListener() {
        if (mPDFReader == null) return;
        mSettingBar.registerListener(mDayNightModeChangeListener);
        mSettingBar.registerListener(mLinkToSystemChangeListener);
        mSettingBar.registerListener(mBrightnessSeekValueChangeListener);
    }

    private void unRegisterMLListener() {
        if (mPDFReader == null) return;
        mSettingBar.unRegisterListener(mDayNightModeChangeListener);
        mSettingBar.unRegisterListener(mLinkToSystemChangeListener);
        mSettingBar.unRegisterListener(mBrightnessSeekValueChangeListener);
    }

    private int getSavedBrightSeekValue() {
        int progress;
        progress = getSysBrightnessProgress();
        if (progress <= 0 || progress > 255) {
            progress = (int) (0.4 * 255);
        }
        return progress;
    }

    private int getSysBrightnessProgress() {
        int progress = 3;
        // remove check isAutoBrightness;
        // so the value of "progress" will be same as the value of system screen brightness
        try {
            progress = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            progress = (int) (0.4 * 255);
        }

        return progress;
    }


    private void setSystemBrightness() {
        Activity activity = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        activity.getWindow().setAttributes(params);
    }

    private void setManualBrightness() {
        if (mBrightnessSeekValue <= 0 || mBrightnessSeekValue > 255) {
            mBrightnessSeekValue = getSysBrightnessProgress();
        }
        Activity activity = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        if (mBrightnessSeekValue < 3) {
            params.screenBrightness = 0.01f;
        } else {
            params.screenBrightness = mBrightnessSeekValue / 255.0f;
        }
        activity.getWindow().setAttributes(params);
        if (mBrightnessSeekValue < 1) {
            mBrightnessSeekValue = 1;
        }
        if (mBrightnessSeekValue > 255) {
            mBrightnessSeekValue = 255;
        }
    }

    MultiLineBar.IML_ValueChangeListener mLinkToSystemChangeListener = new MultiLineBar.IML_ValueChangeListener() {
        @Override
        public void onValueChanged(int type, Object value) {
            if (type == MultiLineBar.TYPE_SYSLIGHT) {
                mLinkToSystem = (Boolean) value;
                if (mLinkToSystem) {
                    setSystemBrightness();
                } else {
                    setManualBrightness();
                }
            }
        }

        @Override
        public void onDismiss() {

        }

        @Override
        public int getType() {
            return MultiLineBar.TYPE_SYSLIGHT;
        }
    };

    MultiLineBar.IML_ValueChangeListener mBrightnessSeekValueChangeListener = new MultiLineBar.IML_ValueChangeListener() {
        @Override
        public void onValueChanged(int type, Object value) {
            if (type == MultiLineBar.TYPE_LIGHT) {
                mBrightnessSeekValue = (Integer) value;
                if (mLinkToSystem) {
                } else {
                    if (mBrightnessSeekValue <= 1) {
                        mBrightnessSeekValue = 1;
                    }
                    setManualBrightness();
                }
            }
        }

        @Override
        public void onDismiss() {
            if (mBrightnessSeekValue < 1) {
                mBrightnessSeekValue = 1;
            }
            if (mBrightnessSeekValue > 255) {
                mBrightnessSeekValue = 255;
            }
        }

        @Override
        public int getType() {
            return MultiLineBar.TYPE_LIGHT;
        }
    };

    MultiLineBar.IML_ValueChangeListener mDayNightModeChangeListener = new MultiLineBar.IML_ValueChangeListener() {
        @Override
        public void onValueChanged(int type, Object value) {
            if (type == MultiLineBar.TYPE_DAYNIGHT) {
                if ((Boolean) value) {
                    mNightMode = false;
                    mPdfViewCtrl.setBackgroundResource(R.color.ux_bg_color_docviewer);
                } else {
                    mNightMode = true;
                    mPdfViewCtrl.setBackgroundResource(R.color.ux_bg_color_docviewer_night);
                }
                mPdfViewCtrl.setNightMode(mNightMode);
            }
        }

        @Override
        public void onDismiss() {
        }

        @Override
        public int getType() {
            return MultiLineBar.TYPE_DAYNIGHT;
        }
    };

    IStateChangeListener mWindowDismissListener = new IStateChangeListener() {
        public void onStateChanged(int oldState, int newState) {
            if (newState != oldState && oldState == ReadStateConfig.STATE_EDIT) {
                if (mBrightnessSeekValue < 1) {
                    mBrightnessSeekValue = 1;
                }
                if (mBrightnessSeekValue > 255) {
                    mBrightnessSeekValue = 255;
                }
            }
        }
    };

    private ILifecycleEventListener mLifecycleEventListener = new LifecycleEventListener() {
        @Override
        public void onCreate(Activity act, Bundle savedInstanceState) {
            super.onCreate(act, savedInstanceState);
            initValue();
        }

        @Override
        public void onStart(Activity act) {
            if (mPDFReader == null) return;
            initMLBarValue();
            applyValue();
            registerMLListener();
            mPDFReader.registerStateChangeListener(mWindowDismissListener);
        }

        @Override
        public void onStop(Activity act) {
            if (mPDFReader == null) return;
            mPDFReader.unregisterStateChangeListener(mWindowDismissListener);
        }

        @Override
        public void onDestroy(Activity act) {
            if (mPDFReader == null) return;
            unRegisterMLListener();
        }
    };
}
