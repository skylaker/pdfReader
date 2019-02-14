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
package com.foxit.uiextensions.annots.textmarkup.strikeout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.ClipboardManager;
import android.view.MotionEvent;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.QuadPoints;
import com.foxit.sdk.pdf.annots.StrikeOut;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContent;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContentAbs;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupUtil;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.ToolUtil;

import java.util.ArrayList;

class StrikeoutAnnotHandler implements AnnotHandler {
    private Context mContext;
    private Paint mPaintBbox;
    private AnnotMenu mAnnotMenu;
    private ArrayList<Integer> mMenuItems;

    private int mModifyColor;
    private int mModifyOpacity;
    private int mModifyAnnotColor;
    private boolean mIsAnnotModified;
    private Annot mLastAnnot;
    private int mBBoxSpace;
    private StrikeoutToolHandler mStrikeoutToolHandler;

    private boolean mIsEditProperty;
    private PropertyBar mAnnotPropertyBar;

    private PDFViewCtrl mPdfViewCtrl;
    private AppAnnotUtil mAppAnnotUtil;

    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    public StrikeoutAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {

        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mAppAnnotUtil = AppAnnotUtil.getInstance(context);
        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mPaintBbox = new Paint();
        mPaintBbox.setAntiAlias(true);
        mPaintBbox.setStyle(Paint.Style.STROKE);
        mPaintBbox.setStrokeWidth(mAppAnnotUtil.getAnnotBBoxStrokeWidth());
        mPaintBbox.setPathEffect(AppAnnotUtil.getAnnotBBoxPathEffect());

        mDrawLocal_tmpF = new RectF();
        mMenuItems = new ArrayList<Integer>();
    }

    public void setToolHandler(StrikeoutToolHandler toolHandler) {
        mStrikeoutToolHandler = toolHandler;
    }

    public void setAnnotMenu(AnnotMenu annotMenu) {
        mAnnotMenu = annotMenu;
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotMenu;
    }

    public void setPropertyBar(PropertyBar propertyBar) {
        mAnnotPropertyBar = propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mAnnotPropertyBar;
    }

    @Override
    public int getType() {
        return Annot.e_annotStrikeOut;
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
        RectF bbox = getAnnotBBox(annot);
        return bbox.contains(point.x, point.y);
    }

    private int[] mPBColors = new int[PropertyBar.PB_COLORS_STRIKEOUT.length];

    public int getPBCustomColor() {
        return PropertyBar.PB_COLORS_STRIKEOUT[0];
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean needInvalid) {
        try {
            mTmpUndoColor = (int) annot.getBorderColor();
            mTmpUndoOpacity = (int) (((StrikeOut) annot).getOpacity() * 255f + 0.5f);
            mPaintBbox.setColor(mTmpUndoColor | 0xFF000000);

            mAnnotPropertyBar.setArrowVisible(false);
            resetMenuItems();
            mAnnotMenu.setMenuItems(mMenuItems);
            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
                @Override
                public void onAMClick(int btType) {
                    try {
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
                            System.arraycopy(PropertyBar.PB_COLORS_STRIKEOUT, 0, mPBColors, 0, mPBColors.length);
                            mPBColors[0] = getPBCustomColor();
                            mAnnotPropertyBar.setColors(mPBColors);
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, (int) annot.getBorderColor());
                            mAnnotPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100((int) (((StrikeOut) annot).getOpacity() * 255f + 0.5f)));
                            mAnnotPropertyBar.reset(PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY);
                            RectF annotRectF = new RectF();
                            int _pageIndex = annot.getPage().getIndex();

                            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                                mPdfViewCtrl.convertPdfRectToPageViewRect(annot.getRect(), annotRectF, _pageIndex);
                                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRectF, annotRectF, _pageIndex);
                            }
                            mAnnotPropertyBar.show(annotRectF, false);
                            mAnnotPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
                        } else if (btType == AnnotMenu.AM_BT_COMMENT) {
                            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                            UIAnnotReply.showComments(mPdfViewCtrl, ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), annot);
                        } else if (btType == AnnotMenu.AM_BT_REPLY) {
                            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                            UIAnnotReply.replyToAnnot(mPdfViewCtrl, ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), annot);
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
            });
            RectF annotRectF = annot.getRect();
            int _pageIndex = annot.getPage().getIndex();

            if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(annot.getRect(), annotRectF, _pageIndex);
                Rect rect = TextMarkupUtil.rectRoundOut(annotRectF, 0);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRectF, annotRectF, _pageIndex);
                mPdfViewCtrl.refresh(_pageIndex, rect);

                if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                    mLastAnnot = annot;
                }
            } else {
                mLastAnnot = annot;
            }
            mAnnotMenu.show(annotRectF);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
        mAnnotMenu.dismiss();
        try {
            if (mIsEditProperty) {
                mIsEditProperty = false;
            }

            if (mIsAnnotModified && reRender) {
                if (mTmpUndoColor != mModifyAnnotColor || mTmpUndoOpacity != mModifyOpacity) {
                    ModifyAnnot(annot, mModifyColor, mModifyOpacity, null, true, null);
                }
            } else if (mIsAnnotModified) {
                annot.setBorderColor(mTmpUndoColor);
                ((StrikeOut) annot).setOpacity(mTmpUndoOpacity / 255f);
                annot.resetAppearanceStream();
            }
            mIsAnnotModified = false;
            if (reRender) {
                int _pageIndex = annot.getPage().getIndex();

                if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                    RectF rectF = annot.getRect();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(annot.getRect(), rectF, _pageIndex);
                    Rect rect = TextMarkupUtil.rectRoundOut(rectF, 0);
                    mPdfViewCtrl.refresh(_pageIndex, rect);

                    mLastAnnot = null;
                }
                return;
            }

            mLastAnnot = null;
        } catch (PDFException e) {
            if (e.getLastError() == PDFError.OOM.getCode()) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent contentSupplier, boolean addUndo, Event.Callback result) {
        if (mStrikeoutToolHandler != null) {
            if (contentSupplier instanceof TextMarkupContent) {
                mStrikeoutToolHandler.AddAnnot(pageIndex, addUndo, contentSupplier, null,
                        contentSupplier.getBBox(), null, result);
            } else {
                TextMarkupContentAbs tmSelector = TextMarkupContentAbs.class.cast(contentSupplier);
                StrikeoutToolHandler.SelectInfo info = mStrikeoutToolHandler.mSelectInfo;
                info.mSubJect = contentSupplier.getSubject();
                info.clear();
                info.mIsFromTS = true;
                info.mStartChar = tmSelector.getTextSelector().getStart();
                info.mEndChar = tmSelector.getTextSelector().getEnd();
                info.mColor = tmSelector.getColor();
                info.mOpacity = tmSelector.getOpacity();

                mStrikeoutToolHandler.setFromSelector(true);
                mStrikeoutToolHandler.SelectCountRect(pageIndex, info);
                mStrikeoutToolHandler.OnSelectRelease(pageIndex, info, addUndo, result);
            }
        }
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        if (content == null) return;
        try {
            mTmpUndoColor = (int) annot.getBorderColor();
            mTmpUndoOpacity = (int) (((StrikeOut) annot).getOpacity() * 255f);
            mTmpUndoContents = annot.getContent();
            String strikeoutContent = content.getContents();
            if (strikeoutContent == null)
                annot.setContent("");
            else
                annot.setContent(strikeoutContent);

            if (mLastAnnot == annot) {
                mPaintBbox.setColor(content.getColor());
            }
            ModifyAnnot(annot, content.getColor(), content.getOpacity(), content.getModifiedDate(), addUndo, result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
        DeleteAnnot(annot, addUndo, result);
    }

    private void ModifyAnnot(final Annot annot, int color, int opacity, DateTime modifyDate, final boolean addUndo, final Event.Callback callback) {
        try {
            final PDFPage page = annot.getPage();

            if (null == page) return;

            if (modifyDate == null) {
                modifyDate = new DateTime();
                annot.setBorderColor(mModifyAnnotColor);
            } else {

                annot.setBorderColor(color);
                ((StrikeOut) annot).setOpacity(opacity / 255f);
            }

            final int _pageIndex = page.getIndex();

            final StrikeoutModifyUndoItem undoItem = new StrikeoutModifyUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = _pageIndex;
            undoItem.mColor = color;
            undoItem.mOpacity = opacity / 255f;
            undoItem.mModifiedDate = modifyDate;

            undoItem.mRedoColor = color;
            undoItem.mRedoOpacity = opacity / 255f;
            undoItem.mRedoContents = annot.getContent();

            undoItem.mUndoColor = mTmpUndoColor;
            undoItem.mUndoOpacity = mTmpUndoOpacity / 255f;
            undoItem.mUndoContents = mTmpUndoContents;

            undoItem.mPaintBbox = mPaintBbox;

            StrikeoutEvent event = new StrikeoutEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (StrikeOut) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        DocumentManager.getInstance(mPdfViewCtrl).onAnnotModified(page, annot);
                        if (addUndo) {
                            DocumentManager.getInstance(mPdfViewCtrl).addUndoItem(undoItem);
                        }
                        if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                            try {
                                RectF annotRectF = annot.getRect();
                                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, _pageIndex);
                                mPdfViewCtrl.refresh(_pageIndex, AppDmUtil.rectFToRect(annotRectF));
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }

                        }
                    }

                    if (callback != null) {
                        callback.result(null, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            if (e.getLastError() == PDFError.OOM.getCode()) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }


    private void DeleteAnnot(final Annot annot, final boolean addUndo, final Event.Callback result) {
        if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
        }

        try {
            final RectF annotRectF = annot.getRect();
            PDFPage page = annot.getPage();
            final int _pageIndex = page.getIndex();

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);

            final StrikeoutDeleteUndoItem undoItem = new StrikeoutDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = _pageIndex;
            int count = ((StrikeOut)annot).getQuadPointsCount();
            undoItem.mQuadPoints = new QuadPoints[count];
            for (int i = 0; i < count; i ++) {
                undoItem.mQuadPoints[i] = ((StrikeOut) annot).getQuadPoints(i);
            }

            StrikeoutEvent event = new StrikeoutEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (StrikeOut) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (addUndo) {
                            DocumentManager.getInstance(mPdfViewCtrl).addUndoItem(undoItem);
                        }
                        RectF deviceRectF = new RectF();
                        if (mPdfViewCtrl.isPageVisible(_pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRectF, _pageIndex);
                            mPdfViewCtrl.refresh(_pageIndex, AppDmUtil.rectFToRect(deviceRectF));
                        }
                    }

                    if (result != null) {
                        result.result(event, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);

        } catch (PDFException e) {
            if (e.getLastError() == PDFError.OOM.getCode()) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }


    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return false;
    }

    private int mTmpUndoColor;
    private int mTmpUndoOpacity;
    private String mTmpUndoContents;
    private RectF mDrawLocal_tmpF;

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null || !(annot instanceof StrikeOut)) return;
        if (!mPdfViewCtrl.isPageVisible(pageIndex)) return;
        int annotPageIndex = 0;
        try {
            annotPageIndex = annot.getPage().getIndex();

            if (annotPageIndex != pageIndex) return;
            if (mLastAnnot == annot) {
                RectF rectF = annot.getRect();
                RectF deviceRt = new RectF();
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, deviceRt, pageIndex);
                Rect rectBBox = TextMarkupUtil.rectRoundOut(deviceRt, mBBoxSpace);
                canvas.save();
                canvas.drawRect(rectBBox, mPaintBbox);
                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        try{
            PointF pointF = AppAnnotUtil.getPdfPoint(mPdfViewCtrl, pageIndex, motionEvent);
            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pointF)) {
                    return true;
                } else {
                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                }
            } else {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(annot);
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void onDrawForControls(Canvas canvas) {
        Annot curAnnot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (curAnnot == null || !(curAnnot instanceof StrikeOut)) return;
        if (ToolUtil.getCurrentAnnotHandler((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()) != this) return;
        try {
            int annotPageIndex = curAnnot.getPage().getIndex();

            mDrawLocal_tmpF.set(curAnnot.getRect());
            if (mPdfViewCtrl.isPageVisible(annotPageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(curAnnot.getRect(), mDrawLocal_tmpF, annotPageIndex);
                RectF canvasRt = new RectF();
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mDrawLocal_tmpF, canvasRt, annotPageIndex);
                if (mIsEditProperty) {
                    mAnnotPropertyBar.update(canvasRt);
                }
                mAnnotMenu.update(canvasRt);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }


    public void modifyAnnotColor(int color) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null) return;
        try {
            mModifyColor = color & 0xFFFFFF;
            mModifyOpacity = (int) (((StrikeOut) annot).getOpacity() * 255f);

            mModifyAnnotColor = mModifyColor;
            if (annot.getBorderColor() != mModifyAnnotColor) {
                mIsAnnotModified = true;
                annot.setBorderColor(mModifyAnnotColor);
                ((StrikeOut) annot).setOpacity(mModifyOpacity / 255f);
                annot.resetAppearanceStream();
                mPaintBbox.setColor(mModifyAnnotColor | 0xFF000000);
                invalidateForToolModify(annot);
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFError.OOM.getCode()) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    private void invalidateForToolModify(Annot annot) {
        try {
            int pageIndex = annot.getPage().getIndex();
            if (!mPdfViewCtrl.isPageVisible(pageIndex)) return;
            RectF rectF = annot.getRect();
            RectF pvRect = new RectF();
            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, pvRect, pageIndex);
            Rect rect = TextMarkupUtil.rectRoundOut(pvRect, mBBoxSpace);
            rect.inset(-1, -1);
            mPdfViewCtrl.refresh(pageIndex, rect);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void modifyAnnotOpacity(int opacity) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null) return;
        try {
            mModifyColor = (int) annot.getBorderColor() & 0xFFFFFF;
            mModifyOpacity = opacity;

            mModifyAnnotColor = mModifyColor;
            if (((StrikeOut) annot).getOpacity() * 255f != mModifyOpacity) {
                mIsAnnotModified = true;
                annot.setBorderColor(mModifyAnnotColor);
                ((StrikeOut) annot).setOpacity(mModifyOpacity / 255f);
                annot.resetAppearanceStream();
                mPaintBbox.setColor(mModifyAnnotColor | 0xFF000000);
                invalidateForToolModify(annot);
            }
        } catch (PDFException e) {
            if (e.getLastError() == PDFError.OOM.getCode()) {
                mPdfViewCtrl.recoverForOOM();
            }
            return;
        }
    }

    public void resetMenuItems() {
        mMenuItems.clear();

        if (DocumentManager.getInstance(mPdfViewCtrl).canCopy()) {
            mMenuItems.add(AnnotMenu.AM_BT_COPY);
        }
        if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
        } else {
            mMenuItems.add(AnnotMenu.AM_BT_STYLE);
            mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
            mMenuItems.add(AnnotMenu.AM_BT_REPLY);
            mMenuItems.add(AnnotMenu.AM_BT_DELETE);
        }
    }

    public void removeProbarListener() {
        mPropertyChangeListener = null;
    }
}