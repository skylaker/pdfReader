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
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FileAttachment;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UIFileSelectDialog;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.Event;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;



public class FileAttachmentToolHandler implements ToolHandler {

    private int	mColor;
    private int	mOpacity;
    private String mIconName;
    private String mPath;
    private String attachmentName;
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private final int MAX_ATTACHMENT_FILE_SIZE = 1024*1024*300;
    private boolean mIsContinuousCreate = false;
    private HashMap<Annot, String> mAttachmentPath = new HashMap<>();
    public FileAttachmentToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_FileAttachment;
    }

    @Override
    public void onActivate() {

    }

    @Override
    public void onDeactivate() {

    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {

        int action = motionEvent.getActionMasked();
        PointF point = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);

        switch (action) {
            case MotionEvent.ACTION_DOWN:

                PDFPage page = null;
                RectF pageRect = new RectF();
                try {
                    page = mPdfViewCtrl.getDoc().getPage(pageIndex);

                    pageRect = new RectF(0, page.getHeight(), page.getWidth(), 0);

                } catch (PDFException e) {
                    e.printStackTrace();
                }
                if (point.x < pageRect.left) {
                    point.x = pageRect.left;
                }

                if (point.x > pageRect.right - 20) {
                    point.x = pageRect.right - 20;
                }

                if (point.y < 24) {
                    point.y = 24;
                }

                if (point.y > pageRect.top) {
                    point.y = pageRect.top;
                }


                showFileSelectDialog(pageIndex, point);
                break;
            default:
                break;
        }

        return true;
    }


    @Override
    public void onDraw(int pageIndex, Canvas canvas) {

    }

    public boolean getIsContinuousCreate() {
        return mIsContinuousCreate;
    }

    public void setIsContinuousCreate(boolean isContinuousCreate) {
        this.mIsContinuousCreate = isContinuousCreate;
    }


    public void setPaint(int color, int opacity, int FlagType) {

        mColor = color;
        mOpacity = opacity;
        String[] iconNames= FileAttachmentUtil.getIconNames();
        mIconName = iconNames[FlagType];
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() == this) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                return true;
            }
        }
        return false;
    }

    protected String getAttachmentPath(final Annot annot)
    {
        String path = null;
        if(annot!= null ) {
            path = mAttachmentPath.get(annot);
            if(path == null)
            {
                String fileName = null;
                try {
                    fileName = ((FileAttachment)annot).getFileSpec().getFileName();
                } catch (PDFException e) {
                    e.printStackTrace();
                }
                final String tmpPath = mContext.getFilesDir() + "/" + AppDmUtil.randomUUID(null) + fileName;
                FileAttachmentUtil.saveAttachment(mPdfViewCtrl, tmpPath, annot, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        mAttachmentPath.put(annot, tmpPath);
                    }
                });
                return tmpPath;
            }

        }
        return path;
    }

    protected void setAttachmentPath(Annot annot, String path)
    {
        mAttachmentPath.put(annot, path);
    }

    protected void addAnnot(final int pageIndex, final RectF rect, final Event.Callback result) {

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            final FileAttachment annot = (FileAttachment) page.addAnnot(Annot.e_annotFileAttachment, rect);
            final FileAttachmentAddUndoItem undoItem = new FileAttachmentAddUndoItem(mPdfViewCtrl);

            undoItem.mPageIndex = pageIndex;
            undoItem.mNM = AppDmUtil.randomUUID(null);
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mFlags = Annot.e_annotFlagPrint;
            undoItem.mColor = mColor;
            undoItem.mOpacity = AppDmUtil.opacity100To255(mOpacity) / 255f;
            undoItem.mIconName = mIconName;
            undoItem.mPath = mPath;
            undoItem.attacheName = attachmentName;
            undoItem.mBBox = new RectF(rect);
            FileAttachmentEvent event = new FileAttachmentEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);

            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                if (success) {
                    DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, annot);
                    DocumentManager.getInstance(mPdfViewCtrl).addUndoItem(undoItem);
                    if (mPdfViewCtrl.isPageVisible(pageIndex))
                        invalidate(pageIndex, rect, result);

                }

                }
            });
            mPdfViewCtrl.addTask(task);
            if (!mIsContinuousCreate) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
            }

            mPdfViewCtrl.getDoc().closePage(pageIndex);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void invalidate(int pageIndex, RectF annotRect, final Event.Callback result) {
        if(annotRect == null) {
            if (result != null) {
                result.result(null, true);
            }
            return;
        }
        RectF rectF = new RectF();
        rectF.set(annotRect);
        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
        Rect rect = new Rect();
        rectF.roundOut(rect);
        mPdfViewCtrl.refresh(pageIndex, rect);

    }


    private int MaxFileSize;
    private UIFileSelectDialog mfileSelectDialog;
    private void showFileSelectDialog(final int pageIndex, final PointF pointf) {
        if (mfileSelectDialog != null && mfileSelectDialog.isShowing()) return;
        final PointF point = new PointF();
        if (pointf != null) {
            point.set(pointf.x, pointf.y);
        }

        MaxFileSize = MAX_ATTACHMENT_FILE_SIZE;
        Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
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
                    attachmentName = mfileSelectDialog.getSelectedFiles().get(0).name;
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


                    //add attachment
                    mfileSelectDialog.dismiss();
                    RectF rectF = new RectF(point.x, point.y, point.x + 20, point.y - 24);
                    addAnnot(pageIndex, rectF, null);



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

    public void setColor(int color) {
        mColor = color;
    }

    public int getColor() {
        return mColor;
    }

    public void setOpacity(int opacity) {
        mOpacity = opacity;
    }

    public int getOpacity() {
        return mOpacity;
    }

    public String getIconName() {
        return mIconName;
    }

    public void setIconName(String iconName) {
        mIconName = iconName;
    }



}
