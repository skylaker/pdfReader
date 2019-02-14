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

import android.content.Context;
import android.graphics.Canvas;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Library;
import com.foxit.sdk.common.PDFException;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.controls.menu.MoreMenuModule;
import com.foxit.uiextensions.modules.DocInfoModule;
import com.foxit.uiextensions.pdfreader.impl.PDFReader;
import com.foxit.uiextensions.security.certificate.CertificateSupport;
import com.foxit.uiextensions.security.certificate.CertificateViewSupport;
import com.foxit.uiextensions.utils.AppSQLite;
import com.foxit.uiextensions.utils.ToolUtil;

import java.io.File;
import java.util.ArrayList;

public class DigitalSignatureModule implements Module {

	private DigitalSignatureSecurityHandler mSecurityHandler;

	private DigitalSignatureAnnotHandler mAnnotHandler;

	public DigitalSignatureUtil getDSG_Util() {
		return mDigitalSignature_Util;
	}

	private DigitalSignatureUtil mDigitalSignature_Util;
	private CertificateSupport mSupport;
    private Context mContext;
	private ViewGroup mParent;
	private PDFViewCtrl mPdfViewCtrl;
	private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

	private CertificateSupport mCertSupport;
	private CertificateViewSupport mViewSupport;


	private static final String DB_TABLE_DSG_PFX 			= "_pfx_dsg_cert";
	private static final String PUBLISHER 				= "publisher";
	private static final String ISSUER 					= "issuer";
	private static final String SERIALNUMBER			= "serial_number";
	private static final String FILEPATH				= "file_path";
	private static final String CHANGEFILEPATH				= "file_change_path";
	private static final String FILENAME				= "file_name";
	private static final String PASSWORD				= "password";

	public DigitalSignatureModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
		this.mContext = context;
		this.mParent = parent;
		this.mPdfViewCtrl = pdfViewCtrl;
		mUiExtensionsManager = uiExtensionsManager;
	}

	@Override
	public String getName() {
		return Module.MODULE_NAME_DIGITALSIGNATURE;
	}


	public AnnotHandler getAnnotHandler() {
		return mAnnotHandler;
	}

    @Override
	public boolean loadModule() {
		if (!AppSQLite.getInstance(mContext).isDBOpened()) {
			AppSQLite.getInstance(mContext).openDB();
		}
		mCertSupport = new CertificateSupport(mContext);
		mViewSupport = new CertificateViewSupport(mContext, mPdfViewCtrl, mCertSupport);
		mSupport = new CertificateSupport(mContext);
		mSecurityHandler = new DigitalSignatureSecurityHandler(mContext,mPdfViewCtrl, mSupport);
		mAnnotHandler = new DigitalSignatureAnnotHandler(mContext,mParent, mPdfViewCtrl, mSecurityHandler);

		if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
			ToolUtil.registerAnnotHandler((UIExtensionsManager) mUiExtensionsManager, mAnnotHandler);
			((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
		}

		mDigitalSignature_Util = new DigitalSignatureUtil(mContext, mPdfViewCtrl);
		initDBTableForDSG();
		try {
			Library.registerDefaultSignatureHandler();
		} catch (PDFException e) {
			e.printStackTrace();
			return  false;
		}
		mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
		mPdfViewCtrl.registerRecoveryEventListener(recoveryEventListener);

		//for signature sign operation.
		mDocPathChangeListener = new DigitalSignatureModule.DocPathChangeListener() {
			@Override
			public void onDocPathChange(String newPath) {
				PDFReader pdfReader = ((PDFReader)((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(MODULE_NAME_PDFREADER));
				if(pdfReader != null){
					pdfReader.setFilePath(newPath);
					return;
				}

				MoreMenuModule module = ((MoreMenuModule) ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_MORE_MENU));
				if(module != null) {
					module.setFilePath(newPath);
					return;
				}

				DocInfoModule docInfoModule = (DocInfoModule)((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_DOCINFO);
				if (docInfoModule != null) {
					docInfoModule.setFilePath(newPath);
					return;
				}
			}
		};

		return true;
	}


	@Override
	public boolean unloadModule() {
		if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
			ToolUtil.unregisterAnnotHandler((UIExtensionsManager) mUiExtensionsManager, mAnnotHandler);
		}
		mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
		mPdfViewCtrl.unregisterRecoveryEventListener(recoveryEventListener);
		mDocPathChangeListener = null;
		mDrawEventListener = null;
		recoveryEventListener = null;
        return true;
	}


	private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {


		@Override
		public void onDraw(int pageIndex, Canvas canvas) {
			if(mAnnotHandler!= null)
				mAnnotHandler.onDrawForControls(canvas);
		}
	};

	private DocPathChangeListener mDocPathChangeListener = null;

	public void setDocPathChangeListener(DocPathChangeListener listener) {
		mDocPathChangeListener = listener;
	}

	public DocPathChangeListener getDocPathChangeListener() {
		return mDocPathChangeListener;
	}

	public interface DocPathChangeListener {
		void onDocPathChange(String newPath);
	}

	private void initDBTableForDSG() {
		if (!AppSQLite.getInstance(mContext).isTableExist(DB_TABLE_DSG_PFX)) {
			ArrayList<AppSQLite.FieldInfo> fieldInfos  = new ArrayList<AppSQLite.FieldInfo>();
			fieldInfos.add(new AppSQLite.FieldInfo(SERIALNUMBER, AppSQLite.KEY_TYPE_VARCHAR));
			fieldInfos.add(new AppSQLite.FieldInfo(ISSUER, AppSQLite.KEY_TYPE_VARCHAR));
			fieldInfos.add(new AppSQLite.FieldInfo(PUBLISHER, AppSQLite.KEY_TYPE_VARCHAR));
			fieldInfos.add(new AppSQLite.FieldInfo(FILEPATH, AppSQLite.KEY_TYPE_VARCHAR));
			fieldInfos.add(new AppSQLite.FieldInfo(CHANGEFILEPATH, AppSQLite.KEY_TYPE_VARCHAR));
			fieldInfos.add(new AppSQLite.FieldInfo(FILENAME, AppSQLite.KEY_TYPE_VARCHAR));
			fieldInfos.add(new AppSQLite.FieldInfo(PASSWORD, AppSQLite.KEY_TYPE_VARCHAR));
			AppSQLite.getInstance(mContext).createTable(DB_TABLE_DSG_PFX, fieldInfos);
		}
		String filePath = mContext.getFilesDir() + "/DSGCert";
		File file = new File(filePath);
		if(!file.exists()) {
			file.mkdirs();
		}
	}

	PDFViewCtrl.IRecoveryEventListener recoveryEventListener = new PDFViewCtrl.IRecoveryEventListener() {
		@Override
		public void onWillRecover() {

		}

		@Override
		public void onRecovered() {
			try {
				Library.registerDefaultSignatureHandler();
			} catch (PDFException e) {
			}
		}
	};
}
