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
package com.foxit.uiextensions.modules.thumbnail;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UIFileSelectDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UIFolderSelectDialog;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.BaseItem;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.modules.PageNavigationModule;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.OnPageEventListener;
import com.foxit.uiextensions.utils.UIToast;

import java.io.File;
import java.io.FileFilter;

import static com.foxit.uiextensions.DocumentManager.getInstance;


public class ThumbnailSupport extends android.support.v4.app.DialogFragment implements View.OnClickListener, ThumbnailAdapterCallback {

    private final static String TAG = ThumbnailSupport.class.getSimpleName();
    private Context mContext;
    private Activity mAttachActivity;
    private AppDisplay mDisplay;
    private BaseBar mThumbnailTopBar;

    private BaseItem mSelectAllItem;
    private BaseItem mInsertItem;
    private BaseItem mThumbnailTitle;

    private GridLayoutManager mGridLayoutManager;
    private ThumbnailAdapter mAdapter;
    private RecyclerView mThumbnailGridView;
    private ThumbnailItem mCurEditItem;
    private boolean mbEditMode = false;
    private int mSpanCount;
    private int mVerSpacing;
    final private int mHorSpacing = 5;
    private PDFViewCtrl mPDFView;
    private Point mThumbnailSize;
    private boolean mbNeedRelayout = false;
    private UIFileSelectDialog mFileSelectDialog = null;
    private UIFolderSelectDialog mFolderSelectDialog = null;
    private ProgressDialog mProgressDialog = null;

    private View bottomBar;
    private TextView deleteTV;
    private TextView copyTV;
    private TextView extractTV;
    private TextView rotateTV;

    private AlertDialog alertDialog;
    public final static int ALBUM_REQUEST_CODE = 1;
    public final static int CROP_REQUEST = 2;
    public final static int CAMERA_REQUEST_CODE = 3;
    public static String SAVED_IMAGE_DIR_PATH = Environment.getExternalStorageDirectory().getPath()
                    + "/FoxitSDK/camera/";// Photo storage path
    String cameraPath;


    public PDFViewCtrl getPDFView() {
        return mPDFView;
    }

    public boolean isEditMode() {
        return mbEditMode;
    }

    public Context getContext() {
        return mContext;
    }

    public void init(PDFViewCtrl pdfViewCtrl) {
        mPDFView = pdfViewCtrl;
        mPDFView.registerPageEventListener(mPageEventListener);
    }

    protected UIFileSelectDialog getFileSelectDialog() {
        if (mFileSelectDialog == null) {
            mFileSelectDialog = new UIFileSelectDialog(mAttachActivity, null);
            mFileSelectDialog.init(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return !(pathname.isHidden() || !pathname.canRead()) && !(pathname.isFile() && !pathname.getName().toLowerCase().endsWith(".pdf"));
                }
            }, true);
            mFileSelectDialog.setTitle(AppResource.getString(mContext, R.string.fx_string_import));
            mFileSelectDialog.setCanceledOnTouchOutside(true);
        }
        return mFileSelectDialog;
    }

    public ThumbnailAdapter.ThumbViewHolder getViewHolderByItem(ThumbnailItem item) {
        int position = mAdapter.mThumbnailList.indexOf(item);
        ThumbnailAdapter.ThumbViewHolder viewHolder = (ThumbnailAdapter.ThumbViewHolder) mThumbnailGridView.findViewHolderForAdapterPosition(position);
        return viewHolder;
    }

    public boolean isThumbnailItemVisible(ThumbnailItem item) {
        int position = mAdapter.mThumbnailList.indexOf(item);
        return position >= mGridLayoutManager.findFirstVisibleItemPosition() && position <= mGridLayoutManager.findLastVisibleItemPosition();
    }

    protected UIFolderSelectDialog getFolderSelectDialog() {
        if (mFolderSelectDialog == null) {
            if (mPDFView.getUIExtensionsManager() == null) {
                return null;
            }
            Context context = ((UIExtensionsManager) mPDFView.getUIExtensionsManager()).getAttachedActivity();
            if (context == null) {
                return null;
            }
            mFolderSelectDialog = new UIFolderSelectDialog(context);
            mFolderSelectDialog.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return !(pathname.isHidden() || !pathname.canRead()) && !pathname.isFile();
                }
            });
            mFolderSelectDialog.setTitle(AppResource.getString(mContext, R.string.fx_string_extract));
            mFolderSelectDialog.setButton(MatchDialog.DIALOG_OK);
            mFolderSelectDialog.setCanceledOnTouchOutside(true);
        }
        return mFolderSelectDialog;
    }

    ProgressDialog getProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mAttachActivity);
            mProgressDialog.setCancelable(false);
        }
        return mProgressDialog;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        computeSize();
        if (mGridLayoutManager != null) {
            mGridLayoutManager.setSpanCount(mSpanCount);
            mGridLayoutManager.requestLayout();
        }
        if (mFileSelectDialog != null && mFileSelectDialog.isShowing()) {
            mFileSelectDialog.setHeight(mFileSelectDialog.getDialogHeight());
            mFileSelectDialog.showDialog();
        }
        if (mFolderSelectDialog != null && mFolderSelectDialog.isShowing()) {
            mFolderSelectDialog.setHeight(mFolderSelectDialog.getDialogHeight());
            mFolderSelectDialog.showDialog();
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return initView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ALBUM_REQUEST_CODE) {
                Uri uri = data.getData();
                Log.d(TAG, "path=" + getAbsolutePath(mContext, uri));
                String path = getAbsolutePath(mContext, uri);
                //if cannot get path data when sometime. We should get the path use other way.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && AppUtil.isBlank(path)) {
                    if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                        final String docId = DocumentsContract.getDocumentId(uri);
                        final String[] split = docId.split(":");
                        final String type = split[0];

                        Uri contentUri = null;
                        if ("image".equals(type)) {
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        }

                        final String selection = "_id=?";
                        final String[] selectionArgs = new String[]{
                                split[1]
                        };

                        path = getAbsolutePath(mContext, contentUri, selection, selectionArgs);
                    }else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())){
                        Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                                Long.valueOf(DocumentsContract.getDocumentId(uri)));
                        path = getAbsolutePath(mContext, contentUri, null, null);
                    }else if ("com.android.externalstorage.documents".equals(uri.getAuthority())){
                        String[] split = DocumentsContract.getDocumentId(uri).split(":");
                        if ("primary".equalsIgnoreCase(split[0])) {
                            path = Environment.getExternalStorageDirectory() + "/" + split[1];
                        }
                    }
                }
                boolean result = mAdapter.importPagesFromDCIM(mAdapter.getEditPosition(), path);
                if (!result) {
                    try {
                        if (mPDFView.getDoc().isXFA()) {
                            Toast.makeText(getActivity(), "XFA file is not supported to add image.", Toast.LENGTH_LONG).show();
                        }else {
                            Toast.makeText(getActivity(), AppResource.getString(mContext, R.string.rv_page_import_error),Toast.LENGTH_LONG).show();
                        }
                    } catch (PDFException ee) {
                        ee.printStackTrace();
                    }
                }
            } else if (requestCode == CAMERA_REQUEST_CODE) {
                boolean result = mAdapter.importPagesFromCamera(mAdapter.getEditPosition(), cameraPath);
                if (!result) {
                    Log.e(TAG, "add new page fail...");
                    try {
                        if (mPDFView.getDoc().isXFA()) {
                            Toast.makeText(getActivity(), "XFA file is not supported to add image.", Toast.LENGTH_LONG).show();
                        }else {
                            Toast.makeText(getActivity(), AppResource.getString(mContext, R.string.rv_page_import_error),Toast.LENGTH_LONG).show();
                        }
                    } catch (PDFException ee) {
                        ee.printStackTrace();
                    }
                }
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
            return;
        }
    }

    public String getAbsolutePath(final Context context, final Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri,
                    new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    public String getAbsolutePath(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public void startCamera() {
        if (Build.VERSION.SDK_INT >= 23) {
            int permission = ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.CAMERA);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
                return;
            }
        }
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            cameraPath = SAVED_IMAGE_DIR_PATH + System.currentTimeMillis() + ".png";
            Intent intent = new Intent();
            // set the action to open system camera.
            intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
            String out_file_path = SAVED_IMAGE_DIR_PATH;
            File dir = new File(out_file_path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // file path 2 uri
            Uri uri = Uri.fromFile(new File(cameraPath));
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(mContext, "Please confirm that the SD card has been inserted",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAttachActivity = this.getActivity();
        mContext = mAttachActivity.getApplicationContext();
        mDisplay = AppDisplay.getInstance(mContext);
        mAdapter = new ThumbnailAdapter(this);
        computeSize();
        int theme;
        if (Build.VERSION.SDK_INT >= 21) {
            theme = android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen;
        } else if (Build.VERSION.SDK_INT >= 14) {
            theme = android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen;
        } else if (Build.VERSION.SDK_INT >= 13) {
            theme = android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen;
        } else {
            theme = android.R.style.Theme_Light_NoTitleBar_Fullscreen;
        }
        setStyle(STYLE_NO_TITLE, theme);
        initAlert();
    }

    @Override
    public void onDetach() {
        getProgressDialog().dismiss();
        mAdapter.clear();
        mPDFView.unregisterPageEventListener(mPageEventListener);
        super.onDetach();
    }


    private void initAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mAttachActivity);
        String[] items = new String[]{
                AppResource.getString(mContext, R.string.thumbnail_import_file),
                AppResource.getString(mContext, R.string.thumbnail_import_dcim),
                AppResource.getString(mContext, R.string.thumbnail_import_camera),
        };

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                switch (which) {
                    case 0: // from file
                        mAdapter.importPagesFromSpecialFile(mAdapter.getEditPosition());
                        break;
                    case 1: // from album
//                        mAdapter.importPagesFromDCIM(mAdapter.getEditPosition());
                        //19 == Build.VERSION_CODES.KITKAT
                        String action = Build.VERSION.SDK_INT >= 19 ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT;
                        Intent intent = new Intent(action,null);
                        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(intent, ALBUM_REQUEST_CODE);
                        break;
                    case 2: // from camera
                        //mAdapter.importPagesFromCamera(mAdapter.getEditPosition());
                        startCamera();
                        break;
                    default:
                        break;
                }
            }
        });

        builder.setCancelable(true);
        alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(true);
    }

    private void resetCurEditThumbnailItem() {
        if (mCurEditItem != null) {
            mCurEditItem.editViewFlag = ThumbnailItem.EDIT_NO_VIEW;
            int position = mAdapter.mThumbnailList.indexOf(mCurEditItem);
            ThumbnailAdapter.ThumbViewHolder viewHolder = getViewHolderByItem(mCurEditItem);
            if (viewHolder != null) {
                viewHolder.changeLeftEditView(position, true);
                viewHolder.changeRightEditView(position, true);
            }
            mCurEditItem = null;
        }
    }

    private void changeCurEditThumbnailItem(final int position, int flags) {
        mCurEditItem = mAdapter.mThumbnailList.get(position);
        mCurEditItem.editViewFlag = flags;
    }

    //remove bitmap from cache.
    private final PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener() {
        @Override
        public void onPagesRemoved(boolean success, int[] pageIndexes) {
            if (!success) {
                showTips(AppResource.getString(mContext, R.string.rv_page_remove_error));
                return;
            }
            mCurEditItem = null;
            mbNeedRelayout = true;
            for (int i = 0; i < pageIndexes.length; i++) {
                ThumbnailItem item = mAdapter.mThumbnailList.get(pageIndexes[i] - i);
                mAdapter.updateCacheListInfo(item, false);
                mAdapter.updateSelectListInfo(item, false);
                mAdapter.mThumbnailList.remove(item);
            }
            updateTopLayout();
        }

        @Override
        public void onPageMoved(boolean success, int index, int dstIndex) {
            if (success)
                mbNeedRelayout = true;
        }

        @Override
        public void onPagesRotated(boolean success, int[] pageIndexes, int rotation) {
            if (!success) {
                showTips(AppResource.getString(mContext, R.string.rv_page_rotate_error));
                return;
            }
            mbNeedRelayout = true;
            for (int i = 0; i < pageIndexes.length; i++) {
                ThumbnailItem item = mAdapter.mThumbnailList.get(pageIndexes[i]);
                mAdapter.updateCacheListInfo(item, false);
            }
            updateTopLayout();
        }

        @Override
        public void onPagesInserted(boolean success, int dstIndex, int[] range) {
            if (!success) {
                showTips(AppResource.getString(mContext, R.string.rv_page_import_error));
                return;
            }
            mbNeedRelayout = true;

            boolean a = (range.length == 2 && range[range.length - 1] > 1) ? true : false;

            if (a) {
                for (int i = 0; i < range.length / 2; i++) {
                    for (int index = range[2 * i]; index < range[2 * i + 1]; index++) {
                        ThumbnailItem item = new ThumbnailItem(dstIndex, getThumbnailBackgroundSize(), mPDFView);
                        mAdapter.mThumbnailList.add(dstIndex, item);
                        dstIndex++;
                    }
                }
            } else {
                for (int i = 0; i < range.length / 2; i++) {
                    ThumbnailItem item = new ThumbnailItem(dstIndex, getThumbnailBackgroundSize(), mPDFView);
                    mAdapter.mThumbnailList.add(dstIndex, item);
                    dstIndex++;
                }
            }
            updateTopLayout();
        }

        private void updateTopLayout() {
            mAttachActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setSelectViewMode(mAdapter.isSelectedAll());
                }
            });
        }

        private void showTips(final String tips) {
            mAttachActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    UIToast.getInstance(mContext).show(tips, Toast.LENGTH_LONG);
                }
            });
        }
    };

    private void updateRecycleLayout() {
        if (mbNeedRelayout) {
            mPDFView.updatePagesLayout();
        }
    }

    private View initView() {
        View dialogView = View.inflate(mAttachActivity, R.layout.rd_thumnail_dialog, null);
        final LinearLayout thumbnailLayout = (LinearLayout) dialogView.findViewById(R.id.thumbnailist);
        mThumbnailGridView = (RecyclerView) dialogView.findViewById(R.id.thumbnail_grid_view);
        //bottomBar
        bottomBar = dialogView.findViewById(R.id.thumbnail_bottom_toolbar);
        rotateTV = (TextView) dialogView.findViewById(R.id.thumbnail_bottom_toolbar_rotate);
        copyTV = (TextView) dialogView.findViewById(R.id.thumbnail_bottom_toolbar_copy);
        deleteTV = (TextView) dialogView.findViewById(R.id.thumbnail_bottom_toolbar_delete);
        extractTV = (TextView) dialogView.findViewById(R.id.thumbnail_bottom_toolbar_extract);
        copyTV.setOnClickListener(this);
        rotateTV.setOnClickListener(this);
        deleteTV.setOnClickListener(this);
        extractTV.setOnClickListener(this);

        bottomBar.setVisibility(View.GONE);
        mThumbnailGridView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                for (int position = mGridLayoutManager.findFirstVisibleItemPosition(); position <= mGridLayoutManager.findLastVisibleItemPosition(); position++) {
                    ThumbnailAdapter.ThumbViewHolder viewHolder = (ThumbnailAdapter.ThumbViewHolder) mThumbnailGridView.findViewHolderForAdapterPosition(position);
                    viewHolder.drawThumbnail(mAdapter.getThumbnailItem(position), position);
                }
            }
        });
        if (mDisplay.isPad()) {
            ((RelativeLayout.LayoutParams) thumbnailLayout.getLayoutParams()).topMargin = (int) AppResource.getDimension(mContext, R.dimen.ux_toolbar_height_pad);
        } else {
            ((RelativeLayout.LayoutParams) thumbnailLayout.getLayoutParams()).topMargin = (int) AppResource.getDimension(mContext, R.dimen.ux_toolbar_height_phone);
        }
        mThumbnailTopBar = new TopBarImpl(mContext);
        RelativeLayout dialogTitle = (RelativeLayout) dialogView.findViewById(R.id.rd_viewmode_dialog_title);
        changeEditState(false);

        dialogTitle.removeAllViews();
        dialogTitle.addView(mThumbnailTopBar.getContentView());
        mThumbnailGridView = (RecyclerView) mThumbnailGridView.findViewById(R.id.thumbnail_grid_view);
        mThumbnailGridView.setHasFixedSize(true);
        mThumbnailGridView.setAdapter(mAdapter);
        mGridLayoutManager = new GridLayoutManager(mContext, mSpanCount);
        mThumbnailGridView.setLayoutManager(mGridLayoutManager);
        ThumbnailItemTouchCallback.OnDragListener dragListener = new ThumbnailItemTouchCallback.OnDragListener() {
            @Override
            public void onFinishDrag() {
                mAdapter.notifyDataSetChanged();
            }
        };
        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ThumbnailItemTouchCallback(mAdapter).setOnDragListener(dragListener));
        final SpacesItemDecoration mSpacesItemDecoration = new SpacesItemDecoration();
        mThumbnailGridView.addItemDecoration(mSpacesItemDecoration);
        itemTouchHelper.attachToRecyclerView(mThumbnailGridView);

        mThumbnailGridView.addOnItemTouchListener(new OnThumbnailItemTouchListener(mThumbnailGridView) {
            @Override
            public void onLongPress(RecyclerView.ViewHolder vh) {
                if (!mbEditMode) {
                    changeEditState(true);
                } else {
                    resetCurEditThumbnailItem();
                    itemTouchHelper.startDrag(vh);
                }
                Vibrator vib = (Vibrator) getActivity().getSystemService(Service.VIBRATOR_SERVICE);
                vib.vibrate(70);
            }

            @Override
            public boolean onItemClick(RecyclerView.ViewHolder vh) {
                ThumbnailAdapter.ThumbViewHolder viewHolder = (ThumbnailAdapter.ThumbViewHolder) vh;
                int position = vh.getAdapterPosition();
                ThumbnailItem thumbnailItem = mAdapter.getThumbnailItem(position);
                if (mbEditMode) {
                    if (!thumbnailItem.equals(mCurEditItem)) {
                        boolean isSelected = !thumbnailItem.isSelected();
                        mAdapter.updateSelectListInfo(thumbnailItem, isSelected);
                        setSelectViewMode(mAdapter.isSelectedAll());
                        viewHolder.changeSelectView(isSelected);
                        mThumbnailTitle.setText(String.format("%d", mAdapter.getSelectedItemCount()));
                    }
                    resetCurEditThumbnailItem();
                } else {
                    updateRecycleLayout();
                    mPDFView.gotoPage(thumbnailItem.getIndex());
                    PageNavigationModule module = (PageNavigationModule) ((UIExtensionsManager) mPDFView.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_PAGENAV);
                    if (module != null)
                        module.resetJumpView();

                    if (ThumbnailSupport.this.getDialog() != null) {
                        ThumbnailSupport.this.getDialog().dismiss();
                    }
                    ThumbnailSupport.this.dismiss();
                }
                return true;
            }

            @Override
            public boolean onToRightFling(RecyclerView.ViewHolder vh) {
                if (!mbEditMode)
                    return false;
                resetCurEditThumbnailItem();
                ThumbnailAdapter.ThumbViewHolder viewHolder = (ThumbnailAdapter.ThumbViewHolder) vh;
                int position = vh.getAdapterPosition();
                changeCurEditThumbnailItem(position, ThumbnailItem.EDIT_LEFT_VIEW);
                viewHolder.changeLeftEditView(vh.getAdapterPosition(), true);
                return true;
            }

            @Override
            public boolean onToLeftFling(RecyclerView.ViewHolder vh) {
                if (!mbEditMode)
                    return false;
                resetCurEditThumbnailItem();
                ThumbnailAdapter.ThumbViewHolder viewHolder = (ThumbnailAdapter.ThumbViewHolder) vh;
                int position = vh.getAdapterPosition();
                changeCurEditThumbnailItem(position, ThumbnailItem.EDIT_RIGHT_VIEW);
                viewHolder.changeRightEditView(position, true);
                return true;
            }
        });
        this.getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (mbEditMode) {
                            changeEditState(false);
                        } else {
                            if (ThumbnailSupport.this.getDialog() != null) {
                                ThumbnailSupport.this.getDialog().dismiss();
                            }
                            ThumbnailSupport.this.dismiss();
                            updateRecycleLayout();
                            PageNavigationModule module = (PageNavigationModule) ((UIExtensionsManager) mPDFView.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_PAGENAV);
                            if (module != null)
                                module.resetJumpView();
                        }
                    }
                    return true;
                }
                return false;
            }

        });
        return dialogView;
    }

    private void setDrawables(TextView v, int id) {
        Drawable drawable = ContextCompat.getDrawable(mContext, id);
        v.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
    }

    private void setSelectViewMode(boolean selectedAll) {
        if (mAdapter.getSelectedItemCount() == 0 || getInstance(mPDFView).isXFA()) {
            rotateTV.setEnabled(false);
            deleteTV.setEnabled(false);
            extractTV.setEnabled(false);
            copyTV.setEnabled(false);
            setDrawables(rotateTV, R.drawable.icon_thumnail_toolbar_rotate_disable);
            setDrawables(deleteTV, R.drawable.icon_thumnail_toolbar_delete_disable);
            setDrawables(copyTV, R.drawable.icon_thumnail_toolbar_copy_disable);
            setDrawables(extractTV, R.drawable.icon_thumnail_toolbar_extract_disable);

        } else {
            rotateTV.setEnabled(true);
            deleteTV.setEnabled(true);
            setDrawables(rotateTV, R.drawable.icon_thumnail_toolbar_rotate_able);
            setDrawables(deleteTV, R.drawable.icon_thumnail_toolbar_delete_able);

            if (DocumentManager.getInstance(mPDFView).canCopy()) {
                extractTV.setEnabled(true);
                copyTV.setEnabled(true);
                setDrawables(extractTV, R.drawable.icon_thumnail_toolbar_extract_able);
                setDrawables(copyTV, R.drawable.icon_thumnail_toolbar_copy_able);
            } else {
                extractTV.setEnabled(false);
                copyTV.setEnabled(false);
                setDrawables(extractTV, R.drawable.icon_thumnail_toolbar_extract_disable);
                setDrawables(copyTV, R.drawable.icon_thumnail_toolbar_copy_disable);
            }
        }
        if (selectedAll) {
            mSelectAllItem.setImageResource(R.drawable.thumbnail_selected_all);
        } else {
            mSelectAllItem.setImageResource(R.drawable.thumbnail_select_all);
        }
        mThumbnailTitle.setText(String.format("%d", mAdapter.getSelectedItemCount()));
    }


    private void changeEditState(boolean isEditMode) {
        boolean canModifyContents = getInstance(mPDFView).canModifyContents();
        mbEditMode = isEditMode && canModifyContents;
        boolean isXFA = getInstance(mPDFView).isXFA();
        if (isXFA) {
            mbEditMode = false;
            UIToast.getInstance(mContext).show("XFA file is not supported to edit");
        }
        mThumbnailTopBar.removeAllItems();
        final BaseItem mCloseThumbnailBtn = new BaseItemImpl(mContext);
        mCloseThumbnailBtn.setImageResource(R.drawable.cloud_back);
        mThumbnailTopBar.addView(mCloseThumbnailBtn, BaseBar.TB_Position.Position_LT);
        mThumbnailTitle = new BaseItemImpl(mContext);
        mThumbnailTitle.setTextColorResource(R.color.ux_text_color_title_light);
        mThumbnailTitle.setTextSize(mDisplay.px2dp(mContext.getResources().getDimension(R.dimen.ux_text_height_title)));
        mThumbnailTopBar.addView(mThumbnailTitle, BaseBar.TB_Position.Position_LT);

        if (mbEditMode) {
            bottomBar.setVisibility(View.VISIBLE);
            mThumbnailTitle.setText(String.format("%d", mAdapter.getSelectedItemCount()));
            mSelectAllItem = new BaseItemImpl(mContext);
            mInsertItem = new BaseItemImpl(mContext);
            mInsertItem.setImageResource(R.drawable.thumbnail_add_page_selector);

            mThumbnailTopBar.addView(mInsertItem, BaseBar.TB_Position.Position_RB);
            mThumbnailTopBar.addView(mSelectAllItem, BaseBar.TB_Position.Position_RB);

            mSelectAllItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isSelectedAll = !mAdapter.isSelectedAll();
                    mAdapter.selectAll(isSelectedAll);
                    setSelectViewMode(isSelectedAll);
                }
            });

            mInsertItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //mAdapter.importPages(mAdapter.getEditPosition());
                    mAdapter.prepareOnClickAdd();
                    alertDialog.show();
                }
            });

            mCloseThumbnailBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeEditState(false);
                }
            });
            setSelectViewMode(mAdapter.isSelectedAll());
        } else {
            bottomBar.setVisibility(View.GONE);
            if (canModifyContents && !isXFA) {
                BaseItem mEditThumbnailNtu = new BaseItemImpl(mContext);
                mEditThumbnailNtu.setText(AppResource.getString(mContext, R.string.rv_page_present_thumbnail_edit));
                mEditThumbnailNtu.setTextSize(mDisplay.px2dp(mContext.getResources().getDimension(R.dimen.ux_text_height_title)));
                mEditThumbnailNtu.setTextColorResource(R.color.ux_text_color_title_light);
                mThumbnailTopBar.addView(mEditThumbnailNtu, BaseBar.TB_Position.Position_RB);
                mEditThumbnailNtu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeEditState(true);
                    }
                });
            }
            mCloseThumbnailBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ThumbnailSupport.this.getDialog() != null) {
                        ThumbnailSupport.this.getDialog().dismiss();
                    }
                    ThumbnailSupport.this.dismiss();
                    updateRecycleLayout();
                    PageNavigationModule module = (PageNavigationModule) ((UIExtensionsManager) mPDFView.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_PAGENAV);
                    if (module != null)
                        module.resetJumpView();
                }
            });
            mThumbnailTopBar.addView(mCloseThumbnailBtn, BaseBar.TB_Position.Position_LT);
            mThumbnailTopBar.setBackgroundResource(R.color.ux_bg_color_toolbar_colour);
            mThumbnailTitle.setText(AppResource.getString(mContext, R.string.rv_page_present_thumbnail));
            resetCurEditThumbnailItem();
        }
        mAdapter.notifyDataSetChanged();
    }

    protected final static int REMOVE_ALL_PAGES_TIP = 0;
    protected final static int REMOVE_SOME_PAGES_TIP = 1;

    void showTipsDlg(int removeType) {
        final UITextEditDialog dialog = new UITextEditDialog(mAttachActivity);
        dialog.getInputEditText().setVisibility(View.GONE);
        dialog.setTitle(AppResource.getString(mContext, R.string.fx_string_delete));
        switch (removeType) {
            case REMOVE_ALL_PAGES_TIP:
                dialog.getCancelButton().setVisibility(View.GONE);
                dialog.getPromptTextView().setText(AppResource.getString(mContext, R.string.rv_page_delete_all_thumbnail));
                dialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                break;
            case REMOVE_SOME_PAGES_TIP:
                dialog.getPromptTextView().setText(AppResource.getString(mContext, R.string.rv_page_delete_thumbnail));
                dialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mAdapter.removeSelectedPages();
                        dialog.dismiss();
                    }
                });
                break;
            default:
                break;
        }
        dialog.show();
    }

    public Point getThumbnailBackgroundSize() {
        if (mThumbnailSize == null) {
            float dpi = mContext.getResources().getDisplayMetrics().densityDpi;
            if (dpi == 0) {
                dpi = 240;
            }
            float scale;
            try {
                float width = mPDFView.getDoc().getPage(0).getWidth();
                float height = mPDFView.getDoc().getPage(0).getHeight();
                scale = width > height ? height / width : width / height;
            } catch (PDFException e) {
                scale = 0.7f;
            }
            mThumbnailSize = new Point((int) (dpi * 0.7f), (int) (dpi * 0.7f / scale));
        }
        return mThumbnailSize;
    }

    private void computeSize() {
        int displayWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        int displayHeight = mContext.getResources().getDisplayMetrics().heightPixels;
        Point size = getThumbnailBackgroundSize();
        mSpanCount = Math.max(1, (displayWidth - mHorSpacing) / (mHorSpacing + size.x + 2));
        int tasksMax = mSpanCount * (displayHeight / size.y + 2);
        int bitmapMax = Math.max(64, tasksMax);
        mAdapter.setCacheSize(tasksMax, bitmapMax);
        mVerSpacing = (displayWidth - size.x * mSpanCount) / (mSpanCount + 1);
    }

    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.thumbnail_bottom_toolbar_copy) {
            mAdapter.copyPages(mPDFView.getDoc());
        } else if (view.getId() == R.id.thumbnail_bottom_toolbar_rotate) {
            mAdapter.rotateSelectedPages();
        } else if (view.getId() == R.id.thumbnail_bottom_toolbar_extract) {
            mAdapter.extractPages();
        } else if (view.getId() == R.id.thumbnail_bottom_toolbar_delete) {
            showTipsDlg(mAdapter.isSelectedAll() ? REMOVE_ALL_PAGES_TIP : REMOVE_SOME_PAGES_TIP);
        } else {

        }
    }

    @Override
    public void insertImage() {
        alertDialog.show();
    }

    private class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            if (mSpanCount > 0) {
                int position = parent.getChildAdapterPosition(view);
                int spanIndex = position % mSpanCount;

                outRect.left = mVerSpacing - spanIndex * mVerSpacing / mSpanCount;
                outRect.right = (spanIndex + 1) * mVerSpacing / mSpanCount;

                outRect.top = mHorSpacing;
                outRect.bottom = mHorSpacing;
            } else {
                outRect.setEmpty();
            }
        }
    }


}












