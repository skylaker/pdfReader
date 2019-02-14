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
package com.foxit.uiextensions.controls.menu;

import android.content.Context;
import android.content.res.Configuration;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.form.FormFillerModule;
import com.foxit.uiextensions.security.standard.PasswordModule;

public class MoreMenuModule implements Module {
    private Context mContext;
    private PDFViewCtrl mPdfViewer;
    private ViewGroup mParent = null;
    private MoreMenuView mMoreMenuView = null;
    private boolean mHasFormFillerModule = false;
    private boolean mHasDocInfoModule = false;
    private FormFillerModule mFormFillerModule = null;

    //for password
    private boolean mHasPasswordModule = false;
    private PasswordModule mPasswordModule = null;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
//    private IStateChangeListener mPasswordStateChangeListner;

    public MoreMenuModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewer, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewer = pdfViewer;
        mParent = parent;
        mUiExtensionsManager = uiExtensionsManager;
    }

    public MoreMenuView getView() {
        return mMoreMenuView;
    }

    @Override
    public String getName() {
        return Module.MODULE_MORE_MENU;
    }

    public void onConfigurationChanged(Configuration newConfig){
        if(mMoreMenuView != null)
            mMoreMenuView.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean loadModule() {
        if (mMoreMenuView == null) {
            mMoreMenuView = new MoreMenuView(mContext, mParent, mPdfViewer);
        }
        mMoreMenuView.initView();
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
            configDocInfoModule(((UIExtensionsManager) mUiExtensionsManager).getModuleByName(MODULE_NAME_DOCINFO));
            configFormFillerModule(((UIExtensionsManager) mUiExtensionsManager).getModuleByName(MODULE_NAME_FORMFILLER));
            configPasswordModule(((UIExtensionsManager) mUiExtensionsManager).getModuleByName(MODULE_NAME_PASSWORD));
        }

        if(mHasDocInfoModule) {
            mMoreMenuView.addDocInfoItem();
        }

        if (mHasPasswordModule) {
            mMoreMenuView.addPasswordItems(mPasswordModule);
        }

        if(mHasFormFillerModule) {
            mMoreMenuView.addFormItem(mFormFillerModule);
        }

        mPdfViewer.registerDocEventListener(mDocumentEventListener);
        mPdfViewer.registerPageEventListener(mPageEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        mPdfViewer.unregisterDocEventListener(mDocumentEventListener);
        mPdfViewer.unregisterPageEventListener(mPageEventListener);
        mDocumentEventListener = null;
        mPageEventListener = null;
        return true;
    }

    public void setFilePath(String filePath) {
        mMoreMenuView.setFilePath(filePath);
    }

    private PDFViewCtrl.IDocEventListener mDocumentEventListener = new PDFViewCtrl.IDocEventListener() {

        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode != PDFError.NO_ERROR.getCode()) {
                return;
            }
            if (mHasFormFillerModule) {
                mMoreMenuView.reloadFormItems();
            }

            if (mHasPasswordModule) {
                mMoreMenuView.reloadPasswordItem(mPasswordModule);

                try {
                    if (mPdfViewer.getDoc().getEncryptionType() == PDFDoc.e_encryptPassword) {
                        mPasswordModule.getPasswordSupport().isOwner();
                    }

//                    mRead.registerStateChangeListener(mPasswordStateChangeListner = new IStateChangeListener() {
//
//                        @Override
//                        public void onStateChanged(int oldState, int newState) {
//                            mMoreMenuView.reloadPasswordItem(mPasswordModule);
//                        }
//                    });
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onDocWillClose(PDFDoc document) {

        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
            if (errCode != PDFError.NO_ERROR.getCode()) {
                return;
            }

            if (mHasPasswordModule) {
                mPasswordModule.getPasswordSupport().setDocOpenAuthEvent(true);
                mPasswordModule.getPasswordSupport().setIsOwner(false);
//                mRead.unregisterStateChangeListener(mPasswordStateChangeListner);
            }
        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }
    };

    private PDFViewCtrl.IPageEventListener mPageEventListener = new PDFViewCtrl.IPageEventListener() {
        @Override
        public void onPageVisible(int index) {

        }

        @Override
        public void onPageInvisible(int index) {

        }

        @Override
        public void onPageChanged(int oldPageIndex, int curPageIndex) {

        }

        @Override
        public void onPageJumped() {

        }

        @Override
        public void onPagesWillRemove(int[] pageIndexes) {

        }

        @Override
        public void onPageWillMove(int index, int dstIndex) {

        }

        @Override
        public void onPagesWillRotate(int[] pageIndexes, int rotation) {

        }

        @Override
        public void onPagesRemoved(boolean success, int[] pageIndexes) {

        }

        @Override
        public void onPageMoved(boolean success, int index, int dstIndex) {

        }

        @Override
        public void onPagesRotated(boolean success, int[] pageIndexes, int rotation) {

        }

        @Override
        public void onPagesInserted(boolean success, int dstIndex, int[] pageRanges) {
            if (success) {
                if (mHasFormFillerModule) {
                    mMoreMenuView.reloadFormItems();
                }
            }
        }

        @Override
        public void onPagesWillInsert(int dstIndex, int[] pageRanges) {

        }
    };

    public void configFormFillerModule(Module module)
    {
        if(module == null)
            return;
        mHasFormFillerModule = true;
        mFormFillerModule = (FormFillerModule)module;
    }

    public void configDocInfoModule(Module module) {
        if (module == null) {
            return;
        }
        mHasDocInfoModule = true;
    }

    public void configPasswordModule(Module module) {
        if (module == null) {
            return;
        }

        mHasPasswordModule = true;
        mPasswordModule = (PasswordModule) module;
    }
}

