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
package com.foxit.uiextensions.controls.menu;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.text.Selection;
import android.text.Spannable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.form.FormFillerModule;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UIFileSelectDialog;
import com.foxit.uiextensions.controls.filebrowser.imp.FileItem;
import com.foxit.uiextensions.modules.DocInfoModule;
import com.foxit.uiextensions.modules.DocInfoView;
import com.foxit.uiextensions.security.standard.PasswordConstants;
import com.foxit.uiextensions.security.standard.PasswordModule;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.LayoutConfig;
import com.foxit.uiextensions.utils.UIToast;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import static com.foxit.sdk.pdf.PDFDoc.e_permFillForm;

public class MoreMenuView {
    private Context mContext = null;
    private PDFViewCtrl mPdfViewCtrl = null;
    private ViewGroup mParent = null;
    private MenuViewImpl mMoreMenu = null;
    private PopupWindow mMenuPopupWindow = null;
    private ViewGroup mRootView = null;
    private String mFilePath = null;
    private MenuItemImpl mImportMenuItem = null;
    private MenuItemImpl mExportMenuItem = null;
    private MenuItemImpl mResetMenuItem = null;
    private String mExportFilePath = null;
    public MoreMenuView(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;
    }

    public void show() {
        this.showMoreMenu();
    }

    public void hide() {
        this.hideMoreMenu();
    }

    public void initView() {
        if (mMoreMenu == null) {
            mMoreMenu = new MenuViewImpl(mContext, new MenuViewImpl.MenuCallback() {
                @Override
                public void onClosed() {
                    hideMoreMenu();
                }
            });
        }
        setMoreMenuView(mMoreMenu.getContentView());
    }

    /**
     * add a menu item to more menu group, click this item will show the file information and permission.
     */
    public void addDocInfoItem() {
        MenuGroupImpl group = mMoreMenu.getMenuGroup(MoreMenuConfig.GROUP_FILE);
        if (group == null) {
            group = new MenuGroupImpl(mContext, MoreMenuConfig.GROUP_FILE, AppResource.getString(mContext, R.string.rd_menu_file));
            mMoreMenu.addMenuGroup(group);
        }

        MenuItemImpl item = new MenuItemImpl(mContext, MoreMenuConfig.ITEM_DOCINFO,
                AppResource.getString(mContext, R.string.rv_doc_info), 0,
                new MenuViewCallback() {
                    /**
                     * when click "Properties", will show the document information, and hide the current more menu.
                     */
                    @Override
                    public void onClick(MenuItemImpl item) {

                        DocInfoModule docInfoModule = (DocInfoModule)((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_DOCINFO);
                        if (docInfoModule != null) {
                            DocInfoView docInfoView = docInfoModule.getView();
                            if (docInfoView != null)
                                docInfoView.show();
                        }

                        hideMoreMenu();
                    }
                });
        mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_FILE, item);


    }
    public void setFilePath(String filePath) {
        mFilePath = filePath;
        DocInfoModule docInfoModule = (DocInfoModule)((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_DOCINFO);
        if (docInfoModule != null) {
            docInfoModule.setFilePath(mFilePath);
        }

    }

    public void addFormItem(final FormFillerModule module) {
        MenuGroupImpl group = mMoreMenu.getMenuGroup(MoreMenuConfig.GROUP_FORM);
        if (group == null) {
            group = new MenuGroupImpl(mContext, MoreMenuConfig.GROUP_FORM, "Form");
        }
        mMoreMenu.addMenuGroup(group);
        if (mImportMenuItem == null) {
            mImportMenuItem = new MenuItemImpl(
                    mContext,
                    MoreMenuConfig.ITEM_IMPORT_FORM,
                    AppResource.getString(mContext, R.string.menu_more_item_import),
                    0,
                    new MenuViewCallback() {
                        @Override
                        public void onClick(MenuItemImpl item) {
                            importFormFromXML(module);
                        }
                    });
        }
        if (mExportMenuItem == null) {
            mExportMenuItem = new MenuItemImpl(
                    mContext,
                    MoreMenuConfig.ITEM_EXPORT_FORM,
                    AppResource.getString(mContext, R.string.menu_more_item_export),
                    0,
                    new MenuViewCallback() {
                        @Override
                        public void onClick(MenuItemImpl item) {
                            exportFormToXML(module);
                        }
                    });
        }
        if (mResetMenuItem == null){
            mResetMenuItem = new MenuItemImpl(
                    mContext,
                    MoreMenuConfig.ITEM_RESET_FORM,
                    AppResource.getString(mContext, R.string.menu_more_item_reset),
                    0,
                    new MenuViewCallback() {
                        @Override
                        public void onClick(MenuItemImpl item) {
                            resetForm(module);
                        }
                    });
        }
        mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_FORM, mImportMenuItem);
        mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_FORM, mExportMenuItem);
        mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_FORM, mResetMenuItem);

    }

    protected void reloadFormItems() {
        if (mImportMenuItem != null)
            mImportMenuItem.setEnable(false);
        if (mExportMenuItem != null)
            mExportMenuItem.setEnable(false);
        if (mResetMenuItem != null)
            mResetMenuItem.setEnable(false);
        PDFDoc doc = mPdfViewCtrl.getDoc();
        try {
            if (doc != null && doc.hasForm()) {
                if ((doc.getUserPermissions() & e_permFillForm) == e_permFillForm) {
                    mImportMenuItem.setEnable(true);
                    mResetMenuItem.setEnable(true);
                }
                mExportMenuItem.setEnable(true);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    /**
     * create one popup window, which will contain the view of more menu
     *
     * @param view
     */
    private void setMoreMenuView(View view) {
        if (mMenuPopupWindow == null) {
            mMenuPopupWindow = new PopupWindow(view, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, true);
        }

        mMenuPopupWindow.setBackgroundDrawable(new ColorDrawable(0x00FFFFFF));
        mMenuPopupWindow.setAnimationStyle(R.style.View_Animation_RtoL);
        mMenuPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
            }
        });
    }

    /**
     * if rotate the screen, will reset the size of more menu.to fit the screen.
     */
    public void onConfigurationChanged(Configuration newConfig) {
        if (mMenuPopupWindow != null && mMenuPopupWindow.isShowing()) {
            updateMoreMenu();
        }
    }

    private void showMoreMenu() {
        mRootView = (ViewGroup) mParent.getChildAt(0);
        int width = AppDisplay.getInstance(mContext).getScreenWidth();
        int height = AppDisplay.getInstance(mContext).getScreenHeight();
        if (AppDisplay.getInstance(mContext).isPad()) {
            float scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_V;
            if (width > height) {
                scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_H;
            }
            width = (int) (AppDisplay.getInstance(mContext).getScreenWidth() * scale);
        }
        mMenuPopupWindow.setWidth(width);
        mMenuPopupWindow.setHeight(height);
        mMenuPopupWindow.setSoftInputMode(PopupWindow.INPUT_METHOD_NEEDED);
        mMenuPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        mMenuPopupWindow.showAtLocation(mRootView, Gravity.RIGHT | Gravity.TOP, 0, 0);
    }

    void updateMoreMenu() {
        int width = AppDisplay.getInstance(mContext).getScreenWidth();
        int height = AppDisplay.getInstance(mContext).getScreenHeight();
        if (AppDisplay.getInstance(mContext).isPad()) {
            float scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_V;
            if (width > height) {
                scale = LayoutConfig.RD_PANEL_WIDTH_SCALE_H;
            }
            width = (int)(AppDisplay.getInstance(mContext).getScreenWidth() * scale);
        }
        mMenuPopupWindow.update(width, height);
    }

    private void hideMoreMenu() {
        if (mMenuPopupWindow.isShowing()) {
            mMenuPopupWindow.dismiss();
        }
    }

    private void importFormFromXML(final FormFillerModule module)
    {
        final UIFileSelectDialog dialog = new UIFileSelectDialog(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
        dialog.init(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isHidden() || !pathname.canRead()) return false;
                if (pathname.isFile() && !pathname.getName().toLowerCase().endsWith(".xml"))
                    return false;
                return true;
            }
        }, true);
        dialog.showDialog();
        dialog.setTitle(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity().getApplicationContext().getResources().getString(R.string.formfiller_import_title));
        dialog.setButton(UIMatchDialog.DIALOG_CANCEL | UIMatchDialog.DIALOG_OK);
        dialog.setButtonEnable(false, UIMatchDialog.DIALOG_OK);
        dialog.setListener(new UIMatchDialog.DialogListener() {
            @Override
            public void onResult(long btType) {
                if (btType == UIMatchDialog.DIALOG_OK) {
                    List<FileItem> files = dialog.getSelectedFiles();

                    dialog.dismiss();
                    hideMoreMenu();
                    module.importFormFromXML(files.get(0).path);
                } else if (btType == UIMatchDialog.DIALOG_CANCEL) {
                    dialog.dismiss();
                }
            }

            @Override
            public void onBackClick() {

            }
        });
    }

    public void exportFormToXML(final FormFillerModule module) {
        hideMoreMenu();
        final UITextEditDialog dialog = new UITextEditDialog(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity()
        );
        dialog.setTitle(mContext.getResources().getString(R.string.formfiller_export_title));
        dialog.getInputEditText().setVisibility(View.VISIBLE);
        String fileNameWithoutExt = AppFileUtil.getFileNameWithoutExt(mFilePath);
        dialog.getInputEditText().setText(fileNameWithoutExt + ".xml");
        CharSequence text = dialog.getInputEditText().getText();
        if (text instanceof Spannable) {
            Spannable spanText = (Spannable) text;
            Selection.setSelection(spanText, 0, fileNameWithoutExt.length());
        }
        AppUtil.showSoftInput(dialog.getInputEditText());
        dialog.getCancelButton().setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.getOKButton().setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();

                String name = dialog.getInputEditText().getText().toString();
                if (name.toLowerCase().endsWith(".xml")) {
                    mExportFilePath = AppFileUtil.getFileFolder(mFilePath) + "/" + name;
                } else {
                    mExportFilePath = AppFileUtil.getFileFolder(mFilePath) + "/" + name + ".xml";
                }
                File file = new File(mExportFilePath);
                if (file.exists()) {

                    final UITextEditDialog rmDialog = new UITextEditDialog(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
                    rmDialog.setTitle(R.string.fm_file_exist);
                    rmDialog.getPromptTextView().setText(R.string.fx_string_filereplace_warning);
                    rmDialog.getInputEditText().setVisibility(View.GONE);
                    rmDialog.show();

                    rmDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            rmDialog.dismiss();
                            module.exportFormToXML(mExportFilePath);
                        }
                    });

                    rmDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            rmDialog.dismiss();
                            exportFormToXML(module);
                        }
                    });
                } else {
                    boolean ret = module.exportFormToXML(mExportFilePath);
                            if (!ret) {
                                UIToast.getInstance(mContext).show(mContext.getResources().getString(R.string.formfiller_export_error));
                            }
                }
            }
        });

        dialog.show();
    }
    public void resetForm(final FormFillerModule module) {
        module.resetForm();
        hideMoreMenu();
    }


    // For Password Encryption
    private MenuItemImpl enItem;
    private MenuItemImpl deItem;
    private UITextEditDialog mSwitchDialog;
    public void addPasswordItems(final PasswordModule module) {
        enItem = new MenuItemImpl(mContext, MoreMenuConfig.ITEM_PASSWORD,
                AppResource.getString(mContext, R.string.rv_doc_encrpty_standard), 0,
                new MenuViewCallback() {
                    @Override
                    public void onClick(MenuItemImpl item) {
                        try {
                            int type = mPdfViewCtrl.getDoc().getEncryptionType();
                            if (type != PDFDoc.e_encryptNone) {
                                mSwitchDialog = new UITextEditDialog(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
                                mSwitchDialog.getInputEditText().setVisibility(View.GONE);
                                mSwitchDialog.setTitle(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity().getApplicationContext().getString(R.string.rv_doc_encrypt_standard_switch_title));
                                mSwitchDialog.getPromptTextView().setText(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity().getApplicationContext().getString(R.string.rv_doc_encrypt_standard_switch_content));
                                mSwitchDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {

                                    @Override
                                    public void onClick(View v) {
                                        mSwitchDialog.dismiss();
                                    }
                                });
                                mSwitchDialog.getOKButton().setOnClickListener(new View.OnClickListener() {

                                    @Override
                                    public void onClick(View v) {
                                        mSwitchDialog.dismiss();
                                        if (module.getPasswordSupport() != null) {
                                            if (module.getPasswordSupport().getFilePath() == null) {
                                                module.getPasswordSupport().setFilePath(mFilePath);
                                            }
                                            module.getPasswordSupport().passwordManager(PasswordConstants.OPERATOR_TYPE_CREATE);
                                        }
                                    }
                                });
                                mSwitchDialog.show();
                            } else {
                                if (module.getPasswordSupport() != null) {
                                    if (module.getPasswordSupport().getFilePath() == null) {
                                        module.getPasswordSupport().setFilePath(mFilePath);
                                    }
                                    module.getPasswordSupport().passwordManager(PasswordConstants.OPERATOR_TYPE_CREATE);
                                }
                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                    }
                });

        deItem = new MenuItemImpl(
                mContext,
                MoreMenuConfig.ITEM_REMOVESECURITY_PASSWORD,
                AppResource.getString(mContext, R.string.menu_more_item_remove_encrytion),
                0,
                new MenuViewCallback() {
                    @Override
                    public void onClick(MenuItemImpl item) {
                        if (module.getPasswordSupport() != null) {
                            if (module.getPasswordSupport().getFilePath() == null) {
                                module.getPasswordSupport().setFilePath(mFilePath);
                            }
                            module.getPasswordSupport().passwordManager(PasswordConstants.OPERATOR_TYPE_REMOVE);
                        }
                    }
                });
    }

    public void reloadPasswordItem(PasswordModule module) {
        MenuGroupImpl group = mMoreMenu.getMenuGroup(MoreMenuConfig.GROUP_PROTECT);
        if (group == null) {
            group = new MenuGroupImpl(mContext,
                    MoreMenuConfig.GROUP_PROTECT,
                    AppResource.getString(mContext, R.string.menu_more_group_protect));
            mMoreMenu.addMenuGroup(group);
        }
        if (mPdfViewCtrl.getDoc() != null) {
            try {
                int encryptType = mPdfViewCtrl.getDoc().getEncryptionType();
                if (encryptType == PDFDoc.e_encryptPassword) {
                    mMoreMenu.removeMenuItem(MoreMenuConfig.GROUP_PROTECT, MoreMenuConfig.ITEM_PASSWORD);
                    mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_PROTECT, deItem);
                } else if (encryptType != PDFDoc.e_encryptNone) {
                    mMoreMenu.removeMenuItem(MoreMenuConfig.GROUP_PROTECT, MoreMenuConfig.ITEM_REMOVESECURITY_PASSWORD);
                    mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_PROTECT, enItem);
                } else {
                    mMoreMenu.removeMenuItem(MoreMenuConfig.GROUP_PROTECT, MoreMenuConfig.ITEM_REMOVESECURITY_PASSWORD);
                    mMoreMenu.addMenuItem(MoreMenuConfig.GROUP_PROTECT, enItem);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }

            if (module.getSecurityHandler().isAvailable()) {
                enItem.setEnable(true);
                deItem.setEnable(true);
            } else {
                enItem.setEnable(false);
                deItem.setEnable(false);
            }
        }
    }
}
