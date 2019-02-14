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
package com.foxit.uiextensions.modules;

import android.content.Context;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.DefaultAnnotHandler;
import com.foxit.uiextensions.utils.OnPageEventListener;
import com.foxit.uiextensions.utils.ToolUtil;


public class UndoModule implements Module {
	private Context mContext;
	private DefaultAnnotHandler	mDefAnnotHandler;

	private PDFViewCtrl mPdfViewCtrl;
	private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

	public UndoModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
		mContext = context;
		mPdfViewCtrl = pdfViewCtrl;
		mUiExtensionsManager = uiExtensionsManager;
	}

	@Override
	public String getName() {
		return MODULE_NAME_UNDO;
	}

	@Override
	public boolean loadModule() {
		mDefAnnotHandler = new DefaultAnnotHandler(mContext, mPdfViewCtrl);
		mPdfViewCtrl.registerPageEventListener(mPageEventListener);

		if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
			ToolUtil.registerAnnotHandler((UIExtensionsManager) mUiExtensionsManager, mDefAnnotHandler);
			((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
		}
		return true;
	}

	public AnnotHandler getAnnotHandler() {
		return mDefAnnotHandler;
	}

	private PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener(){
		@Override
		public void onPageMoved(boolean success, int index, int dstIndex) {
			DocumentManager.getInstance(mPdfViewCtrl).onPageMoved(success,index,dstIndex);
		}

		@Override
		public void onPagesRemoved(boolean success, int[] pageIndexes) {
			for(int i = 0; i < pageIndexes.length; i++)
				DocumentManager.getInstance(mPdfViewCtrl).onPageRemoved(success,pageIndexes[i] - i);
		}

		@Override
		public void onPagesInserted(boolean success, int dstIndex, int[] range) {
			DocumentManager.getInstance(mPdfViewCtrl).onPagesInsert(success, dstIndex, range);
		}
	};

	@Override
	public boolean unloadModule() {
		mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
		if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
			ToolUtil.unregisterAnnotHandler((UIExtensionsManager) mUiExtensionsManager, mDefAnnotHandler);
		}
		return true;
	}

	public void undo() {
		if (DocumentManager.getInstance(mPdfViewCtrl).canUndo()) {
			DocumentManager.getInstance(mPdfViewCtrl).undo();
		}
	}

	public void redo() {
		if (DocumentManager.getInstance(mPdfViewCtrl).canRedo()) {
			DocumentManager.getInstance(mPdfViewCtrl).redo();
		}
	}
}
