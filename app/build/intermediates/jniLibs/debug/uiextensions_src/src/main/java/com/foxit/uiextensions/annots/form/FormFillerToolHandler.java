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
import android.graphics.Canvas;
import android.view.MotionEvent;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.annots.AnnotActionHandler;


public class FormFillerToolHandler implements ToolHandler {
	private AnnotActionHandler mActionHandler;
	private PDFViewCtrl mPdfViewCtrl;
	private Context mContext;

	public FormFillerToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
		mContext = context;
		mPdfViewCtrl = pdfViewCtrl;
		initActionHandler();
	}

	protected void initActionHandler() {
		mActionHandler = (AnnotActionHandler) DocumentManager.getInstance(mPdfViewCtrl).getActionHandler();
		if (mActionHandler == null) {
			mActionHandler = new AnnotActionHandler(mContext);
		}
		DocumentManager.getInstance(mPdfViewCtrl).setActionHandler(mActionHandler);
	}

	@Override
	public String getType() {
		return ToolHandler.TH_TYPE_FORMFILLER;
	}

	@Override
	public void onActivate() {

	}

	@Override
	public void onDeactivate() {

	}

	@Override
	public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
		return false;
	}

	@Override
	public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
		return false;
	}

	@Override
	public void onDraw(int pageIndex, Canvas canvas) {

	}

}
