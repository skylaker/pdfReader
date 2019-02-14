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
package com.foxit.uiextensions.modules.thumbnail;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.modules.PageNavigationModule;

public class ThumbnailModule implements Module {
    private final Context mContext;
    private final PDFViewCtrl mPdfView;
    private boolean mSinglePage = true;//true:SINGLE_PAGE,false:CONTINUOUS_PAGE
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    public ThumbnailModule(Context context, PDFViewCtrl pdfView, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfView = pdfView;
        mUiExtensionsManager = uiExtensionsManager;
    }

    public void show() {
        initApplyValue();
        applyValue();
        showThumbnailDialog();
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_THUMBNAIL;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        return true;
    }

    @Override
    public boolean unloadModule() {
        return true;
    }

    private void initApplyValue() {
        mSinglePage = getViewModePosition() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE;
    }

    private void applyValue() {
        if (mSinglePage) {
            mPdfView.setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_SINGLE);

        } else {
            mPdfView.setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_CONTINUOUS);
        }

        PageNavigationModule pageNumberJump = (PageNavigationModule) ((UIExtensionsManager) mUiExtensionsManager).getModuleByName(MODULE_NAME_PAGENAV);
        if (pageNumberJump != null)
            pageNumberJump.resetJumpView();
    }


    private int getViewModePosition() {
        switch (mPdfView.getPageLayoutMode()) {
            case PDFViewCtrl.PAGELAYOUTMODE_SINGLE:
                return PDFViewCtrl.PAGELAYOUTMODE_SINGLE;
            case PDFViewCtrl.PAGELAYOUTMODE_CONTINUOUS:
                return PDFViewCtrl.PAGELAYOUTMODE_CONTINUOUS;
            default:
                return PDFViewCtrl.PAGELAYOUTMODE_SINGLE;
        }
    }

    private void showThumbnailDialog() {
        if(DocumentManager.getInstance(mPdfView).getCurrentAnnot() != null){
            DocumentManager.getInstance(mPdfView).setCurrentAnnot(null);
        }

        if (mUiExtensionsManager == null) {
            return;
        }
        Activity activity = ((UIExtensionsManager)mUiExtensionsManager).getAttachedActivity();
        if (activity == null) {
            return;
        }

        if (!(activity instanceof FragmentActivity)) {
            Toast.makeText(mContext, "The attached activity is not a FragmentActivity", Toast.LENGTH_LONG);
            return;
        }

        FragmentActivity act = (FragmentActivity) activity;
        ThumbnailSupport support = (ThumbnailSupport) act.getSupportFragmentManager().findFragmentByTag("ThumbnailSupport");
        if (support == null) {
            support = new ThumbnailSupport();
        }
        support.init(mPdfView);
     
        AppDialogManager.getInstance().showAllowManager(support, act.getSupportFragmentManager(), "ThumbnailSupport", null);
    }
}



