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
package com.foxit.uiextensions.annots.form;


import android.content.Context;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.form.Form;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.OnPageEventListener;
import com.foxit.uiextensions.utils.ToolUtil;


public class FormFillerModule implements Module, PropertyBar.PropertyChangeListener {

    private FormFillerToolHandler mToolHandler;
    private FormFillerAnnotHandler mAnnotHandler;
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;
    private Form mForm = null;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    private PDFViewCtrl.IDocEventListener mDocumentEventListener = new PDFViewCtrl.IDocEventListener() {

        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode != PDFException.e_errSuccess) {
                return;
            }
            try {
                if (document != null) {
                    boolean hasForm = document.hasForm();
                    if (!hasForm)
                        return ;
                    mForm = document.getForm();
                    mAnnotHandler.init(mForm);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDocWillClose(PDFDoc document) {
            mAnnotHandler.clear();
        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }
    };

    private PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener(){
        @Override
        public void onPagesInserted(boolean success, int dstIndex, int[] range) {
            if(!success || mAnnotHandler.hasInitialized())
                return;
            try {
                if (mPdfViewCtrl.getDoc() != null) {
                    boolean hasForm = mPdfViewCtrl.getDoc().hasForm();
                    if (!hasForm)
                        return ;
                    mForm = mPdfViewCtrl.getDoc().getForm();
                    mAnnotHandler.init(mForm);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    };



    private PDFViewCtrl.IScaleGestureEventListener mScaleGestureEventListener = new PDFViewCtrl.IScaleGestureEventListener(){

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            if (mAnnotHandler.getFormFillerAssist() != null) {
                mAnnotHandler.getFormFillerAssist().setScaling(true);
            }

            return false;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mAnnotHandler.getFormFillerAssist() != null) {
                mAnnotHandler.getFormFillerAssist().setScaling(false);
            }
        }
    };


    public FormFillerModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_FORMFILLER;
    }

    public ToolHandler getToolHandler() {
        return mToolHandler;
    }

    public AnnotHandler getAnnotHandler() {
        return mAnnotHandler;
    }

    public boolean resetForm()
    {
        try {
            return mForm.reset();
        } catch (PDFException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean exportFormToXML(String path)
    {
        try {
            return mForm.exportToXML(path);
        } catch (PDFException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean importFormFromXML(String path)
    {
        try {
            return mForm.importFromXML(path);
        } catch (PDFException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean loadModule() {
        mToolHandler = new FormFillerToolHandler(mContext, mPdfViewCtrl);
        mAnnotHandler = new FormFillerAnnotHandler(mContext, mParent, mPdfViewCtrl);

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mToolHandler);
            ToolUtil.registerAnnotHandler((UIExtensionsManager) mUiExtensionsManager, mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        mPdfViewCtrl.registerDocEventListener(mDocumentEventListener);
        mPdfViewCtrl.registerPageEventListener(mPageEventListener);
        mPdfViewCtrl.registerScaleGestureEventListener(mScaleGestureEventListener);
        mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mToolHandler);
            ToolUtil.unregisterAnnotHandler((UIExtensionsManager) mUiExtensionsManager, mAnnotHandler);
        }
        mPdfViewCtrl.unregisterDocEventListener(mDocumentEventListener);
        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        mPdfViewCtrl.unregisterScaleGestureEventListener(mScaleGestureEventListener);
        mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
        return true;
    }

    PDFViewCtrl.IRecoveryEventListener memoryEventListener = new PDFViewCtrl.IRecoveryEventListener() {
        @Override
        public void onWillRecover() {
            DocumentManager.getInstance(mPdfViewCtrl).reInit();
        }

        @Override
        public void onRecovered() {
            mToolHandler.initActionHandler();
        }
    };

    @Override
    public void onValueChanged(long property, int value) {

    }

    @Override
    public void onValueChanged(long property, float value) {

    }

    @Override
    public void onValueChanged(long property, String value) {

    }

    public boolean onKeyBack() {
        return mAnnotHandler.onKeyBack();
    }
}
