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
package com.foxit.pdfreader;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.WindowManager;

import com.foxit.App;
import com.foxit.home.R;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.home.IHomeModule;
import com.foxit.uiextensions.pdfreader.impl.MainFrame;
import com.foxit.uiextensions.pdfreader.impl.PDFReader;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppTheme;

import java.io.InputStream;

public class PDFReaderActivity extends FragmentActivity {
    public PDFReader mPDFReader;
    private boolean mLicenseValid = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLicenseValid = App.instance().checkLicense();
        if(!mLicenseValid)
            return;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        AppTheme.setThemeFullScreen(this);
        AppTheme.setThemeNeedMenuKey(this);


        InputStream stream = this.getApplicationContext().getResources().openRawResource(R.raw.uiextensions_config);
        UIExtensionsManager.Config config = new UIExtensionsManager.Config(stream);
        if (!config.isLoadDefaultReader()) {
            AlertDialog dialog = new AlertDialog.Builder(this).
                    setMessage("Default reader could not be loaded.").
                    setPositiveButton("Sure", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finishActivity();
                        }
                    }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            return;
        }
        PDFViewCtrl pdfViewerCtrl = new PDFViewCtrl(this.getApplicationContext());
        UIExtensionsManager uiExtensionsManager = new UIExtensionsManager(this.getApplicationContext(), null, pdfViewerCtrl, config);

        pdfViewerCtrl.setUIExtensionsManager(uiExtensionsManager);
        uiExtensionsManager.setAttachedActivity(this);
        uiExtensionsManager.registerModule(App.instance().getLocalModule()); // use to refresh file list
        mPDFReader = (PDFReader) uiExtensionsManager.getPDFReader();
        mPDFReader.onCreate(this, pdfViewerCtrl, savedInstanceState);
        mPDFReader.openDocument(AppFileUtil.getFilePath(this, getIntent(), IHomeModule.FILE_EXTRA), null);
        setContentView(mPDFReader.getContentView());
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(mPDFReader == null)
            return;
        if (mPDFReader.getMainFrame().getAttachedActivity() != this)
            return;
        setIntent(intent);
        mPDFReader.openDocument(AppFileUtil.getFilePath(this, intent, IHomeModule.FILE_EXTRA), null);
    }


    @Override
    protected void onStart() {
        super.onStart();
        if(mPDFReader == null)
            return;
        mPDFReader.onStart(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mPDFReader == null)
            return;
        mPDFReader.onPause(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mPDFReader == null)
            return;
        mPDFReader.onResume(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mPDFReader == null)
            return;
        mPDFReader.onStop(this);
    }

    @Override
    protected void onDestroy() {
        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(mPDFReader != null)
            mPDFReader.onDestroy(this);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(mPDFReader != null)
            mPDFReader.onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mPDFReader == null)
            return;
        ((MainFrame) mPDFReader.getMainFrame()).updateSettingBar();
        mPDFReader.onConfigurationChanged(this, newConfig);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mPDFReader != null && mPDFReader.onKeyDown(this, keyCode, event))
            return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mPDFReader != null && !mPDFReader.onPrepareOptionsMenu(this, menu))
            return false;
        return super.onPrepareOptionsMenu(menu);
    }

    private void finishActivity() {
        this.finish();
    }

}
