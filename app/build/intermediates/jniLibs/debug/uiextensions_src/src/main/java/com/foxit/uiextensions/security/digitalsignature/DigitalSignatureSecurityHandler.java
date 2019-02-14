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

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.CommonDefines;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.signature.Signature;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.security.certificate.CertificateFileInfo;
import com.foxit.uiextensions.security.certificate.CertificateSupport;
import com.foxit.uiextensions.security.certificate.CertificateViewSupport;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.UIToast;

import java.util.Calendar;
import java.util.TimeZone;


public class DigitalSignatureSecurityHandler {

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ProgressDialog mProgressDialog;
    private Event.Callback mCallback;
    private boolean mSuccess;
    private long mVerifyResult = 0;
    private boolean mIsFileChanged = false;
    private CertificateViewSupport mViewSupport;

    public DigitalSignatureSecurityHandler(Context context, PDFViewCtrl pdfViewCtrl, CertificateSupport support) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;

        mViewSupport = new CertificateViewSupport(context, pdfViewCtrl, support);
    }


    class AddSignatureTask extends Task {
        private int mPageIndex;
        private Bitmap mBitmap;
        private RectF mRect;
        private CertificateFileInfo mInfo;
        private String mDocPath;

        public AddSignatureTask(String docPath, CertificateFileInfo info, int pageIndex, Bitmap bitmap, RectF rect) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    if (mProgressDialog != null) {
                        if (mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                        }
                        mProgressDialog = null;
                    }

                    if (mCallback != null)
                        mCallback.result(null, mSuccess);
                }
            });
            mDocPath = docPath;
            mInfo = info;
            mPageIndex = pageIndex;
            mBitmap = bitmap;
            mRect = rect;
        }

        @Override
        protected void execute() {
            try {
                String filter = "Adobe.PPKLite";
                String subfilter = "adbe.pkcs7.detached";
                String dn = "dn";
                String location = "location";
                String reason = "reason";
                String contactInfo = "contactInfo";
                String signer = "signer";
                String text = "text";
                long state = 0;

                //set current time to dateTime.
                DateTime dateTime = new DateTime();
                Calendar c = Calendar.getInstance();
                TimeZone timeZone = c.getTimeZone();
                int offset = timeZone.getRawOffset();
                int tzHour = offset / (3600 * 1000);
                int tzMinute = (offset / 1000) % 3600;
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH) + 1;
                int day = c.get(Calendar.DATE);
                int hour = c.get(Calendar.HOUR);
                int minute = c.get(Calendar.MINUTE);
                int second = c.get(Calendar.SECOND);
                dateTime.set(year, month, day, hour, minute, second, 0, (short) tzHour, tzMinute);


                Signature signature = mPdfViewCtrl.getDoc().getPage(mPageIndex).addSignature(mRect);
                signature.setKeyValue(Signature.e_signatureKeyNameFilter, filter);
                signature.setKeyValue(Signature.e_signatureKeyNameSubFilter, subfilter);
                signature.setKeyValue(Signature.e_signatureKeyNameDN, dn);
                signature.setKeyValue(Signature.e_signatureKeyNameLocation, location);
                signature.setKeyValue(Signature.e_signatureKeyNameReason, reason);
                signature.setKeyValue(Signature.e_signatureKeyNameContactInfo, contactInfo);
                signature.setKeyValue(Signature.e_signatureKeyNameSigner, signer);
                signature.setKeyValue(Signature.e_signatureKeyNameText, text);
                signature.setSigningTime(dateTime);
                signature.setBitmap(mBitmap);
                long flags = Signature.e_signatureAPFlagBitmap;

                signature.setAppearanceFlags(flags);
                int progress = signature.startSign(mDocPath, mInfo.filePath, mInfo.password.getBytes(), Signature.e_digestSHA1, null, null);
                while (progress == CommonDefines.e_progressToBeContinued) {
                    progress = signature.continueSign();
                }
                if (progress == CommonDefines.e_progressError) {
                    mSuccess = false;
                    return;
                }
                state = signature.getState();
                if (state != Signature.e_signatureStateSigned || !signature.isSigned()) {
                    return;
                }
                mPdfViewCtrl.getDoc().closePage(mPageIndex);
                mSuccess = true;
            } catch (PDFException e) {
                mSuccess = false;
            }
        }
    }

    public void addSignature(final String docPath, final CertificateFileInfo info, final Bitmap bitmap, int pageIndex, final RectF rect, Event.Callback callback) {
        if (mPdfViewCtrl.getUIExtensionsManager() == null) {
            return;
        }
        Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) {
            return;
        }
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setMessage(mContext.getResources().getString(R.string.rv_sign_waiting));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.show();
        mCallback = callback;

        mPdfViewCtrl.addTask(new AddSignatureTask(docPath, info, pageIndex, bitmap, rect));

    }

    class VerifySignatureTask extends Task {
        private Annot mAnnot;

        public VerifySignatureTask(final Annot annot) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    if (mProgressDialog != null) {
                        if (mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                        }
                        mProgressDialog = null;
                    }

                    Signature signature = (Signature) annot;
                    int theme;
                    if (Build.VERSION.SDK_INT >= 21) {
                        theme = android.R.style.Theme_Material_Light_Dialog_NoActionBar;
                    } else if (Build.VERSION.SDK_INT >= 14) {
                        theme = android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar;
                    } else if (Build.VERSION.SDK_INT >= 11) {
                        theme = android.R.style.Theme_Holo_Light_Dialog_NoActionBar;
                    } else {
                        theme = R.style.rv_dialog_style;
                    }

                    if (mPdfViewCtrl.getUIExtensionsManager() == null) {
                        return;
                    }
                    Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                    if (context == null) {
                        return;
                    }
                    final Dialog dialog = new Dialog(context, theme);

                    View view = View.inflate(mContext, R.layout.rv_security_dsg_verify, null);
                    dialog.setContentView(view, new LayoutParams(AppDisplay.getInstance(mContext).getDialogWidth(), LayoutParams.WRAP_CONTENT));
                    TextView tv = (TextView) view.findViewById(R.id.rv_security_dsg_verify_result);
                    String resultText = "";
                    switch ((int) mVerifyResult) {
                        case Signature.e_signatureStateVerifyInvalid:
                            resultText += mContext.getString(R.string.rv_security_dsg_verify_invalid) + "\n";
                            break;
                        case Signature.e_signatureStateVerifyValid:
                            if (mIsFileChanged)
                                resultText += mContext.getString(R.string.rv_security_dsg_verify_perm) + "\n";
                            else
                                resultText += mContext.getString(R.string.rv_security_dsg_verify_valid) + "\n";
                            break;
                        case Signature.e_signatureStateVerifyErrorByteRange:
                            resultText += mContext.getString(R.string.rv_security_dsg_verify_errorByteRange) + "\n";
                            break;
                        default:
                            resultText += mContext.getString(R.string.rv_security_dsg_verify_otherState) + "\n";
                            break;
                    }

                    {

                        try {
                            resultText += mContext.getString(R.string.rv_security_dsg_cert_publisher) + AppUtil.getEntryName(signature.getCertificateInfo("Issuer"), "CN=") + "\n";
                            resultText += mContext.getString(R.string.rv_security_dsg_cert_serialNumber) + signature.getCertificateInfo("SerialNumber") + "\n";
                            resultText += mContext.getString(R.string.rv_security_dsg_cert_emailAddress) + AppUtil.getEntryName(signature.getCertificateInfo("Subject"), "E=") + "\n";
                            resultText += mContext.getString(R.string.rv_security_dsg_cert_validityStarts) + signature.getCertificateInfo("ValidPeriodFrom") + "\n";
                            resultText += mContext.getString(R.string.rv_security_dsg_cert_validityEnds) + signature.getCertificateInfo("ValidPeriodTo") + "\n";

                            String signedDate = null;

                            signedDate = mContext.getString(R.string.rv_security_dsg_cert_signedTime)
                                    + AppDmUtil.getLocalDateString(signature.getSigningTime());

                            resultText += signedDate + "\n";
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                    }
                    tv.setText(resultText);
                    dialog.setCanceledOnTouchOutside(true);
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.show();
                        }
                    });
                }
            });
            mAnnot = annot;
        }


        @Override
        protected void execute() {
            if (mAnnot == null || !(mAnnot instanceof Signature))
                return;
            Signature signature = (Signature) mAnnot;

            try {
                int progress = signature.startVerify(null, null);
                while (progress == CommonDefines.e_progressToBeContinued) {
                    progress = signature.continueVerify();
                }
                mVerifyResult = signature.getState();
                int[] byteRanges = signature.getByteRanges();
                long fileLength = mPdfViewCtrl.getDoc().getFileSize();

                if (byteRanges != null && fileLength != byteRanges[2] + byteRanges[3])
                    mIsFileChanged = true;
                else
                    mIsFileChanged = false;
            } catch (PDFException e) {
                e.printStackTrace();
                UIToast.getInstance(mContext).show(mContext.getResources().getString(R.string.rv_security_dsg_verify_error));
            }

        }
    }

    public void verifySignature(final Annot annot) throws PDFException {
        if (mPdfViewCtrl.getUIExtensionsManager() == null) {
            return;
        }
        Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) {
            return;
        }
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setMessage(mContext.getResources().getString(R.string.rv_sign_waiting));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.show();
        mPdfViewCtrl.addTask(new VerifySignatureTask(annot));
    }

}
