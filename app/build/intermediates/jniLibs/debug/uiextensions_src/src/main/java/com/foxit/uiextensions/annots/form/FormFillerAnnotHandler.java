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
package com.foxit.uiextensions.annots.form;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.form.Form;
import com.foxit.sdk.pdf.form.FormControl;
import com.foxit.sdk.pdf.form.FormField;
import com.foxit.sdk.pdf.form.FormFiller;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.ToolUtil;

import java.util.concurrent.CountDownLatch;


public class FormFillerAnnotHandler implements AnnotHandler {

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;
    private FormFiller mFormFiller;
    private FormFillerAssistImpl mAssist;
    private static Form mForm;
    private EditText mEditView = null;
    private PointF mLastTouchPoint = new PointF(0, 0);
//    protected static boolean mIsNeedRefresh = true;
    public FormNavigationModule mFNModule = null;
    private int mPageOffset;
    private boolean mIsBackBtnPush = false; //for some input method, double backspace click
    private boolean mAdjustPosition = false;
    private boolean mIsShowEditText = false;
//    protected static long mLastInputInvalidateTime = 0;
    private String mLastInputText = "";
    private String mChangeText = null;
    private Paint mPathPaint;
    private Blink mBlink = null;
    private Handler mHandler = null;
    private boolean bInitialize = false;
    private int mOffset;

    public FormFillerAnnotHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mParent = parent;
    }

    public void init(final Form form) {
        mAssist = new FormFillerAssistImpl(mPdfViewCtrl);
        mAssist.bWillClose = false;
        mForm = form;
        mPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPathPaint.setStyle(Paint.Style.STROKE);
        mPathPaint.setAntiAlias(true);
        mPathPaint.setDither(true);
        mHandler = new Handler(Looper.getMainLooper());
        PathEffect effects = new DashPathEffect(new float[]{1, 2, 4, 8}, 1);
        mPathPaint.setPathEffect(effects);
        try {
            mFormFiller = FormFiller.create(form, mAssist);

            boolean mEnableFormHighlight = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).isFormHighlightEnable();
            mFormFiller.highlightFormFields(mEnableFormHighlight);
            if (mEnableFormHighlight) {
                mFormFiller.setHighlightColor(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getFormHighlightColor());
            }
        } catch (PDFException e) {
            e.printStackTrace();
            return;
        }

        mFNModule = (FormNavigationModule) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_FORM_NAVIGATION);
        if (mFNModule != null) {
            mFNModule.getLayout().setVisibility(View.INVISIBLE);
            mFNModule.getPreView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(preNavigation).start();
                }
            });

            mFNModule.getNextView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(nextNavigation).start();
                }
            });

            mFNModule.getClearView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
                    if (annot != null && annot instanceof FormControl) {
                        FormControl formControl = (FormControl) annot;
                        try {
                            mFormFiller.setFocus(null);
                            FormField field = formControl.getField();
                            field.reset();
                            mFormFiller.setFocus(formControl);
                            refreshField(field);
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            mFNModule.getFinishView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() != null) {

                        if (shouldShowInputSoft(DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot())) {
                            if (mBlink != null)
                                mBlink.removeCallbacks((Runnable) mBlink);
                            mBlink = null;
                            AppUtil.dismissInputSoft(mEditView);
                            mParent.removeView(mEditView);
                        }
                        DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                    }
                    mFNModule.getLayout().setVisibility(View.INVISIBLE);
                    resetDocViewerOffset();
                }
            });

            mFNModule.setClearEnable(false);
        }
        bInitialize = true;
    }

    protected boolean hasInitialized() {
        return bInitialize;
    }

    private void postDismissNavigation() {
        dismissNavigation dn = new dismissNavigation();
        dn.postDelayed(dn, 500);
    }

    private class dismissNavigation extends Handler implements Runnable {

        @Override
        public void run() {
            if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) return;
            if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() == null || !(DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() instanceof FormControl)) {
                if (mFNModule != null)
                    mFNModule.getLayout().setVisibility(View.INVISIBLE);
                AppUtil.dismissInputSoft(mEditView);
                resetDocViewerOffset();
            }
        }
    }

    private boolean shouldShowNavigation(Annot annot) {
        if (annot == null) return false;
        if (!(annot instanceof FormControl)) return false;
        if (FormFillerUtil.getAnnotFieldType(mForm, annot) == FormField.e_formFieldPushButton)
            return false;
        else
            return true;
    }

    public void NavigationDismiss() {
        if (mFNModule != null) {
            mFNModule.getLayout().setVisibility(View.INVISIBLE);
            mFNModule.getLayout().setPadding(0, 0, 0, 0);
        }
        if (mBlink != null)
            mBlink.removeCallbacks((Runnable) mBlink);
        mBlink = null;
        if (mEditView != null) {
            mParent.removeView(mEditView);
        }
        resetDocViewerOffset();
        AppUtil.dismissInputSoft(mEditView);
    }

    private boolean isFind = false;
    private boolean isDocFinish = false;
    private PDFPage curPage = null;
    private int prePageIdx;
    private int preAnnotIdx;
    private int nextPageIdx;
    private int nextAnnotIdx;
    private CountDownLatch mCountDownLatch;
    private Runnable preNavigation = new Runnable() {

        @Override
        public void run() {

            Annot curAnnot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
            try {
                if (curAnnot != null && curAnnot instanceof FormControl) {
                    refreshField(((FormControl) curAnnot).getField());
                    curPage = curAnnot.getPage();
                    final int curPageIdx = curPage.getIndex();
                    prePageIdx = curPageIdx;
                    final int curAnnotIdx = curAnnot.getIndex();
                    preAnnotIdx = curAnnotIdx;
                    isFind = false;
                    isDocFinish = false;
                    while (prePageIdx >= 0) {
                        mCountDownLatch = new CountDownLatch(1);
                        curPage = mPdfViewCtrl.getDoc().getPage(prePageIdx);
                        if (prePageIdx == curPageIdx && isDocFinish == false) {
                            preAnnotIdx = curAnnotIdx - 1;
                        } else {
                            preAnnotIdx = curPage.getAnnotCount() - 1;
                        }

                        while (curPage != null && preAnnotIdx >= 0) {
                            final Annot preAnnot = curPage.getAnnot(preAnnotIdx);
                            if (preAnnot != null
                                    && preAnnot instanceof FormControl
                                    && FormFillerUtil.isReadOnly(preAnnot) == false
                                    && FormFillerUtil.isVisible(preAnnot) == true
                                    && FormFillerUtil.getAnnotFieldType(mForm, preAnnot) != FormField.e_formFieldPushButton
                                    && FormFillerUtil.getAnnotFieldType(mForm, preAnnot) != FormField.e_formFieldSignature) {
                                isFind = true;
                                mHandler.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        try {
                                            UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
                                            if (FormFillerUtil.getAnnotFieldType(mForm, preAnnot) == FormField.e_formFieldComboBox) {
                                                RectF rect = preAnnot.getRect();
                                                rect.left += 5;
                                                rect.top -= 5;
                                                mLastTouchPoint.set(rect.left, rect.top);
                                            }
                                            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                                            if (preAnnot != null) {
                                                if (uiExtensionsManager.getCurrentToolHandler() != null)
                                                    uiExtensionsManager.setCurrentToolHandler(null);
                                                RectF rect = preAnnot.getRect();

                                                if (mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, prePageIdx)) {
                                                    float devX = rect.left - (mPdfViewCtrl.getWidth() - rect.width()) / 2;
                                                    float devY = rect.top - (mPdfViewCtrl.getHeight() - rect.height()) / 2;
                                                    mPdfViewCtrl.gotoPage(prePageIdx, devX, devY);
                                                } else {
                                                    mPdfViewCtrl.gotoPage(prePageIdx, new PointF(rect.left, rect.top));
                                                }
                                                if (uiExtensionsManager.getCurrentToolHandler() != null) {
                                                    uiExtensionsManager.setCurrentToolHandler(null);
                                                }
                                                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(preAnnot);
                                                if (preAnnot != null && preAnnot instanceof FormControl) {
                                                    try {
//                                                        mIsNeedRefresh = true;
                                                        mFormFiller.setFocus((FormControl) preAnnot);
                                                    } catch (PDFException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                        } catch (PDFException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });

                                break;
                            } else {
                                preAnnotIdx--;
                            }
                        }
                        mCountDownLatch.countDown();


                        try {
                            if (mCountDownLatch.getCount() > 0)
                                mCountDownLatch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (isFind) break;
                        prePageIdx--;
                        if (prePageIdx < 0) {
                            prePageIdx = mPdfViewCtrl.getDoc().getPageCount() - 1;
                            isDocFinish = true;
                        }
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    };

    private void refreshField(FormField field) {
        int nPageCount = mPdfViewCtrl.getPageCount();
        for (int i = 0; i < nPageCount; i++) {
            if (!mPdfViewCtrl.isPageVisible(i))
                continue;
            RectF rectF = getRefreshRect(field, i);
            if (rectF == null)
                continue;
            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, i);
            mPdfViewCtrl.refresh(i, AppDmUtil.rectFToRect(rectF));
        }
    }

    private RectF getRefreshRect(FormField field, int pageIndex) {
        RectF rectF = null;
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            int nControlCount = field.getControlCount(page);
            for (int i = 0; i < nControlCount; i++) {
                FormControl formControl = field.getControl(page, i);
                if (rectF == null) {
                    rectF = new RectF(formControl.getRect());
                } else {
                    rectF.union(formControl.getRect());
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return rectF;
    }

    private Runnable nextNavigation = new Runnable() {

        @Override
        public void run() {
            Annot curAnnot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
            try {
                if (curAnnot != null && curAnnot instanceof FormControl) {
                    refreshField(((FormControl) curAnnot).getField());
                    curPage = curAnnot.getPage();

                    final int curPageIdx = curPage.getIndex();
                    nextPageIdx = curPageIdx;
                    final int curAnnotIdx = curAnnot.getIndex();
                    nextAnnotIdx = curAnnotIdx;
                    isFind = false;
                    isDocFinish = false;

                    while (nextPageIdx < mPdfViewCtrl.getDoc().getPageCount()) {

                        mCountDownLatch = new CountDownLatch(1);
                        curPage = mPdfViewCtrl.getDoc().getPage(nextPageIdx);
                        if (nextPageIdx == curPageIdx && isDocFinish == false) {
                            nextAnnotIdx = curAnnotIdx + 1;
                        } else {
                            nextAnnotIdx = 0;
                        }

                        while (curPage != null && nextAnnotIdx < curPage.getAnnotCount()) {
                            final Annot nextAnnot = curPage.getAnnot(nextAnnotIdx);
                            if (nextAnnot != null
                                    && nextAnnot instanceof FormControl
                                    && FormFillerUtil.isReadOnly(nextAnnot) == false
                                    && FormFillerUtil.isVisible(nextAnnot) == true
                                    && FormFillerUtil.getAnnotFieldType(mForm, nextAnnot) != FormField.e_formFieldPushButton
                                    && FormFillerUtil.getAnnotFieldType(mForm, nextAnnot) != FormField.e_formFieldSignature) {
                                isFind = true;

                                mHandler.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        try {
                                            UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
                                            if (FormFillerUtil.getAnnotFieldType(mForm, nextAnnot) == FormField.e_formFieldComboBox) {
                                                RectF rect = nextAnnot.getRect();
                                                rect.left += 5;
                                                rect.top -= 5;
                                                mLastTouchPoint.set(rect.left, rect.top);
                                            }
                                            DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                                            if (nextAnnot != null) {
                                                if (uiExtensionsManager.getCurrentToolHandler() != null)
                                                    uiExtensionsManager.setCurrentToolHandler(null);
                                                RectF rect = nextAnnot.getRect();

                                                if (mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, nextPageIdx)) {
                                                    float devX = rect.left - (mPdfViewCtrl.getWidth() - rect.width()) / 2;
                                                    float devY = rect.top - (mPdfViewCtrl.getHeight() - rect.height()) / 2;
                                                    mPdfViewCtrl.gotoPage(nextPageIdx, devX, devY);
                                                } else {
                                                    mPdfViewCtrl.gotoPage(nextPageIdx, new PointF(rect.left, rect.top));
                                                }
                                                if (uiExtensionsManager.getCurrentToolHandler() != null) {
                                                    uiExtensionsManager.setCurrentToolHandler(null);
                                                }
                                                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(nextAnnot);
                                                if (nextAnnot != null && nextAnnot instanceof FormControl) {
//                                                    mIsNeedRefresh = true;
                                                    mFormFiller.setFocus((FormControl) nextAnnot);
                                                }
                                            }
                                        } catch (PDFException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                });

                                break;
                            } else {
                                nextAnnotIdx++;
                            }
                        }
                        mCountDownLatch.countDown();


                        try {
                            if (mCountDownLatch.getCount() > 0)
                                mCountDownLatch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (isFind) break;
                        nextPageIdx++;
                        if (nextPageIdx >= mPdfViewCtrl.getDoc().getPageCount()) {
                            nextPageIdx = 0;
                            isDocFinish = true;
                        }
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    };

    protected void clear() {
        if (mAssist != null) {
            mAssist.bWillClose = true;
        }

        if (mFormFiller != null) {
            try {
                mFormFiller.release();
                mFormFiller = null;
            } catch (PDFException e) {
            }
        }
    }

    public FormFillerAssistImpl getFormFillerAssist() {
        return mAssist;
    }

    @Override
    public int getType() {
        return Annot.e_annotWidget;
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

        try {
            RectF r = annot.getRect();
            RectF rf = new RectF(r.left, r.top, r.right, r.bottom);
            PointF p = new PointF(point.x, point.y);
            int pageIndex = annot.getPage().getIndex();
            FormControl control = AppAnnotUtil.getControlAtPos(annot.getPage(), p, 1);

            mPdfViewCtrl.convertPdfRectToPageViewRect(rf, rf, pageIndex);
            mPdfViewCtrl.convertPdfPtToPageViewPt(p, p, pageIndex);

            if (rf.contains(p.x, p.y)) {
                return true;
            } else {
                if (AppAnnotUtil.isSameAnnot(annot, control))
                    return true;
                return false;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return true;
    }


    public void onBackspaceBtnDown() {
        try {
//            mIsNeedRefresh = true;
            mFormFiller.input((char) 8);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean needInvalid) {
        if (shouldShowInputSoft(annot)) {
            mIsShowEditText = true;
            mAdjustPosition = true;
            mLastInputText = " ";

            if (mEditView != null) {
                mParent.removeView(mEditView);
            }
            mEditView = new EditText(mContext);
            mEditView.setLayoutParams(new LayoutParams(1, 1));
            mEditView.setSingleLine(false);
            mEditView.setText(" ");
            mEditView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        new Thread(nextNavigation).start();
                        return true;
                    }
                    return false;
                }
            });

            mParent.addView(mEditView);
            AppUtil.showSoftInput(mEditView);

            mEditView.setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                        onBackspaceBtnDown();
                        mIsBackBtnPush = true;
                    }
                    return false;
                }
            });

            mEditView.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        if (s.length() >= mLastInputText.length()) {
                            String afterchange = s.subSequence(start, start + before).toString();
                            if (mChangeText.equals(afterchange)) {
                                for (int i = 0; i < s.length() - mLastInputText.length(); i++) {
                                    char c = s.charAt(mLastInputText.length() + i);
                                    if (FormFillerUtil.isEmojiCharacter((int) c))
                                        break;
                                    if ((int) c == 10)
                                        c = 13;
                                    final char value = c;

//                                    mIsNeedRefresh = true;
                                    mFormFiller.input(value);
//                                    mLastInputInvalidateTime = System.currentTimeMillis();
                                }
                            } else {
                                for (int i = 0; i < before; i++) {
                                    onBackspaceBtnDown();
                                }
                                for (int i = 0; i < count; i++) {
                                    char c = s.charAt(s.length() - count + i);

                                    if (FormFillerUtil.isEmojiCharacter((int) c))
                                        break;
                                    if ((int) c == 10)
                                        c = 13;
                                    final char value = c;

//                                    mIsNeedRefresh = true;
                                    mFormFiller.input(value);
//                                    mLastInputInvalidateTime = System.currentTimeMillis();
                                }
                            }
                        } else if (s.length() < mLastInputText.length()) {

                            if (mIsBackBtnPush == false)
                                onBackspaceBtnDown();
                            mIsBackBtnPush = false;
                        }

                        if (s.toString().length() == 0)
                            mLastInputText = " ";
                        else
                            mLastInputText = s.toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count,
                                              int after) {
                    mChangeText = s.subSequence(start, start + count).toString();
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.toString().length() == 0)
                        s.append(" ");
                }
            });
            if (mBlink == null) {
                mBlink = new Blink(annot);
                mBlink.postDelayed((Runnable) mBlink, 300);
            } else {
                mBlink.setAnnot(annot);
            }
        }

        int fieldType = FormFillerUtil.getAnnotFieldType(mForm, annot);

        if (mFNModule != null) {
            if (!FormFillerUtil.isReadOnly(annot))
                mFNModule.setClearEnable(true);
            else
                mFNModule.setClearEnable(false);
            if (fieldType != FormField.e_formFieldPushButton)
                mFNModule.getLayout().setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAnnotDeselected(final Annot annot, boolean needInvalid) {
//        mIsNeedRefresh = true;
        postDismissNavigation();
        try {
            mFormFiller.setFocus(null);
            if (annot != null && annot instanceof FormControl)
                refreshField(((FormControl) annot).getField());
        } catch (PDFException e) {
            e.printStackTrace();
        }
        if (mIsShowEditText) {
            AppUtil.dismissInputSoft(mEditView);
            mParent.removeView(mEditView);
            mIsShowEditText = false;
        }
    }

    private boolean isDown = false;
    PointF oldPoint = new PointF();

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
//        mIsNeedRefresh = false;

        try {
            if (!DocumentManager.getInstance(mPdfViewCtrl).canFillForm()) return false;
            if (FormFillerUtil.isReadOnly(annot))
                return false;
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);

            final RectF annotRectF = annot.getRect();
            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);

            PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
            PointF pageViewPt = new PointF();
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, pageViewPt, pageIndex);
            PointF pdfPointF = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, pdfPointF, pageIndex);


            int action = motionEvent.getActionMasked();
            switch (action) {

                case MotionEvent.ACTION_DOWN:

                    if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() && pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pdfPointF)) {
                        isDown = true;
                        mFormFiller.touchDown(page, pdfPointF);
//                        refresh(pageIndex);

                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (getDistanceOfPoints(pageViewPt, oldPoint) > 0 && annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() && pageIndex == annot.getPage().getIndex()) {
                        oldPoint.set(pageViewPt);
                        mFormFiller.touchMove(page, pdfPointF);
//                        refresh(pageIndex);
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:

                    if (pageIndex == annot.getPage().getIndex() && (isHitAnnot(annot, pdfPointF) || isDown)) {
                        isDown = false;

                        mFormFiller.touchUp(page, pdfPointF);
//                        refresh(pageIndex);
                        return true;
                    }
                    return false;

            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;

    }

//    private void refresh(int pageIndex) {
//        RectF r = new RectF(mAssist.getInvalidateRect());
//        mAssist.resetInvalidateRect();
//        r.inset(-5, -5);
//        mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(r));
//    }

    private double getDistanceOfPoints(PointF p1, PointF p2) {
        return Math.sqrt(Math.abs((p1.x - p2.x)
                * (p1.x - p2.x) + (p1.y - p2.y)
                * (p1.y - p2.y)));
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        if (!DocumentManager.getInstance(mPdfViewCtrl).canFillForm()) return false;
        if (FormFillerUtil.isReadOnly(annot))
            return false;
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        mLastTouchPoint.set(0, 0);
        boolean ret = false;
        PDFPage page = null;
        if (!DocumentManager.getInstance(mPdfViewCtrl).canFillForm()) return false;
        if (FormFillerUtil.isReadOnly(annot))
            return false;
        try {
            PointF docViewerPt = new PointF(motionEvent.getX(), motionEvent.getY());

            PointF point = new PointF();
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(docViewerPt, point, pageIndex);
            PointF pageViewPt = new PointF(point.x, point.y);
            final PointF pdfPointF = new PointF();
            mPdfViewCtrl.convertPageViewPtToPdfPt(pageViewPt, pdfPointF, pageIndex);
            page = mPdfViewCtrl.getDoc().getPage(pageIndex);

            Annot annotTmp = page.getAnnotAtPos(pdfPointF, 1);

            boolean isHit = isHitAnnot(annot, pdfPointF);

            if (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHit) {
                    ret = true;
                } else {
                    if (shouldShowNavigation(annot)) {
                        if (mBlink != null)
                            mBlink.removeCallbacks((Runnable) mBlink);
                        mBlink = null;
                        if (mFNModule != null) {
                            mFNModule.getLayout().setVisibility(View.INVISIBLE);
                            mFNModule.getLayout().setPadding(0, 0, 0, 0);
                        }
                        resetDocViewerOffset();
                    }
                    DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                    ret = false;
                }
            } else {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(annot);
                ret = true;
            }

            final PDFPage finalPage = page;
            if (annotTmp == null || (annot == DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot()
                    && pageIndex == annot.getPage().getIndex() && isHit)) {
                mFormFiller.click(finalPage, pdfPointF);
//                refresh(pageIndex);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private static PointF getPageViewOrigin(PDFViewCtrl pdfViewCtrl, int pageIndex, float x, float y) {
        PointF pagePt = new PointF(x, y);
        pdfViewCtrl.convertPageViewPtToDisplayViewPt(pagePt, pagePt, pageIndex);
        RectF rect = new RectF(0, 0, pagePt.x, pagePt.y);
        pdfViewCtrl.convertDisplayViewRectToPageViewRect(rect, rect, pageIndex);
        PointF originPt = new PointF(x - rect.width(), y - rect.height());
        return originPt;
    }

    private int getKeyboardHeight() {
        if (mPdfViewCtrl.getUIExtensionsManager() == null) {
            return 0;
        }
        Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) {
            return 0;
        }
        Rect r = new Rect();

        mParent.getWindowVisibleDisplayFrame(r);
        DisplayMetrics metric = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metric);
        int screenHeight = metric.heightPixels;
        return screenHeight - (r.bottom - r.top);

    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        if (annot == null || !(annot instanceof FormControl))
            return;
        if (ToolUtil.getCurrentAnnotHandler((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()) != this)
            return;
        try {
            RectF rect = annot.getRect();

            PointF viewpoint = new PointF(rect.left, rect.bottom);
            PointF point = new PointF(rect.left, rect.bottom);
            mPdfViewCtrl.convertPdfPtToPageViewPt(viewpoint, viewpoint, pageIndex);
            mPdfViewCtrl.convertPdfPtToPageViewPt(point, point, pageIndex);
            mPdfViewCtrl.convertPageViewPtToDisplayViewPt(viewpoint, viewpoint, pageIndex);
            int type = FormFillerUtil.getAnnotFieldType(mForm, annot);

            if ((type == FormField.e_formFieldTextField) ||
                    (type == FormField.e_formFieldComboBox && (((FormControl) annot).getField().getFlags() & FormField.e_formFieldFlagComboEdit) != 0)) {
                if (mAdjustPosition && getKeyboardHeight() > AppDisplay.getInstance(mContext).getRawScreenHeight() / 5) {
                    if (AppDisplay.getInstance(mContext).getRawScreenHeight() - viewpoint.y < (getKeyboardHeight() + AppDisplay.getInstance(mContext).dp2px(116))) {
                        int keyboardHeight = getKeyboardHeight();
                        int rawScreenHeight = AppDisplay.getInstance(mContext).getRawScreenHeight();
                        mPageOffset = (int) (keyboardHeight - (rawScreenHeight - viewpoint.y));

                        if (mPageOffset != 0 && pageIndex == mPdfViewCtrl.getPageCount() - 1
                                || mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE) {

                            PointF point1 = new PointF(0, mPdfViewCtrl.getPageViewHeight(pageIndex));
                            mPdfViewCtrl.convertPageViewPtToDisplayViewPt(point1, point1, pageIndex);
                            float screenHeight = AppDisplay.getInstance(mContext).getScreenHeight();
                            if (point1.y <= screenHeight) {
                                int offset = mPageOffset + AppDisplay.getInstance(mContext).dp2px(116);
                                setBottomOffset(offset);
                            }
                        }
                        mPdfViewCtrl.gotoPage(pageIndex,
                                getPageViewOrigin(mPdfViewCtrl, pageIndex, point.x, point.y).x,
                                getPageViewOrigin(mPdfViewCtrl, pageIndex, point.x, point.y).y + mPageOffset + AppDisplay.getInstance(mContext).dp2px(116));
                        mAdjustPosition = false;
                    } else {
                        resetDocViewerOffset();
                    }
                }
            }

            if ((pageIndex != mPdfViewCtrl.getPageCount() - 1 && mPdfViewCtrl.getPageLayoutMode() != PDFViewCtrl.PAGELAYOUTMODE_SINGLE)) {
                resetDocViewerOffset();
            }
            if (getKeyboardHeight() < AppDisplay.getInstance(mContext).getRawScreenHeight() / 5
                    && (pageIndex == mPdfViewCtrl.getPageCount() - 1 || mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE)) {
                resetDocViewerOffset();
            }
            DocumentManager documentManager = DocumentManager.getInstance(mPdfViewCtrl);
            Annot currentAnnot = documentManager.getCurrentAnnot();
            int fieldType = FormFillerUtil.getAnnotFieldType(mForm, currentAnnot);
            if (mFNModule != null) {
                if (currentAnnot != null && currentAnnot instanceof FormControl && fieldType != FormField.e_formFieldPushButton) {
                    if ((fieldType == FormField.e_formFieldTextField ||
                            (fieldType == FormField.e_formFieldComboBox && (((FormControl) annot).getField().getFlags() & FormField.e_formFieldFlagComboEdit) != 0))) {
                        int paddingBottom = 0;

                        paddingBottom = getKeyboardHeight();
                        if (Build.VERSION.SDK_INT < 14 && getKeyboardHeight() < AppDisplay.getInstance(mContext).getRawScreenHeight() / 5) {
                            paddingBottom = 0;
                        }
                        mFNModule.getLayout().setPadding(0, 0, 0, paddingBottom);

                    } else {
                        mFNModule.getLayout().setPadding(0, 0, 0, 0);
                    }
                }
                if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() == null) {
                    mFNModule.getLayout().setVisibility(View.INVISIBLE);
                }
            }
            canvas.save();
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            if (annot.getPage().getIndex() == pageIndex && fieldType != FormField.e_formFieldPushButton) {
                RectF bbox = annot.getRect();
                mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
                bbox.sort();
                bbox.inset(-5, -5);

                canvas.drawLine(bbox.left, bbox.top, bbox.left, bbox.bottom, mPathPaint);
                canvas.drawLine(bbox.left, bbox.bottom, bbox.right, bbox.bottom, mPathPaint);
                canvas.drawLine(bbox.right, bbox.bottom, bbox.right, bbox.top, mPathPaint);
                canvas.drawLine(bbox.left, bbox.top, bbox.right, bbox.top, mPathPaint);
            }
            canvas.restore();
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent contentSupplier, boolean addUndo,
                         Event.Callback result) {
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {

    }


    private boolean shouldShowInputSoft(Annot annot) {
        if (annot == null) return false;
        if (!(annot instanceof FormControl)) return false;
        int type = FormFillerUtil.getAnnotFieldType(mForm, annot);
        try {
            if ((type == FormField.e_formFieldTextField) ||
                    (type == FormField.e_formFieldComboBox && (((FormControl) annot).getField().getFlags() & FormField.e_formFieldFlagComboEdit) != 0))
                return true;

        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void resetDocViewerOffset() {
        if (mPageOffset != 0) {
            mPageOffset = 0;
            setBottomOffset(0);
        }
    }

    private void setBottomOffset(int offset) {
        if (mOffset == -offset)
            return;
        mOffset = -offset;
        mPdfViewCtrl.layout(0, 0 + mOffset, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight() + mOffset);
    }

    private class Blink extends Handler implements Runnable {
        private Annot mAnnot;
        private int mHeight;

        public Blink(Annot annot) {
            mAnnot = annot;
        }

        public void setAnnot(Annot annot) {
            mAnnot = annot;
        }

        @Override
        public void run() {

            //TODO:navigation
            if (mFNModule != null) {
                int height = getKeyboardHeight();
                int value = AppDisplay.getInstance(mContext).getRawScreenHeight() / 5;
                if (height < value) {
                    mFNModule.getLayout().setPadding(0, 0, 0, 0);
                }
                if (mHeight != height) {
                    mFNModule.getLayout().setPadding(0, 0, 0, height);
                    mHeight = height;
                }
                postDelayed(Blink.this, 500);
            }
        }

    }

    protected boolean onKeyBack() {
        Annot curAnnot = DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot();
        try {
            if (curAnnot == null) return false;
            if (curAnnot.getType() != Annot.e_annotWidget) return false;
            FormField field = ((FormControl) curAnnot).getField();
            if (field != null && field.getType() != FormField.e_formFieldSignature &&
                    field.getType() != FormField.e_formFieldUnknownType) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                NavigationDismiss();
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return false;
    }
}
