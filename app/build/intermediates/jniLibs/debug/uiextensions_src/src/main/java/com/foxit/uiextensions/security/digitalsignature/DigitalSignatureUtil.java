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
package com.foxit.uiextensions.security.digitalsignature;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.security.certificate.CertificateFileInfo;
import com.foxit.uiextensions.security.certificate.CertificateFragment;
import com.foxit.uiextensions.security.certificate.CertificateSupport;
import com.foxit.uiextensions.security.certificate.CertificateViewSupport;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppSQLite;
import com.foxit.uiextensions.utils.Event;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class DigitalSignatureUtil implements IDigitalSignatureUtil {
    private static final String DB_TABLE_DSG_PFX = "_pfx_dsg_cert";
    private static final String PUBLISHER = "publisher";
    private static final String ISSUER = "issuer";
    private static final String SERIALNUMBER = "serial_number";
    private static final String FILEPATH = "file_path";
    private static final String CHANGEFILEPATH = "file_change_path";
    private static final String FILENAME = "file_name";
    private static final String PASSWORD = "password";
    public static final int FULLPERMCODE = 0xf3c;
    public DigitalSignatureSecurityHandler mSecurityHandler;
    public DigitalSignatureAnnotHandler mAnnotHandler;
    public CertificateSupport mSupport;
    public Context mContext;
    public PDFViewCtrl mPdfViewCtrl;
    public CertificateSupport mCertSupport;
    public CertificateViewSupport mViewSupport;
    public CertificateFileInfo mFileInfo;

    public DigitalSignatureUtil(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mCertSupport = new CertificateSupport(mContext);
        mViewSupport = new CertificateViewSupport(context, pdfViewCtrl, mCertSupport);
        mSupport = new CertificateSupport(mContext);
        mSecurityHandler = new DigitalSignatureSecurityHandler(mContext,mPdfViewCtrl, mSupport);
    }

    @Override
    public void addCertSignature(final String docPath, final String certPath, final Bitmap bitmap, final RectF rectF, final int pageIndex, final IDigitalSignatureCreateCallBack callBack) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor cursor = AppSQLite.getInstance(mContext).select(DB_TABLE_DSG_PFX, null, null, null, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String newPath = certPath + "x";
                        if (newPath.equals(cursor.getString(cursor.getColumnIndex(CHANGEFILEPATH)))) {

                            copyFile(newPath, cursor.getString(cursor.getColumnIndex(FILEPATH)));
                            final CertificateFileInfo info = new CertificateFileInfo();
                            info.serialNumber = cursor.getString(cursor.getColumnIndex(SERIALNUMBER));
                            info.issuer = cursor.getString(cursor.getColumnIndex(ISSUER));
                            info.publisher = cursor.getString(cursor.getColumnIndex(PUBLISHER));
                            info.filePath = cursor.getString(cursor.getColumnIndex(FILEPATH));
                            info.fileName = cursor.getString(cursor.getColumnIndex(FILENAME));
                            info.password = cursor.getString(cursor.getColumnIndex(PASSWORD));
                            info.isCertFile = false;
                            info.permCode = FULLPERMCODE;

                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    DigitalSignatureUtil.this.creatDSGSign(bitmap, docPath, pageIndex, rectF, callBack, info);
                                }
                            });
                            break;
                        }
                    }
                    cursor.close();
                }
            }
        }).start();

    }

    @Override
    public void addCertList(final IDigitalSignatureCallBack callBack) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mViewSupport.showAllPfxFileDialog(true, true, new CertificateFragment.ICertDialogCallback() {
                    @Override
                    public void result(boolean succeed, Object result, Bitmap forSign) {
                        if (succeed) {
                            if (result == null) return;
                            mFileInfo = (CertificateFileInfo) result;

                            String newCertPath = mContext.getFilesDir() + "/DSGCert/" + mFileInfo.fileName;
                            String changeCertPath = newCertPath + "x";
                            copyFile(mFileInfo.filePath, changeCertPath);
                            ContentValues values = new ContentValues();
                            values.put(ISSUER, mFileInfo.issuer);
                            values.put(PUBLISHER, mFileInfo.publisher);
                            values.put(SERIALNUMBER, mFileInfo.serialNumber);
                            values.put(FILEPATH, newCertPath);
                            values.put(CHANGEFILEPATH, changeCertPath);
                            values.put(FILENAME, mFileInfo.fileName);
                            values.put(PASSWORD, mFileInfo.password);
                            Cursor cursor = AppSQLite.getInstance(mContext).select(DB_TABLE_DSG_PFX, null, null, null, null, null, null);

                            AppSQLite.getInstance(mContext).insert(DB_TABLE_DSG_PFX, values);
                            if (callBack != null) {
                                callBack.onCertSelect(newCertPath, mFileInfo.fileName);
                            }
                        } else {
                            mViewSupport.dismissPfxDialog();
                        }
                    }
                });
            }
        });
    }

    public void creatDSGSign(Bitmap bitmap, final String docPath, final int pageIndex, final RectF rectF, final IDigitalSignatureCreateCallBack callBack, final CertificateFileInfo info) {

        mSecurityHandler.addSignature(docPath, info, bitmap, pageIndex, rectF, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                if (!success) {
                    Toast.makeText(mContext, AppResource.getString(mContext, R.string.dsg_sign_failed), Toast.LENGTH_SHORT).show();
                    callBack.onCreateFinish(false);
                    return;
                }

                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(rectF));

                Toast.makeText(mContext, AppResource.getString(mContext, R.string.dsg_sign_succeed), Toast.LENGTH_SHORT).show();
                callBack.onCreateFinish(true);

            }
        });

    }


    public void copyFile(String oldPath, String newPath) {
        try {
            int byteread = 0;
            File file = new File(newPath);
            if (!file.exists()) {
                InputStream inStream = new FileInputStream(oldPath);
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                while ((byteread = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

}