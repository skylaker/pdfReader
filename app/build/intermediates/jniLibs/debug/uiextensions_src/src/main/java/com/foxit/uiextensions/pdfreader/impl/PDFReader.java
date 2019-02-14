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
package com.foxit.uiextensions.pdfreader.impl;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.IUndoItem;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AbstractToolHandler;
import com.foxit.uiextensions.annots.caret.CaretModule;
import com.foxit.uiextensions.annots.caret.CaretToolHandler;
import com.foxit.uiextensions.annots.circle.CircleModule;
import com.foxit.uiextensions.annots.circle.CircleToolHandler;
import com.foxit.uiextensions.annots.fileattachment.FileAttachmentModule;
import com.foxit.uiextensions.annots.fileattachment.FileAttachmentToolHandler;
import com.foxit.uiextensions.annots.form.FormFillerModule;
import com.foxit.uiextensions.annots.freetext.typewriter.TypewriterModule;
import com.foxit.uiextensions.annots.freetext.typewriter.TypewriterToolHandler;
import com.foxit.uiextensions.annots.ink.EraserModule;
import com.foxit.uiextensions.annots.ink.InkModule;
import com.foxit.uiextensions.annots.line.LineModule;
import com.foxit.uiextensions.annots.note.NoteModule;
import com.foxit.uiextensions.annots.note.NoteToolHandler;
import com.foxit.uiextensions.annots.square.SquareModule;
import com.foxit.uiextensions.annots.square.SquareToolHandler;
import com.foxit.uiextensions.annots.stamp.StampModule;
import com.foxit.uiextensions.annots.stamp.StampToolHandler;
import com.foxit.uiextensions.annots.textmarkup.highlight.HighlightModule;
import com.foxit.uiextensions.annots.textmarkup.highlight.HighlightToolHandler;
import com.foxit.uiextensions.annots.textmarkup.squiggly.SquigglyModule;
import com.foxit.uiextensions.annots.textmarkup.squiggly.SquigglyToolHandler;
import com.foxit.uiextensions.annots.textmarkup.strikeout.StrikeoutModule;
import com.foxit.uiextensions.annots.textmarkup.strikeout.StrikeoutToolHandler;
import com.foxit.uiextensions.annots.textmarkup.underline.UnderlineModule;
import com.foxit.uiextensions.annots.textmarkup.underline.UnderlineToolHandler;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UIFolderSelectDialog;
import com.foxit.uiextensions.controls.menu.MoreMenuModule;
import com.foxit.uiextensions.controls.propertybar.MoreTools;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.BaseItem;
import com.foxit.uiextensions.controls.toolbar.CircleItem;
import com.foxit.uiextensions.controls.toolbar.PropertyCircleItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.CircleItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.PropertyCircleItemImp;
import com.foxit.uiextensions.home.local.LocalModule;
import com.foxit.uiextensions.modules.PageNavigationModule;
import com.foxit.uiextensions.modules.ReadingBookmarkModule;
import com.foxit.uiextensions.modules.SearchModule;
import com.foxit.uiextensions.modules.SearchView;
import com.foxit.uiextensions.modules.UndoModule;
import com.foxit.uiextensions.modules.crop.CropModule;
import com.foxit.uiextensions.modules.panel.filespec.FileSpecPanelModule;
import com.foxit.uiextensions.modules.signature.SignatureModule;
import com.foxit.uiextensions.modules.signature.SignatureToolHandler;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.IMainFrame;
import com.foxit.uiextensions.pdfreader.IPDFReader;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.UIToast;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;


public class PDFReader implements IPDFReader, Module {

    private final static String TAG = PDFReader.class.getSimpleName();
    private Context mContext;
    private RelativeLayout mActivityLayout;
    private MainFrame mMainFrame;
    private DocumentManager mDocMgr;
    private PDFViewCtrl mDocViewerCtrl;
    private String mLanguage;
    private AlertDialog mSaveAlertDlg;
    private String mDocPath;
    protected boolean bDocClosed = false;
    private String currentFileCachePath;
    private boolean isSaveDocInCurPath = false;
    private boolean mPasswordError = false;

    private ProgressDialog mProgressDlg;
    private String mSavePath = null;


    private ArrayList<ILifecycleEventListener> mLifecycleEventList;
    private ArrayList<IStateChangeListener> mStateChangeEventList;

    private int mState = ReadStateConfig.STATE_NORMAL;

    private static PDFReader mPDFReader;
    private UIExtensionsManager.Config mModulesConfig;
    public PDFReader(Context context,PDFViewCtrl pdfViewCtrl, UIExtensionsManager.Config config) {
        mLifecycleEventList = new ArrayList<ILifecycleEventListener>();
        mStateChangeEventList = new ArrayList<IStateChangeListener>();
        mContext = context;
        mPDFReader = this;
        mModulesConfig = config;
        mDocViewerCtrl = pdfViewCtrl;
    }

    public void release() {
        mLifecycleEventList.clear();
        mStateChangeEventList.clear();

        mActivityLayout.removeAllViews();
        mActivityLayout = null;

        mContext = null;
        ((UIExtensionsManager)mDocViewerCtrl.getUIExtensionsManager()).destroy();;
        mDocMgr = null;
        mDocViewerCtrl = null;
        mMainFrame.release();
        mMainFrame = null;
        mPDFReader = null;
    }

    @Override
    public void init() {
        mDocMgr = DocumentManager.getInstance(mDocViewerCtrl);

        if (mActivityLayout == null) {
            mActivityLayout = new RelativeLayout(mContext);
        } else {
            mActivityLayout.removeAllViews();
            mActivityLayout = new RelativeLayout(mContext);
        }
        mActivityLayout.setId(R.id.rd_main_id);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        mMainFrame.init(this);
        mMainFrame.addDocView(mDocViewerCtrl);
        mActivityLayout.addView(mPDFReader.getMainFrame().getContentView(), params);
        mDocViewerCtrl.registerTouchEventListener(new PDFViewCtrl.ITouchEventListener() {
            @Override
            public boolean onTouchEvent(MotionEvent motionEvent) {
                return false;
            }
        });
        mDocViewerCtrl.registerDoubleTapEventListener(new PDFViewCtrl.IDoubleTapEventListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                if (getMainFrame().isToolbarsVisible()) {
                    getMainFrame().hideToolbars();
                } else {
                    getMainFrame().showToolbars();
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent motionEvent) {
                if (getMainFrame().isToolbarsVisible()) {
                    getMainFrame().hideToolbars();
                }
                return false;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent motionEvent) {
                return false;
            }
        });

        mDocViewerCtrl.registerRecoveryEventListener(new PDFViewCtrl.IRecoveryEventListener() {
            @Override
            public void onWillRecover() {
                synchronized (AppFileUtil.getInstance().isOOMHappened) {
                    AppFileUtil.getInstance().isOOMHappened = true;
                }
            }

            @Override
            public void onRecovered() {
                AppFileUtil.getInstance().isOOMHappened = false;
            }
        });

        mDocViewerCtrl.registerDocEventListener(new PDFViewCtrl.IDocEventListener() {
            @Override
            public void onDocWillOpen() {

            }

            @Override
            public void onDocOpened(PDFDoc pdfDoc, int errCode) {
                if (mProgressDlg != null && mProgressDlg.isShowing()) {
                    mProgressDlg.dismiss();
                    mProgressDlg = null;
                }

                PDFError error = PDFError.valueOf(errCode);
                switch (error) {
                    case NO_ERROR:
                        bDocClosed = false;
                        mPasswordError = false;
                        changeState(ReadStateConfig.STATE_NORMAL);
                        return;
                    case PASSWORD_INVALID:
                        String tips;
                        if (mPasswordError) {
                            tips = AppResource.getString(mContext, R.string.rv_tips_password_error);
                        } else {
                            tips = AppResource.getString(mContext, R.string.rv_tips_password);
                        }
                        final UITextEditDialog uiTextEditDialog = new UITextEditDialog(mMainFrame.getAttachedActivity());
                        uiTextEditDialog.getDialog().setCanceledOnTouchOutside(false);
                        uiTextEditDialog.getInputEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        uiTextEditDialog.setTitle(R.string.fx_string_passwordDialog_title);
                        uiTextEditDialog.getPromptTextView().setText(tips);
                        uiTextEditDialog.show();
                        AppUtil.showSoftInput(uiTextEditDialog.getInputEditText());
                        uiTextEditDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                uiTextEditDialog.dismiss();
                                AppUtil.dismissInputSoft(uiTextEditDialog.getInputEditText());
                                String pw = uiTextEditDialog.getInputEditText().getText().toString();
                                mDocViewerCtrl.openDoc(mDocPath, pw.getBytes());
                            }
                        });

                        uiTextEditDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                uiTextEditDialog.dismiss();
                                AppUtil.dismissInputSoft(uiTextEditDialog.getInputEditText());
                                mPasswordError = false;
                                bDocClosed = true;
                                openDocumentFailed();
                            }
                        });

                        uiTextEditDialog.getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
                            @Override
                            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                if (keyCode == KeyEvent.KEYCODE_BACK) {
                                    uiTextEditDialog.getDialog().cancel();
                                    mPasswordError = false;
                                    bDocClosed = true;
                                    openDocumentFailed();
                                    return true;
                                }
                                return false;
                            }
                        });


                        if (!mPasswordError)
                            mPasswordError = true;
                        return;
                    default:
                        bDocClosed = true;
                        UIToast.getInstance(mContext).show(R.string.rv_page_open_error);
                        openDocumentFailed();
                        break;
                }
            }

            @Override
            public void onDocWillClose(PDFDoc pdfDoc) {
                if (mProgressDlg == null && mMainFrame.getAttachedActivity() != null) {
                    mProgressDlg = new ProgressDialog(mMainFrame.getAttachedActivity());
                    mProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mProgressDlg.setCancelable(false);
                    mProgressDlg.setIndeterminate(false);
                }
                if (mProgressDlg != null) {
                    mProgressDlg.setMessage("closing");
                    mProgressDlg.show();
                }

                _resetStatusAfterClose();
            }

            @Override
            public void onDocClosed(PDFDoc pdfDoc, int i) {
                if (mProgressDlg != null && mProgressDlg.isShowing()) {
                    mProgressDlg.dismiss();
                    mProgressDlg = null;
                }

                bDocClosed = true;
                closeDocumentSucceed();
                if (i == PDFError.NO_ERROR.getCode() && isSaveDocInCurPath) {
                    updateThumbnail(mSavePath);
                }
            }

            @Override
            public void onDocWillSave(PDFDoc pdfDoc) {
                if (mProgressDlg == null && mMainFrame.getAttachedActivity() != null) {
                    mProgressDlg = new ProgressDialog(mMainFrame.getAttachedActivity());
                    mProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mProgressDlg.setCancelable(false);
                    mProgressDlg.setIndeterminate(false);
                }
                if (mProgressDlg != null) {
                    mProgressDlg.setMessage("saving");
                    mProgressDlg.show();
                }
            }

            @Override
            public void onDocSaved(PDFDoc pdfDoc, int i) {
                mProgressDlg.dismiss();

                if (i == PDFError.NO_ERROR.getCode() && !isSaveDocInCurPath) {
                    updateThumbnail(mSavePath);
                }
            }
        });
    }

    private void updateThumbnail(String path) {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager)mDocViewerCtrl.getUIExtensionsManager();
        if (uiExtensionsManager == null) return;
        LocalModule module = (LocalModule) uiExtensionsManager.getModuleByName(MODULE_NAME_LOCAL);
        if (module != null && path != null) {
            module.updateThumbnail(path);
        }
    }

    @Override
    public boolean registerLifecycleListener(ILifecycleEventListener listener) {
        mLifecycleEventList.add(listener);
        return true;
    }

    public UIExtensionsManager.ToolHandlerChangedListener getToolHandlerChangedListener() {
        return mToolHandlerChangedListener;
    }

    private UIExtensionsManager.ToolHandlerChangedListener mToolHandlerChangedListener = new UIExtensionsManager.ToolHandlerChangedListener() {
        @Override
        public void onToolHandlerChanged(ToolHandler oldToolHandler, ToolHandler newToolHandler) {
            if (newToolHandler instanceof SignatureToolHandler) {
                ((MainFrame)mPDFReader.getMainFrame()).resetAnnotCustomBottomBar();
                ((MainFrame)mPDFReader.getMainFrame()).resetAnnotCustomTopBar();
                mPDFReader.changeState(ReadStateConfig.STATE_SIGNATURE);
            } else if (newToolHandler != null) {
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
            } else if (mPDFReader.getState() == ReadStateConfig.STATE_ANNOTTOOL) {
                mPDFReader.changeState(ReadStateConfig.STATE_EDIT);
            }
            if(oldToolHandler instanceof SignatureToolHandler && newToolHandler == null)
                mPDFReader.changeState(ReadStateConfig.STATE_NORMAL);
            if (oldToolHandler != null && newToolHandler != null) {
                ((MainFrame)mPDFReader.getMainFrame()).showToolbars();
            }
        }
    };

    @Override
    public boolean unregisterLifecycleListener(ILifecycleEventListener listener) {
        mLifecycleEventList.remove(listener);
        return true;
    }

    @Override
    public boolean registerStateChangeListener(IStateChangeListener listener) {
        mStateChangeEventList.add(listener);
        return true;
    }

    @Override
    public boolean unregisterStateChangeListener(IStateChangeListener listener) {
        mStateChangeEventList.remove(listener);
        return true;
    }

    @Override
    public IMainFrame getMainFrame() {
        return mMainFrame;
    }

    @Override
    public PDFViewCtrl.UIExtensionsManager getUIExtensionsManager() {
        return mDocViewerCtrl.getUIExtensionsManager();
    }

    @Override
    public PDFViewCtrl getDocViewer() {
        return mDocViewerCtrl;
    }

    @Override
    public DocumentManager getDocMgr() {
        return mDocMgr;
    }

    @Override
    public int getState() {
        return mState;
    }

    @Override
    public void changeState(int state) {
        int oldState = mState;
        mState = state;
        for (IStateChangeListener listener : mStateChangeEventList) {
            listener.onStateChanged(oldState, state);
        }

        PageNavigationModule module = (PageNavigationModule) ((UIExtensionsManager) this.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_PAGENAV);
        if (module != null) {
            module.changPageNumberState(getMainFrame().isToolbarsVisible());
        }
    }

    void _resetStatusAfterClose() {
        changeState(ReadStateConfig.STATE_NORMAL);
    }

    void _resetStatusBeforeOpen() {
        mMainFrame.showToolbars();
        mState = ReadStateConfig.STATE_NORMAL;
    }

    @Override
    public void backToPrevActivity() {
        if (((UIExtensionsManager) mDocViewerCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null) {
            ((UIExtensionsManager) mDocViewerCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
        }
        if (DocumentManager.getInstance(mDocViewerCtrl) != null && DocumentManager.getInstance(mDocViewerCtrl).getCurrentAnnot() != null) {
            DocumentManager.getInstance(mDocViewerCtrl).setCurrentAnnot(null);
        }

        if (mMainFrame.getAttachedActivity() == null) {
            closeAllDocuments();
            return;
        }
        try {
            if (mDocViewerCtrl.getDoc() == null || !mDocViewerCtrl.getDoc().isModified()) {
                closeAllDocuments();
                return;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        final boolean hideSave = !DocumentManager.getInstance(mDocViewerCtrl).canModifyContents();

        Builder builder = new AlertDialog.Builder(mMainFrame.getAttachedActivity());
        String[] items;
        if (hideSave) {
            items = new String[]{
                    AppResource.getString(mContext, R.string.rv_back_saveas),
                    AppResource.getString(mContext, R.string.rv_back_discard_modify),
            };
        } else {
            items = new String[]{
                    AppResource.getString(mContext, R.string.rv_back_save),
                    AppResource.getString(mContext, R.string.rv_back_saveas),
                    AppResource.getString(mContext, R.string.rv_back_discard_modify),
            };
        }

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (hideSave) {
                    which += 1;
                }
                switch (which) {
                    case 0: // save
                        mDocViewerCtrl.saveDoc(getCacheFile(),PDFDoc.e_saveFlagIncremental);
                        isSaveDocInCurPath = true;
                        closeAllDocuments();
                        break;
                    case 1: // save as
                        onSaveAsClicked();
                        break;
                    case 2: // discard modify
                        closeAllDocuments();
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
                mSaveAlertDlg = null;
            }

            void showInputFileNameDialog(final String fileFolder) {
                String newFilePath = fileFolder + "/" + AppFileUtil.getFileName(mDocPath);
                final String filePath = AppFileUtil.getFileDuplicateName(newFilePath);
                final String fileName = AppFileUtil.getFileNameWithoutExt(filePath);

                final UITextEditDialog rmDialog = new UITextEditDialog(mMainFrame.getAttachedActivity());
                rmDialog.setPattern("[/\\:*?<>|\"\n\t]");
                rmDialog.setTitle(AppResource.getString(mContext,  R.string.fx_string_saveas));
                rmDialog.getPromptTextView().setVisibility(View.GONE);
                rmDialog.getInputEditText().setText(fileName);
                rmDialog.getInputEditText().selectAll();
                rmDialog.show();
                AppUtil.showSoftInput(rmDialog.getInputEditText());

                rmDialog.getOKButton().setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        rmDialog.dismiss();
                        String inputName = rmDialog.getInputEditText().getText().toString();
                        String newPath = fileFolder + "/" + inputName;
                        newPath += ".pdf";
                        File file = new File(newPath);
                        if (file.exists()) {
                            showAskReplaceDialog(fileFolder,newPath);
                        } else {
                            mSavePath = newPath;
                            mDocViewerCtrl.saveDoc(newPath, PDFDoc.e_saveFlagIncremental);
                            closeAllDocuments();
                        }
                    }
                });
            }

            void showAskReplaceDialog(final String fileFolder,final String newPath) {
                final UITextEditDialog rmDialog = new UITextEditDialog(mMainFrame.getAttachedActivity());
                rmDialog.setTitle(R.string.fx_string_saveas);
                rmDialog.getPromptTextView().setText(R.string.fx_string_filereplace_warning);
                rmDialog.getInputEditText().setVisibility(View.GONE);
                rmDialog.show();

                rmDialog.getOKButton().setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        rmDialog.dismiss();
                        mSavePath = newPath;
                        if (newPath.equalsIgnoreCase(mDocPath)) {
                            isSaveDocInCurPath = true;
                            mDocViewerCtrl.saveDoc(getCacheFile(), PDFDoc.e_saveFlagIncremental);
                        } else {
                            mDocViewerCtrl.saveDoc(newPath, PDFDoc.e_saveFlagIncremental);
                        }

                        closeAllDocuments();
                    }
                });

                rmDialog.getCancelButton().setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        rmDialog.dismiss();
                        showInputFileNameDialog(fileFolder);
                    }
                });
            }

            void onSaveAsClicked() {
                final UIFolderSelectDialog dialog = new UIFolderSelectDialog(mMainFrame.getAttachedActivity());
                dialog.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return !(pathname.isHidden() || !pathname.canRead()) && !pathname.isFile();
                    }
                });
                dialog.setTitle(AppResource.getString(mContext, R.string.fx_string_saveas));
                dialog.setButton(MatchDialog.DIALOG_OK | MatchDialog.DIALOG_CANCEL);
                dialog.setListener(new MatchDialog.DialogListener() {
                    @Override
                    public void onResult(long btType) {
                        if (btType == MatchDialog.DIALOG_OK) {
                            String fileFolder = dialog.getCurrentPath();
                            showInputFileNameDialog(fileFolder);
                        }
                        dialog.dismiss();
                    }

                    @Override
                    public void onBackClick() {
                    }
                });
                dialog.showDialog();
            }
        });

        mSaveAlertDlg = builder.create();
        mSaveAlertDlg.setCanceledOnTouchOutside(true);
        mSaveAlertDlg.show();
    }

    private String getCacheFile(){
        mSavePath = mDocPath;
        File file = new File(mDocPath);
        String dir = file.getParent()+"/";
        while(file.exists()){
            currentFileCachePath = dir+AppDmUtil.randomUUID(null)+".pdf";
            file = new File(currentFileCachePath);
        }
        return currentFileCachePath;
    }


    public RelativeLayout getContentView() {
        return mActivityLayout;
    }

    public void onCreate(Activity act, PDFViewCtrl pdfViewCtrl, Bundle bundle) {

        if (mModulesConfig.isLoadDefaultReader()) {
            if(mModulesConfig.isLoadAnnotations())
                addAnnotsListener();
            if(mModulesConfig.isLoadReadingBookmark()) {
                ReadingBookmarkModule readingBookmarkModule = (ReadingBookmarkModule) ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_BOOKMARK);
                addBookmarkListener(readingBookmarkModule);
            }
            if(mModulesConfig.isLoadSearch()) {
                SearchModule searchModule = (SearchModule) ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_SEARCH);
                addSearchListener(searchModule);
            }
        }

        if (mMainFrame.getAttachedActivity() != null && mMainFrame.getAttachedActivity() != act) {
            for (ILifecycleEventListener listener : mLifecycleEventList) {
                listener.onDestroy(act);
            }
        }
        if (mLanguage == null)
            mLanguage = act.getResources().getConfiguration().locale.getLanguage();
        mMainFrame.setAttachedActivity(act);

        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onCreate(act, bundle);
        }
    }

    public void onStart(Activity act) {
        if (mMainFrame.getAttachedActivity() != act) return;
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onStart(act);
        }
    }

    public void onPause(Activity act) {
        if (mMainFrame.getAttachedActivity() != act) return;
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onPause(act);
        }
    }

    public void onResume(Activity act) {
        if (mMainFrame.getAttachedActivity() != act) return;
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onResume(act);
        }
        String curLanguage = act.getResources().getConfiguration().locale.getLanguage();
        if (!mLanguage.equals(curLanguage)) {
            mLanguage = curLanguage;
        }
    }

    public void onStop(Activity act) {
        if (mMainFrame.getAttachedActivity() != act) return;
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onStop(act);
        }
    }

    public void onDestroy(Activity act) {
        FileAttachmentModule module = (FileAttachmentModule) ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_FILEATTACHMENT);
        if (module != null) {
            module.unregisterAttachmentDocEventListener(mAttachmentDocEvent);
        }

        if (mMainFrame.getAttachedActivity() != act) return;
        for (ILifecycleEventListener listener : mLifecycleEventList) {
            listener.onDestroy(act);
        }
        mMainFrame.setAttachedActivity(null);
        closeAllDocuments();
        release();
        AppDialogManager.getInstance().closeAllDialog();
    }

    public void onActivityResult(Activity act, int requestCode, int resultCode, Intent data) {
    }

    public void onConfigurationChanged(Activity act, Configuration newConfig) {
        if (mMainFrame.getAttachedActivity() != act)
            return;

        mMainFrame.onConfigurationChanged(newConfig);

        if (mDocViewerCtrl.getUIExtensionsManager() != null) {
            ((UIExtensionsManager)mDocViewerCtrl.getUIExtensionsManager()).onConfigurationChanged(newConfig);
        }

        MoreMenuModule module = ((MoreMenuModule) ((UIExtensionsManager) mDocViewerCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_MORE_MENU));
        if(module != null) {
            module.onConfigurationChanged(newConfig);
        }
    }

    public boolean onKeyDown(Activity act, int keyCode, KeyEvent event) {
        if (mMainFrame.getAttachedActivity() != act) return false;
		if (keyCode == KeyEvent.KEYCODE_BACK) {
            UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mDocViewerCtrl.getUIExtensionsManager();
            SearchModule searchModule = ((SearchModule) uiExtensionsManager.getModuleByName(Module.MODULE_NAME_SEARCH));
            if (searchModule != null && searchModule.onKeyBack()) {
                changeState(ReadStateConfig.STATE_NORMAL);
                getMainFrame().showToolbars();
                return true;
            }

            FormFillerModule formFillerModule = (FormFillerModule) uiExtensionsManager.getModuleByName(Module.MODULE_NAME_FORMFILLER);
            if (formFillerModule != null && formFillerModule.onKeyBack()) {
                changeState(ReadStateConfig.STATE_NORMAL);
                getMainFrame().showToolbars();
                return true;
            }

            ToolHandler currentToolHandler = uiExtensionsManager.getCurrentToolHandler();
            SignatureModule signature_module = (SignatureModule) uiExtensionsManager.getModuleByName(Module.MODULE_NAME_PSISIGNATURE);
            if (signature_module != null && currentToolHandler instanceof SignatureToolHandler && signature_module.onKeyBack()) {
                changeState(ReadStateConfig.STATE_NORMAL);
                getMainFrame().showToolbars();
                mDocViewerCtrl.invalidate();
                return true;
            }

            FileAttachmentModule fileattachmodule = (FileAttachmentModule) uiExtensionsManager.getModuleByName(Module.MODULE_NAME_FILEATTACHMENT);
            if (fileattachmodule != null && fileattachmodule.onKeyDown(keyCode, event)) {
                getMainFrame().showToolbars();
                return true;
            }

            FileSpecPanelModule fileSpecPanelModule = (FileSpecPanelModule)uiExtensionsManager.getModuleByName(Module.MODULE_NAME_FILE_PANEL);
            if(fileSpecPanelModule != null && fileSpecPanelModule.onKeyDown(keyCode,event)){
                getMainFrame().showToolbars();
                return true;
            }

            CropModule cropModule = (CropModule) uiExtensionsManager.getModuleByName(MODULE_NAME_CROP);
            if (cropModule != null && cropModule.onKeyDown(keyCode, event)) {
                getMainFrame().showToolbars();
                return true;
            }

            if (uiExtensionsManager.onKeyDown(keyCode, event)) return true;

            if (DocumentManager.getInstance(mDocViewerCtrl).onKeyDown(keyCode, event)) {
                return true;
            }

            if(currentToolHandler != null)
            {
                uiExtensionsManager.setCurrentToolHandler(null);
                return true;
            }

            if(getState() != ReadStateConfig.STATE_NORMAL){
                changeState(ReadStateConfig.STATE_NORMAL);
                return true;
            }

            if (event.getRepeatCount() == 0) {
                backToPrevActivity();
                return true;
            }
        }
        return false;
    }

    public boolean onPrepareOptionsMenu(Activity act, Menu menu) {
        return mMainFrame.getAttachedActivity() == act && mDocViewerCtrl.getDoc() != null;

    }


    BaseItem mSearchButtonItem;
    BaseItem mBookmarkAddButton;

    private BaseItem mNoteItem;
    private BaseItem mHighlightItem;
    private BaseItem mStrikeoutItem;
    private BaseItem mTypewriterItem;
    private BaseItem mInsertTextItem;
    private BaseItem mReplaceItem;

    private PropertyBar mPropertyBar;
    private PropertyCircleItem mPropertyItem;
    private BaseItem mMoreItem;
    private BaseItem mOKItem;
    private BaseItem mContinuousCreateItem;
    private boolean mIsContinuousCreate = false;
    private int mHistoryState;

    private FileAttachmentModule.IAttachmentDocEvent mAttachmentDocEvent = null;

    //This is for the module that manager the related ToolHandler implement.
    void addAnnotsListener() {
        if (!mModulesConfig.isLoadDefaultReader()) return;
        Module module;

        module = ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_SQUIGGLY);
        addSquigglyListener((SquigglyModule) module);

        module = ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_STRIKEOUT);
        addStrikeoutListener((StrikeoutModule) module);

        module = ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_UNDERLINE);
        addUnderlineListener((UnderlineModule) module);

        module = ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_HIGHLIGHT);
        addHighlightListener((HighlightModule) module);

        module = ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_NOTE);
        addNoteListener((NoteModule) module);

        module = ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_CIRCLE);
        addCircleListener((CircleModule) module);

        module = ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_SQUARE);
        addSquareListener((SquareModule) module);

        module = ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_TYPEWRITER);
        addTypewriterListener((TypewriterModule) module);

        module = ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_STAMP);
        addStampListener((StampModule) module);

        module = ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_CARET);
        addInsertTextListener((CaretModule) module);
        addReplaceListener((CaretModule) module);

        module = ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_INK);
        addInkListener((InkModule) module);

        module = ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_ERASER);
        addEraserListener((EraserModule) module);

        module = ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_LINE);
        addLineListener((LineModule) module, MoreTools.MT_TYPE_LINE);
        addLineListener((LineModule) module, MoreTools.MT_TYPE_ARROW);

        module = ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_FILEATTACHMENT);
        if (module != null) {
            addFileAttachmentListener((FileAttachmentModule)module);

            ((FileAttachmentModule)module).registerAttachmentDocEventListener(mAttachmentDocEvent = new FileAttachmentModule.IAttachmentDocEvent() {

                @Override
                public void onAttachmentDocWillOpen() {
                    mPDFReader.getMainFrame().getPanelManager().hidePanel();
                }

                @Override
                public void onAttachmentDocOpened(PDFDoc document, int errCode) {
                    if (errCode == PDFError.NO_ERROR.getCode()) {
                        mHistoryState = mPDFReader.getState();
                        mPDFReader.changeState(ReadStateConfig.STATE_NORMAL);
                        PageNavigationModule module = (PageNavigationModule) ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_PAGENAV);
                        if (module != null) {
                            module.changPageNumberState(false);
                        }
                    }
                }

                @Override
                public void onAttachmentDocWillClose() {
                    mPDFReader.changeState(mHistoryState);
                }

                @Override
                public void onAttachmentDocClosed() {

                }
            });
        }

        module = ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_UNDO);
        addUndo((UndoModule) module);
        DocumentManager.getInstance(mPDFReader.getDocViewer()).registerAnnotEventListener(new DocumentManager.AnnotEventListener() {
            @Override
            public void onAnnotAdded(PDFPage page, Annot annot) {
                if (mPDFReader.getState() != ReadStateConfig.STATE_ANNOTTOOL)
                    return;
                if (!mIsContinuousCreate) {
                    mPDFReader.changeState(ReadStateConfig.STATE_EDIT);
                    mPDFReader.getMainFrame().showToolbars();
                }
            }

            @Override
            public void onAnnotDeleted(PDFPage page, Annot annot) {

            }

            @Override
            public void onAnnotModified(PDFPage page, Annot annot) {

            }

            @Override
            public void onAnnotChanged(Annot lastAnnot, Annot currentAnnot) {

            }
        });
    }

    private void addSearchListener(SearchModule module) {
        if (!mModulesConfig.isLoadDefaultReader()) return;
        // add search button to top toolbar
        if (module == null) return;
        mSearchButtonItem = new BaseItemImpl(mContext);
        mSearchButtonItem.setTag(ToolbarItemConfig.ITEM_SEARCH_TAG);
        mSearchButtonItem.setImageResource(R.drawable.rd_search_selector);
        mSearchButtonItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                ((UIExtensionsManager)getUIExtensionsManager()).triggerDismissMenuEvent();
                mPDFReader.getMainFrame().hideToolbars();
                mPDFReader.changeState(ReadStateConfig.STATE_SEARCH);

                SearchView searchView = ((SearchModule) ((UIExtensionsManager) mPDFReader.getDocViewer().getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_SEARCH)).getSearchView();
                searchView.setSearchCancelListener(new SearchView.SearchCancelListener() {
                    @Override
                    public void onSearchCancel() {
                        mPDFReader.changeState(ReadStateConfig.STATE_NORMAL);
                        mPDFReader.getMainFrame().showToolbars();
                    }
                });
                searchView.launchSearchView();
                searchView.show();
            }
        });
        mPDFReader.getMainFrame().getTopToolbar().addView(mSearchButtonItem, BaseBar.TB_Position.Position_RB);
    }

    private void addBookmarkListener(ReadingBookmarkModule module) {
        if (!mModulesConfig.isLoadDefaultReader()) return;
        if (module == null) return;
        mBookmarkAddButton = new BaseItemImpl(mContext);
        mBookmarkAddButton.setTag(ToolbarItemConfig.ITEM_READINGMARK_TAG);
        mBookmarkAddButton.setImageResource(R.drawable.rd_readingmark_add_selector);

        module.addMarkedItem(mBookmarkAddButton);
        mPDFReader.getMainFrame().getTopToolbar().addView(mBookmarkAddButton, BaseBar.TB_Position.Position_RB);
    }


    private void resetAnnotBarToTextMarkup(final Module module) {
        if (module == null) return;
        mPDFReader.getMainFrame().getToolSetBar().removeAllItems();
        mPropertyBar = mPDFReader.getMainFrame().getPropertyBar();
        int color = 0;
        ToolHandler toolHandler;
        if (module instanceof HighlightModule) {
            toolHandler = ((HighlightModule) module).getToolHandler();
            ((HighlightToolHandler) toolHandler).setIsContinuousCreate(false);

            int[] mPBColors = new int[PropertyBar.PB_COLORS_HIGHLIGHT.length];
            long supportProperty = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY;
            System.arraycopy(PropertyBar.PB_COLORS_HIGHLIGHT, 0, mPBColors, 0, mPBColors.length);
            mPBColors[0] = PropertyBar.PB_COLORS_HIGHLIGHT[0];
            mPropertyBar.setColors(mPBColors);
            mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, ((HighlightToolHandler) toolHandler).getColor());
            mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100(((HighlightToolHandler) toolHandler).getOpacity()));
            mPropertyBar.reset(supportProperty);

            mPropertyBar.setPropertyChangeListener((HighlightModule) module);
            color = ((HighlightToolHandler) toolHandler).getColor();
            ((HighlightModule) module).setColorChangeListener(new HighlightModule.ColorChangeListener() {
                @Override
                public void onColorChange(int color) {
                    mPropertyItem.setCentreCircleColor(color);
                }
            });
        } else if (module instanceof UnderlineModule) {
            toolHandler = ((UnderlineModule) module).getToolHandler();
            ((UnderlineToolHandler) toolHandler).setIsContinuousCreate(false);

            int[] mPBColors = new int[PropertyBar.PB_COLORS_UNDERLINE.length];
            long supportProperty = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY;
            System.arraycopy(PropertyBar.PB_COLORS_UNDERLINE, 0, mPBColors, 0, mPBColors.length);
            mPBColors[0] = PropertyBar.PB_COLORS_UNDERLINE[0];
            mPropertyBar.setColors(mPBColors);
            mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, ((UnderlineToolHandler) toolHandler).getColor());
            mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100(((UnderlineToolHandler) toolHandler).getOpacity()));
            mPropertyBar.reset(supportProperty);

            mPropertyBar.setPropertyChangeListener((UnderlineModule) module);
            color = ((UnderlineToolHandler) toolHandler).getColor();
            ((UnderlineModule) module).setColorChangeListener(new UnderlineModule.ColorChangeListener() {
                @Override
                public void onColorChange(int color) {
                    mPropertyItem.setCentreCircleColor(color);
                }
            });
        } else if (module instanceof StrikeoutModule) {
            toolHandler = ((StrikeoutModule) module).getToolHandler();
            ((StrikeoutToolHandler) toolHandler).setIsContinuousCreate(false);

            int[] mPBColors = new int[PropertyBar.PB_COLORS_STRIKEOUT.length];
            long supportProperty = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY;
            System.arraycopy(PropertyBar.PB_COLORS_STRIKEOUT, 0, mPBColors, 0, mPBColors.length);
            mPBColors[0] = PropertyBar.PB_COLORS_STRIKEOUT[0];
            mPropertyBar.setColors(mPBColors);
            mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, ((StrikeoutToolHandler) toolHandler).getColor());
            mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100(((StrikeoutToolHandler) toolHandler).getOpacity()));
            mPropertyBar.reset(supportProperty);

            mPropertyBar.setPropertyChangeListener((StrikeoutModule) module);
            color = ((StrikeoutToolHandler) toolHandler).getColor();
            ((StrikeoutModule) module).setColorChangeListener(new StrikeoutModule.ColorChangeListener() {
                @Override
                public void onColorChange(int color) {
                    mPropertyItem.setCentreCircleColor(color);
                }
            });
        } else if (module instanceof SquigglyModule) {
            toolHandler = ((SquigglyModule) module).getToolHandler();
            ((SquigglyToolHandler) toolHandler).setIsContinuousCreate(false);

            int[] mPBColors = new int[PropertyBar.PB_COLORS_SQUIGGLY.length];
            long supportProperty = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY;
            System.arraycopy(PropertyBar.PB_COLORS_SQUIGGLY, 0, mPBColors, 0, mPBColors.length);
            mPBColors[0] = PropertyBar.PB_COLORS_SQUIGGLY[0];
            mPropertyBar.setColors(mPBColors);
            mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, ((SquigglyToolHandler) toolHandler).getColor());
            mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100(((SquigglyToolHandler) toolHandler).getOpacity()));
            mPropertyBar.reset(supportProperty);

            mPropertyBar.setPropertyChangeListener((SquigglyModule) module);
            color = ((SquigglyToolHandler) toolHandler).getColor();
            ((SquigglyModule) module).setColorChangeListener(new SquigglyModule.ColorChangeListener() {
                @Override
                public void onColorChange(int color) {
                    mPropertyItem.setCentreCircleColor(color);
                }
            });
        }

        mMoreItem = new CircleItemImpl(mContext) {

            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                ToolHandler toolHandler = null;
                if (module instanceof HighlightModule)
                    toolHandler = ((HighlightModule) module).getToolHandler();
                else if (module instanceof UnderlineModule)
                    toolHandler = ((UnderlineModule) module).getToolHandler();
                else if (module instanceof StrikeoutModule)
                    toolHandler = ((StrikeoutModule) module).getToolHandler();
                else if (module instanceof SquigglyModule)
                    toolHandler = ((SquigglyModule) module).getToolHandler();
                if (((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler() == toolHandler) {
                    Rect rect = new Rect();
                    mMoreItem.getContentView().getGlobalVisibleRect(rect);
                    mPDFReader.getMainFrame().getMoreToolsBar().update(new RectF(rect));
                }
            }
        };
        mMoreItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_MORE);
        mMoreItem.setImageResource(R.drawable.mt_more_selector);
        mMoreItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rect rect = new Rect();
                mMoreItem.getContentView().getGlobalVisibleRect(rect);
                mPDFReader.getMainFrame().getMoreToolsBar().show(new RectF(rect), true);
            }
        });

        mOKItem = new CircleItemImpl(mContext);
        mOKItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_OK);
        mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
        mOKItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPDFReader.changeState(ReadStateConfig.STATE_EDIT);
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(null);
            }
        });

        mPropertyItem = new PropertyCircleItemImp(mContext) {

            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                ToolHandler toolHandler = null;
                if (module instanceof HighlightModule)
                    toolHandler = ((HighlightModule) module).getToolHandler();
                else if (module instanceof UnderlineModule)
                    toolHandler = ((UnderlineModule) module).getToolHandler();
                else if (module instanceof StrikeoutModule)
                    toolHandler = ((StrikeoutModule) module).getToolHandler();
                else if (module instanceof SquigglyModule)
                    toolHandler = ((SquigglyModule) module).getToolHandler();
                if (((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler() == toolHandler) {
                    if (mPropertyBar.isShowing()) {
                        Rect rect = new Rect();
                        mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                        mPropertyBar.update(new RectF(rect));
                    }
                }
            }
        };

        mPropertyItem.setTag(ToolbarItemConfig.ITEM_PROPERTY_TAG);
        mPropertyItem.setCentreCircleColor(color);
        final Rect rect = new Rect();

        mPropertyItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPDFReader.getMainFrame().getPropertyBar().setArrowVisible(true);
                mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                mPDFReader.getMainFrame().getPropertyBar().show(new RectF(rect), true);
            }
        });

        mContinuousCreateItem = new CircleItemImpl(mContext);
        mContinuousCreateItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_CONTINUE);

        mIsContinuousCreate = false;
        mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);

        mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                if (module instanceof HighlightModule) {
                    HighlightToolHandler hltToolHandler = (HighlightToolHandler) ((HighlightModule) module).getToolHandler();
                    if (hltToolHandler.getIsContinuousCreate()) {
                        hltToolHandler.setIsContinuousCreate(false);
                        mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
                    } else {
                        hltToolHandler.setIsContinuousCreate(true);
                        mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
                    }

                } else if (module instanceof UnderlineModule) {
                    UnderlineToolHandler unlToolHandler = (UnderlineToolHandler) ((UnderlineModule) module).getToolHandler();
                    if (unlToolHandler.getIsContinuousCreate()) {
                        unlToolHandler.setIsContinuousCreate(false);
                        mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
                    } else {
                        unlToolHandler.setIsContinuousCreate(true);
                        mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
                    }

                } else if (module instanceof StrikeoutModule) {
                    StrikeoutToolHandler stoToolHandler = (StrikeoutToolHandler) ((StrikeoutModule) module).getToolHandler();
                    if (stoToolHandler.getIsContinuousCreate()) {
                        stoToolHandler.setIsContinuousCreate(false);
                        mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
                    } else {
                        stoToolHandler.setIsContinuousCreate(true);
                        mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
                    }
                } else if (module instanceof SquigglyModule) {
                    SquigglyToolHandler sqgToolHandler = (SquigglyToolHandler) ((SquigglyModule) module).getToolHandler();
                    if (sqgToolHandler.getIsContinuousCreate()) {
                        sqgToolHandler.setIsContinuousCreate(false);
                        mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
                    } else {
                        sqgToolHandler.setIsContinuousCreate(true);
                        mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
                    }
                }
                mIsContinuousCreate = !mIsContinuousCreate;

                AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinuousCreate);
            }
        });
        mPDFReader.getMainFrame().getToolSetBar().addView(mMoreItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);
    }

    private void addHighlightListener(final HighlightModule module) {
        if (module == null) return;
        mHighlightItem = new CircleItemImpl(mContext);
        mHighlightItem.setImageResource(R.drawable.annot_highlight_selector);
        mHighlightItem.setTag(ToolbarItemConfig.ITEM_HIGHLIGHT_TAG);
        mPDFReader.getMainFrame().getEditBar().addView(mHighlightItem, BaseBar.TB_Position.Position_CENTER);

        mHighlightItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HighlightToolHandler toolHandler = (HighlightToolHandler) module.getToolHandler();
                toolHandler.getTextSelector().clear();
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(toolHandler);
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBarToTextMarkup(module);
            }
        });
        mHighlightItem.setEnable(true);

        mPDFReader.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getToolHandler());
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBarToTextMarkup(module);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_HIGHLIGHT;
            }
        });
    }

    private void addSquigglyListener(final SquigglyModule module) {
        if (module == null) return;
        mPDFReader.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getToolHandler());
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBarToTextMarkup(module);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_SQUIGGLY;
            }
        });
    }

    private void addUnderlineListener(final UnderlineModule module) {
        if (module == null) return;
        mPDFReader.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getToolHandler());
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBarToTextMarkup(module);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_UNDERLINE;
            }
        });

    }

    private void addStrikeoutListener(final StrikeoutModule module) {
        if (module == null) return;
        //Annot Icon
        mStrikeoutItem = new CircleItemImpl(mContext);
        mStrikeoutItem.setImageResource(R.drawable.annot_sto_selector);
        mStrikeoutItem.setTag(ToolbarItemConfig.ANNOTS_BAR_ITEM_STO_TAG);
        mPDFReader.getMainFrame().getEditBar().addView(mStrikeoutItem, BaseBar.TB_Position.Position_CENTER);

        mStrikeoutItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StrikeoutToolHandler toolHandler = (StrikeoutToolHandler) module.getToolHandler();
                toolHandler.resetLineData();
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(toolHandler);
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBarToTextMarkup(module);
            }
        });
        mPDFReader.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getToolHandler());
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBarToTextMarkup(module);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_STRIKEOUT;
            }
        });
    }

    public void resetAnnotBarToNote(final NoteModule module) {
        if (module == null) return;
        mPDFReader.getMainFrame().getToolSetBar().removeAllItems();
        mPropertyBar = mPDFReader.getMainFrame().getPropertyBar();
        mPropertyBar.setPropertyChangeListener(module);
        final NoteToolHandler toolHandler = (NoteToolHandler) module.getToolHandler();
        mPropertyItem = new PropertyCircleItemImp(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                super.onItemLayout(l, t, r, b);

                if (((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler() == toolHandler) {
                    if (mPropertyBar.isShowing()) {
                        Rect rect = new Rect();
                        mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                        mPropertyBar.update(new RectF(rect));
                    }
                }
            }
        };

        int[] colors = new int[PropertyBar.PB_COLORS_TEXT.length];
        System.arraycopy(PropertyBar.PB_COLORS_TEXT, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_TEXT[0];
        mPropertyBar.setColors(colors);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, toolHandler.getColor());
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, toolHandler.getOpacity());
        mPropertyBar.setProperty(PropertyBar.PROPERTY_ANNOT_TYPE, toolHandler.getIconType());
        mPropertyBar.setPropertyChangeListener(module);
        final long supportProperty = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY | PropertyBar.PROPERTY_ANNOT_TYPE;

        mPropertyItem.setTag(ToolbarItemConfig.ITEM_PROPERTY_TAG);
        mPropertyItem.setCentreCircleColor(toolHandler.getColor());
        mPropertyItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPropertyBar.reset(supportProperty);
                mPropertyBar.setArrowVisible(true);
                Rect rect = new Rect();
                mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                mPropertyBar.show(new RectF(rect), true);

            }
        });

        module.setColorChangeListener(new NoteModule.ColorChangeListener() {
            @Override
            public void onColorChange(int color) {
                mPropertyItem.setCentreCircleColor(color);
            }
        });

        mMoreItem = new CircleItemImpl(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                super.onItemLayout(l, t, r, b);

                if (((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler() == module.getToolHandler()) {
                    if (mPDFReader.getMainFrame().getMoreToolsBar().isShowing()) {
                        Rect rect = new Rect();
                        mMoreItem.getContentView().getGlobalVisibleRect(rect);
                        mPDFReader.getMainFrame().getMoreToolsBar().update(new RectF(rect));
                    }
                }
            }
        };
        mMoreItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_MORE);
        mMoreItem.setImageResource(R.drawable.mt_more_selector);
        mMoreItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Rect rect = new Rect();
                mMoreItem.getContentView().getGlobalVisibleRect(rect);
                mPDFReader.getMainFrame().getMoreToolsBar().show(new RectF(rect), true);
            }
        });

        mOKItem = new CircleItemImpl(mContext);
        mOKItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_OK);
        mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
        mOKItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(null);
                mPDFReader.changeState(ReadStateConfig.STATE_EDIT);
            }
        });

        mContinuousCreateItem = new CircleItemImpl(mContext);
        mContinuousCreateItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_CONTINUE);
        mIsContinuousCreate = false;
        ((NoteToolHandler) module.getToolHandler()).setIsContinuousCreate(false);
        mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);

        mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                if (((NoteToolHandler) module.getToolHandler()).getIsContinuousCreate()) {
                    mIsContinuousCreate = false;
                    ((NoteToolHandler) module.getToolHandler()).setIsContinuousCreate(false);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
                } else {
                    mIsContinuousCreate = true;
                    ((NoteToolHandler) module.getToolHandler()).setIsContinuousCreate(true);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
                }

                AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinuousCreate);
            }
        });

        mPDFReader.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mMoreItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);
    }

    private void addNoteListener(final NoteModule module) {
        if (module == null) return;
        mNoteItem = new CircleItemImpl(mContext);
        mNoteItem.setTag(ToolbarItemConfig.ITEM_NOTE_TAG);
        mNoteItem.setImageResource(R.drawable.mt_iv_note_selector);
        mNoteItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getToolHandler());
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBarToNote(module);
            }
        });
        mPDFReader.getMainFrame().getEditBar().addView(mNoteItem, BaseBar.TB_Position.Position_CENTER);

        mPDFReader.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getToolHandler());
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBarToNote(module);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_ANNOTTEXT;
            }
        });
        mNoteItem.setEnable(true);

    }

    private BaseItem mCircleItem;

    private void addCircleListener(final CircleModule module) {
        if (module == null) return;
        mCircleItem = new CircleItemImpl(mContext);
        mCircleItem.setTag(ToolbarItemConfig.ANNOTS_BAR_ITEM_CIR_TAG);
        mCircleItem.setImageResource(R.drawable.annot_circle_selector);

        mPDFReader.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getToolHandler());
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBarToCircle(module);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_CIRCLE;
            }
        });
        mCircleItem.setEnable(true);
    }

    private void addSquareListener(final SquareModule module) {
        if (module == null) return;
        mCircleItem = new CircleItemImpl(mContext);
        mCircleItem.setTag(ToolbarItemConfig.ANNOTS_BAR_ITEM_SQU_TAG);
        mCircleItem.setImageResource(R.drawable.annot_square_selector);

        mPDFReader.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getToolHandler());
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBarToSquare(module);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_SQUARE;
            }
        });
        mCircleItem.setEnable(true);
    }

    private void resetAnnotBarToCircle(final CircleModule module) {
        if (module == null) return;
        mPDFReader.getMainFrame().getToolSetBar().removeAllItems();
        mMoreItem = new CircleItemImpl(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (module.getToolHandler() == ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler()) {
                    if (mPDFReader.getMainFrame().getMoreToolsBar().isShowing()) {
                        Rect rect = new Rect();
                        mMoreItem.getContentView().getGlobalVisibleRect(rect);
                        mPDFReader.getMainFrame().getMoreToolsBar().update(new RectF(rect));
                    }
                }
            }
        };
        mMoreItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_MORE);
        mMoreItem.setImageResource(R.drawable.mt_more_selector);
        mMoreItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rect rect = new Rect();
                mMoreItem.getContentView().getGlobalVisibleRect(rect);
                mPDFReader.getMainFrame().getMoreToolsBar().show(new RectF(rect), true);
            }
        });

        mPropertyBar = mPDFReader.getMainFrame().getPropertyBar();
        mPropertyBar.setPropertyChangeListener(module);
        final CircleToolHandler toolHandler = (CircleToolHandler) module.getToolHandler();
        mPropertyItem = new PropertyCircleItemImp(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler() == toolHandler) {
                    if (mPropertyBar.isShowing()) {
                        Rect mProRect = new Rect();
                        mPropertyItem.getContentView().getGlobalVisibleRect(mProRect);
                        mPropertyBar.update(new RectF(mProRect));
                    }
                }
            }
        };
        int[] colors = new int[PropertyBar.PB_COLORS_CIRCLE.length];
        System.arraycopy(PropertyBar.PB_COLORS_CIRCLE, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_CIRCLE[0];
        mPropertyBar.setColors(colors);

        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, toolHandler.getColor());
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, toolHandler.getOpacity());
        mPropertyBar.setProperty(PropertyBar.PROPERTY_LINEWIDTH, toolHandler.getLineWidth());

        mPropertyBar.setPropertyChangeListener(module);

        mPropertyItem.setTag(ToolbarItemConfig.ITEM_PROPERTY_TAG);
        mPropertyItem.setCentreCircleColor(toolHandler.getColor());
        mPropertyItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long supportProperty = PropertyBar.PROPERTY_COLOR
                        | PropertyBar.PROPERTY_OPACITY
                        | PropertyBar.PROPERTY_LINEWIDTH;

                mPropertyBar.setArrowVisible(true);
                mPropertyBar.reset(supportProperty);
                Rect mProRect = new Rect();
                mPropertyItem.getContentView().getGlobalVisibleRect(mProRect);
                mPDFReader.getMainFrame().getPropertyBar().show(new RectF(mProRect), true);
            }
        });
        mOKItem = new CircleItemImpl(mContext);
        mOKItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_OK);
        mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
        mOKItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(null);
                mPDFReader.changeState(ReadStateConfig.STATE_EDIT);
            }
        });

        module.setColorChangeListener(new CircleModule.ColorChangeListener() {
            @Override
            public void onColorChange(int color) {
                mPropertyItem.setCentreCircleColor(color);
            }
        });

        mContinuousCreateItem = new CircleItemImpl(mContext);
        mContinuousCreateItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_CONTINUE);
        mIsContinuousCreate = false;
        ((CircleToolHandler) module.getToolHandler()).setIsContinuousCreate(false);
        if (mIsContinuousCreate) {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
        } else {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
        }
        mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                if (((CircleToolHandler) module.getToolHandler()).getIsContinuousCreate()) {
                    mIsContinuousCreate = false;
                    ((CircleToolHandler) module.getToolHandler()).setIsContinuousCreate(mIsContinuousCreate);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
                } else {
                    mIsContinuousCreate = true;
                    ((CircleToolHandler) module.getToolHandler()).setIsContinuousCreate(mIsContinuousCreate);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
                }

                AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinuousCreate);
            }
        });
        mPDFReader.getMainFrame().getToolSetBar().addView(mMoreItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);
    }

    private void resetAnnotBarToSquare(final SquareModule module) {
        if (module == null) return;
        mPDFReader.getMainFrame().getToolSetBar().removeAllItems();
        mMoreItem = new CircleItemImpl(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (module.getToolHandler() == ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler()) {
                    if (mPDFReader.getMainFrame().getMoreToolsBar().isShowing()) {
                        Rect rect = new Rect();
                        mMoreItem.getContentView().getGlobalVisibleRect(rect);
                        mPDFReader.getMainFrame().getMoreToolsBar().update(new RectF(rect));
                    }
                }
            }
        };
        mMoreItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_MORE);
        mMoreItem.setImageResource(R.drawable.mt_more_selector);
        mMoreItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rect rect = new Rect();
                mMoreItem.getContentView().getGlobalVisibleRect(rect);
                mPDFReader.getMainFrame().getMoreToolsBar().show(new RectF(rect), true);
            }
        });

        mPropertyBar = mPDFReader.getMainFrame().getPropertyBar();
        mPropertyBar.setPropertyChangeListener(module);
        final SquareToolHandler toolHandler = (SquareToolHandler) module.getToolHandler();
        mPropertyItem = new PropertyCircleItemImp(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler() == toolHandler) {
                    if (mPropertyBar.isShowing()) {
                        Rect mProRect = new Rect();
                        mPropertyItem.getContentView().getGlobalVisibleRect(mProRect);
                        mPropertyBar.update(new RectF(mProRect));
                    }
                }
            }
        };
        int[] colors = new int[PropertyBar.PB_COLORS_SQUARE.length];
        System.arraycopy(PropertyBar.PB_COLORS_SQUARE, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_SQUARE[0];
        mPropertyBar.setColors(colors);

        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, toolHandler.getColor());
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, toolHandler.getOpacity());
        mPropertyBar.setProperty(PropertyBar.PROPERTY_LINEWIDTH, toolHandler.getLineWidth());

        mPropertyBar.setPropertyChangeListener(module);

        mPropertyItem.setTag(ToolbarItemConfig.ITEM_PROPERTY_TAG);
        mPropertyItem.setCentreCircleColor(toolHandler.getColor());
        mPropertyItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long supportProperty = PropertyBar.PROPERTY_COLOR
                        | PropertyBar.PROPERTY_OPACITY
                        | PropertyBar.PROPERTY_LINEWIDTH;

                mPropertyBar.setArrowVisible(true);
                mPropertyBar.reset(supportProperty);
                Rect mProRect = new Rect();
                mPropertyItem.getContentView().getGlobalVisibleRect(mProRect);
                mPDFReader.getMainFrame().getPropertyBar().show(new RectF(mProRect), true);
            }
        });
        mOKItem = new CircleItemImpl(mContext);
        mOKItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_OK);
        mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
        mOKItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(null);
                mPDFReader.changeState(ReadStateConfig.STATE_EDIT);
            }
        });

        module.setColorChangeListener(new SquareModule.ColorChangeListener() {
            @Override
            public void onColorChange(int color) {
                mPropertyItem.setCentreCircleColor(color);
            }
        });

        mContinuousCreateItem = new CircleItemImpl(mContext);
        mContinuousCreateItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_CONTINUE);
        mIsContinuousCreate = false;
        ((SquareToolHandler) module.getToolHandler()).setIsContinuousCreate(false);
        if (mIsContinuousCreate) {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
        } else {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
        }
        mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                if (((SquareToolHandler) module.getToolHandler()).getIsContinuousCreate()) {
                    mIsContinuousCreate = false;
                    ((SquareToolHandler) module.getToolHandler()).setIsContinuousCreate(mIsContinuousCreate);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
                } else {
                    mIsContinuousCreate = true;
                    ((SquareToolHandler) module.getToolHandler()).setIsContinuousCreate(mIsContinuousCreate);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
                }

                AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinuousCreate);
            }
        });
        mPDFReader.getMainFrame().getToolSetBar().addView(mMoreItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);
    }

    private void addTypewriterListener(final TypewriterModule module) {
        if (module == null) return;
        mTypewriterItem = new CircleItemImpl(mContext);
        mTypewriterItem.setTag(ToolbarItemConfig.ANNOTS_BAR_ITEM_TYPEWRITE);
        mTypewriterItem.setImageResource(R.drawable.annot_typewriter_selector);

        mTypewriterItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getToolHandler());
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBarToTypewriter(module);
            }
        });
        mPDFReader.getMainFrame().getEditBar().addView(mTypewriterItem, BaseBar.TB_Position.Position_CENTER);

        mPDFReader.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getToolHandler());
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBarToTypewriter(module);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_TYPEWRITER;
            }
        });
        mTypewriterItem.setEnable(true);
    }

    private void resetAnnotBarToTypewriter(final TypewriterModule module) {
        if (module == null) return;
        mPDFReader.getMainFrame().getToolSetBar().removeAllItems();
        mMoreItem = new CircleItemImpl(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (module.getToolHandler() == ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler()) {
                    if (mPDFReader.getMainFrame().getMoreToolsBar().isShowing()) {
                        Rect rect = new Rect();
                        mMoreItem.getContentView().getGlobalVisibleRect(rect);
                        mPDFReader.getMainFrame().getMoreToolsBar().update(new RectF(rect));
                    }
                }
            }
        };
        mMoreItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_MORE);
        mMoreItem.setImageResource(R.drawable.mt_more_selector);
        mMoreItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rect rect = new Rect();
                mMoreItem.getContentView().getGlobalVisibleRect(rect);
                mPDFReader.getMainFrame().getMoreToolsBar().show(new RectF(rect), true);
            }
        });

        mPropertyBar = mPDFReader.getMainFrame().getPropertyBar();
        mPropertyBar.setPropertyChangeListener(module);
        final TypewriterToolHandler toolHandler = (TypewriterToolHandler) module.getToolHandler();
        mPropertyItem = new PropertyCircleItemImp(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (toolHandler == ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler()) {
                    if (mPropertyBar.isShowing()) {
                        Rect mProRect = new Rect();
                        mPropertyItem.getContentView().getGlobalVisibleRect(mProRect);
                        mPropertyBar.update(new RectF(mProRect));
                    }
                }
            }
        };

        int[] colors = new int[PropertyBar.PB_COLORS_TYPEWRITER.length];
        System.arraycopy(PropertyBar.PB_COLORS_TYPEWRITER, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_TYPEWRITER[0];
        mPropertyBar.setColors(colors);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, toolHandler.getColor());
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, toolHandler.getOpacity());
        mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTNAME, toolHandler.getFontName());
        mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTSIZE, toolHandler.getFontSize());

        mPropertyBar.setPropertyChangeListener(module);

        mPropertyItem.setTag(ToolbarItemConfig.ITEM_PROPERTY_TAG);
        mPropertyItem.setCentreCircleColor(toolHandler.getColor());
        mPropertyItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long supportProperty = PropertyBar.PROPERTY_COLOR
                        | PropertyBar.PROPERTY_OPACITY
                        | PropertyBar.PROPERTY_FONTSIZE
                        | PropertyBar.PROPERTY_FONTNAME;
                mPropertyBar.setArrowVisible(true);
                mPropertyBar.reset(supportProperty);

                Rect rect = new Rect();
                mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                mPDFReader.getMainFrame().getPropertyBar().show(new RectF(rect), true);
            }
        });

        module.setColorChangeListener(new TypewriterModule.ColorChangeListener() {
            @Override
            public void onColorChange(int color) {
                mPropertyItem.setCentreCircleColor(color);
            }
        });
        mContinuousCreateItem = new CircleItemImpl(mContext);
        mContinuousCreateItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_CONTINUE);
        mIsContinuousCreate = false;
        ((TypewriterToolHandler) module.getToolHandler()).setIsContinuousCreate(false);
        if (mIsContinuousCreate) {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
        } else {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
        }
        mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                if (((TypewriterToolHandler) module.getToolHandler()).getIsContinuousCreate()) {
                    mIsContinuousCreate = false;
                    ((TypewriterToolHandler) module.getToolHandler()).setIsContinuousCreate(mIsContinuousCreate);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
                } else {
                    mIsContinuousCreate = true;
                    ((TypewriterToolHandler) module.getToolHandler()).setIsContinuousCreate(mIsContinuousCreate);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
                }
                AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinuousCreate);
            }
        });
        mOKItem = new CircleItemImpl(mContext);
        mOKItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_OK);
        mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
        mOKItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(null);
                mPDFReader.changeState(ReadStateConfig.STATE_EDIT);
            }
        });
        mPDFReader.getMainFrame().getToolSetBar().addView(mMoreItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);
    }

    private void addStampListener(final StampModule module) {
        if (module == null) return;
        mPDFReader.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                if (type == MoreTools.MT_TYPE_STAMP) {
                    ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getToolHandler());
                    mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                    resetAnnotBarToStamp(module);
                }
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_STAMP;
            }
        });
    }

    private void resetAnnotBarToStamp(final StampModule module) {
        if (module == null) return;
        mPDFReader.getMainFrame().getToolSetBar().removeAllItems();
        mOKItem = new CircleItemImpl(mContext);
        mOKItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_OK);
        mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
        mOKItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(null);
                mPDFReader.changeState(ReadStateConfig.STATE_EDIT);
            }
        });

        mPropertyBar = mPDFReader.getMainFrame().getPropertyBar();
        mPropertyBar.setPropertyChangeListener(module);
        final StampToolHandler toolHandler = (StampToolHandler) module.getToolHandler();

        mPropertyItem = new PropertyCircleItemImp(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                super.onItemLayout(l, t, r, b);
                if (((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler() == toolHandler) {
                    if (mPropertyBar.isShowing()) {
                        toolHandler.resetPropertyBar(mPropertyBar);
                        Rect rect = new Rect();
                        mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                        mPropertyBar.update(new RectF(rect));
                    }
                }
            }
        };
        toolHandler.initDisplayItems(mPropertyBar, mPropertyItem);
        mPropertyItem.setTag(ToolbarItemConfig.ITEM_PROPERTY_TAG);
        mPropertyItem.setCentreCircleColor(Color.parseColor("#179CD8"));
        mPropertyItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rect rect = new Rect();
                mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                RectF rectF = new RectF(rect);
                mPropertyBar.show(rectF, true);
            }
        });

        mContinuousCreateItem = new CircleItemImpl(mContext);
        mContinuousCreateItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_CONTINUE);
        mIsContinuousCreate = false;
        toolHandler.setIsContinuousCreate(mIsContinuousCreate);
        if (mIsContinuousCreate) {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
        } else {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
        }
        mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                if (((StampToolHandler) module.getToolHandler()).getIsContinuousCreate()) {
                    mIsContinuousCreate = false;
                    ((StampToolHandler) module.getToolHandler()).setIsContinuousCreate(mIsContinuousCreate);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
                } else {
                    mIsContinuousCreate = true;
                    ((StampToolHandler) module.getToolHandler()).setIsContinuousCreate(mIsContinuousCreate);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
                }

                AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinuousCreate);
            }
        });
        mMoreItem = new CircleItemImpl(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler() == toolHandler) {
                    if (mPDFReader.getMainFrame().getMoreToolsBar().isShowing()) {
                        Rect rect = new Rect();
                        mMoreItem.getContentView().getGlobalVisibleRect(rect);
                        mPDFReader.getMainFrame().getMoreToolsBar().update(new RectF(rect));
                    }
                }
            }
        };
        mMoreItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_MORE);
        mMoreItem.setImageResource(R.drawable.mt_more_selector);
        mMoreItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rect rect = new Rect();
                mMoreItem.getContentView().getGlobalVisibleRect(rect);
                mPDFReader.getMainFrame().getMoreToolsBar().show(new RectF(rect), true);
            }
        });


        mPDFReader.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mMoreItem, BaseBar.TB_Position.Position_CENTER);
    }

    private void addInsertTextListener(final CaretModule module) {
        if (module == null) return;
        mInsertTextItem = new CircleItemImpl(mContext);
        mInsertTextItem.setImageResource(R.drawable.annot_insert_selector);
        mInsertTextItem.setTag(ToolbarItemConfig.ANNOTS_BAR_ITEM_CARET);
        mPDFReader.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getISToolHandler());
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBarToInsertText(module);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_INSERTTEXT;
            }
        });
        mInsertTextItem.setEnable(true);
    }

    private void resetAnnotBarToInsertText(final CaretModule module) {
        if (module == null) return;
        mPDFReader.getMainFrame().getToolSetBar().removeAllItems();
        mMoreItem = new CircleItemImpl(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (module.getISToolHandler() == ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler()) {
                    if (mPDFReader.getMainFrame().getMoreToolsBar().isShowing()) {
                        Rect rect = new Rect();
                        mMoreItem.getContentView().getGlobalVisibleRect(rect);
                        mPDFReader.getMainFrame().getMoreToolsBar().update(new RectF(rect));
                    }
                }
            }
        };
        mMoreItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_MORE);
        mMoreItem.setImageResource(R.drawable.mt_more_selector);
        mMoreItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rect rect = new Rect();
                mMoreItem.getContentView().getGlobalVisibleRect(rect);
                mPDFReader.getMainFrame().getMoreToolsBar().show(new RectF(rect), true);
            }
        });

        mPropertyBar = mPDFReader.getMainFrame().getPropertyBar();
        mPropertyBar.setPropertyChangeListener(module);
        final CaretToolHandler toolHandler = (CaretToolHandler) module.getISToolHandler();
        mPropertyItem = new PropertyCircleItemImp(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (toolHandler == ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler()) {
                    if (mPropertyBar.isShowing()) {
                        Rect mProRect = new Rect();
                        mPropertyItem.getContentView().getGlobalVisibleRect(mProRect);
                        mPropertyBar.update(new RectF(mProRect));
                    }
                }
            }
        };

        int[] colors = new int[PropertyBar.PB_COLORS_CARET.length];
        System.arraycopy(PropertyBar.PB_COLORS_CARET, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_CARET[0];
        mPropertyBar.setColors(colors);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, toolHandler.getColor());
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, toolHandler.getOpacity());
        mPropertyBar.setPropertyChangeListener(module);

        mPropertyItem.setTag(ToolbarItemConfig.ITEM_PROPERTY_TAG);
        mPropertyItem.setCentreCircleColor(toolHandler.getColor());
        mPropertyItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long supportProperty = PropertyBar.PROPERTY_COLOR
                        | PropertyBar.PROPERTY_OPACITY;
                mPropertyBar.setArrowVisible(true);
                mPropertyBar.reset(supportProperty);

                Rect rect = new Rect();
                mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                mPDFReader.getMainFrame().getPropertyBar().show(new RectF(rect), true);
            }
        });

        module.setColorChangeListener(new CaretModule.ColorChangeListener() {
            @Override
            public void onColorChange(int color) {
                mPropertyItem.setCentreCircleColor(color);
            }
        });
        mContinuousCreateItem = new CircleItemImpl(mContext);
        mContinuousCreateItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_CONTINUE);
        mIsContinuousCreate = false;
        ((CaretToolHandler) module.getISToolHandler()).setIsContinuousCreate(false);
        if (mIsContinuousCreate) {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
        } else {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
        }
        mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                if (((CaretToolHandler) module.getISToolHandler()).getIsContinuousCreate()) {
                    mIsContinuousCreate = false;
                    ((CaretToolHandler) module.getISToolHandler()).setIsContinuousCreate(mIsContinuousCreate);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
                } else {
                    mIsContinuousCreate = true;
                    ((CaretToolHandler) module.getISToolHandler()).setIsContinuousCreate(mIsContinuousCreate);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
                }

                AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinuousCreate);
            }
        });
        mOKItem = new CircleItemImpl(mContext);
        mOKItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_OK);
        mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
        mOKItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(null);
                mPDFReader.changeState(ReadStateConfig.STATE_EDIT);
            }
        });
        mPDFReader.getMainFrame().getToolSetBar().addView(mMoreItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);
    }

    private void addReplaceListener(final CaretModule module) {
        if (module == null) return;
        mReplaceItem = new CircleItemImpl(mContext);
        mReplaceItem.setImageResource(R.drawable.annot_replace_selector);
        mReplaceItem.setTag(ToolbarItemConfig.ANNOTS_BAR_ITEM_REPLACE);
        mPDFReader.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getRPToolHandler());
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBarToReplace(module);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_REPLACE;
            }
        });
        mInsertTextItem.setEnable(true);
    }

    private void resetAnnotBarToReplace(final CaretModule module) {
        if (module == null) return;
        mPDFReader.getMainFrame().getToolSetBar().removeAllItems();
        mMoreItem = new CircleItemImpl(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (module.getRPToolHandler() == ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler()) {
                    if (mPDFReader.getMainFrame().getMoreToolsBar().isShowing()) {
                        Rect rect = new Rect();
                        mMoreItem.getContentView().getGlobalVisibleRect(rect);
                        mPDFReader.getMainFrame().getMoreToolsBar().update(new RectF(rect));
                    }
                }
            }
        };
        mMoreItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_MORE);
        mMoreItem.setImageResource(R.drawable.mt_more_selector);
        mMoreItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rect rect = new Rect();
                mMoreItem.getContentView().getGlobalVisibleRect(rect);
                mPDFReader.getMainFrame().getMoreToolsBar().show(new RectF(rect), true);
            }
        });

        mPropertyBar = mPDFReader.getMainFrame().getPropertyBar();
        mPropertyBar.setPropertyChangeListener(module);
        final CaretToolHandler toolHandler = (CaretToolHandler) module.getRPToolHandler();
        mPropertyItem = new PropertyCircleItemImp(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (toolHandler == ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler()) {
                    if (mPropertyBar.isShowing()) {
                        Rect mProRect = new Rect();
                        mPropertyItem.getContentView().getGlobalVisibleRect(mProRect);
                        mPropertyBar.update(new RectF(mProRect));
                    }
                }
            }
        };

        int[] colors = new int[PropertyBar.PB_COLORS_CARET.length];
        System.arraycopy(PropertyBar.PB_COLORS_CARET, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_CARET[0];
        mPropertyBar.setColors(colors);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, toolHandler.getColor());
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, toolHandler.getOpacity());
        mPropertyBar.setPropertyChangeListener(module);

        mPropertyItem.setTag(ToolbarItemConfig.ITEM_PROPERTY_TAG);
        mPropertyItem.setCentreCircleColor(toolHandler.getColor());
        mPropertyItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long supportProperty = PropertyBar.PROPERTY_COLOR
                        | PropertyBar.PROPERTY_OPACITY;
                mPropertyBar.setArrowVisible(true);
                mPropertyBar.reset(supportProperty);

                Rect rect = new Rect();
                mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                mPDFReader.getMainFrame().getPropertyBar().show(new RectF(rect), true);
            }
        });

        module.setColorChangeListener(new CaretModule.ColorChangeListener() {
            @Override
            public void onColorChange(int color) {
                mPropertyItem.setCentreCircleColor(color);
            }
        });
        mContinuousCreateItem = new CircleItemImpl(mContext);
        mContinuousCreateItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_CONTINUE);
        mIsContinuousCreate = false;
        ((CaretToolHandler) module.getRPToolHandler()).setIsContinuousCreate(false);
        if (mIsContinuousCreate) {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
        } else {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
        }
        mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                if (((CaretToolHandler) module.getRPToolHandler()).getIsContinuousCreate()) {
                    mIsContinuousCreate = false;
                    ((CaretToolHandler) module.getRPToolHandler()).setIsContinuousCreate(mIsContinuousCreate);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
                } else {
                    mIsContinuousCreate = true;
                    ((CaretToolHandler) module.getRPToolHandler()).setIsContinuousCreate(mIsContinuousCreate);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
                }

                AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinuousCreate);
            }
        });
        mOKItem = new CircleItemImpl(mContext);
        mOKItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_OK);
        mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
        mOKItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(null);
                mPDFReader.changeState(ReadStateConfig.STATE_EDIT);
            }
        });
        mPDFReader.getMainFrame().getToolSetBar().addView(mMoreItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);
    }

    BaseItem mInkItem;

    private void addInkListener(final InkModule module) {
        if (module == null) return;
        mInkItem = new CircleItemImpl(mContext);
        mInkItem.setTag(ToolbarItemConfig.ANNOTS_BAR_ITEM_PEN);
        mInkItem.setImageResource(R.drawable.annot_pencil_selector);
        mInkItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getToolHandler());
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBar(module, MoreTools.MT_TYPE_INK);
            }
        });
        mPDFReader.getMainFrame().getEditBar().addView(mInkItem, BaseBar.TB_Position.Position_CENTER);

        mPDFReader.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getToolHandler());
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBar(module, MoreTools.MT_TYPE_INK);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_INK;
            }
        });
        mInkItem.setEnable(true);
    }

    private AbstractToolHandler getToolHandler(Module module) {
        return getToolHandler(module, -1);
    }

    private AbstractToolHandler getToolHandler(Module module, int tag) {
        if (module == null) return null;
        AbstractToolHandler toolHandler = null;
        switch (module.getName()) {
            case Module.MODULE_NAME_ERASER:
                toolHandler = (AbstractToolHandler) ((EraserModule) module).getToolHandler();
                break;
            case Module.MODULE_NAME_INK:
                toolHandler = (AbstractToolHandler) ((InkModule) module).getToolHandler();
                break;
            case Module.MODULE_NAME_LINE:
                if (tag == MoreTools.MT_TYPE_ARROW) {
                    toolHandler = (AbstractToolHandler) ((LineModule) module).getArrowToolHandler();
                } else if (tag == MoreTools.MT_TYPE_LINE) {
                    toolHandler = (AbstractToolHandler) ((LineModule) module).getLineToolHandler();
                }
                break;
        }

        return toolHandler;
    }

    private void resetAnnotBar(final Module module, final int tag) {
        if (module == null) return;
        mPDFReader.getMainFrame().getToolSetBar().removeAllItems();

        // more annotation tools button
        mMoreItem = new CircleItemImpl(mContext) {
            public void onItemLayout(int l, int t, int r, int b) {
                if (getToolHandler(module, tag) == ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler()) {
                    if (mPDFReader.getMainFrame().getMoreToolsBar().isShowing()) {
                        Rect rect = new Rect();
                        mMoreItem.getContentView().getGlobalVisibleRect(rect);
                        mPDFReader.getMainFrame().getMoreToolsBar().update(new RectF(rect));
                    }
                }
            }
        };
        mMoreItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_MORE);
        mMoreItem.setImageResource(R.drawable.mt_more_selector);
        mMoreItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rect rect = new Rect();
                mMoreItem.getContentView().getGlobalVisibleRect(rect);
                mPDFReader.getMainFrame().getMoreToolsBar().show(new RectF(rect), true);
            }
        });

        mPropertyBar = mPDFReader.getMainFrame().getPropertyBar();
        // property button
        final PropertyCircleItem mPropertyBtn = new PropertyCircleItemImp(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (getToolHandler(module, tag) == ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler()) {
                    if (mPDFReader.getMainFrame().getMoreToolsBar().isShowing()) {
                        Rect rect = new Rect();
                        mMoreItem.getContentView().getGlobalVisibleRect(rect);
                        mPDFReader.getMainFrame().getMoreToolsBar().update(new RectF(rect));
                    }
                }
            }
        };
        mPropertyBtn.setTag(ToolbarItemConfig.ITEM_PROPERTY_TAG);
        final AbstractToolHandler toolHandler = getToolHandler(module, tag);
        final int color = toolHandler.getColor();
        final int opacity = toolHandler.getOpacity();
        final float thickness = toolHandler.getThickness();

        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, color);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, opacity);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_LINEWIDTH, thickness);
        mPropertyBar.setPropertyChangeListener(toolHandler);
        final long properties = toolHandler.getSupportedProperties();
        mPropertyBtn.setCentreCircleColor(color);
        mPropertyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (AppDisplay.getInstance(mContext).isPad()) {
                    mPropertyBar.setArrowVisible(true);
                } else {
                    mPropertyBar.setArrowVisible(false);
                }

                mPropertyBar.reset(properties);
                Rect rect = new Rect();
                mPropertyBtn.getContentView().getGlobalVisibleRect(rect);
                if (AppDisplay.getInstance(mContext).isPad()) {
                    mPropertyBar.show(new RectF(rect), true);
                } else {
                    mPropertyBar.show(new RectF(rect), true);
                }
            }
        });


        toolHandler.setColorChangeListener(new AbstractToolHandler.ColorChangeListener() {
            @Override
            public void onColorChange(int color) {
                mPropertyBtn.setCentreCircleColor(color);
            }
        });


        // continuous create annot button
        mContinuousCreateItem = new CircleItemImpl(mContext);
        mContinuousCreateItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_CONTINUE);
        mIsContinuousCreate = (tag == MoreTools.MT_TYPE_INK || tag == MoreTools.MT_TYPE_ERASER);
        toolHandler.setIsContinuousCreate(mIsContinuousCreate);
        if (mIsContinuousCreate) {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
        } else {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
        }
        mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                if (toolHandler.getIsContinuousCreate()) {
                    mIsContinuousCreate = false;
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
                } else {
                    mIsContinuousCreate = true;
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
                }
                toolHandler.setIsContinuousCreate(mIsContinuousCreate);

                AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinuousCreate);
            }
        });

        // end create annot button
        mOKItem = new CircleItemImpl(mContext);
        mOKItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_OK);
        mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
        mOKItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPDFReader.changeState(ReadStateConfig.STATE_EDIT);
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(null);
            }
        });

        mPDFReader.getMainFrame().getToolSetBar().addView(mMoreItem, BaseBar.TB_Position.Position_CENTER);
        if (!(module.getName().equalsIgnoreCase(Module.MODULE_NAME_INK)
                || module.getName().equalsIgnoreCase(Module.MODULE_NAME_ERASER))) {
            mPDFReader.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);
        }
        mPDFReader.getMainFrame().getToolSetBar().addView(mPropertyBtn, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
    }

    private void addEraserListener(final EraserModule module) {
        if (module == null) return;
        mPDFReader.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                if (!DocumentManager.getInstance(mPDFReader.getDocViewer()).canAddAnnot()) return;
                if (((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler() != module.getToolHandler()) {
                    ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getToolHandler());
                } else {
                    ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(null);
                }

                if (((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler() != null) {
                    mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                } else if (mPDFReader.getState() == ReadStateConfig.STATE_ANNOTTOOL){
                    mPDFReader.changeState(ReadStateConfig.STATE_EDIT);
                }

                resetAnnotBar(module, MoreTools.MT_TYPE_ERASER);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_ERASER;
            }
        });
    }

    BaseItem mLineItem;

    private void addLineListener(final LineModule module, final int tag) {
        if (module == null) return;
        mLineItem = new CircleItemImpl(mContext);
        mLineItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(getToolHandler(module, tag));
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBar(module, tag);
            }
        });
        mPDFReader.getMainFrame().getEditBar().addView(mInkItem, BaseBar.TB_Position.Position_CENTER);

        mPDFReader.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(getToolHandler(module, tag));
                mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                resetAnnotBar(module, tag);
            }

            @Override
            public int getType() {
                return tag;
            }
        });
        mLineItem.setEnable(true);
    }

    private CircleItem mUndoButton;
    private CircleItem		mRedoButton;
    private void addUndo(final UndoModule module) {
        if (module == null) return;
        mUndoButton = new CircleItemImpl(mContext);
        mUndoButton.setTag(ToolbarItemConfig.DONE_BAR_ITEM_UNDO);
        mUndoButton.setImageResource(R.drawable.annot_undo_pressed);
        mUndoButton.setEnable(false);
        mPDFReader.getMainFrame().getEditDoneBar().addView(mUndoButton, BaseBar.TB_Position.Position_LT);

        mRedoButton = new CircleItemImpl(mContext);
        mRedoButton.setTag(ToolbarItemConfig.DONE_BAR_ITEM_REDO);
        mRedoButton.setImageResource(R.drawable.annot_redo_pressed);
        mRedoButton.setEnable(false);
        mPDFReader.getMainFrame().getEditDoneBar().addView(mRedoButton, BaseBar.TB_Position.Position_LT);

        mUndoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick())
                    return;
                module.undo();
            }
        });

        mRedoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick())
                    return;
                module.redo();
            }
        });

        DocumentManager.getInstance(mPDFReader.getDocViewer()).registerUndoEventListener(new DocumentManager.IUndoEventListener() {
            @Override
            public void itemWillAdd(DocumentManager dm, IUndoItem item) {

            }

            @Override
            public void itemAdded(DocumentManager dm, IUndoItem item) {
                changeButtonStatus();
            }

            @Override
            public void willUndo(DocumentManager dm, IUndoItem item) {

            }

            @Override
            public void undoFinished(DocumentManager dm, IUndoItem item) {
                changeButtonStatus();
            }

            @Override
            public void willRedo(DocumentManager dm, IUndoItem item) {

            }

            @Override
            public void redoFinished(DocumentManager dm, IUndoItem item) {
                changeButtonStatus();
            }

            @Override
            public void willClearUndo(DocumentManager dm) {

            }

            @Override
            public void clearUndoFinished(DocumentManager dm) {
                changeButtonStatus();
            }
        });

        mPDFReader.registerStateChangeListener(new IStateChangeListener() {
            @Override
            public void onStateChanged(int oldState, int newState) {
                changeButtonStatus();
            }
        });
    }

    private void changeButtonStatus() {
        DocumentManager dm = DocumentManager.getInstance(mPDFReader.getDocViewer());
        if (dm.canUndo()) {
            mUndoButton.setImageResource(R.drawable.annot_undo_enabled);
            mUndoButton.setEnable(true);
        } else {
            mUndoButton.setImageResource(R.drawable.annot_undo_pressed);
            mUndoButton.setEnable(false);
        }
        if (dm.canRedo()) {
            mRedoButton.setImageResource(R.drawable.annot_redo_enabled);
            mRedoButton.setEnable(true);
        } else {
            mRedoButton.setImageResource(R.drawable.annot_redo_pressed);
            mRedoButton.setEnable(false);
        }
        if (((UIExtensionsManager) mPDFReader.getDocViewer().getUIExtensionsManager()).getCurrentToolHandler() != null
                && ((UIExtensionsManager) mPDFReader.getDocViewer().getUIExtensionsManager()).getCurrentToolHandler().getType().equals(ToolHandler.TH_TYPE_INK)) {
            mUndoButton.getContentView().setVisibility(View.INVISIBLE);
            mRedoButton.getContentView().setVisibility(View.INVISIBLE);
        } else {
            mUndoButton.getContentView().setVisibility(View.VISIBLE);
            mRedoButton.getContentView().setVisibility(View.VISIBLE);
        }
    }

    private void addFileAttachmentListener(final FileAttachmentModule module) {

        mPDFReader.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                if (type == MoreTools.MT_TYPE_FILEATTACHMENT) {
                    ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(module.getToolHandler());
                    mPDFReader.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                    resetAnnotBarToFileAttachment(module);
                    module.resetPropertyBar();
                }

            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_FILEATTACHMENT;
            }
        });

    }


    private void resetAnnotBarToFileAttachment(final FileAttachmentModule module) {
        //PropertyBar
        mPropertyBar = module.getPropertyBar();
        mPDFReader.getMainFrame().getToolSetBar().removeAllItems();
        mMoreItem = new CircleItemImpl(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler() == module.getToolHandler()) {
                    Rect rect = new Rect();
                    mMoreItem.getContentView().getGlobalVisibleRect(rect);
                    mPDFReader.getMainFrame().getMoreToolsBar().update(new RectF(rect));
                }
            }
        };

        mMoreItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_MORE);
        mMoreItem.setImageResource(R.drawable.mt_more_selector);
        mMoreItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rect rect = new Rect();
                mMoreItem.getContentView().getGlobalVisibleRect(rect);
                mPDFReader.getMainFrame().getMoreToolsBar().show(new RectF(rect), true);
            }
        });

        mOKItem = new CircleItemImpl(mContext);
        mOKItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_OK);
        mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
        mOKItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPDFReader.changeState(ReadStateConfig.STATE_EDIT);
                ((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).setCurrentToolHandler(null);
            }
        });

        mPropertyItem = new PropertyCircleItemImp(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (((UIExtensionsManager) mPDFReader.getUIExtensionsManager()).getCurrentToolHandler() == module.getToolHandler()) {
                    if (mPropertyBar.isShowing()) {
                        Rect rect = new Rect();
                        mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                        mPropertyBar.update(new RectF(rect));
                    }
                }
            }
        };
        mPropertyItem.setTag(ToolbarItemConfig.ITEM_PROPERTY_TAG);
        mPropertyItem.setCentreCircleColor(((FileAttachmentToolHandler)module.getToolHandler()).getColor());

        final Rect rect = new Rect();
        mPropertyItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPropertyBar.setArrowVisible(true);
                mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                mPropertyBar.show(new RectF(rect), true);
            }
        });
        module.setColorChangeListener(new FileAttachmentModule.ColorChangeListener() {
            @Override
            public void onColorChange(int color) {
                mPropertyItem.setCentreCircleColor(color);
            }
        });

        mContinuousCreateItem = new CircleItemImpl(mContext);
        mContinuousCreateItem.setTag(ToolbarItemConfig.ANNOT_BAR_ITEM_CONTINUE);
        mIsContinuousCreate = false;
        ((FileAttachmentToolHandler) module.getToolHandler()).setIsContinuousCreate(false);
        if (mIsContinuousCreate) {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
        } else {
            mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
        }
        mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                if (mIsContinuousCreate) {
                    mIsContinuousCreate = true;
                    ((FileAttachmentToolHandler) module.getToolHandler()).setIsContinuousCreate(mIsContinuousCreate);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_false_selector);
                } else {
                    mIsContinuousCreate = true;
                    ((FileAttachmentToolHandler) module.getToolHandler()).setIsContinuousCreate(mIsContinuousCreate);
                    mContinuousCreateItem.setImageResource(R.drawable.rd_annot_create_continuously_true_selector);
                }
                AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinuousCreate);
            }
        });

        mPDFReader.getMainFrame().getToolSetBar().addView(mMoreItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
        mPDFReader.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);

    }

    public void setFilePath(String path) {
        mDocPath = path;
        MoreMenuModule module = ((MoreMenuModule) ((UIExtensionsManager)mDocViewerCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_MORE_MENU));
        if(module != null) {
            module.setFilePath(path);
        }
    }

    private void closeAllDocuments() {
        if (!bDocClosed) {
            _closeDocument();
        }else if (mMainFrame.getAttachedActivity() != null) {
            mMainFrame.getAttachedActivity().finish();
        }
    }

    void _closeDocument() {
        getDocViewer().closeDoc();
        mMainFrame.resetMaskView();
    }

    public void closeDocumentSucceed() {
        if (mMainFrame != null && mMainFrame.getAttachedActivity() != null && mMainFrame.getCloseAttachedActivity()) {
            mMainFrame.getAttachedActivity().finish();
        }
        if(isSaveDocInCurPath) {
            File file = new File(currentFileCachePath);
            File docFile = new File(mDocPath);
            if (file.exists()) {
                docFile.delete();
                if (!file.renameTo(docFile))
                    UIToast.getInstance(mContext).show("Save document failed!");
            }else{
                UIToast.getInstance(mContext).show("Save document failed!");
            }

        }

    }

    public void openDocumentFailed() {
        if (mMainFrame.getAttachedActivity() != null)
            mMainFrame.getAttachedActivity().finish();
    }

    public void openDocument(String path, byte[] password) {
        if (mProgressDlg == null && mMainFrame.getAttachedActivity() != null) {
            mProgressDlg = new ProgressDialog(mMainFrame.getAttachedActivity());
            mProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDlg.setCancelable(false);
            mProgressDlg.setIndeterminate(false);
        }

        if (mProgressDlg != null && !mProgressDlg.isShowing()) {
            mProgressDlg.setMessage("opening");
            mProgressDlg.show();
        }
        setFilePath(path);
        mDocViewerCtrl.openDoc(path, password);
    }

    @Override
    public String getName() {
        return MODULE_NAME_PDFREADER;
    }

    @Override
    public boolean loadModule() {
        mMainFrame = new MainFrame(mContext, mModulesConfig);
        init();
        return true;
    }

    @Override
    public boolean unloadModule() {
        return true;
    }

    public void enableTopToolbar(boolean isEnabled) {
        if (mMainFrame != null) {
            mMainFrame.enableTopToolbar(isEnabled);
        }
    }

    public void enableBottomToolbar(boolean isEnabled) {
        if (mMainFrame != null) {
            mMainFrame.enableBottomToolbar(isEnabled);
        }
    }
}
