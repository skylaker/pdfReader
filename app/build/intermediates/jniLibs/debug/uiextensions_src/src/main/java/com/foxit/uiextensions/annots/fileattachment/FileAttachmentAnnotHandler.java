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

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.text.ClipboardManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.FileSpec;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FileAttachment;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppIntentUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.ToolUtil;

import java.io.File;
import java.util.ArrayList;

public class FileAttachmentAnnotHandler implements AnnotHandler {

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private Paint mPaintBbox;
    private Paint mPaintAnnot;
    private ArrayList<Integer> mMenuItems;
    private Annot mBitmapAnnot;
    private PropertyBar mAnnotPropertyBar;
    private int mModifyColor;
    private float mModifyOpacity;
    private String mModifyIconName;
    private int mBBoxSpace;
    private boolean mIsAnnotModified;
    private FileAttachmentToolHandler mFileAttachmentToolHandler;
    private boolean mIsEditProperty;
    private AnnotMenu mAnnotMenu;
    private FileAttachmentModule mFileAttachmentModule;
    private PointF mDownPoint;
    private PointF mLastPoint;
    private boolean mTouchCaptured = false;
    private String TMP_PATH;

    private View mOpenView;
    private TextView mOpenView_filenameTV;
    private ImageView mOpenView_backIV;
    private LinearLayout mOpenView_contentLy;
    private LinearLayout mOpenView_titleLy;

    public FileAttachmentAnnotHandler(Context context, PDFViewCtrl pdfViewer, FileAttachmentModule fileAttachmentModule) {
        mContext = context;
        mPdfViewCtrl = pdfViewer;
        mFileAttachmentModule = fileAttachmentModule;
        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mPaintBbox = new Paint();
        mPaintBbox.setAntiAlias(true);
        mPaintBbox.setStyle(Paint.Style.STROKE);
        AppAnnotUtil annotUtil = new AppAnnotUtil(mContext);
        mPaintBbox.setStrokeWidth(annotUtil.getAnnotBBoxStrokeWidth());
        mPaintBbox.setPathEffect(annotUtil.getAnnotBBoxPathEffect());

        mPaintAnnot = new Paint();
        mPaintAnnot.setStyle(Paint.Style.STROKE);
        mPaintAnnot.setAntiAlias(true);
        mDrawLocal_tmpF = new RectF();

        mDownPoint = new PointF();
        mLastPoint = new PointF();

        mMenuItems = new ArrayList<Integer>();
        mAnnotMenu = new AnnotMenuImpl(mContext, mPdfViewCtrl);
        mAnnotMenu.setMenuItems(mMenuItems);

        TMP_PATH = Environment.getExternalStorageDirectory() + "/FoxitSDK/AttaTmp/";

        mAnnotPropertyBar = fileAttachmentModule.getPropertyBar();
    }

    public String getTMP_PATH() {
        return TMP_PATH;
    }

    public void deleteTMP_PATH() {
        File file = new File(TMP_PATH);
        deleteDir(file);
    }

    private void deleteDir(File path) {
        if (!path.exists())
            return;
        if (path.isFile()) {
            path.delete();
            return;
        }
        File[] files = path.listFiles();
        if (files == null) return;
        for (int i = 0; i < files.length; i++) {
            deleteDir(files[i]);
        }
        path.delete();
    }

    private FileAttachmentPBAdapter mPropertyListViewAdapter;

    public void setPropertyListViewAdapter(FileAttachmentPBAdapter adapter) {
        mPropertyListViewAdapter = adapter;
    }

    @Override
    public int getType() {
        return Annot.e_annotFileAttachment;
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
    }

    @Override
    public RectF getAnnotBBox(Annot annot) {
        try {
            return annot.getRect();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        RectF rectF = getAnnotBBox(annot);
        if (mPdfViewCtrl != null) {
            try {
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, annot.getPage().getIndex());
            } catch (Exception e) {
                e.printStackTrace();
            }

            rectF.inset(-10, -10);
        }
        return rectF.contains(point.x, point.y);
    }

    public void resetMenuItems() {
        mMenuItems.clear();

        if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
        } else {
            mMenuItems.add(AnnotMenu.AM_BT_STYLE);
            mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
            mMenuItems.add(AnnotMenu.AM_BT_DELETE);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mOpenView == null){
            return false;
        }else if (mOpenView.getVisibility() == View.VISIBLE && keyCode == KeyEvent.KEYCODE_BACK) {
            closeAttachment();
            mOpenView.setVisibility(View.GONE);
            return true;
        }

        if (ToolUtil.getCurrentAnnotHandler((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()) == this) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                return true;
            }
        }

        return false;
    }

    private LinearLayout mIconLayout;

    public void setPropertyBarIconLayout(LinearLayout layout) {
        mIconLayout = layout;
    }

    private int mTmpUndoColor;
    private float mTmpUndoOpacity;
    private String mTmpUndoIconName;
    private RectF mTmpUndoBbox;
    private String mTmpUndoModifiedDate;

    @Override
    public void onAnnotSelected(final Annot annotation, boolean reRender) {
        if (annotation == null || !(annotation instanceof FileAttachment))
            return;

        final FileAttachment annot = (FileAttachment) annotation;
        try {

            mTmpUndoColor = (int) annot.getBorderColor();

            mTmpUndoOpacity = annot.getOpacity();
            mTmpUndoIconName = annot.getIconName();
            mTmpUndoBbox = annot.getRect();
            mTmpUndoModifiedDate = AppDmUtil.getLocalDateString(annot.getModifiedDateTime());
            mModifyIconName = mTmpUndoIconName;
            mModifyOpacity = mTmpUndoOpacity;
            mModifyColor = mTmpUndoColor;

            mBitmapAnnot = annot;
            mAnnotPropertyBar.setArrowVisible(false);
            resetMenuItems();
            mAnnotMenu.setMenuItems(mMenuItems);

            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
                @Override
                public void onAMClick(int btType) {
                    try {
                        final int pageIndex = annotation.getPage().getIndex();
                        if (btType == AnnotMenu.AM_BT_COPY) {
                            ClipboardManager clipboard = null;
                            clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);

                            clipboard.setText(annot.getContent());

                            AppAnnotUtil.toastAnnotCopy(mContext);
                            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                        } else if (btType == AnnotMenu.AM_BT_DELETE) {
                            DeleteAnnot(annot, true, null);
                        } else if (btType == AnnotMenu.AM_BT_STYLE) {
                            mAnnotMenu.dismiss();
                            mIsEditProperty = true;
                            System.arraycopy(PropertyBar.PB_COLORS_FILEATTACHMENT, 0, mPBColors, 0, mPBColors.length);
                            mPBColors[0] = PropertyBar.PB_COLORS_FILEATTACHMENT[0];
                            mAnnotPropertyBar.setColors(mPBColors);
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, annot.getBorderColor());
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100((int) (annot.getOpacity() * 255f + 0.5f)));
                            mAnnotPropertyBar.reset(PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY);

                             mAnnotPropertyBar.addTab("", 0, mContext.getResources().getString(R.string.pb_type_tab), 0);
                            mPropertyListViewAdapter.setNoteIconType(FileAttachmentUtil.getIconType(annot.getIconName()));
                            mAnnotPropertyBar.addCustomItem(PropertyBar.PROPERTY_FILEATTACHMENT, mIconLayout, 0, 0);

                            mAnnotPropertyBar.setPropertyChangeListener(mFileAttachmentModule);
                            RectF annotRectF = annot.getRect();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRectF, annotRectF, pageIndex);


                            mAnnotPropertyBar.show(annotRectF, false);
                        } else if (btType == AnnotMenu.AM_BT_COMMENT) {

                            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                            _onOpenAttachment(annot);

                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
            });

            RectF annotRectF = annot.getRect();
            int pageIndex = annot.getPage().getIndex();
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {


                RectF modifyRectF = new RectF(annotRectF);


                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRectF, annotRectF, pageIndex);
                mAnnotMenu.show(annotRectF);
                mPdfViewCtrl.convertPdfRectToPageViewRect(modifyRectF, modifyRectF, pageIndex);
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(modifyRectF));
                if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                    mBitmapAnnot = annot;
                }
            } else {
                mBitmapAnnot = annot;
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
        if (annot == null || !(annot instanceof FileAttachment))
            return;
        mAnnotMenu.dismiss();

        if (mIsEditProperty) {
            mIsEditProperty = false;
        }
        try {
            PDFPage page = annot.getPage();
            if (page == null)
                return;
            int pageIndex = page.getIndex();

            if (mIsAnnotModified && reRender) {
                if (mTmpUndoColor != mModifyColor
                        || mTmpUndoOpacity != mModifyOpacity
                        || mTmpUndoIconName != mModifyIconName
                        || !mTmpUndoBbox.equals(annot.getRect())) {
                    ModifyAnnot(annot, mModifyColor, mModifyOpacity, mModifyIconName, null, true, null);
                }
            } else if (mIsAnnotModified) {
                annot.setBorderColor(mTmpUndoColor);
                ((FileAttachment) annot).setOpacity(AppDmUtil.opacity100To255((int) mTmpUndoOpacity) / 255f);
                ((FileAttachment) annot).setIconName(mTmpUndoIconName);

                annot.move(mTmpUndoBbox);
                annot.setModifiedDateTime(AppDmUtil.parseDocumentDate(mTmpUndoModifiedDate));
                annot.resetAppearanceStream();
            }
            mIsAnnotModified = false;
            RectF rect = annot.getRect();
            mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, pageIndex);
            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(rect));

            mBitmapAnnot = null;

        } catch (PDFException e) {
            e.printStackTrace();
        }
        mBitmapAnnot = null;

    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, Event.Callback result) {

        if (mFileAttachmentToolHandler != null) {
            mFileAttachmentToolHandler.addAnnot(pageIndex, content.getBBox(), result);
        } else {
            if (result != null) {
                result.result(null, false);
            }
        }
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        if (annot == null || !(annot instanceof FileAttachment))
            return;
        try {
            mTmpUndoColor = (int) annot.getBorderColor();
            mTmpUndoOpacity = ((FileAttachment) annot).getOpacity();
            mTmpUndoIconName = ((FileAttachment) annot).getIconName();
            mTmpUndoBbox = annot.getRect();
            mTmpUndoModifiedDate = AppDmUtil.getLocalDateString(annot.getModifiedDateTime());
            mIsAnnotModified = true;


            if (content != null) {
                IFileAttachmentAnnotContent annotContent = IFileAttachmentAnnotContent.class.cast(content);
                ModifyAnnot(annot, annotContent.getColor(), annotContent.getOpacity(), ((FileAttachment) annot).getIconName(),
                        AppDmUtil.getLocalDateString(annotContent.getModifiedDate()), addUndo, result);
            } else {
                ModifyAnnot(annot, (int) annot.getBorderColor(), ((FileAttachment) annot).getOpacity(), ((FileAttachment) annot).getIconName(),
                        AppDmUtil.getLocalDateString(annot.getModifiedDateTime()), addUndo, result);
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
        DeleteAnnot(annot, addUndo, result);
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
        int action = motionEvent.getActionMasked();
        PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
        PointF point = new PointF();
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, point, pageIndex);
        PointF pageViewPt = new PointF(point.x, point.y);
        try {
            float envX = point.x;
            float envY = point.y;

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                        try {
                            if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt)) {
                                mDownPoint.set(envX, envY);
                                mLastPoint.set(envX, envY);
                                mTouchCaptured = true;
                                return true;
                            }
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    try {
                        if (mTouchCaptured && annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()
                                && pageIndex == annot.getPage().getIndex()
                                && DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
                            if (envX != mLastPoint.x || envY != mLastPoint.y) {
                                RectF pageViewRectF = annot.getRect();
                                mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRectF, pageViewRectF, annot.getIndex());
                                RectF rectInv = new RectF(pageViewRectF);
                                RectF rectChanged = new RectF(pageViewRectF);

                                rectInv.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                rectChanged.offset(envX - mDownPoint.x, envY - mDownPoint.y);

                                float adjustx = 0;
                                float adjusty = 0;
                                if (rectChanged.left < 0) {
                                    adjustx = -rectChanged.left;
                                }
                                if (rectChanged.top < 0) {
                                    adjusty = -rectChanged.top;
                                }
                                if (rectChanged.right > mPdfViewCtrl.getPageViewWidth(pageIndex)) {
                                    adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - rectChanged.right;
                                }
                                if (rectChanged.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex)) {
                                    adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - rectChanged.bottom;
                                }
                                rectChanged.offset(adjustx, adjusty);
                                rectInv.union(rectChanged);
                                rectInv.inset(-mBBoxSpace - 3, -mBBoxSpace - 3);
                                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInv, rectInv, pageIndex);
                                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectInv));
                                RectF rectInViewerF = new RectF(rectChanged);
                                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInViewerF, rectInViewerF, pageIndex);
                                if (mAnnotMenu.isShowing()) {
                                    mAnnotMenu.dismiss();
                                    mAnnotMenu.update(rectInViewerF);
                                }
                                if (mIsEditProperty) {
                                    mAnnotPropertyBar.dismiss();
                                }
                                mLastPoint.set(envX, envY);
                                mLastPoint.offset(adjustx, adjusty);
                            }
                            return true;
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }

                    return false;
                case MotionEvent.ACTION_UP:
                    if (mTouchCaptured && annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() &&
                            mPdfViewCtrl.getCurrentPage() == pageIndex
                            && DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
                        RectF pageRectF = annot.getRect();
                        RectF pageViewRectF = new RectF();
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageRectF, pageViewRectF, pageIndex);

                        RectF rectInv = new RectF(pageViewRectF);
                        RectF rectChanged = new RectF(pageViewRectF);

                        rectInv.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                        rectChanged.offset(envX - mDownPoint.x, envY - mDownPoint.y);
                        float adjustx = 0;
                        float adjusty = 0;
                        if (rectChanged.left < 0) {
                            adjustx = -rectChanged.left;
                        }
                        if (rectChanged.top < 0) {
                            adjusty = -rectChanged.top;
                        }
                        if (rectChanged.right > mPdfViewCtrl.getPageViewWidth(pageIndex)) {
                            adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - rectChanged.right;
                        }
                        if (rectChanged.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex)) {
                            adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - rectChanged.bottom;
                        }
                        rectChanged.offset(adjustx, adjusty);
                        rectInv.union(rectChanged);
                        rectInv.inset(-mBBoxSpace - 3, -mBBoxSpace - 3);

                        Rect invalidateRect = AppDmUtil.rectFToRect(rectInv);
                        mPdfViewCtrl.refresh(pageIndex, invalidateRect);
                        RectF rectInViewerF = new RectF(rectChanged);

                        RectF canvasRectF = new RectF();
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInViewerF, canvasRectF, pageIndex);
                        if (mIsEditProperty) {
                            if (mAnnotPropertyBar.isShowing()) {
                                mAnnotPropertyBar.update(canvasRectF);
                            } else {
                                mAnnotPropertyBar.show(canvasRectF, false);
                            }
                        } else {
                            if (mAnnotMenu.isShowing()) {
                                mAnnotMenu.update(canvasRectF);
                            } else {
                                mAnnotMenu.show(canvasRectF);
                            }
                        }

                        RectF rect = new RectF();
                        mPdfViewCtrl.convertPageViewRectToPdfRect(rectChanged, rect, pageIndex);
                        if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {

                            mIsAnnotModified = true;
                            annot.move(rect);
                            pageViewRectF.inset(-mBBoxSpace - 3, -mBBoxSpace - 3);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(pageViewRectF));
                            mModifyColor = (int) annot.getBorderColor();
                            mModifyOpacity = ((FileAttachment) annot).getOpacity();
                            mModifyIconName = ((FileAttachment) annot).getIconName();
                        }

                        mTouchCaptured = false;
                        mDownPoint.set(0, 0);
                        mLastPoint.set(0, 0);
                        return true;
                    }
                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    return false;
                case MotionEvent.ACTION_CANCEL:
                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    return false;
            }
        } catch (PDFException e1) {
            if (e1.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        PointF pageViewPt = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
            try {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt)) {
                } else {
                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        } else {
            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(annot);
        }
        return true;

    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (AppUtil.isFastDoubleClick()) {
            return true;
        }
        PointF pageViewPt = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
            try {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt)) {
                } else {
                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        } else {
            _onOpenAttachment(annot);
        }
        return true;
    }


    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null)
            return;
        try {
            int id = annot.getPage().getIndex();
            if (mBitmapAnnot == annot && id == pageIndex) {
                canvas.save();
                RectF frameRectF = new RectF();
                RectF rect = annot.getRect();

                mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, annot.getPage().getIndex());
                rect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                frameRectF.set(rect.left - mBBoxSpace, rect.top - mBBoxSpace, rect.right + mBBoxSpace, rect.bottom + mBBoxSpace);
                int color = (int) (annot.getBorderColor() | 0xFF000000);
                mPaintBbox.setColor(color);
                canvas.drawRect(frameRectF, mPaintBbox);
                canvas.restore();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private RectF mDrawLocal_tmpF;

    public void onDrawForControls(Canvas canvas) {
        Annot curAnnot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();

        try {
            if (curAnnot != null && ToolUtil.getAnnotHandlerByType((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager(), curAnnot.getType()) == this) {
                int pageIndex = curAnnot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    RectF rect = curAnnot.getRect();

                    mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, curAnnot.getPage().getIndex());
                    rect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rect, rect, curAnnot.getPage().getIndex());

                    mAnnotMenu.update(rect);
                    mDrawLocal_tmpF.set(rect);
                    if (mIsEditProperty) {
                        mAnnotPropertyBar.update(mDrawLocal_tmpF);
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setToolHandler(FileAttachmentToolHandler toolHandler) {
        mFileAttachmentToolHandler = toolHandler;
    }

    private int[] mPBColors = new int[PropertyBar.PB_COLORS_FILEATTACHMENT.length];


    public void modifyAnnotColor(int color) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null) return;
        mModifyColor = color;
        try {
            if (mModifyColor != annot.getBorderColor()) {
                mIsAnnotModified = true;
                annot.setBorderColor(mModifyColor);
                annot.resetAppearanceStream();
                invalidateForToolModify(annot);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void modifyAnnotOpacity(int opacity) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null || !(annot instanceof FileAttachment)) return;
        mModifyOpacity = AppDmUtil.opacity100To255(opacity) / 255f;
        try {
            if (mModifyOpacity != ((FileAttachment) annot).getOpacity()) {
                mIsAnnotModified = true;
                ((FileAttachment) annot).setOpacity(mModifyOpacity);
                annot.resetAppearanceStream();
                invalidateForToolModify(annot);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void modifyIconType(int type) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null) return;
        String[] iconNames = FileAttachmentUtil.getIconNames();
        mModifyIconName = iconNames[type];

        try {
            if (mModifyIconName != ((FileAttachment) annot).getIconName()) {
                mIsAnnotModified = true;
                ((FileAttachment) annot).setIconName(mModifyIconName);
                annot.resetAppearanceStream();
                invalidateForToolModify(annot);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void invalidateForToolModify(Annot annot) {
        if (annot == null)
            return;
        int pageIndex = 0;
        try {
            pageIndex = annot.getPage().getIndex();

            if (!mPdfViewCtrl.isPageVisible(pageIndex))
                return;

            RectF rectF = annot.getRect();
            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);

            Rect rect = rectRoundOut(rectF, mBBoxSpace);
            rect.inset(-1, -1);
            mPdfViewCtrl.refresh(pageIndex, rect);

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private Rect rectRoundOut(RectF rectF, int roundSize) {
        Rect rect = new Rect();
        rectF.roundOut(rect);
        rect.inset(-roundSize, -roundSize);
        return rect;
    }

    public void DeleteAnnot(final Annot annot, final Boolean addUndo, final Event.Callback result) {
        final DocumentManager dm_Doc = DocumentManager.getInstance(mPdfViewCtrl);
        if (annot == dm_Doc.getCurrentAnnot()) dm_Doc.setCurrentAnnot(null);
        try {
            final PDFPage page = annot.getPage();
            if (page == null) {
                if (result != null) {
                    result.result(null, false);
                }
                return;
            }

            final RectF annotRectF = annot.getRect();
            final int pageIndex = page.getIndex();
            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);
            final FileAttachmentDeleteUndoItem undoItem = new FileAttachmentDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mIconName = ((FileAttachment) annot).getIconName();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mPath = mFileAttachmentToolHandler.getAttachmentPath(annot);
            undoItem.attacheName = ((FileAttachment) annot).getFileSpec().getFileName();
            FileAttachmentEvent event = new FileAttachmentEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (FileAttachment) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (addUndo) {
                            DocumentManager.getInstance(mPdfViewCtrl).addUndoItem(undoItem);
                        }

                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
                        }
                    }

                    if (result != null) {
                        result.result(event, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);

        } catch (PDFException e) {
            if (e.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }

    }

    public void ModifyAnnot(final Annot annot, int color, float opacity, String iconName, String modifyDate,
                            final boolean addUndo, final Event.Callback callback) {
        PDFPage page = null;
        try {
            page = annot.getPage();

            if (null == page) return;
            final FileAttachmentModifyUndoItem undoItem = new FileAttachmentModifyUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mIconName = iconName;

            undoItem.mRedoColor = color;
            undoItem.mRedoOpacity = opacity;
            undoItem.mRedoIconName = iconName;
            undoItem.mRedoBbox = annot.getRect();
            undoItem.mUndoColor = mTmpUndoColor;
            undoItem.mUndoOpacity = mTmpUndoOpacity;
            undoItem.mUndoIconName = mTmpUndoIconName;
            undoItem.mUndoBbox = mTmpUndoBbox;
            FileAttachmentEvent event = new FileAttachmentEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (FileAttachment) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {

                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (addUndo) {
                            DocumentManager.getInstance(mPdfViewCtrl).addUndoItem(undoItem);
                        }

                        if (callback != null) {
                            callback.result(event, success);
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void initOpenView() {
        mOpenView = View.inflate(mContext, R.layout.attachment_view, null);
        mOpenView_titleLy = (LinearLayout) mOpenView.findViewById(R.id.attachment_view_topbar_ly);
        mOpenView_contentLy = (LinearLayout) mOpenView.findViewById(R.id.attachment_view_content_ly);
        mOpenView_backIV = (ImageView) mOpenView.findViewById(R.id.attachment_view_topbar_back);
        mOpenView_filenameTV = (TextView) mOpenView.findViewById(R.id.attachment_view_topbar_name);
        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView().addView(mOpenView);
        mOpenView.setVisibility(View.GONE);

        int margin_left = 0;
        int margin_right = 0;
        if (AppDisplay.getInstance(mContext).isPad()) {
            margin_left = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_left_margin_pad);
            margin_right = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_right_margin_pad);
            LinearLayout.LayoutParams clp = (LinearLayout.LayoutParams) mOpenView_titleLy.getLayoutParams();
            clp.setMargins(margin_left, 0, margin_right, 0);
        }

        mOpenView_backIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOpenView.setVisibility(View.GONE);
                onAttachmentDocWillClose();
                mAttachPdfViewCtrl.closeDoc();
            }
        });

        mOpenView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });


    }

    private boolean mbAttachengOpening = false;
    public boolean isAttachmentOpening() {
        return mbAttachengOpening;
    }

    private PDFViewCtrl mAttachPdfViewCtrl;
    private ArrayList<FileAttachmentModule.IAttachmentDocEvent> mAttachDocEventListeners = new ArrayList<FileAttachmentModule.IAttachmentDocEvent>();

    protected void registerAttachmentDocEventListener(FileAttachmentModule.IAttachmentDocEvent listener) {
        mAttachDocEventListeners.add(listener);
    }

    protected void unregisterAttachmentDocEventListener(FileAttachmentModule.IAttachmentDocEvent listener) {
        mAttachDocEventListeners.remove(listener);
    }

    private void onAttachmentDocWillOpen() {
        for (FileAttachmentModule.IAttachmentDocEvent docEvent : mAttachDocEventListeners) {
            docEvent.onAttachmentDocWillOpen();
        }
    }

    private void onAttachmentDocOpened(PDFDoc document, int errCode) {
        for (FileAttachmentModule.IAttachmentDocEvent docEvent : mAttachDocEventListeners) {
            docEvent.onAttachmentDocOpened(document, errCode);
        }
    }

    private void onAttachmentDocWillClose() {
        for (FileAttachmentModule.IAttachmentDocEvent docEvent : mAttachDocEventListeners) {
            docEvent.onAttachmentDocWillClose();
        }
    }

    private void onAttachmentDocClosed() {
        for (FileAttachmentModule.IAttachmentDocEvent docEvent : mAttachDocEventListeners) {
            docEvent.onAttachmentDocClosed();
        }
    }


    public void openAttachment(final String filePath) {
        onAttachmentDocWillOpen();
        mAttachPdfViewCtrl = new PDFViewCtrl(mContext);
        String filename = filePath.substring(filePath.lastIndexOf('/') + 1);
        initOpenView();
        mOpenView_filenameTV.setText(filename);
        mOpenView_contentLy.removeAllViews();
        mOpenView_contentLy.addView(mAttachPdfViewCtrl);
        mOpenView.setVisibility(View.VISIBLE);
        mAttachPdfViewCtrl.registerDocEventListener(new PDFViewCtrl.IDocEventListener() {
            @Override
            public void onDocWillOpen() {

            }

            @Override
            public void onDocOpened(PDFDoc document, int errCode) {
                if (errCode == PDFError.NO_ERROR.getCode()) {
                    mAttachPdfViewCtrl.setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_CONTINUOUS);
                    mbAttachengOpening = true;
                } else {
                    Toast.makeText(mContext, R.string.rv_document_open_failed, Toast.LENGTH_SHORT).show();
                }

                onAttachmentDocOpened(document, errCode);
            }

            @Override
            public void onDocWillClose(PDFDoc document) {

            }

            @Override
            public void onDocClosed(PDFDoc document, int errCode) {
                onAttachmentDocClosed();
            }

            @Override
            public void onDocWillSave(PDFDoc document) {

            }

            @Override
            public void onDocSaved(PDFDoc document, int errCode) {

            }
        });
        mAttachPdfViewCtrl.openDoc(filePath, null);

    }

    private void closeAttachment() {
        if(mbAttachengOpening) {
            mbAttachengOpening = false;
            onAttachmentDocWillClose();
            mAttachPdfViewCtrl.closeDoc();
        }
    }


    private void _onOpenAttachment(Annot annot) {
        FileSpec fileSpec = null;
        try {
            fileSpec = ((FileAttachment) annot).getFileSpec();

            String fileName = fileSpec.getFileName();
            String tmpPath = getTMP_PATH();
            String uuid = annot.getUniqueID();
            if (uuid == null)
                uuid = AppDmUtil.randomUUID("");
            tmpPath = tmpPath + uuid + "/";
            File file = new File(tmpPath);
            file.mkdirs();
            final String newFilePath = tmpPath + fileName;

            FileAttachmentUtil.saveAttachment(mPdfViewCtrl, newFilePath, annot, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        String ExpName = newFilePath.substring(newFilePath.lastIndexOf('.') + 1).toLowerCase();
                        if (ExpName.equals("pdf")) {
                            openAttachment(newFilePath);
                        } else {
                            if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
                            Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                            if (context == null) return;
                            AppIntentUtil.openFile((Activity) context, newFilePath);
                        }
                    }
                }

            });
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

}
