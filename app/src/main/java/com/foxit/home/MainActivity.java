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
package com.foxit.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import com.foxit.App;
import com.foxit.pdfreader.PDFReaderActivity;
import com.foxit.uiextensions.home.IHomeModule;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{

    private boolean mLicenseValid = false;
    private IHomeModule.onFileItemEventListener mOnFileItemEventListener = null;

    private int i  = 10;
    public static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLicenseValid = App.instance().checkLicense();
        if(!mLicenseValid)
            return;
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (Build.VERSION.SDK_INT >= 23) {
            int permission = ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        }

        App.instance().getLocalModule().setFileItemEventListener(mOnFileItemEventListener = new IHomeModule.onFileItemEventListener() {
            @Override
            public void onFileItemClicked(String fileExtra, String filePath) {
                onFileSelected(fileExtra, filePath);
            }
        });


        setContentView(App.instance().getLocalModule().getContentView(this.getApplicationContext()));
    }

    @Override
    protected void onDestroy() {
        if (mLicenseValid) {
            mOnFileItemEventListener = null;
            App.instance().onDestroy();
        }

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (mLicenseValid && requestCode == REQUEST_EXTERNAL_STORAGE
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            App.instance().getLocalModule().updateStoragePermissionGranted();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void onFileSelected(String fileExtra, String filePath) {
        Intent intent = new Intent();
        intent.putExtra(fileExtra, filePath);
        intent.setClass(this.getApplicationContext(), PDFReaderActivity.class);
        this.startActivity(intent);
    }
}
