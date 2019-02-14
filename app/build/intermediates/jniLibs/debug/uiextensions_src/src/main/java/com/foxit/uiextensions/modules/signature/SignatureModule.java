/** * Copyright (C) 2003-2017, Foxit Software Inc.. * All Rights Reserved. * <p> * http://www.foxitsoftware.com * <p> * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to * distribute any parts of Foxit Mobile PDF SDK to third party or public without permission unless an agreement * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions. * Review legal.txt for additional license and legal information. */package com.foxit.uiextensions.modules.signature;import android.content.Context;import android.graphics.Canvas;import android.view.ViewGroup;import com.foxit.sdk.PDFViewCtrl;import com.foxit.uiextensions.Module;import com.foxit.uiextensions.ToolHandler;import com.foxit.uiextensions.UIExtensionsManager;public class SignatureModule implements Module {    private Context mContext;    private ViewGroup mParent;    private PDFViewCtrl mPdfViewCtrl;    private SignatureToolHandler mToolHandler;    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;    public SignatureModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {        mContext = context;        mParent = parent;        mPdfViewCtrl = pdfViewCtrl;        mUiExtensionsManager = uiExtensionsManager;    }    public ToolHandler getToolHandler() {        return mToolHandler;    }    @Override    public String getName() {        return Module.MODULE_NAME_PSISIGNATURE;    }    @Override    public boolean loadModule() {        mToolHandler = new SignatureToolHandler(mContext,mParent, mPdfViewCtrl);        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {            ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mToolHandler);            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);        }        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);        return true;    }    @Override    public boolean unloadModule() {        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mToolHandler);        }        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);        return true;    }    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {        @Override        public void onDraw(int pageIndex, Canvas canvas) {            if(mToolHandler!= null)                mToolHandler.onDrawForControls(canvas);        }    };    public boolean onKeyBack() {        return mToolHandler.onKeyBack();    }}