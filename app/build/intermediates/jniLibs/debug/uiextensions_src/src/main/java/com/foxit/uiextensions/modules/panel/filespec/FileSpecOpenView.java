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
package com.foxit.uiextensions.modules.panel.filespec;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;

/**
 * <br><time>2/5/17</time>
 *
 * @see
 */
public class FileSpecOpenView extends View{


    private View mOpenView;
    private TextView mOpenView_filenameTV;
    private ImageView mOpenView_backIV;
    private LinearLayout mOpenView_contentLy;
    private LinearLayout mOpenView_titleLy;

    private PDFViewCtrl mAttachPdfViewCtrl;


    private Context mContext;
    private ViewGroup mParent;

    public FileSpecOpenView(Context context, ViewGroup parent){
        super(context);
        this.mContext = context;
        this.mParent = parent;

        initOpenView();
    }


    private void initOpenView() {
        mOpenView = View.inflate(mContext, R.layout.attachment_view, null);
        mOpenView_titleLy = (LinearLayout) mOpenView.findViewById(R.id.attachment_view_topbar_ly);
        mOpenView_contentLy = (LinearLayout) mOpenView.findViewById(R.id.attachment_view_content_ly);
        mOpenView_backIV = (ImageView) mOpenView.findViewById(R.id.attachment_view_topbar_back);
        mOpenView_filenameTV = (TextView) mOpenView.findViewById(R.id.attachment_view_topbar_name);
        mParent.addView(mOpenView);
        mOpenView.setVisibility(View.GONE);

        int margin_left = 0;
        int margin_right = 0;
        if (AppDisplay.getInstance(mContext).isPad()) {
            margin_left = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_left_margin_pad);
            margin_right = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_right_margin_pad);
            LinearLayout.LayoutParams clp = (LinearLayout.LayoutParams) mOpenView_titleLy.getLayoutParams();
            clp.setMargins(margin_left, 0, margin_right, 0);
        }

        mOpenView_backIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOpenView.setVisibility(View.GONE);
                mAttachPdfViewCtrl.closeDoc();
            }
        });

        mOpenView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        setVisibility(GONE);

    }

    public void openAttachment(final String filePath,final String filename) {

        mAttachPdfViewCtrl = new PDFViewCtrl(mContext);

        mOpenView_filenameTV.setText(filename);
        mOpenView_contentLy.removeAllViews();
        mOpenView_contentLy.addView(mAttachPdfViewCtrl);
        mOpenView.setVisibility(View.VISIBLE);
        mOpenView_contentLy.setVisibility(View.VISIBLE);
        mAttachPdfViewCtrl.registerDocEventListener(new PDFViewCtrl.IDocEventListener() {
            @Override
            public void onDocWillOpen() {

            }

            @Override
            public void onDocOpened(PDFDoc document, int errCode) {
                if (errCode == PDFError.NO_ERROR.getCode()) {
                    mAttachPdfViewCtrl.setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_CONTINUOUS);
                    //mbAttachengOpening = true;
                } else {
                    Toast.makeText(mContext, R.string.rv_document_open_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDocWillClose(PDFDoc document) {

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
        });
        mAttachPdfViewCtrl.setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_CONTINUOUS);
        mAttachPdfViewCtrl.openDoc(filePath, null);

    }

    public void closeAttachment() {
        if (mAttachPdfViewCtrl ==null)
            return;
        mAttachPdfViewCtrl.closeDoc();
        mOpenView.setVisibility(GONE);
        setVisibility(GONE);
    }



}
