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
package com.foxit.uiextensions.home.local;

import android.content.Context;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.RelativeLayout;

import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.filebrowser.FileBrowser;
import com.foxit.uiextensions.controls.filebrowser.FileComparator;
import com.foxit.uiextensions.controls.filebrowser.FileDelegate;
import com.foxit.uiextensions.controls.filebrowser.imp.FileBrowserImpl;
import com.foxit.uiextensions.controls.filebrowser.imp.FileItem;
import com.foxit.uiextensions.controls.filebrowser.imp.FileThumbnail;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.home.IHomeModule;
import com.foxit.uiextensions.home.view.PathView;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppStorageManager;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.UIToast;
import com.foxit.uiextensions.utils.thread.AppAsyncTask;
import com.foxit.uiextensions.utils.thread.AppThreadManager;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocalModule implements Module, IHomeModule{
    public static final int STATE_NORMAL = 0;
    public static final int STATE_ALL_PDF = 1;

    protected static final int MSG_UPDATE_PDFs = 11002;
    protected static final int MSG_PDFs_STOP = 11008;

    protected static final int MSG_FILE_OBSERVER = 11010;
    protected static final int MSG_UPDATE_THUMBNAIL = 11012;
    private final Context mContext;

    private RelativeLayout mRootView;
    private RelativeLayout mContentView;
    private RelativeLayout mTopToolBar;
    private LocalView mLocalView;
    private PathView mPathView;
    private BaseBar mTopBar;
    private FileBrowser mFileBrowser;
    private FileObserver mFileObserver;

    private String mCurrentPath;
    private int mCurrentState = STATE_ALL_PDF;
    private int mSortMode = 1;
    private boolean isSortUp = true;

    private onFileItemEventListener mOnFileItemEventListener = null;

    private final List<FileItem> mFileItems = new ArrayList<FileItem>();
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case MSG_UPDATE_PDFs:
                    if (msg.obj instanceof FileItem[]) {
                        FileItem[] items = (FileItem[]) msg.obj;
                        if (mCurrentState == STATE_ALL_PDF) {
                            Collections.addAll(mFileItems, items);
                        }
                        mFileBrowser.updateDataSource(true);
                    }
                    break;
                case MSG_FILE_OBSERVER:
                    mFileBrowser.clearCheckedItems();
                    mFileItems.clear();
                    LocalTask.AllPDFs.start(mContext, mHandler);
                    mFileBrowser.updateDataSource(false);
                    break;
                case MSG_UPDATE_THUMBNAIL:
                    mFileBrowser.updateDataSource(false);
                default:
                    break;
            }
        }
    };

    @Override
    public String getName() {
        return MODULE_NAME_LOCAL;
    }

    public LocalModule(Context context) {
        mContext = context;
        mFileObserver = new SDCardFileObserver(Environment.getExternalStorageDirectory().getPath());
    }

    @Override
    public boolean loadModule() {
        mFileObserver.startWatching();
        loadHomeModule(mContext);
        onActivated();
        return true;
    }

    @Override
    public boolean unloadModule() {
        mFileObserver.stopWatching();
        mOnFileItemEventListener = null;
        return true;
    }

    @Override
    public String getTag() {
        return HOME_MODULE_TAG_LOCAL;
    }

    @Override
    public void loadHomeModule(Context context) {
        if (context == null) return;
        initItems(context);
        if (mTopBar == null) {
            mTopBar = new TopBarImpl(context);
            mTopBar.setBackgroundColor(context.getResources().getColor( R.color.ux_text_color_subhead_colour));
        }
        if (mLocalView == null) {
            mLocalView = new LocalView(context);
            mPathView = new PathView(context);

            mFileBrowser = new FileBrowserImpl(context, mFileBrowserDelegate);
            mLocalView.addFileView(mFileBrowser.getContentView());
            mPathView.setPathChangedListener(new PathView.pathChangedListener() {
                @Override
                public void onPathChanged(String newPath) {
                    mFileBrowser.setPath(newPath);
                }
            });
        }
        if (AppFileUtil.isSDAvailable()) {
            File file = new File(AppFileUtil.getSDPath() + File.separator + "FoxitSDK");
            if (!file.exists())
                file.mkdirs();
            if (!file.exists()) {
                mCurrentPath = AppFileUtil.getSDPath();
            } else {
                mCurrentPath = file.getPath();
            }
            mPathView.setPath(mCurrentPath);
            mFileBrowser.setPath(mCurrentPath);

            if (!new File(file.getPath() + File.separator + "Sample.pdf").exists()) {
                CopyAsy task = new CopyAsy();
                AppThreadManager.getInstance().startThread(task, file.getPath());
            }
            if (!new File(file.getPath() + File.separator + "complete_pdf_viewer_guide_android.pdf").exists()) {
                CopyAsy task = new CopyAsy();
                AppThreadManager.getInstance().startThread(task, file.getPath());
            }
        }

        if (AppDisplay.getInstance(context).isPad())
            mRootView = (RelativeLayout) View.inflate(mContext, R.layout.hf_home_right_pad, null);
        else
            mRootView = (RelativeLayout) View.inflate(mContext, R.layout.hf_home_right_phone, null);

        mTopToolBar = (RelativeLayout) mRootView.findViewById(R.id.toptoolbar);
        mContentView = (RelativeLayout) mRootView.findViewById(R.id.contentview);

        mContentView.removeAllViews();
        mContentView.addView(mLocalView);

        View view = mTopBar.getContentView();
        if (view == null) {
            mTopToolBar.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mContentView.getLayoutParams();
            params.topMargin = 0;
            mContentView.setLayoutParams(params);
        } else {
            mTopToolBar.setVisibility(View.VISIBLE);
            mTopToolBar.addView(view);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mContentView.getLayoutParams();
            if (AppDisplay.getInstance(context).isPad())
                params.topMargin = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
            else
                params.topMargin = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_phone);
            mContentView.setLayoutParams(params);
        }

        resetSortMode(); //init sort mode
//        mCurrentState = STATE_ALL_PDF;
//        setStateAllPDFs();

        //List folders and PDFs
        switchState(STATE_NORMAL);
    }

    @Override
    public void unloadHomeModule(Context context) {
    }

    @Override
    public View getTopToolbar(Context context) {
        return mTopBar.getContentView();
    }

    @Override
    public View getContentView(Context context) {
        return mRootView;
    }

    @Override
    public boolean isNewVersion() {
        return false;
    }

    private void setStateAllPDFs() {
        mTopBar.removeAllItems();
        mTopBar.addView(mDocumentItem, BaseBar.TB_Position.Position_LT);
        mLocalView.removeAllTopView();
        mLocalView.setTopLayoutVisible(false);
        mLocalView.setBottomLayoutVisible(false);
    }

    private void setStateNormal() {
        mTopBar.removeAllItems();
        mTopBar.addView(mDocumentItem, BaseBar.TB_Position.Position_LT);
        mLocalView.removeAllTopView();
        mLocalView.setTopLayoutVisible(!AppUtil.isEmpty(mFileBrowser.getDisplayPath()));
        mLocalView.addPathView(mPathView.getContentView());

        mLocalView.setBottomLayoutVisible(false);
    }

    @Override
    public void onActivated() {
        if (mCurrentState == STATE_ALL_PDF) {
            setStateAllPDFs();
            LocalTask.AllPDFs.stop();
            mFileItems.clear();
            LocalTask.AllPDFs.start(mContext, mHandler);
            mFileBrowser.updateDataSource(true);
        }
    }

    @Override
    public void onDeactivated() {
    }

    @Override
    public boolean onWillDestroy() {
        return false;
    }

    @Override
    public void setFileItemEventListener(onFileItemEventListener listener) {
        mOnFileItemEventListener = listener;
    }


    private FileDelegate mFileBrowserDelegate = new FileDelegate() {

        @Override
        public List<FileItem> getDataSource() {
            return mFileItems;
        }

        @Override
        public void onPathChanged(String path) {
            if (mCurrentState != STATE_NORMAL) return;
            if (AppUtil.isEmpty(path)) {
                mPathView.setPath(null);
                mLocalView.setTopLayoutVisible(false);
                mFileItems.clear();
                List<String> paths = AppStorageManager.getInstance(mContext).getVolumePaths();
                for (String p : paths) {
                    File f = new File(p);
                    FileItem item = new FileItem();
                    item.parentPath = path;
                    item.path = f.getPath();
                    item.name = f.getName();
                    item.date = AppDmUtil.getLocalDateString(AppDmUtil.javaDateToDocumentDate(f.lastModified()));
                    item.lastModifyTime = f.lastModified();
                    item.type = FileItem.TYPE_ROOT;
                    File[] fs = f.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            if (pathname.isHidden() || !pathname.canRead()) return false;
                            return true;
                        }
                    });
                    if (fs != null) {
                        item.fileCount = fs.length;
                    } else {
                        item.fileCount = 0;
                    }
                    mFileItems.add(item);
                }
                return;
            }
            File file = new File(path);
            if (!file.exists()) return;
            File[] files;
            try {
                files = file.listFiles(mFileFilter);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            mCurrentPath = path;
            mLocalView.setTopLayoutVisible(true);
            mFileItems.clear();
            mPathView.setPath(mCurrentPath);
            if (files == null) return;
            for (File f : files) {
                FileItem item = new FileItem();
                item.parentPath = file.getPath();
                item.path = f.getPath();
                item.name = f.getName();
                item.date = AppDmUtil.getLocalDateString(AppDmUtil.javaDateToDocumentDate(f.lastModified()));
                item.lastModifyTime = f.lastModified();
                if (f.isDirectory()) {
                    item.type = FileItem.TYPE_FOLDER;
                    File[] fs = f.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            if (pathname.isHidden() || !pathname.canRead()) return false;
                            if (pathname.isDirectory()) return true;
                            return pathname.isFile() && pathname.getName().toLowerCase().endsWith(".pdf");
                        }
                    });
                    if (fs != null) {
                        item.fileCount = fs.length;
                    } else {
                        item.fileCount = 0;
                    }
                    mFileItems.add(item);
                    continue;
                }
                item.type = FileItem.TYPE_FILE;
                item.size = AppFileUtil.formatFileSize(f.length());
                item.length = f.length();
                mFileItems.add(item);
            }

            Collections.sort(mFileItems, mFileBrowser.getComparator());
        }

        @Override
        public void onItemClicked(View view, FileItem item) {
            if (item.type == FileItem.TYPE_FOLDER || item.type == FileItem.TYPE_ROOT) {
                mFileBrowser.setPath(item.path);
            } else if ((item.type & FileItem.TYPE_FILE) != 0) {
//                if (mContext == null) return;
//                Intent intent = new Intent();
//                intent.putExtra("filePath", item.path);
//                intent.setClass(mContext, mReadActivityClass);
//                mContext.startActivity(intent);

                if (mOnFileItemEventListener != null) {
                    mOnFileItemEventListener.onFileItemClicked(FILE_EXTRA, item.path);
                } else {
                    UIToast.getInstance(mContext).show("The OnFileItemEventListener is null");
                }
            }
        }

        @Override
        public void onItemsCheckedChanged(boolean isAllSelected, int folderCount, int fileCount) {

        }

    };

    private void switchState(int state) {
        if (mCurrentState == state) return;
        if (mCurrentState == STATE_ALL_PDF) {
            LocalTask.AllPDFs.stop();
        }
        if (state == STATE_NORMAL) {
            mCurrentState = state;
            setStateNormal();
            mFileBrowser.setPath(mFileBrowser.getDisplayPath());
            return;
        }

        if (state == STATE_ALL_PDF) {
            mCurrentState = state;
            setStateAllPDFs();

            mFileItems.clear();
            LocalTask.AllPDFs.start(mContext, mHandler);

            mFileBrowser.updateDataSource(true);
        }
    }

    private void resetSortMode() {
        if (mSortMode == 0) {
            if (isSortUp) {
                mFileBrowser.getComparator().setOrderBy(FileComparator.ORDER_TIME_UP);
            } else {
                mFileBrowser.getComparator().setOrderBy(FileComparator.ORDER_TIME_DOWN);
            }
        } else if (mSortMode == 1) {
            if (isSortUp) {
                mFileBrowser.getComparator().setOrderBy(FileComparator.ORDER_NAME_UP);
            } else {
                mFileBrowser.getComparator().setOrderBy(FileComparator.ORDER_NAME_DOWN);
            }
        } else if (mSortMode == 2) {
            if (isSortUp) {
                mFileBrowser.getComparator().setOrderBy(FileComparator.ORDER_SIZE_UP);
            } else {
                mFileBrowser.getComparator().setOrderBy(FileComparator.ORDER_SIZE_DOWN);
            }
        }
        if (!mFileItems.isEmpty()) {
            Collections.sort(mFileItems, mFileBrowser.getComparator());
            mFileBrowser.updateDataSource(true);
        }
    }

    private FileFilter mFileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            if (pathname.isHidden() || !pathname.canRead()) return false;
            if (mCurrentState == STATE_NORMAL) {
                if (pathname.isDirectory()) return true;
            }
            String name = pathname.getName().toLowerCase();
            return pathname.isFile() && name.endsWith(".pdf");
        }
    };

    private BaseItemImpl mDocumentItem;

    private void initItems(Context context) {

        mDocumentItem = new BaseItemImpl(context);
        mDocumentItem.setText(R.string.hm_document);
        mDocumentItem.setTextSize(AppDisplay.getInstance(context).px2dp(context.getResources().getDimensionPixelOffset(R.dimen.ux_text_height_title)));
        mDocumentItem.setTextColor(context.getResources().getColor(R.color.ux_color_white));
    }

    public void updateStoragePermissionGranted() {
        resetSortMode(); //init sort mode
        mCurrentState = STATE_ALL_PDF;
        switchState(STATE_NORMAL);
        onActivated();
    }

    public void updateThumbnail(String filePath) {
        mFileBrowser.updateThumbnail(filePath, new FileThumbnail.ThumbnailCallback() {
            @Override
            public void result(boolean succeed, String filePath) {
                Message msg = new Message();
                msg.what = MSG_UPDATE_THUMBNAIL;
                mHandler.sendMessage(msg);
            }
        });
    }

    class SDCardFileObserver extends FileObserver {
        public SDCardFileObserver(String path, int mask) {
            super(path, mask);
        }

        public SDCardFileObserver(String path) {
            super(path);
        }

        @Override
        public void onEvent(int event, String path) {
            final int action = event & FileObserver.ALL_EVENTS;
            switch (action) {
                case FileObserver.ACCESS:
                case FileObserver.OPEN:
                    break;
                case FileObserver.CREATE:
                case FileObserver.DELETE:
                case FileObserver.MODIFY:

                    Message msg = new Message();
                    msg.what = MSG_FILE_OBSERVER;
                    mHandler.sendMessage(msg);

//                    mFileBrowser.clearCheckedItems();
//                    mFileItems.clear();
//                    LocalTask.AllPDFs.start(mContext, mHandler);
//                    mFileBrowser.updateDataSource(false);
                    break;
            }
        }

    }


    class CopyAsy extends AppAsyncTask {

        @Override
        public String doInBackground(Object... params) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File[] files = new File[2];
                files[0] = new File(params[0] + File.separator + "Sample.pdf");
                files[1] = new File(params[0] + File.separator + "complete_pdf_viewer_guide_android.pdf");
                String[] assertFiles = new String[]{"Sample.pdf", "complete_pdf_viewer_guide_android.pdf"};
                if (mergeFiles(files, assertFiles)) {
                    return params[0] + File.separator + "Sample.pdf";
                }
            }
            return null;
        }

        @Override
        public void onPostExecute(Object result) {
            if (result != null) {
                mFileBrowser.setPath(mCurrentPath);
                mFileBrowser.updateDataSource(true);
            }
        }
    }

    private boolean mergeFiles(File[] outFile, String[] files) {
        boolean success = false;
        for(int i = 0; i < outFile.length; i++) {
            OutputStream os = null;

            try {
                os = new FileOutputStream(outFile[i]);
                byte[] buffer = new byte[1 << 13];

                InputStream is = mContext.getAssets().open(files[i]);
                int len = is.read(buffer);
                while (len != -1) {
                    os.write(buffer, 0, len);
                    len = is.read(buffer);
                }
                is.close();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (os != null) {
                        os.flush();
                        os.close();
                        success = true;
                    }
                } catch (IOException ignore) {
                }
            }
        }
        return success;
    }
}
