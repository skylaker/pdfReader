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
package com.foxit.uiextensions.annots.caret;

import android.content.Context;
import android.graphics.Canvas;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.utils.ToolUtil;

public class CaretModule implements Module, PropertyBar.PropertyChangeListener {

    private final Context mContext;
    private PDFViewCtrl mPdfViewCtrl;

    private CaretToolHandler mIS_ToolHandler;
    private CaretToolHandler mRP_ToolHandler;
    private CaretAnnotHandler mAnnotHandler;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public CaretModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    private ColorChangeListener mColorChangeListener = null;

    public void setColorChangeListener(ColorChangeListener listener) {
        mColorChangeListener = listener;
    }

    public interface ColorChangeListener {
        void onColorChange(int color);
    }

    @Override
    public boolean loadModule() {
        mAnnotHandler = new CaretAnnotHandler(mContext, mPdfViewCtrl);
        mIS_ToolHandler = new CaretToolHandler(mContext, mPdfViewCtrl);
        mRP_ToolHandler = new CaretToolHandler(mContext, mPdfViewCtrl);
        mAnnotHandler.setPropertyChangeListener(this);
        mIS_ToolHandler.setPropertyChangeListener(this);
        mRP_ToolHandler.setPropertyChangeListener(this);
        mAnnotHandler.setAnnotMenu(new AnnotMenuImpl(mContext, mPdfViewCtrl));
        mAnnotHandler.setPropertyBar(new PropertyBarImpl(mContext, mPdfViewCtrl));
        mIS_ToolHandler.setPropertyBar(new PropertyBarImpl(mContext, mPdfViewCtrl));
        mRP_ToolHandler.setPropertyBar(new PropertyBarImpl(mContext, mPdfViewCtrl));

        mIS_ToolHandler.init(true);
        mRP_ToolHandler.init(false);

        mAnnotHandler.setToolHandler("Replace", mRP_ToolHandler);
        mAnnotHandler.setToolHandler("Insert Text", mIS_ToolHandler);

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mIS_ToolHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mRP_ToolHandler);
            ToolUtil.registerAnnotHandler((UIExtensionsManager) mUiExtensionsManager, mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }

        mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);


        return true;
    }


    @Override
    public boolean unloadModule() {
        mRP_ToolHandler.removePropertyBarListener();
        mIS_ToolHandler.removePropertyBarListener();
        mAnnotHandler.removePropertyBarListener();

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mRP_ToolHandler);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mIS_ToolHandler);
            ToolUtil.unregisterAnnotHandler((UIExtensionsManager) mUiExtensionsManager, mAnnotHandler);
        }

        mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        return true;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_CARET;
    }

    @Override
    public void onValueChanged(long property, int value) {
        UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
        AnnotHandler currentAnnotHandler = ToolUtil.getCurrentAnnotHandler(uiExtensionsManager);
        if (property == PropertyBar.PROPERTY_COLOR || property == PropertyBar.PROPERTY_SELF_COLOR) {
            if (uiExtensionsManager.getCurrentToolHandler() == mIS_ToolHandler) {
                mIS_ToolHandler.changeCurrentColor(value);
            } else if (uiExtensionsManager.getCurrentToolHandler() == mRP_ToolHandler) {
                mRP_ToolHandler.changeCurrentColor(value);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onColorValueChanged(value);
            }
            if (mColorChangeListener != null) {
                mColorChangeListener.onColorChange(value);
            }
        } else if (property == PropertyBar.PROPERTY_OPACITY) {
            if (uiExtensionsManager.getCurrentToolHandler() == mIS_ToolHandler) {
                mIS_ToolHandler.changeCurrentOpacity(value);
            } else if (uiExtensionsManager.getCurrentToolHandler() == mRP_ToolHandler) {
                mRP_ToolHandler.changeCurrentOpacity(value);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.onOpacityValueChanged(value);
            }
        }
    }

    public ToolHandler getISToolHandler() {
        return mIS_ToolHandler;
    }

    public ToolHandler getRPToolHandler() {
        return mRP_ToolHandler;
    }

    public AnnotHandler getAnnotHandler() {
        return mAnnotHandler;
    }

    @Override
    public void onValueChanged(long property, float value) {

    }

    @Override
    public void onValueChanged(long property, String value) {

    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {

        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mAnnotHandler.onDrawForControls(canvas);
        }
    };

    PDFViewCtrl.IRecoveryEventListener memoryEventListener = new PDFViewCtrl.IRecoveryEventListener() {
        @Override
        public void onWillRecover() {
            if (mAnnotHandler.getAnnotMenu() != null && mAnnotHandler.getAnnotMenu().isShowing()) {
                mAnnotHandler.getAnnotMenu().dismiss();
            }

            if (mAnnotHandler.getPropertyBar() != null && mAnnotHandler.getPropertyBar().isShowing()) {
                mAnnotHandler.getPropertyBar().dismiss();
            }
        }

        @Override
        public void onRecovered() {
        }
    };


}
