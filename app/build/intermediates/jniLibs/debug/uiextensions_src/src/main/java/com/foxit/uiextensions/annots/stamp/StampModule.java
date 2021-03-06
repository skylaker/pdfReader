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
import android.graphics.Canvas;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.utils.ToolUtil;

public class StampModule implements Module, PropertyBar.PropertyChangeListener {

    private StampToolHandler mToolHandlerSTP;
    private StampAnnotHandler mAnnotHandlerSTP;
    private PropertyBar mPropertyBar;

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public StampModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_STAMP;
    }

    @Override
    public boolean loadModule() {
        mPropertyBar = new PropertyBarImpl(mContext, mPdfViewCtrl);
        mPropertyBar.setPropertyChangeListener(this);
        mToolHandlerSTP = new StampToolHandler(mContext, mPdfViewCtrl);
        mToolHandlerSTP.setPropertyBar(mPropertyBar);
        mAnnotHandlerSTP = new StampAnnotHandler(mContext, mPdfViewCtrl);
        mAnnotHandlerSTP.setAnnotMenu(new AnnotMenuImpl(mContext, mPdfViewCtrl));
        mAnnotHandlerSTP.setToolHandler(mToolHandlerSTP);

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mToolHandlerSTP);
            ToolUtil.registerAnnotHandler((UIExtensionsManager) mUiExtensionsManager, mAnnotHandlerSTP);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);

        return true;
    }

    @Override
    public boolean unloadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mToolHandlerSTP);
            ToolUtil.unregisterAnnotHandler((UIExtensionsManager) mUiExtensionsManager, mAnnotHandlerSTP);
        }
        mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        return false;
    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {

        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mAnnotHandlerSTP.onDrawForControls(canvas);
        }
    };

    public ToolHandler getToolHandler() {
        return mToolHandlerSTP;
    }

    public AnnotHandler getAnnotHandler() {
        return mAnnotHandlerSTP;
    }

    @Override
    public void onValueChanged(long property, int value) {

    }

    @Override
    public void onValueChanged(long property, float value) {

    }

    @Override
    public void onValueChanged(long property, String value) {

    }

    PDFViewCtrl.IRecoveryEventListener memoryEventListener = new PDFViewCtrl.IRecoveryEventListener() {
        @Override
        public void onWillRecover() {
            if (mAnnotHandlerSTP.getAnnotMenu() != null && mAnnotHandlerSTP.getAnnotMenu().isShowing()) {
                mAnnotHandlerSTP.getAnnotMenu().dismiss();
            }

            if (mToolHandlerSTP.getPropertyBar() != null && mToolHandlerSTP.getPropertyBar().isShowing()) {
                mToolHandlerSTP.getPropertyBar().dismiss();
            }

            DocumentManager.getInstance(mPdfViewCtrl).reInit();
        }

        @Override
        public void onRecovered() {
            mToolHandlerSTP.initAnnotIconProvider();
        }
    };
}
