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
package com.foxit.uiextensions.textselect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.KeyEvent;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;

public class TextSelectModule implements Module {

    private TextSelectToolHandler mToolHandler;
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public TextSelectModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_SELECTION;
    }

    public ToolHandler getToolHandler() {
        return mToolHandler;
    }

    @Override
    public boolean loadModule() {
        mToolHandler = new TextSelectToolHandler(mContext, mPdfViewCtrl);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mToolHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
            ((UIExtensionsManager) mUiExtensionsManager).registerMenuEventListener(mMenuEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerToolHandlerChangedListener(mToolHandler.getHandlerChangedListener());
        }
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        mPdfViewCtrl.registerRecoveryEventListener(mRecoveryEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mToolHandler);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterMenuEventListener(mMenuEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandlerChangedListener(mToolHandler.getHandlerChangedListener());
        }
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        mPdfViewCtrl.unregisterRecoveryEventListener(mRecoveryEventListener);
        mToolHandler.uninit();
        return true;
    }

    public void triggerDismissMenu() {
        if (mToolHandler != null) mToolHandler.dismissMenu();
    }

    private UIExtensionsManager.MenuEventListener mMenuEventListener = new UIExtensionsManager.MenuEventListener() {
        @Override
        public void onTriggerDismissMenu() {
            if (mToolHandler != null) mToolHandler.dismissMenu();
        }
    };

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {


        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mToolHandler.onDrawForAnnotMenu(canvas);
        }
    };

    private PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {
        }

        @Override
        public void onDocOpened(PDFDoc pdfDoc, int err) {
            if (err != PDFException.e_errSuccess)
                return;
            mToolHandler.mIsEdit = false;
        }

        @Override
        public void onDocWillClose(PDFDoc pdfDoc) {
        }

        @Override
        public void onDocClosed(PDFDoc pdfDoc, int err) {
            if (err != PDFException.e_errSuccess)
                return;
            mToolHandler.mSelectInfo.clear();
            mToolHandler.mAnnotationMenu.dismiss();
        }

        @Override
        public void onDocWillSave(PDFDoc pdfDoc) {
        }

        @Override
        public void onDocSaved(PDFDoc pdfDoc, int i) {
        }
    };

    PDFViewCtrl.IRecoveryEventListener mRecoveryEventListener = new PDFViewCtrl.IRecoveryEventListener(){

        @Override
        public void onWillRecover() {
            if (mToolHandler.getAnnotationMenu() != null && mToolHandler.getAnnotationMenu().isShowing()) {
                mToolHandler.getAnnotationMenu().dismiss();
            }

            if (mToolHandler.getSelectInfo() != null) {
                mToolHandler.getSelectInfo().clear();
            }
        }

        @Override
        public void onRecovered() {

        }
    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (mToolHandler.mIsEdit) {
                mToolHandler.mIsEdit = false;
                mToolHandler.mAnnotationMenu.dismiss();

                RectF rectF = new RectF(mToolHandler.mSelectInfo.getBbox());
                mToolHandler.mSelectInfo.clear();
                if(!mPdfViewCtrl.isPageVisible(mToolHandler.mCurrentIndex))
                    return true;
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, mToolHandler.mCurrentIndex);
                RectF rF = mToolHandler.calculate(rectF, mToolHandler.mTmpRect);
                Rect rect = new Rect();
                rF.roundOut(rect);
                mToolHandler.getInvalidateRect(rect);
                mPdfViewCtrl.invalidate(rect);

                return true;
            }
        }

        return false;
    }
}
