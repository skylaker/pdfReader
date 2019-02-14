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
package com.foxit.uiextensions.annots.freetext.typewriter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.DefaultAppearance;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.freetext.FtTextUtil;
import com.foxit.uiextensions.annots.freetext.FtUtil;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.ToolUtil;

import java.util.ArrayList;


public class TypewriterAnnotHandler implements AnnotHandler {

    private Context mContext;

    private AnnotMenu mAnnotMenu;
    private PropertyBar mPropertyBar;
    private boolean mEditingProperty;
    private ArrayList<Integer> mMenuText;
    private boolean mModifyed;

    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    private Annot mBitmapAnnot;
    private int mBBoxSpace;
    private int mOffset;
    private Paint mPaintOut;
    private boolean mTouchCaptured = false;
    private PointF mDownPoint;
    private PointF mLastPoint;
    private EditText mEditView;
    private FtTextUtil mTextUtil;
    private float mBBoxWidth;
    private float mBBoxHeight;

    private int mTempLastColor;
    private int mTempLastOpacity;
    private String mTempLastFont;
    private float mTempLastFontSize;
    private RectF mTempLastBBox;
    private String mTempLastContent;
    private ArrayList<String> mTempLastComposedText = new ArrayList<String>();
    private boolean mEditState;
    private PointF mEditPoint = new PointF(0, 0);
    private RectF mDocViewBBox = new RectF();
    private boolean mIsSelcetEndText = false;

    private PDFViewCtrl mPdfViewCtrl;
    private float deltaWidth = 0f;
    private float deltaHeight = 0f;
    private boolean isDeleteAnnot = false;

    TypewriterAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;

        mDownPoint = new PointF();
        mLastPoint = new PointF();

        mPaintOut = new Paint();
        mPaintOut.setAntiAlias(true);
        mPaintOut.setStyle(Paint.Style.STROKE);
        mPaintOut.setPathEffect(AppAnnotUtil.getAnnotBBoxPathEffect());
        mPaintOut.setStrokeWidth(AppAnnotUtil.getInstance(context).getAnnotBBoxStrokeWidth());

        mMenuText = new ArrayList<Integer>();
        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mBitmapAnnot = null;
    }

    public void setAnnotMenu(AnnotMenu annotMenu) {
        mAnnotMenu = annotMenu;
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotMenu;
    }

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    public void setPropertyBar(PropertyBar propertyBar) {
        mPropertyBar = propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mPropertyBar;
    }

    @Override
    public int getType() {
        return Annot.e_annotFreeText;
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
        if (bbox == null) return false;
        try {
            mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, annot.getPage().getIndex());
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return bbox.contains(point.x, point.y);
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean needInvalid) {
        deltaWidth = 0;
        deltaHeight = 0;
        mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);
        mEditView = new EditText(mContext);
        mEditView.setLayoutParams(new LayoutParams(1, 1));
        try {
            mEditView.setText(annot.getContent());
            Font oldFont = ((FreeText) annot).getDefaultAppearance().getFont();
            String fontName = "";
            if (oldFont != null) {
                fontName = mTextUtil.getSupportFontName(oldFont.getName());
            }

            Font font = null;
            DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
            if (oldFont == null) {
                da.setFlags(DefaultAppearance.e_defaultAPFont | DefaultAppearance.e_defaultAPTextColor | DefaultAppearance.e_defaultAPFontSize);
            }
            font = mTextUtil.getSupportFont(fontName);
            da.setFont(font);
            ((FreeText) annot).setDefaultAppearance(da);

            DefaultAppearance defaultAppearance = ((FreeText) annot).getDefaultAppearance();

            mTempLastColor = (int) (defaultAppearance.getTextColor());
            mTempLastOpacity = (int) (((FreeText) annot).getOpacity() * 255f + 0.5f);
            mTempLastBBox = annot.getRect();
            Font daFont = defaultAppearance.getFont();
            mTempLastFont = mTextUtil.getSupportFontName(daFont.getName());
            mTempLastFontSize = defaultAppearance.getFontSize();
            mTempLastContent = annot.getContent();
            if (mTempLastContent == null) {
                mTempLastContent = "";
            }

            int pageIndex = annot.getPage().getIndex();

            RectF _rect = annot.getRect();
            RectF mPageViewRect = new RectF(_rect.left, _rect.top, _rect.right, _rect.bottom);
            mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
            RectF menuRect = new RectF(mPageViewRect);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(menuRect, menuRect, pageIndex);
            prepareAnnotMenu(annot);
            mAnnotMenu.show(menuRect);
            preparePropertyBar();
        } catch (PDFException e) {
            e.printStackTrace();
        }

        mTextUtil.setOnWidthChanged(new FtTextUtil.OnTextValuesChangedListener() {

            @Override
            public void onMaxWidthChanged(float maxWidth) {
                if (mBBoxWidth != maxWidth) {
                    mBBoxWidth = maxWidth;
                    try {
                        RectF textRect = ((FreeText) annot).getRect();
                        int pageIndex = annot.getPage().getIndex();
                        mPdfViewCtrl.convertPdfRectToPageViewRect(textRect, textRect, pageIndex);
                        if (mPdfViewCtrl.isPageVisible(pageIndex) && mBBoxWidth > textRect.width()) {
                            textRect.set(textRect.left, textRect.top, textRect.left + mBBoxWidth, textRect.bottom);
                            RectF rectChanged = new RectF(textRect);
                            mPdfViewCtrl.convertPageViewRectToPdfRect(textRect, textRect, pageIndex);
                            ((FreeText) annot).move(textRect);
                            annot.resetAppearanceStream();
                            rectChanged.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);

                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectChanged, rectChanged, pageIndex);
                            mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectChanged));
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onMaxHeightChanged(float maxHeight) {
                if (mBBoxHeight != maxHeight) {
                    mBBoxHeight = maxHeight;
                    try {
                        RectF textRect = ((FreeText) annot).getRect();
                        int pageIndex = annot.getPage().getIndex();
                        mPdfViewCtrl.convertPdfRectToPageViewRect(textRect, textRect, pageIndex);
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            textRect.set(textRect.left, textRect.top, textRect.right, textRect.top + mBBoxHeight);
                            RectF rectChanged = new RectF(textRect);
                            mPdfViewCtrl.convertPageViewRectToPdfRect(textRect, textRect, pageIndex);
                            ((FreeText) annot).move(textRect);
                            annot.resetAppearanceStream();
                            rectChanged.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectChanged, rectChanged, pageIndex);
                            mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectChanged));

                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }

                }

            }

            @Override
            public void onCurrentSelectIndex(int selectIndex) {
                System.err.println("selectindex  " + selectIndex);
                if (selectIndex >= mEditView.getText().length()) {
                    selectIndex = mEditView.getText().length();
                    mIsSelcetEndText = true;
                } else {
                    mIsSelcetEndText = false;
                }
                mEditView.setSelection(selectIndex);
            }

            @Override
            public void onEditPointChanged(float editPointX, float editPointY) {

                try {
                    int pageIndex = annot.getPage().getIndex();
                    PointF point = new PointF(editPointX, editPointY);
                    mPdfViewCtrl.convertPdfPtToPageViewPt(point, point, pageIndex);
                    mEditPoint.set(point.x, point.y);
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }
        });

        mEditView.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    annot.setContent(String.valueOf(s));
                    annot.resetAppearanceStream();
                    RectF pageViewRect = annot.getRect();
                    int pageIndex = annot.getPage().getIndex();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                    RectF pdfRectF = new RectF(pageViewRect.left, pageViewRect.top,
                            pageViewRect.left + mBBoxWidth, pageViewRect.top + mBBoxHeight);
                    RectF rect = new RectF(pdfRectF.left, pdfRectF.top,
                            pdfRectF.left + mBBoxWidth, pdfRectF.top + mBBoxHeight);
                    Rect mRect = new Rect((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom);
                    mRect.inset(-AppDisplay.getInstance(mContext).dp2px(200), -AppDisplay.getInstance(mContext).dp2px(200));
                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(AppDmUtil.rectToRectF(mRect), AppDmUtil.rectToRectF(mRect), pageIndex);
                        mPdfViewCtrl.invalidate(mRect);
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        // get annot bitmap and update page view
        try {
            RectF _rect = annot.getRect();
            RectF viewRect = new RectF(_rect.left, _rect.top, _rect.right, _rect.bottom);
            int pageIndex = annot.getPage().getIndex();
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
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
    public void onAnnotDeselected(final Annot annot, boolean needInvalid) {
        mAnnotMenu.dismiss();
        if (mEditingProperty) {
            mEditingProperty = false;
            mPropertyBar.dismiss();
        }

        if (isDeleteAnnot) {
            needInvalid = false;
        }

        try {
            PDFPage page = annot.getPage();
            if (page != null) {
                RectF pdfRect = annot.getRect();
                final RectF viewRect = new RectF(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                final int pageIndex = page.getIndex();
                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                if (mEditView != null && !mEditView.getText().toString().equals(mTempLastContent)) {
                    RectF pageViewRect = annot.getRect();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                    RectF pdfRectF = new RectF(pageViewRect.left, pageViewRect.top, pageViewRect.left + mBBoxWidth,
                            pageViewRect.top + mBBoxHeight);

                    mPdfViewCtrl.convertPageViewRectToPdfRect(pdfRectF, pdfRectF, pageIndex);
                    annot.move(new RectF(pdfRectF.left, pdfRectF.top, pdfRectF.right, pdfRectF.bottom));

                    RectF rectF = new RectF(pdfRectF);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                    String content = mEditView.getText().toString();
                    String font = mTextUtil.getSupportFontName(da);
                    float fontSize = da.getFontSize();
                    String annotContent = "";

                    ArrayList<String> composeText = mTextUtil.getComposedText(mPdfViewCtrl, pageIndex, rectF, content, font, fontSize);
                    for (int i = 0; i < composeText.size(); i ++) {
                        annotContent += composeText.get(i);
                        if (i != composeText.size() - 1 && annotContent.charAt(annotContent.length() - 1) != '\n'
                                && annotContent.charAt(annotContent.length() - 1) != '\r') {
                            annotContent += "\r";
                        }
                    }
                    modifyAnnot(pageIndex, annot, annot.getRect(), (int) da.getTextColor(), (int) (((FreeText) annot).getOpacity() * 255f),
                            font, fontSize, annotContent, false);
                }
                if (needInvalid && mModifyed) {
                    if (mTempLastColor == da.getTextColor() && mTempLastOpacity == (int) (((FreeText) annot).getOpacity() * 255f)
                            && mTempLastBBox.equals(annot.resetAppearanceStream()) && mTempLastContent.equals(annot.getContent())
                            && mTempLastFontSize == da.getFontSize()
                            && mTempLastFont == mTextUtil.getSupportFontName(da)) {
                        modifyAnnot(pageIndex, annot, annot.getRect(), (int) da.getTextColor(),
                                (int) (((FreeText) annot).getOpacity() * 255f + 0.5f), mTextUtil.getSupportFontName(da),
                                da.getFontSize(), annot.getContent(), false);
                    } else {
                        modifyAnnot(pageIndex, annot, annot.getRect(), (int) da.getTextColor(),
                                (int) (((FreeText) annot).getOpacity() * 255f + 0.5f), mTextUtil.getSupportFontName(da),
                                da.getFontSize(), annot.getContent(), true);
                    }
                } else {
                    da.setTextColor(mTempLastColor);
                    ((FreeText) annot).setOpacity(mTempLastOpacity / 255f);
                    annot.move(mTempLastBBox);
                    da.setFontSize(mTempLastFontSize);
                     Font font = mTextUtil.getSupportFont(mTempLastFont);
                    da.setFont(font);
                    ((FreeText) annot).setDefaultAppearance(da);
                    annot.setContent(mTempLastContent);
                    annot.resetAppearanceStream();
                }


                if (mPdfViewCtrl.isPageVisible(pageIndex) && needInvalid) {
                    final RectF rect2 = annot.getRect();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                    Task.CallBack callBack = new Task.CallBack() {
                        @Override
                        public void result(Task task) {
                            if (mBitmapAnnot != DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                                mBitmapAnnot = null;
                                AppUtil.dismissInputSoft(mEditView);
                                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView().removeView(mEditView);
                                mEditState = false;
                                mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                                mBBoxWidth = 0;
                                mBBoxHeight = 0;
                                mEditPoint.set(0, 0);
                                mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                                if (mPdfViewCtrl.isPageVisible(pageIndex) && (pageIndex == mPdfViewCtrl.getPageCount() - 1
                                        || mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE)
                                        && pageIndex == mPdfViewCtrl.getCurrentPage()) {
                                    PointF endPoint = new PointF(mPdfViewCtrl.getPageViewWidth(pageIndex), mPdfViewCtrl.getPageViewHeight(pageIndex));
                                    mPdfViewCtrl.convertPageViewPtToDisplayViewPt(endPoint, endPoint, pageIndex);
                                    if (AppDisplay.getInstance(mContext).getRawScreenHeight() - (endPoint.y - mTextUtil.getKeyboardOffset()) > 0) {
                                        mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                                        mTextUtil.setKeyboardOffset(0);
                                        mPdfViewCtrl.convertPdfRectToPageViewRect(rect2, rect2, pageIndex);
                                        PointF startPoint = new PointF(rect2.left, rect2.top);
                                        mPdfViewCtrl.gotoPage(pageIndex,
                                                mTextUtil.getPageViewOrigin(mPdfViewCtrl, pageIndex, startPoint.x, startPoint.y).x,
                                                mTextUtil.getPageViewOrigin(mPdfViewCtrl, pageIndex, startPoint.x, startPoint.y).y);


                                    }
                                }

                            }
                        }
                    };
                    mPdfViewCtrl.addTask(new Task(callBack) {
                        @Override
                        protected void execute() {

                        }
                    });

                } else {
                    mBitmapAnnot = null;
                    AppUtil.dismissInputSoft(mEditView);
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView().removeView(mEditView);
                    mEditState = false;
                    mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                    mBBoxWidth = 0;
                    mBBoxHeight = 0;
                    mEditPoint.set(0, 0);
                }
            } else {
                mBitmapAnnot = null;
                AppUtil.dismissInputSoft(mEditView);
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView().removeView(mEditView);
                mEditState = false;
                mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
                mBBoxWidth = 0;
                mBBoxHeight = 0;
                mEditPoint.set(0, 0);
            }
            mModifyed = false;
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }


    private void preparePropertyBar() {
        FreeText curAnnot = null;
        if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() instanceof FreeText) {
            curAnnot = (FreeText) DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        }
        if (curAnnot == null) {
            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
            return;
        } else {
            int[] colors = new int[PropertyBar.PB_COLORS_TYPEWRITER.length];
            System.arraycopy(PropertyBar.PB_COLORS_TYPEWRITER, 0, colors, 0, colors.length);
            colors[0] = PropertyBar.PB_COLORS_TYPEWRITER[0];
            mPropertyBar.setColors(colors);
            try {
                DefaultAppearance da = curAnnot.getDefaultAppearance();
                mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, da.getTextColor());
                mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, AppDmUtil.opacity255To100((int) (curAnnot.getOpacity() * 255f + 0.5f)));
                mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTNAME, mTextUtil.getSupportFontName(da));
                mPropertyBar.setProperty(PropertyBar.PROPERTY_FONTSIZE, da.getFontSize());
            } catch (PDFException e) {
                e.printStackTrace();
            }

        }
        mPropertyBar.setArrowVisible(false);
        mPropertyBar.reset(getSupportedProperties());

        mPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
    }

    private long getSupportedProperties() {
        return PropertyBar.PROPERTY_COLOR
                | PropertyBar.PROPERTY_OPACITY
                | PropertyBar.PROPERTY_FONTSIZE
                | PropertyBar.PROPERTY_FONTNAME;
    }

    private void prepareAnnotMenu(final Annot annot) {
        resetAnnotationMenuResource(annot);
        mAnnotMenu.setMenuItems(mMenuText);

        mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
            @Override
            public void onAMClick(int btType) {
                if (btType == AnnotMenu.AM_BT_DELETE) {
                    if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                        deleteAnnot(annot, true, null);
                    }
                } else if (btType == AnnotMenu.AM_BT_EDIT) {
                    mAnnotMenu.dismiss();
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView().addView(mEditView);
                    mTextUtil.getBlink().postDelayed((Runnable) mTextUtil.getBlink(), 500);
                    mEditView.setSelection(mEditView.getText().length());
                    AppUtil.showSoftInput(mEditView);
                    mEditState = true;
                    try {
                        int pageIndex = annot.getPage().getIndex();
                        RectF rectF = annot.getRect();
                        final RectF viewRect = new RectF(rectF.left, rectF.top, rectF.right, rectF.bottom);
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewRect, viewRect, pageIndex);
                            mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(viewRect));
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }

                } else if (btType == AnnotMenu.AM_BT_STYLE) {
                    mPropertyBar.show(mDocViewBBox, false);
                    mAnnotMenu.dismiss();
                }
            }
        });
    }

    /**
     * reset mAnnotationMenu text
     */
    private void resetAnnotationMenuResource(Annot annot) {
        mMenuText.clear();
        if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            mMenuText.add(AnnotMenu.AM_BT_EDIT);
            mMenuText.add(AnnotMenu.AM_BT_STYLE);
            mMenuText.add(AnnotMenu.AM_BT_DELETE);
        }
    }

    @Override
    public void addAnnot(final int pageIndex, AnnotContent content, final boolean addUndo, final Event.Callback result) {
        TypewriterAnnotContent lContent = (TypewriterAnnotContent) content;
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            final FreeText annot = (FreeText) page.addAnnot(Annot.e_annotFreeText, content.getBBox());
            final TypewriterAddUndoItem undoItem = new TypewriterAddUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(lContent);
            undoItem.mPageIndex = pageIndex;
            undoItem.mFont = mTextUtil.getSupportFont(lContent.getFontName());
            undoItem.mFontSize = lContent.getFontSize();
            undoItem.mTextColor = lContent.getColor();
            undoItem.mDaFlags = DefaultAppearance.e_defaultAPFont | DefaultAppearance.e_defaultAPTextColor | DefaultAppearance.e_defaultAPFontSize;
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mFlags = Annot.e_annotFlagPrint;
            undoItem.mIntent = "FreeTextTypewriter";

            TypewriterEvent event = new TypewriterEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        DocumentManager.getInstance(mPdfViewCtrl).onAnnotAdded(page, annot);
                        if (addUndo) {
                            DocumentManager.getInstance(mPdfViewCtrl).addUndoItem(undoItem);
                        }
                        try {
                            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                                RectF viewRect = annot.getRect();
                                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                                Rect rect = new Rect();
                                viewRect.roundOut(rect);
                                rect.inset(-10, -10);
                                mPdfViewCtrl.refresh(pageIndex, rect);
                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }

                    }

                    if (result != null) {
                        result.result(null, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
        deleteAnnot(annot, addUndo, result);
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e, Annot annot) {
        if (!DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()) {
            return false;
        }
        PointF devPoint = new PointF(e.getX(), e.getY());
        PointF point = new PointF();
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(devPoint, point, pageIndex);

        float evX = point.x;
        float evY = point.y;
        int action = e.getAction();
        try {

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()
                            && isHitAnnot(annot, point) && !mEditState) {
                        mDownPoint.set(evX, evY);
                        mLastPoint.set(evX, evY);
                        mTouchCaptured = true;
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (mTouchCaptured && pageIndex == annot.getPage().getIndex()
                            && annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() && !mEditState) {

                        if (evX != mLastPoint.x || evY != mLastPoint.y) {
                            RectF pageViewRect = annot.getRect();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                            pageViewRect.set(pageViewRect.left - mOffset, pageViewRect.top, pageViewRect.left + mBBoxWidth + mOffset, pageViewRect.top + mBBoxHeight);

                            RectF rectInv = new RectF(pageViewRect);
                            RectF rectChanged = new RectF(pageViewRect);

                            rectInv.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                            rectChanged.offset(evX - mDownPoint.x, evY - mDownPoint.y);
                            float deltaXY = FtUtil.widthOnPageView(mPdfViewCtrl, annot.getPage().getIndex(), 2);
                            float adjustx = 0;
                            float adjusty = 0;
                            if (rectChanged.left < deltaXY) {
                                adjustx = -rectChanged.left + deltaXY;
                            }
                            if (rectChanged.top < deltaXY) {
                                adjusty = -rectChanged.top + deltaXY;
                            }
                            if (rectChanged.right > mPdfViewCtrl.getPageViewWidth(pageIndex) - deltaXY) {
                                adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - rectChanged.right - deltaXY;
                            }
                            if (rectChanged.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY) {
                                adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - rectChanged.bottom - deltaXY;
                            }
                            if (rectChanged.top < deltaXY && rectChanged.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY) {
                                adjusty = -rectChanged.top + deltaXY;
                            }
                            rectChanged.offset(adjustx, adjusty);

                            rectInv.union(rectChanged);
                            rectInv.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInv, rectInv, pageIndex);
                            mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectInv));

                            RectF rectInViewerF = new RectF(rectChanged);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInViewerF, rectInViewerF, pageIndex);
                            if (mAnnotMenu.isShowing()) {
                                mAnnotMenu.dismiss();
                                mAnnotMenu.update(rectInViewerF);
                            }
                            if (mEditingProperty) {
                                mPropertyBar.dismiss();
                            }
                            mLastPoint.set(evX, evY);
                            mLastPoint.offset(adjustx, adjusty);
                        }
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                    if (mTouchCaptured && annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()) {
                        RectF pageViewRect = annot.getRect();
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);

                        RectF rectChanged = new RectF(pageViewRect);
                        rectChanged.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                        RectF rectInViewerF = new RectF(rectChanged);
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectInViewerF, rectInViewerF, pageIndex);
                        if (mEditingProperty) {
                        } else {
                            if (mAnnotMenu.isShowing()) {
                                mAnnotMenu.update(rectInViewerF);
                            } else {
                                mAnnotMenu.show(rectInViewerF);
                            }
                        }
                        mPdfViewCtrl.convertPageViewRectToPdfRect(rectChanged, rectChanged, pageIndex);
                        DefaultAppearance da = ((FreeText) (annot)).getDefaultAppearance();
                        if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                            RectF _rect = new RectF(rectChanged);
                            mPdfViewCtrl.convertPdfRectToPageViewRect(_rect, _rect, pageIndex);
                            _rect.right += deltaWidth;
                            _rect.bottom -= deltaHeight;
                            String font = mTextUtil.getSupportFontName(da);
                            float fontSize = da.getFontSize();
                            ArrayList<String> composeText = mTextUtil.getComposedText(mPdfViewCtrl, pageIndex, _rect, annot.getContent(), font, fontSize);

                            String annotContent = "";
                            for (int i = 0; i < composeText.size(); i++) {
                                annotContent += composeText.get(i);
                                char ch = annotContent.charAt(annotContent.length() - 1);
                                if (i != composeText.size() - 1 && ch != '\n' && ch != '\r') {
                                    annotContent += "\r";
                                }
                            }
                            mEditView.setText(annotContent);
                            modifyAnnot(pageIndex, annot, rectChanged,
                                    (int) da.getTextColor(),
                                    (int) (((FreeText) (annot)).getOpacity() * 255f), font, fontSize,
                                    annotContent, false);
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
                    mEditPoint.set(0, 0);
                    return false;
                default:
                    break;
            }
        } catch (PDFException e1) {
            e1.printStackTrace();
        }


        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        return onSingleTapOrLongPress(pageIndex, point, annot);
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        return onSingleTapOrLongPress(pageIndex, point, annot);
    }

    private boolean onSingleTapOrLongPress(int pageIndex, PointF point, Annot annot) {

        try {
            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex()
                        && isHitAnnot(annot, point) && mEditState) {
                    PointF pointF = new PointF(point.x, point.y);
                    mPdfViewCtrl.convertPageViewPtToPdfPt(pointF, pointF, pageIndex);
                    mEditPoint.set(pointF.x, pointF.y);
                    mTextUtil.resetEditState();
                    RectF pageViewRect = annot.getRect();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(pageViewRect, pageViewRect, pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(pageViewRect));
                    AppUtil.showSoftInput(mEditView);
                    return true;
                } else if (pageIndex == annot.getPage().getIndex()
                        && !isHitAnnot(annot, point)
                        && mEditView != null && !mEditView.getText().toString().equals(annot.getContent())) {
                    RectF pageViewRect = annot.getRect();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                    RectF pdfRectF = new RectF(pageViewRect.left, pageViewRect.top,
                            pageViewRect.left + mBBoxWidth, pageViewRect.top + mBBoxHeight);
                    mPdfViewCtrl.convertPageViewRectToPdfRect(pdfRectF, pdfRectF, pageIndex);
                    annot.move(new RectF(pdfRectF.left, pdfRectF.top, pdfRectF.right, pdfRectF.bottom));
                    DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();

                    modifyAnnot(pageIndex, annot, annot.getRect(), (int) da.getTextColor(),
                            (int) (((FreeText) annot).getOpacity() * 255f),
                            mTextUtil.getSupportFontName(da), da.getFontSize(), mEditView.getText()
                                    .toString(), false);
                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                    return true;
                } else {
                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                    return true;
                }
            } else {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(annot);
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null || !(annot instanceof FreeText)) {
            return;
        }
        if (ToolUtil.getCurrentAnnotHandler((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()) != this) return;
        try {
            if (annot.getType() != Annot.e_annotFreeText) {
                return;
            }

            if (mBitmapAnnot == annot && annot.getPage().getIndex() == pageIndex) {
                canvas.save();
                Rect rect1 = new Rect(0, 0, 10, 0);
                mPdfViewCtrl.convertPdfRectToPageViewRect(AppDmUtil.rectToRectF(rect1), AppDmUtil.rectToRectF(rect1), pageIndex);
                mOffset = rect1.width();

                RectF frameRectF = new RectF();
                RectF rect = annot.getRect();
                mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, pageIndex);
                rect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                PointF editPoint = new PointF(mEditPoint.x, mEditPoint.y);
                if (editPoint.x != 0 || editPoint.y != 0) {
                    mPdfViewCtrl.convertPdfPtToPageViewPt(editPoint, editPoint, pageIndex);
                }
                mTextUtil.setTextString(pageIndex, annot.getContent(), mEditState);
                mTextUtil.setStartPoint(new PointF(rect.left, rect.top));
                mTextUtil.setEditPoint(editPoint);
                if (mEditState) {
                    mTextUtil.setMaxRect(mPdfViewCtrl.getPageViewWidth(pageIndex) - rect.left, mPdfViewCtrl.getPageViewHeight(pageIndex) - rect.top);
                } else {
                    mTextUtil.setMaxRect(rect.width() + deltaWidth, mPdfViewCtrl.getPageViewHeight(pageIndex) - rect.top);
                }
                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                int opacity = (int) (((FreeText) annot).getOpacity() * 100);
                mTextUtil.setTextColor((int) da.getTextColor(), AppDmUtil.opacity100To255(opacity));
                mTextUtil.setFont(mTextUtil.getSupportFontName(da),da.getFontSize());
                if (mIsSelcetEndText) {
                    mTextUtil.setEndSelection(mEditView.getSelectionEnd() + 1);
                } else {
                    mTextUtil.setEndSelection(mEditView.getSelectionEnd());
                }
                mTextUtil.loadText();
                mTextUtil.DrawText(canvas);
                if (!mEditState) {
                    frameRectF.set(rect.left - mOffset, rect.top, rect.left + mBBoxWidth + mOffset, rect.top + mBBoxHeight);
                    mPaintOut.setColor((int) da.getTextColor() | 0xFF000000);
                    canvas.drawRect(frameRectF, mPaintOut);
                }
                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    public void onDrawForControls(Canvas canvas) {
        Annot curAnnot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (curAnnot != null && ToolUtil.getCurrentAnnotHandler((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()) == this && !mEditState) {
            try {
                mDocViewBBox = curAnnot.getRect();
                int pageIndex = curAnnot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mDocViewBBox, mDocViewBBox, pageIndex);
                    mDocViewBBox.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mDocViewBBox, mDocViewBBox, pageIndex);
                    mAnnotMenu.update(mDocViewBBox);
                    if (mPropertyBar.isShowing()) {
                        mPropertyBar.update(mDocViewBBox);
                    }

                }
            } catch (PDFException e) {
                e.printStackTrace();
            }

        }
    }


    public void onColorValueChanged(int color) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        try {
            DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
            if (annot != null && ToolUtil.getCurrentAnnotHandler((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()) == this
                    && color != (int) da.getTextColor()) {
                int pageIndex = annot.getPage().getIndex();
                modifyAnnot(pageIndex, annot, annot.getRect(), color, (int) (((FreeText) annot).getOpacity() * 255f),
                        mTextUtil.getSupportFontName(da), da.getFontSize(), annot.getContent(), false);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    public void onOpacityValueChanged(int opacity) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        try {
            if (annot != null && ToolUtil.getCurrentAnnotHandler((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()) == this
                    && AppDmUtil.opacity100To255(opacity) != (int) (((FreeText) annot).getOpacity() * 255f)) {
                int pageIndex = annot.getPage().getIndex();
                DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
                modifyAnnot(pageIndex, annot, annot.getRect(), (int) da.getTextColor(),
                        AppDmUtil.opacity100To255(opacity),
                        mTextUtil.getSupportFontName(da), da.getFontSize(), annot.getContent(), false);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onFontValueChanged(String font) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        try {
            DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
            if (annot != null && ToolUtil.getCurrentAnnotHandler((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()) == this
                    && font != mTextUtil.getSupportFontName(da)) {
                int pageIndex = annot.getPage().getIndex();
                RectF rectF = annot.getRect();
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                if (rectF.width() < mTextUtil.getFontWidth(mPdfViewCtrl, pageIndex, font, da.getFontSize())) {
                    rectF.set(rectF.left, rectF.top,
                            rectF.left + mTextUtil.getFontWidth(mPdfViewCtrl, pageIndex, font, da.getFontSize()),
                            rectF.bottom);
                }
                RectF rectChanged = new RectF(rectF);
                rectF.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, pageIndex);
                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectF));
                mPdfViewCtrl.convertPageViewRectToPdfRect(rectChanged, rectChanged, pageIndex);
                modifyAnnot(pageIndex, annot, rectChanged, (int) da.getTextColor(), (int) (((FreeText) annot).getOpacity() * 255f),
                        font, da.getFontSize(), annot.getContent(), false);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    public void onFontSizeValueChanged(float fontSize) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        try {
            DefaultAppearance da = ((FreeText) annot).getDefaultAppearance();
            if (annot != null && ToolUtil.getCurrentAnnotHandler((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()) == this
                    && fontSize != da.getFontSize()) {
                int pageIndex = annot.getPage().getIndex();
                RectF rectF = annot.getRect();
                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                if (rectF.width() < mTextUtil.getFontWidth(mPdfViewCtrl, pageIndex, mTextUtil.getSupportFontName(da), fontSize)) {
                    rectF.set(rectF.left, rectF.top,
                            rectF.left + mTextUtil.getFontWidth(mPdfViewCtrl, pageIndex, mTextUtil.getSupportFontName(da), fontSize),
                            rectF.bottom);
                }
                RectF rectChanged = new RectF(rectF);
                rectF.inset(-mBBoxSpace - mOffset, -mBBoxSpace - mOffset);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, pageIndex);
                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectF));
                mPdfViewCtrl.convertPageViewRectToPdfRect(rectChanged, rectChanged, pageIndex);
                modifyAnnot(pageIndex, annot, rectChanged, (int) da.getTextColor(), (int) (((FreeText) annot).getOpacity() * 255f),
                        mTextUtil.getSupportFontName(da), fontSize, annot.getContent(), false);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    private void modifyAnnot(int pageIndex, Annot annot, RectF bbox,
                             int color, int opacity, String font, float fontSize, String content, boolean isModifyJni) {
        if (mTextUtil == null) {
            mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);
        }

        modifyAnnot(pageIndex, (FreeText) annot, bbox, color, opacity, font, fontSize, content, isModifyJni, true, "FreeTextTypewriter", null);
    }

    private void deleteAnnot(final Annot annot, final boolean addUndo, final Event.Callback result) {
        if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
            isDeleteAnnot = true;
            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
            AppUtil.dismissInputSoft(mEditView);
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView().removeView(mEditView);
            mTextUtil.getBlink().removeCallbacks((Runnable) mTextUtil.getBlink());
        }

        try {
            final PDFPage page = annot.getPage();
            final int pageIndex = page.getIndex();
            if (mTextUtil == null) {
                mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);
            }

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);
            final RectF viewRect = annot.getRect();

            final TypewriterDeleteUndoItem undoItem = new TypewriterDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            DefaultAppearance da = ((FreeText)annot).getDefaultAppearance();
            undoItem.mFont = da.getFont();
            String fontName = "";
            if (undoItem.mFont != null) {
                fontName = mTextUtil.getSupportFontName(undoItem.mFont.getName());
            }
            undoItem.mFont = mTextUtil.getSupportFont(fontName);
            undoItem.mFontSize = da.getFontSize();
            undoItem.mTextColor = da.getTextColor();
            undoItem.mDaFlags = da.getFlags();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mIntent = ((FreeText) annot).getIntent();

            TypewriterEvent event = new TypewriterEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (FreeText) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    isDeleteAnnot = false;
                    if (success) {
                        if (addUndo) {
                            DocumentManager.getInstance(mPdfViewCtrl).addUndoItem(undoItem);
                        }
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                        }
                    }
                    if (result != null) {
                        result.result(null, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected void deleteAnnot(final Annot annot, final TypewriterDeleteUndoItem undoItem, final Event.Callback result) {
        if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
            isDeleteAnnot = true;
            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
        }

        try {
            final PDFPage page = annot.getPage();
            final int pageIndex = page.getIndex();

            DocumentManager.getInstance(mPdfViewCtrl).onAnnotDeleted(page, annot);
            final RectF viewRect = annot.getRect();

            TypewriterEvent event = new TypewriterEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (FreeText) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    isDeleteAnnot = false;
                    if (success) {

                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                        }
                    }
                    if (result != null) {
                        result.result(null, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        if (content == null) {
            if (result != null) {
                result.result(null, false);
            }
            return;
        }
        modifyAnnot(annot, (TypewriterAnnotContent) content, addUndo, result);
    }

    private void modifyAnnot(Annot annot, TypewriterAnnotContent content, boolean addUndo, Event.Callback result) {
        FreeText lAnnot = (FreeText) annot;

        if (mTextUtil == null) {
            mTextUtil = new FtTextUtil(mContext, mPdfViewCtrl);
        }

        PDFPage page = null;
        try {
            page = annot.getPage();
            int pageIndex = page.getIndex();
            String contents = null;
            String fontName = null;
            float fontSize = 0;
            if (content.getContents() == null || content.getContents().equals("")) {
                contents = " ";
            } else {
                contents = content.getContents();
            }
            contents = FtTextUtil.filterEmoji(contents);
            if (content.getFontName() == null || content.getFontName().equals("")) {
                fontName = "Courier";
            } else {
                if (!content.getFontName().startsWith("Cour") && !content.getFontName().equalsIgnoreCase("Courier")
                        && !content.getFontName().startsWith("Helv") && content.getFontName().equalsIgnoreCase("Helvetica")
                        && !content.getFontName().startsWith("Time") && !content.getFontName().equalsIgnoreCase("Times")) {
                    fontName = "Courier";
                } else {
                    fontName = content.getFontName();
                }
            }
            if (content.getFontSize() == 0) {
                fontSize = 24;
            } else {
                fontSize = content.getFontSize();
            }
            DefaultAppearance da = lAnnot.getDefaultAppearance();
            modifyAnnot(pageIndex, lAnnot, annot.getRect(), (int) da.getTextColor(), (int) (lAnnot.getOpacity() * 255f), fontName, fontSize, contents, true, addUndo, "", result);
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    protected void modifyAnnot(final int pageIndex, final Annot annot, final RectF bbox,
                               final int color, final int opacity, final String fontName, final float fontSize, String content,
                               boolean isModifyJni, final boolean addUndo, final String fromType, final Event.Callback result) {

        final TypewriterModifyUndoItem undoItem = new TypewriterModifyUndoItem(mPdfViewCtrl);
        undoItem.setCurrentValue(annot);
        undoItem.mPageIndex = pageIndex;
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mColor = color;
        undoItem.mOpacity = opacity / 255f;
        undoItem.mBBox = new RectF(bbox);
        if (content == null) {
            content = "";
        }
        undoItem.mContents = content;
        undoItem.mFont = mTextUtil.getSupportFont(fontName);
        undoItem.mFontSize = fontSize;
        undoItem.mTextColor = color;

        undoItem.mOldBBox = new RectF(mTempLastBBox);
        undoItem.mOldColor = mTempLastColor;
        undoItem.mOldOpacity = mTempLastOpacity / 255f;
        undoItem.mOldFont = mTextUtil.getSupportFont(mTempLastFont);
        undoItem.mOldFontSize = mTempLastFontSize;
        undoItem.mOldTextColor = mTempLastColor;
        undoItem.mOldContents = mTempLastContent;

        try {
            final RectF tempRectF = annot.getRect();
            if (isModifyJni) {
                DocumentManager.getInstance(mPdfViewCtrl).setHasModifyTask(true);
                TypewriterEvent event = new TypewriterEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (FreeText) annot, mPdfViewCtrl);
                EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            if (addUndo) {
                                DocumentManager.getInstance(mPdfViewCtrl).addUndoItem(undoItem);
                            }
                            DocumentManager.getInstance(mPdfViewCtrl).setHasModifyTask(false);
                            if (fromType.equals("")) {
                                mModifyed = true;
                            }

                            try {
                                RectF newViewRect = annot.getRect();
                                mPdfViewCtrl.convertPdfRectToPageViewRect(newViewRect, newViewRect, pageIndex);

                                RectF oldViewRect = new RectF(bbox);
                                mPdfViewCtrl.convertPdfRectToPageViewRect(oldViewRect, oldViewRect, pageIndex);

                                float tmp = oldViewRect.width() - newViewRect.width();
                                if (tmp >= 1.0f && deltaWidth < tmp + 10) {
                                    deltaWidth = tmp + 10;
                                }

                                tmp = newViewRect.height() - oldViewRect.height();
                                if (tmp >= 1.0f) {
                                    deltaHeight = tmp;
                                }

                                DocumentManager.getInstance(mPdfViewCtrl).onAnnotModified(annot.getPage(), annot);

                                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                                    RectF viewRect = annot.getRect();
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                                    viewRect.union(tempRectF);
                                    viewRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                                }
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }

                        }

                        if (result != null) {
                            result.result(null, success);
                        }
                    }
                });
                mPdfViewCtrl.addTask(task);

            }

            if (!fromType.equals("")) {
                mModifyed = true;
                if (isModifyJni) {
                    DocumentManager.getInstance(mPdfViewCtrl).onAnnotModified(annot.getPage(), annot);
                }

                if (!isModifyJni) {
                    FreeText ft_Annot = (FreeText) annot;
                    DefaultAppearance da = ft_Annot.getDefaultAppearance();
                    da.setTextColor(color);
                    Font font = mTextUtil.getSupportFont(fontName);
                    da.setFont(font);
                    da.setFontSize(fontSize);
                    ft_Annot.setDefaultAppearance(da);
                    ft_Annot.setOpacity(opacity / 255f);
                    ft_Annot.move(bbox);
                    ft_Annot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());

                    if (content == null) {
                        content = "";
                    }
                    ft_Annot.setContent(content);
                    ft_Annot.resetAppearanceStream();

                    RectF newViewRect = annot.getRect();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(newViewRect, newViewRect, pageIndex);

                    RectF oldViewRect = new RectF(bbox);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(oldViewRect, oldViewRect, pageIndex);

                    float tmp = oldViewRect.width() - newViewRect.width();
                    if (tmp >= 1.0f && deltaWidth < tmp + 10) {
                        deltaWidth = tmp + 10;
                    }

                    tmp = newViewRect.height() - oldViewRect.height();
                    if (tmp >= 1.0f) {
                        deltaHeight = tmp;
                    }

                    RectF viewRect = annot.getRect();
                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                    viewRect.union(tempRectF);
                    viewRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                }
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    public void removePropertyBarListener() {
        mPropertyChangeListener = null;
    }
}
