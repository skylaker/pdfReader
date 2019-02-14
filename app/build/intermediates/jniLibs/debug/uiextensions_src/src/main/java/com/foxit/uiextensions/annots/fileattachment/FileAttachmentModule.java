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
package com.foxit.uiextensions.annots.fileattachment;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.utils.ToolUtil;


public class FileAttachmentModule implements Module, PropertyBar.PropertyChangeListener {

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private FileAttachmentAnnotHandler mAnnotHandler;
    private FileAttachmentToolHandler mToolHandler;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    private PropertyBar mPropertyBar;

    @Override
    public String getName() {
        return Module.MODULE_NAME_FILEATTACHMENT;
    }

    public FileAttachmentModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    public ToolHandler getToolHandler() {
        return mToolHandler;
    }

    public AnnotHandler getAnnotHandler() {
        return mAnnotHandler;
    }

    @Override
    public boolean loadModule() {

        mToolHandler = new FileAttachmentToolHandler(mContext, mPdfViewCtrl);
        mCurrentColor = PropertyBar.PB_COLORS_FILEATTACHMENT[0];
        mCurrentOpacity = 100;
        mFlagType = FileAttachmentConstants.ICONTYPE_PUSHPIN;
        mToolHandler.setPaint(mCurrentColor, mCurrentOpacity, mFlagType);
        initView();
        mAnnotHandler = new FileAttachmentAnnotHandler(mContext, mPdfViewCtrl, this);
        mAnnotHandler.setToolHandler(mToolHandler);
        mAnnotHandler.setPropertyBarIconLayout(mIconItem_ly);
        mAnnotHandler.setPropertyListViewAdapter(mAdapter);

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mToolHandler);
            ToolUtil.registerAnnotHandler((UIExtensionsManager) mUiExtensionsManager, mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mToolHandler);
            ToolUtil.unregisterAnnotHandler((UIExtensionsManager) mUiExtensionsManager, mAnnotHandler);
        }
        return true;
    }

    private int mCurrentColor;
    private int mCurrentOpacity;
    private int mFlagType;

    private ColorChangeListener mColorChangeListener = null;

    public void setColorChangeListener(ColorChangeListener listener) {
        mColorChangeListener = listener;
    }

    public interface ColorChangeListener {
        void onColorChange(int color);
    }

    @Override
    public void onValueChanged(long property, int value) {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;
        AnnotHandler currentAnnotHandler = ToolUtil.getCurrentAnnotHandler(uiExtensionsManager);
        ToolHandler currentToolHandler = uiExtensionsManager.getCurrentToolHandler();
        if (property == PropertyBar.PROPERTY_COLOR || property == PropertyBar.PROPERTY_SELF_COLOR) {
            if (currentToolHandler == mToolHandler) {
                mCurrentColor = value;
                mToolHandler.setPaint(mCurrentColor, mCurrentOpacity, mFlagType);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.modifyAnnotColor(value);
                mToolHandler.setColor(value);
            }
            if (mColorChangeListener != null)
                mColorChangeListener.onColorChange(mCurrentColor);
        } else if (property == PropertyBar.PROPERTY_OPACITY) {
            if (currentToolHandler == mToolHandler) {
                mCurrentOpacity = value;
                mToolHandler.setPaint(mCurrentColor, mCurrentOpacity, mFlagType);
            } else if (currentAnnotHandler == mAnnotHandler) {
                mAnnotHandler.modifyAnnotOpacity(value);
            }
        }
    }

    @Override
    public void onValueChanged(long property, float value) {
    }

    @Override
    public void onValueChanged(long property, String value) {
    }


    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {

        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mAnnotHandler.onDrawForControls(canvas);
        }
    };

    private PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc doc, int err) {
            if (!isAttachmentOpening()) {
                mAnnotHandler.deleteTMP_PATH();
            }
        }

        @Override
        public void onDocWillClose(PDFDoc doc) {

        }

        @Override
        public void onDocClosed(PDFDoc doc, int err) {

        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }


    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return (mToolHandler.onKeyDown(keyCode, event) || mAnnotHandler.onKeyDown(keyCode, event));
    }


    private LinearLayout mIconItem_ly;

    private int[] mTypePicIds = new int[]{R.drawable.pb_fat_type_graph, R.drawable.pb_fat_type_paperclip, R.drawable.pb_fat_type_pushpin,
            R.drawable.pb_fat_type_tag};
    private String[] mTypeNames;
    private FileAttachmentPBAdapter mAdapter;

    private int[] mPBColors = new int[PropertyBar.PB_COLORS_FILEATTACHMENT.length];

    public void resetPropertyBar() {
        final FileAttachmentToolHandler toolHandler = (FileAttachmentToolHandler) getToolHandler();
        long supportProperty = PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY;
        System.arraycopy(PropertyBar.PB_COLORS_FILEATTACHMENT, 0, mPBColors, 0, mPBColors.length);
        mPBColors[0] = PropertyBar.PB_COLORS_FILEATTACHMENT[0];

        mPropertyBar.setColors(mPBColors);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, toolHandler.getColor());

        int opacity = toolHandler.getOpacity();
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, opacity);
        mPropertyBar.reset(supportProperty);

        mPropertyBar.setPropertyChangeListener(this);
        mPropertyBar.addTab("", 0, mContext.getString(com.foxit.uiextensions.R.string.pb_type_tab), 0);
        mPropertyBar.addCustomItem(PropertyBar.PROPERTY_FILEATTACHMENT, mIconItem_ly, 0, 0);
    }

    private void initView() {
        mPropertyBar = new PropertyBarImpl(mContext, mPdfViewCtrl);
        //IconListView
        mIconItem_ly = new LinearLayout(mContext);
        mIconItem_ly.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        mIconItem_ly.setGravity(Gravity.CENTER);
        mIconItem_ly.setOrientation(LinearLayout.HORIZONTAL);

        ListView listView = new ListView(mContext);
        listView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        listView.setCacheColorHint(mContext.getResources().getColor(R.color.ux_color_translucent));

        listView.setDivider(new ColorDrawable(mContext.getResources().getColor(R.color.ux_color_seperator_gray)));
        listView.setDividerHeight(1);
        mIconItem_ly.addView(listView);
        mTypeNames = FileAttachmentUtil.getIconNames();
        mAdapter = new FileAttachmentPBAdapter(mContext, mTypePicIds, mTypeNames);
        mAdapter.setNoteIconType(mFlagType);
        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mFlagType = position;
                mAdapter.setNoteIconType(mFlagType);
                mAdapter.notifyDataSetChanged();

                mAnnotHandler.modifyIconType(mFlagType);
                String[] iconNames = FileAttachmentUtil.getIconNames();
                mToolHandler.setIconName(iconNames[mFlagType]);

            }
        });
//        resetPropertyBar();
    }


    public PropertyBar getPropertyBar() {
        return mPropertyBar;
    }


    public boolean isAttachmentOpening() {
        return mAnnotHandler.isAttachmentOpening();
    }

    public interface IAttachmentDocEvent {
        void onAttachmentDocWillOpen();
        void onAttachmentDocOpened(PDFDoc document, int errCode);
        void onAttachmentDocWillClose();
        void onAttachmentDocClosed();
    }

    public void registerAttachmentDocEventListener(IAttachmentDocEvent listener) {
        mAnnotHandler.registerAttachmentDocEventListener(listener);
    }

    public void unregisterAttachmentDocEventListener(IAttachmentDocEvent listener) {
        mAnnotHandler.unregisterAttachmentDocEventListener(listener);
    }
}
