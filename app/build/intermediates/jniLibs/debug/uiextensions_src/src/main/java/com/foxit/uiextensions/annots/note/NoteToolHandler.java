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
package com.foxit.uiextensions.annots.note;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.Note;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

//ToolHandler is for creation, and AnnotHandler is for edition.
public class NoteToolHandler implements ToolHandler {
    private Context mContext;
    private AppDisplay mDisplay;
    private PDFViewCtrl mPdfViewCtrl;
    private EditText mET_Content;
    private TextView mDialog_title;
    private Button mCancel;
    private Button mSave;
    private int mColor;
    private int mOpacity;
    private String mIconType;

    private boolean mIsContinuousCreate;

    public boolean getIsContinuousCreate() {
        return mIsContinuousCreate;
    }

    public void setIsContinuousCreate(boolean isContinuousCreate) {
        this.mIsContinuousCreate = isContinuousCreate;
    }

    public NoteToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mDisplay = new AppDisplay(context);
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        int action = motionEvent.getActionMasked();
        PointF pageViewPt = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                initDialog(pageIndex, pageViewPt);
                return true;
        }
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    //show dialog to input note content
    public void initDialog(final int pageIndex, final PointF point) {
        Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) {
            return;
        }
        final Dialog dialog;
        View mView = View.inflate(context, R.layout.rd_note_dialog_edit, null);
        mDialog_title = (TextView) mView.findViewById(R.id.rd_note_dialog_edit_title);
        mET_Content = (EditText) mView.findViewById(R.id.rd_note_dialog_edit);
        mCancel = (Button) mView.findViewById(R.id.rd_note_dialog_edit_cancel);
        mSave = (Button) mView.findViewById(R.id.rd_note_dialog_edit_ok);

        mView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog = new Dialog(context, R.style.rv_dialog_style);
        dialog.setContentView(mView, new ViewGroup.LayoutParams(mDisplay.getUITextEditDialogWidth(), ViewGroup.LayoutParams.WRAP_CONTENT));
        mET_Content.setMaxLines(10);

        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dlg_title_bg_4circle_corner_white);

        mDialog_title.setText(mContext.getResources().getString(R.string.fx_string_note));
        mSave.setEnabled(false);
        mSave.setTextColor(mContext.getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));

        mET_Content.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mET_Content.getText().length() == 0) {
                    mSave.setEnabled(false);
                    mSave.setTextColor(mContext.getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));
                } else {
                    mSave.setEnabled(true);
                    mSave.setTextColor(mContext.getResources().getColor(R.color.dlg_bt_text_selector));
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                AppUtil.dismissInputSoft(mET_Content);
            }
        });
        mSave.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                PointF pdfPoint = new PointF(point.x, point.y);
                mPdfViewCtrl.convertPageViewPtToPdfPt(point, pdfPoint, pageIndex);
                final RectF rect = new RectF(pdfPoint.x - 10, pdfPoint.y + 10, pdfPoint.x + 10, pdfPoint.y - 10);

                try {
                    final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
                    final Annot annot = page.addAnnot(Annot.e_annotNote, rect);
                    if (annot == null) {
                        if (!mIsContinuousCreate) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                        }
                        dialog.dismiss();
                        return;
                    }

                    final NoteAddUndoItem undoItem = new NoteAddUndoItem(mPdfViewCtrl);
                    undoItem.mPageIndex = pageIndex;
                    undoItem.mNM = AppDmUtil.randomUUID(null);
                    undoItem.mContents = mET_Content.getText().toString();
                    undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
                    undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
                    undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
                    undoItem.mFlags = Annot.e_annotFlagPrint | Annot.e_annotFlagNoZoom | Annot.e_annotFlagNoRotate;
                    undoItem.mColor = mColor;
                    undoItem.mOpacity = AppDmUtil.opacity100To255(mOpacity) / 255f;
                    undoItem.mOpenStatus = false;
                    undoItem.mIconName = mIconType;
                    undoItem.mBBox = new RectF(rect);

                    addAnnot(pageIndex, annot, undoItem, true, null);

                    dialog.dismiss();
                    AppUtil.dismissInputSoft(mET_Content);

                    if (!mIsContinuousCreate) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }
        });
        dialog.show();

        AppUtil.showSoftInput(mET_Content);
    }

    protected void addAnnot(int pageIndex, NoteAnnotContent content, boolean addUndo, Event.Callback result) {
        if (content.getFromType().equals(Module.MODULE_NAME_SELECTION)) {
            PointF point = new PointF(content.getBBox().left, content.getBBox().top);
            mPdfViewCtrl.convertPdfPtToPageViewPt(point, point, pageIndex);
            initDialog(pageIndex, point);
        } else if (content.getFromType().equals(Module.MODULE_NAME_REPLY)) {
            NoteAddUndoItem undoItem = new NoteAddUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(content);
            undoItem.mIsFromReplyModule = true;
            undoItem.mParentNM = content.getParentNM();
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();

            try {
                PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
                Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getAnnot(page, undoItem.mParentNM);
                Note reply = ((Markup) annot).addReply();
                addAnnot(pageIndex, reply, undoItem, addUndo, result);
            } catch (PDFException e) {
                e.printStackTrace();
            }

        } else {
            if (result != null) {
                result.result(null, false);
            }
        }
    }

    protected void addAnnot(final int pageIndex, final Annot annot, final NoteAddUndoItem undoItem, final boolean addUndo, final Event.Callback result) {
        NoteEvent event = new NoteEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, (Note) annot, mPdfViewCtrl);

        final EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                if (success) {
                    try {
                        DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(annot.getPage(), annot);
                        if (addUndo) {
                            DocumentManager.getInstance(mPdfViewCtrl).addUndoItem(undoItem);
                        }

                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            RectF annotRect = annot.getRect();
                            RectF pageViewRect = new RectF();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, pageViewRect, pageIndex);
                            Rect rectResult = new Rect();
                            pageViewRect.roundOut(rectResult);
                            rectResult.inset(-10, -10);
                            mPdfViewCtrl.refresh(pageIndex, rectResult);
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }

                if (result != null) {
                    result.result(event, success);
                }
            }
        });
        mPdfViewCtrl.addTask(task);

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

    public void setIconType(String iconType) {
        mIconType = iconType;
    }

    public String getIconType() {
        return mIconType;
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_NOTE;
    }

}
