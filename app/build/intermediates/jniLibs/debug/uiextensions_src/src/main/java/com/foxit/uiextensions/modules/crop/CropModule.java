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

import android.content.Context;
import android.view.KeyEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.propertybar.MultiLineBar;
import com.foxit.uiextensions.pdfreader.IPDFReader;

public class CropModule implements Module {
    private Context mContext = null;
    private PDFViewCtrl mPdfViewCtrl = null;
    private ViewGroup mParent = null;
    private IPDFReader mReader;
    private MultiLineBar mSettingBar;
    private boolean mIsCropModule;
    private CropView mCropView;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public CropModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return MODULE_NAME_CROP;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
            mReader = ((UIExtensionsManager) mUiExtensionsManager).getPDFReader();
        }
        mPdfViewCtrl.registerDocEventListener(docEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        mPdfViewCtrl.unregisterDocEventListener(docEventListener);
        return true;
    }

    PDFViewCtrl.IDocEventListener docEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode != PDFError.NO_ERROR.getCode()) return;

            initValue();
            initMLBarValue();
            registerMLListener();

            mCropView = new CropView(mContext, mParent, mPdfViewCtrl);
            if (mReader == null) return;
            mCropView.setSettingBar(mSettingBar);
        }

        @Override
        public void onDocWillClose(PDFDoc document) {

        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
            unRegisterMLListener();
        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }
    };

    private void initValue() {
        // set value with the value of automatic crop page setting from system.
        mIsCropModule = false;
    }

    private void initMLBarValue() {
        if (mReader == null) return;
        mSettingBar = mReader.getMainFrame().getSettingBar();
        mSettingBar.setProperty(MultiLineBar.TYPE_CROP, mIsCropModule);
    }

    private void registerMLListener() {
        if (mReader == null) return;
        mSettingBar.registerListener(mCropChangeListener);
    }

    private void unRegisterMLListener() {
        if (mReader == null) return;
        mSettingBar.unRegisterListener(mCropChangeListener);
    }

    private MultiLineBar.IML_ValueChangeListener mCropChangeListener = new MultiLineBar.IML_ValueChangeListener() {

        @Override
        public void onValueChanged(int type, Object value) {
            if (type == MultiLineBar.TYPE_CROP) {
                mIsCropModule = (boolean) value;
                mCropView.openCropView();
                mReader.getMainFrame().hideSettingBar();
                if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() != null) {
                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                }
            }
        }

        @Override
        public void onDismiss() {

        }

        @Override
        public int getType() {
            return MultiLineBar.TYPE_CROP;
        }
    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mCropView != null) {
            return mCropView.onKeyDown(keyCode, event);
        }
        return false;
    }
}
