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
package com.foxit.uiextensions.security.standard;

import android.view.KeyEvent;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.SecurityHandler;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.security.ISecurityHandler;
import com.foxit.uiextensions.security.ISecurityItemHandler;


public class PasswordSecurityHandler implements ISecurityHandler, ISecurityItemHandler {

	private PasswordStandardSupport mSupport;
	private PDFViewCtrl mPdfViewCtrl;

	public int[] mEncryptItems 	= null;
	public int[] mDecryptItems 	= null;

	public PasswordSecurityHandler(PasswordStandardSupport support, PDFViewCtrl pdfViewCtrl) {
		mSupport = support;
		this.mPdfViewCtrl = pdfViewCtrl;


		mEncryptItems = new int[] {
				R.string.rv_doc_encrpty_standard
		};
		mDecryptItems = new int[] {
				R.string.rv_doc_encrpty_standard_remove
		};
	}

	@Override
	public int getSupportedTypes() {
		return PDFDoc.e_encryptPassword;
	}

	@Override
	public String getName() {
		return  "Standard";
	}

	@Override
	public boolean isOwner(int securityPermission) {
		if(mSupport != null)
			return mSupport.getIsOwner();
		else
			return true;
	}

	@Override
	public boolean canPrintHighQuality(int securityPermission) {
		if (mSupport.getIsOwner()) return true;
		else return (securityPermission & PDFDoc.e_permPrintHigh) != 0;
	}

	@Override
	public boolean canPrint(int securityPermission) {
		if (mSupport.getIsOwner()) return true;
		else return ((securityPermission & PDFDoc.e_permPrintHigh) != 0);
	}

	@Override
	public boolean canCopy(int securityPermission) {
		if (mSupport.getIsOwner()) return true;
		else return (securityPermission & PDFDoc.e_permExtract) != 0;
	}

	@Override
	public boolean canCopyForAssess(int securityPermission) {
		if (mSupport.getIsOwner()) return true;
		else return (securityPermission & PDFDoc.e_permExtractAccess) != 0;
	}

	@Override
	public boolean canAssemble(int securityPermission) {
		if (mSupport.getIsOwner()) return true;
		else return (securityPermission & PDFDoc.e_permAssemble) != 0
				|| (securityPermission & PDFDoc.e_permModify) != 0;
	}

	@Override
	public boolean canFillForm(int securityPermission) {
		if (mSupport.getIsOwner()) return true;
		else return (securityPermission & PDFDoc.e_permFillForm) != 0;
	}

	@Override
	public boolean canAddAnnot(int securityPermission) {
		if (mSupport.getIsOwner()) return true;
		else return (securityPermission & PDFDoc.e_permAnnotForm) != 0;
	}

	@Override
	public boolean canModifyContents(int securityPermission) {
		if (mSupport.getIsOwner()) return true;
		else return ((securityPermission & PDFDoc.e_permModify) != 0);
	}

	@Override
	public int[] getItemIds() {
		if(mPdfViewCtrl.getDoc() == null) return null;
		try {
			int encryptType = mPdfViewCtrl.getDoc().getEncryptionType();
			if (encryptType == PDFDoc.e_encryptPassword) {
				return mDecryptItems;
			} else {
				return mEncryptItems;
			}
		} catch (PDFException e) {
			e.printStackTrace();
		}
		return mDecryptItems;
	}

	@Override
	public boolean isAvailable() {
		if (mPdfViewCtrl.getDoc() == null) return false;
		try {
			if (mPdfViewCtrl.getDoc().isXFA()) {
                return false;
            }

			if (mPdfViewCtrl.getDoc().isEncrypted() && mPdfViewCtrl.getDoc().getEncryptionType() != PDFDoc.e_encryptPassword) {
				return false;
			}

			SecurityHandler securityHandler = mPdfViewCtrl.getDoc().getSecurityHandler();
			boolean isOwner = DocumentManager.getInstance(mPdfViewCtrl).isOwner();
			if (securityHandler != null) {
				isOwner = isOwner(0);
			}
			if (!isOwner &&
					!(securityHandler == null || securityHandler.getSecurityType() == PDFDoc.e_encryptPassword)) {
				return false;
			}
			if (!DocumentManager.getInstance(mPdfViewCtrl).canModifyFile()) {
				if (DocumentManager.getInstance(mPdfViewCtrl).canSaveAsFile()) {
					return true;
				} else {
					return false;
				}
			}
			if (DocumentManager.getInstance(mPdfViewCtrl).isSign()) return false;
		} catch (PDFException e) {
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public void onActive(final int itemId) {

		DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);

		if (mPdfViewCtrl.getUIExtensionsManager() != null) {
			ToolHandler selectionTool = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
			if (selectionTool != null) selectionTool.onDeactivate();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return false;
	}

	@Override
	public boolean canSigning(int permissions) {
		if (canAddAnnot(permissions)) return true;
		if (canFillForm(permissions)) return true;
		if (canModifyContents(permissions)) return true;
		return false;
	}

}
