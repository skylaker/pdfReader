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
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.browser.adapter.FileAttachmentAdapter;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UIFileSelectDialog;
import com.foxit.uiextensions.controls.panel.PanelHost;
import com.foxit.uiextensions.controls.panel.PanelSpec;
import com.foxit.uiextensions.controls.panel.impl.PanelHostImpl;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.ToolUtil;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

public class FileSpecPanelModule implements Module, PanelSpec, FileSpecModuleCallback {
    private PDFViewCtrl mPdfViewCtrl;
    private Context mContext;
    private ViewGroup mParent;
    private AppDisplay mDisplay;
    private View mTopBarView;
    private Boolean mIsPad;
    private View mAddView;

    private View mContentView;
    private TextView mNoInfoView;
    private View listContentView;

    private ArrayList<Boolean> mItemMoreViewShow;

    private PanelHost mPanelHost;
    private PopupWindow mPanelPopupWindow = null;

    private FileSpecOpenView openView;

    private FileAttachmentAdapter fileAttachmentAdapter;
    private boolean mIsLoadAnnotation = true;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public FileSpecPanelModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        if (context == null || pdfViewCtrl == null) {
            throw new NullPointerException();
        }
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
        mParent = parent;
        this.mItemMoreViewShow = new ArrayList<Boolean>();
        mDisplay = new AppDisplay(mContext);
        mIsPad = mDisplay.isPad();
        fileAttachmentAdapter = new FileAttachmentAdapter(mContext, new ArrayList(), mPdfViewCtrl, this);
        if (uiExtensionsManager != null && uiExtensionsManager instanceof UIExtensionsManager) {
            UIExtensionsManager.Config config = ((UIExtensionsManager) uiExtensionsManager).getModulesConfig();
            mIsLoadAnnotation = config.isLoadAnnotations();
        }

    }

    public void setPanelHost(PanelHost panelHost) {
        mPanelHost = panelHost;
    }

    public PanelHost getPanelHost() {
        return mPanelHost;
    }

    public void setPopupWindow(PopupWindow window) {
        mPanelPopupWindow = window;
    }

    public PopupWindow getPopupWindow() {
        return mPanelPopupWindow;
    }

    private static final float RD_PANEL_WIDTH_SCALE_H = 0.338f;
    private static final float RD_PANEL_WIDTH_SCALE_V = 0.535f;

    public void show() {
        int viewWidth = mParent.getWidth();
        int viewHeight = mParent.getHeight();
        int height;
        int width;
        boolean bVertical = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (bVertical) {
            height = Math.max(viewWidth, viewHeight);
            width = Math.min(viewWidth, viewHeight);
        } else {
            height = Math.min(viewWidth, viewHeight);
            width = Math.max(viewWidth, viewHeight);
        }
        if (mDisplay.isPad()) {
            float scale = RD_PANEL_WIDTH_SCALE_V;
            if (width > height) {
                scale = RD_PANEL_WIDTH_SCALE_H;
            }
            width = (int) (mDisplay.getScreenWidth() * scale);
        }
        mPanelPopupWindow.setWidth(width);
        mPanelPopupWindow.setHeight(height);
        mPanelPopupWindow.setSoftInputMode(PopupWindow.INPUT_METHOD_NEEDED);
        // need this, else lock screen back will show input keyboard
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mPanelPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
        mPanelHost.setCurrentSpec(PANELSPEC_TAG_FILEATTACHMENTS);
        mPanelPopupWindow.showAtLocation(mPdfViewCtrl, Gravity.LEFT | Gravity.TOP, 0, 0);
    }

    private UIExtensionsManager.ConfigurationChangedListener mConfigurationChangedListener = new UIExtensionsManager.ConfigurationChangedListener() {
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            if (mPanelPopupWindow != null && mPanelPopupWindow.isShowing() && mPanelHost != null && mPanelHost.getCurrentSpec() == FileSpecPanelModule.this) {
                mPanelPopupWindow.dismiss();
                show();
            }
        }
    };

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            mPanelHost = ((UIExtensionsManager) mUiExtensionsManager).getPanelManager().getPanel();
            mPanelPopupWindow = ((UIExtensionsManager) mUiExtensionsManager).getPanelManager().getPanelWindow();
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        if (mPanelHost == null)
            mPanelHost = new PanelHostImpl(mContext);

        mTopBarView = View.inflate(mContext, R.layout.panel_filespec_topbar, null);
        View closeView = mTopBarView.findViewById(R.id.panel_filespec_top_close_iv);
        TextView topTitle = (TextView) mTopBarView.findViewById(R.id.rv_panel_files_pec_title);
        mAddView = mTopBarView.findViewById(R.id.panel_filespec_top_clear_tv);

        if (mIsPad) {
            closeView.setVisibility(View.GONE);
        } else {
            closeView.setVisibility(View.VISIBLE);
            closeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPanelPopupWindow.isShowing())
                        mPanelPopupWindow.dismiss();
                }
            });
        }
        View topNormalView = mTopBarView.findViewById(R.id.panel_filespec_top_normal);
        topNormalView.setVisibility(View.VISIBLE);

        if (mIsPad) {
            FrameLayout.LayoutParams topNormalLayoutParams = (FrameLayout.LayoutParams) topNormalView.getLayoutParams();
            topNormalLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
            topNormalView.setLayoutParams(topNormalLayoutParams);

            RelativeLayout.LayoutParams topCloseLayoutParams = (RelativeLayout.LayoutParams) closeView.getLayoutParams();
            topCloseLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
            closeView.setLayoutParams(topCloseLayoutParams);
            RelativeLayout.LayoutParams topClearLayoutParams = (RelativeLayout.LayoutParams) mAddView.getLayoutParams();
            topClearLayoutParams.rightMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
            mAddView.setLayoutParams(topClearLayoutParams);
        } else {
            FrameLayout.LayoutParams topNormalLayoutParams = (FrameLayout.LayoutParams) topNormalView.getLayoutParams();
            topNormalLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_phone);
            topNormalView.setLayoutParams(topNormalLayoutParams);

            RelativeLayout.LayoutParams topTitleLayoutParams = (RelativeLayout.LayoutParams) topTitle.getLayoutParams();
            topTitleLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
            topTitleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
            topTitleLayoutParams.leftMargin = mDisplay.dp2px(70.0f);
            topTitle.setLayoutParams(topTitleLayoutParams);

            RelativeLayout.LayoutParams topCloseLayoutParams = (RelativeLayout.LayoutParams) closeView.getLayoutParams();
            topCloseLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
            closeView.setLayoutParams(topCloseLayoutParams);
            RelativeLayout.LayoutParams topClearLayoutParams = (RelativeLayout.LayoutParams) mAddView.getLayoutParams();
            topClearLayoutParams.rightMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
            mAddView.setLayoutParams(topClearLayoutParams);
        }


        mContentView = View.inflate(mContext, R.layout.panel_filespec_content, null);
        mNoInfoView = (TextView) mContentView.findViewById(R.id.rv_panel_filespec_noinfo);
        listContentView = mContentView.findViewById(R.id.rv_panel_attachment_layout);
        RecyclerView mRecyclerView = (RecyclerView) mContentView.findViewById(R.id.rv_panel_filespec_list);

        if (mPanelPopupWindow == null) {
            mPanelPopupWindow = new PopupWindow(mPanelHost.getContentView(),
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, true);
            mPanelPopupWindow.setBackgroundDrawable(new ColorDrawable(0x00FFFFFF));
            mPanelPopupWindow.setAnimationStyle(R.style.View_Animation_LtoR);
            mPanelPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {

                }
            });
        }

        mAddView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fileAttachmentAdapter.reset();
                fileAttachmentAdapter.notifyDataSetChanged();
                showFileSelectDialog();
            }
        });

        listContentView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                fileAttachmentAdapter.reset();
                fileAttachmentAdapter.notifyDataSetChanged();
                return true;
            }
        });

        mRecyclerView.setAdapter(fileAttachmentAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        //mRecyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL_LIST));
        mPanelHost.addSpec(this);
//        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
//        mPdfViewCtrl.registerPageEventListener(mPageEventListener);
        DocumentManager.getInstance(mPdfViewCtrl).registerAnnotEventListener(mAnnotEventListener);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerConfigurationChangedListener(mConfigurationChangedListener);
        }
        return true;
    }

    private String mPath;
    private String name;
    private final int MAX_ATTACHMENT_FILE_SIZE = 1024 * 1024 * 300;
    private int MaxFileSize;
    private UIFileSelectDialog mfileSelectDialog;

    private void showFileSelectDialog() {
        if (mfileSelectDialog != null && mfileSelectDialog.isShowing()) return;

        MaxFileSize = MAX_ATTACHMENT_FILE_SIZE;
        Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) {
            return;
        }

        mfileSelectDialog = new UIFileSelectDialog(context);
        mfileSelectDialog.init(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isHidden() || !pathname.canRead()) return false;
                return true;
            }
        }, true);
        mfileSelectDialog.setTitle(context.getString(R.string.fx_string_open));
        mfileSelectDialog.setButton(UIMatchDialog.DIALOG_CANCEL | UIMatchDialog.DIALOG_OK);
        mfileSelectDialog.setButtonEnable(false, UIMatchDialog.DIALOG_OK);
        mfileSelectDialog.setListener(new UIMatchDialog.DialogListener() {
            @Override
            public void onResult(long btType) {
                if (btType == UIMatchDialog.DIALOG_OK) {

                    mPath = mfileSelectDialog.getSelectedFiles().get(0).path;
                    name = mfileSelectDialog.getSelectedFiles().get(0).name;
                    if (mPath == null || mPath.length() < 1) return;

                    //check file size
                    if (new File(mPath).length() > MaxFileSize) {
                        String msg = String.format(AppResource.getString(mContext, R.string.annot_fat_filesizelimit_meg),
                                MaxFileSize / (1024 * 1024));
                        Toast toast = Toast.makeText(mContext,
                                msg, Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }

                    fileAttachmentAdapter.add(name, mPath);
                    mfileSelectDialog.dismiss();
                } else if (btType == UIMatchDialog.DIALOG_CANCEL) {
                    mfileSelectDialog.dismiss();
                }
            }

            @Override
            public void onBackClick() {
            }
        });
        mfileSelectDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    mfileSelectDialog.dismiss();
                }
                return true;
            }
        });

        mfileSelectDialog.showDialog(false);
    }


    //    private PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener(){
//
//        @Override
//        public void onPagesRemoved(boolean success, int[] pageIndexes) {
//
//        }
//
//        @Override
//        public void onPageMoved(boolean success, int index, int dstIndex) {
//
//        }
//
//        @Override
//        public void onPagesInserted(boolean success, int dstIndex, int[] range) {
//
//        }
//    };
//
//    private PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
//        @Override
//        public void onDocWillOpen() {
//
//        }
//
//        @Override
//        public void onDocOpened(PDFDoc document, int errCode) {
//            if(errCode == PDFException.e_errSuccess) {
//
//            }
//        }
//
//        @Override
//        public void onDocWillClose(PDFDoc document) {
//
//        }
//
//        @Override
//        public void onDocClosed(PDFDoc document, int errCode) {
//
//        }
//
//        @Override
//        public void onDocWillSave(PDFDoc document) {
//
//        }
//
//        @Override
//        public void onDocSaved(PDFDoc document, int errCode) {
//
//        }
//    };
    private DocumentManager.AnnotEventListener mAnnotEventListener = new DocumentManager.AnnotEventListener() {
        @Override
        public void onAnnotAdded(PDFPage page, Annot annot) {
            try {
                if (annot.getType() == Annot.e_annotFileAttachment)
                    fileAttachmentAdapter.add(annot);
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAnnotDeleted(PDFPage page, Annot annot) {
            try {
                if (annot.getType() == Annot.e_annotFileAttachment)
                    fileAttachmentAdapter.deleteByOutside(annot);
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAnnotModified(PDFPage page, Annot annot) {

        }

        @Override
        public void onAnnotChanged(Annot lastAnnot, Annot currentAnnot) {

        }
    };


    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (openView == null) {
            return false;
        } else if (openView.getVisibility() == View.VISIBLE && keyCode == KeyEvent.KEYCODE_BACK) {
            openView.closeAttachment();
            openView.setVisibility(View.GONE);
            return true;
        }

        if (ToolUtil.getCurrentAnnotHandler((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()) == this) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                return true;
            }
        }

        return false;
    }


    @Override
    public boolean unloadModule() {
        mPanelHost.removeSpec(this);
//        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
//        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        DocumentManager.getInstance(mPdfViewCtrl).unregisterAnnotEventListener(mAnnotEventListener);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterConfigurationChangedListener(mConfigurationChangedListener);
        }
        return true;
    }

    @Override
    public String getName() {
        return MODULE_NAME_FILE_PANEL;
    }


    @Override
    public int getTag() {
        return PanelSpec.PANELSPEC_TAG_FILEATTACHMENTS;
    }


    @Override
    public int getIcon() {
        return R.drawable.panel_tabimg_attachment_seletor;
    }

    @Override
    public View getTopToolbar() {
        return mTopBarView;
    }

    @Override
    public View getContentView() {
        return mContentView;
    }

    @Override
    public void onActivated() {
        fileAttachmentAdapter.setPdfDoc(mPdfViewCtrl.getDoc());
        if (mNoInfoView.getVisibility() == View.VISIBLE){
            fileAttachmentAdapter.init(mIsLoadAnnotation);
        }
        openView = new FileSpecOpenView(mContext, mParent);
        mAddView.setEnabled(DocumentManager.getInstance(mPdfViewCtrl).canModifyContents());
    }

    @Override
    public void onDeactivated() {
        openView = null;
    }

    @Override
    public void success() {
        mNoInfoView.setVisibility(View.GONE);
    }

    @Override
    public void fail() {
        mNoInfoView.setVisibility(View.VISIBLE);
    }

    @Override
    public void open(String path, String filename) {
        openView.openAttachment(path, filename);
        openView.setVisibility(View.VISIBLE);
        mPanelPopupWindow.dismiss();
    }


}
