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
package com.foxit.uiextensions.textselect;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.CommonDefines;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.common.PDFException;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.PDFTextSelect;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.note.NoteAnnotContent;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.modules.signature.SignatureModule;
import com.foxit.uiextensions.modules.signature.SignatureToolHandler;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.ToolUtil;

import java.util.ArrayList;

public class BlankSelectToolHandler implements ToolHandler {

    private PDFViewCtrl mPdfViewCtrl;
    private AnnotMenu mAnnotationMenu;
    private DocumentManager.AnnotEventListener mAnnotListener;

    private PointF mMenuPoint;
    private PointF mMenuPdfPoint;
    private RectF mMenuBox;
    private int mCurrentIndex;
    public boolean mIsMenuShow;
    public PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public BlankSelectToolHandler(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        this.mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
        mMenuPoint = null;
        mAnnotationMenu = new AnnotMenuImpl(context, mPdfViewCtrl);

        mAnnotListener = new DocumentManager.AnnotEventListener() {
            @Override
            public void onAnnotAdded(PDFPage page, Annot annot) {
            }

            @Override
            public void onAnnotDeleted(PDFPage page, Annot annot) {
            }

            @Override
            public void onAnnotModified(PDFPage page, Annot annot) {
            }

            @Override
            public void onAnnotChanged(Annot lastAnnot, Annot currentAnnot) {
                if (currentAnnot != null && mIsMenuShow == true) {
                    mIsMenuShow = false;
                    mAnnotationMenu.dismiss();
                }
            }
        };

        DocumentManager.getInstance(mPdfViewCtrl).registerAnnotEventListener(mAnnotListener);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerMenuEventListener(mMenuEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandlerChangedListener(mHandlerChangedListener);
        }
    }

    public void unload() {
        DocumentManager.getInstance(mPdfViewCtrl).unregisterAnnotEventListener(mAnnotListener);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerMenuEventListener(mMenuEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandlerChangedListener(mHandlerChangedListener);
        }
    }

    protected AnnotMenu getAnnotationMenu() {
        return mAnnotationMenu;
    }

    @Override
    public String getType() {
        return TH_TYPE_BLANKSELECT;
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
        if (!mPdfViewCtrl.isPageVisible(mCurrentIndex)) return;
        dismissMenu();
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        PointF pointF = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        try {
            if (DocumentManager.getInstance(mPdfViewCtrl).getCurrentAnnot() != null) {
                DocumentManager.getInstance(mPdfViewCtrl).setCurrentAnnot(null);
                return false;
            }

            TextSelectToolHandler toolHandler = (TextSelectToolHandler) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getToolHandlerByType(ToolHandler.TH_TYPE_TEXTSELECT);
            if (toolHandler != null && toolHandler.getAnnotationMenu().isShowing()) {
                toolHandler.getAnnotationMenu().dismiss();
                return false;
            }

            if (mIsMenuShow == true) {
                mIsMenuShow = false;
                mAnnotationMenu.dismiss();
                return true;
            }

            mCurrentIndex = pageIndex;
            PointF pointPdfView = new PointF(pointF.x, pointF.y);
            mPdfViewCtrl.convertPageViewPtToPdfPt(pointF, pointPdfView, mCurrentIndex);
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mCurrentIndex);
            if (page.isParsed() != true) {
                int ret = page.startParse(PDFPage.e_parsePageNormal, null, false);
                while (ret == CommonDefines.e_progressToBeContinued) {
                    ret = page.continueParse();
                }
            }
            PDFTextSelect textPage = PDFTextSelect.create(page);

            int index = textPage.getIndexAtPos(pointPdfView.x, pointPdfView.y, 30);
            if (index == -1 && (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot() || DocumentManager.getInstance(mPdfViewCtrl).canModifyContents())) {
                mIsMenuShow = true;
                mMenuPoint = new PointF(pointF.x, pointF.y);
                mMenuPdfPoint = new PointF(mMenuPoint.x, mMenuPoint.y);
                mPdfViewCtrl.convertPageViewPtToPdfPt(mMenuPdfPoint, mMenuPdfPoint, mCurrentIndex);

                mMenuBox = new RectF(pointF.x, pointF.y, pointF.x + 1, pointF.y + 1);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mMenuBox, mMenuBox, mCurrentIndex);

                mAnnotationMenu.setMenuItems(getBlankSelectItems());
                mAnnotationMenu.show(mMenuBox);
                mAnnotationMenu.setListener(new AnnotMenu.ClickListener() {
                    @Override
                    public void onAMClick(int btType) {
                        if (btType == AnnotMenu.AM_BT_NOTE) {
                            PDFPage pdfPage = null;
                            try {
                                pdfPage = mPdfViewCtrl.getDoc().getPage(mCurrentIndex);
                            } catch (PDFException e1) {
                                e1.printStackTrace();
                            }
                            if (pdfPage == null) return;
                            PointF p = new PointF(mMenuPdfPoint.x, mMenuPdfPoint.y);
                            DocumentManager.getInstance(mPdfViewCtrl).addAnnot(page, new TextAnnotContent(p, mCurrentIndex), true, null);
                        } else if (btType == AnnotMenu.AM_BT_SIGNATURE) {
                            UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
                            Module module = uiExtensionsManager.getModuleByName(Module.MODULE_NAME_PSISIGNATURE);
                            if (module != null) {
                                SignatureToolHandler toolHandler = (SignatureToolHandler) ((SignatureModule) module).getToolHandler();
                                uiExtensionsManager.setCurrentToolHandler(toolHandler);
                                PointF p = new PointF(mMenuPdfPoint.x, mMenuPdfPoint.y);
                                mPdfViewCtrl.convertPdfPtToPageViewPt(p, p, mCurrentIndex);
                                toolHandler.addSignature(mCurrentIndex, p, true);
                            }
                        }

                        mAnnotationMenu.dismiss();
                        mIsMenuShow = false;
                        mMenuPoint = null;
                    }
                });
                return true;
            }
        } catch (PDFException exception) {
            if (exception.getLastError() == PDFException.e_errOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        if (mIsMenuShow == true) {
            mIsMenuShow = false;
            mAnnotationMenu.dismiss();
            return true;
        }
        return false;
    }

    @Override
    public void onDraw(int i, Canvas canvas) {
        onDrawForAnnotMenu(canvas);
    }

    private ArrayList<Integer> getBlankSelectItems() {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        ArrayList<Integer> mListBlankSelectItems = new ArrayList<Integer>();

        if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()
                && ToolUtil.getAnnotHandlerByType(uiExtensionsManager, Annot.e_annotNote) != null) {

            mListBlankSelectItems.add(AnnotMenu.AM_BT_NOTE);
        }
        if (DocumentManager.getInstance(mPdfViewCtrl).canAddAnnot()
                && uiExtensionsManager.getModuleByName(Module.MODULE_NAME_PSISIGNATURE) != null) {

            mListBlankSelectItems.add(AnnotMenu.AM_BT_SIGNATURE);
        }
        return mListBlankSelectItems;
    }

    public void dismissMenu() {
        if (mIsMenuShow == true) {
            mIsMenuShow = false;
            mAnnotationMenu.dismiss();
        }
    }

    public void onDrawForAnnotMenu(Canvas canvas) {
        if (!mPdfViewCtrl.isPageVisible(mCurrentIndex)) {
            return;
        }

        if (mIsMenuShow == false) {
            return;
        }

        if (mMenuPoint != null) {
            PointF temp = new PointF(mMenuPdfPoint.x, mMenuPdfPoint.y);
            mPdfViewCtrl.convertPdfPtToPageViewPt(mMenuPdfPoint, temp, mCurrentIndex);
            RectF bboxRect = new RectF(temp.x, temp.y, temp.x + 1, temp.y + 1);

            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bboxRect, bboxRect, mCurrentIndex);
            mAnnotationMenu.update(bboxRect);
        }
    }

    private UIExtensionsManager.MenuEventListener mMenuEventListener = new UIExtensionsManager.MenuEventListener() {
        @Override
        public void onTriggerDismissMenu() {
            dismissMenu();
        }
    };

    private UIExtensionsManager.ToolHandlerChangedListener mHandlerChangedListener = new UIExtensionsManager.ToolHandlerChangedListener() {
        @Override
        public void onToolHandlerChanged(ToolHandler lastTool, ToolHandler currentTool) {
            if (currentTool != null && mIsMenuShow == true) {
                mAnnotationMenu.dismiss();
                mIsMenuShow = false;
            }
        }
    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (mIsMenuShow) {
                mIsMenuShow = false;
                mAnnotationMenu.dismiss();
                return true;
            }
        }
        return false;
    }
}

class TextAnnotContent implements NoteAnnotContent {
    private PointF p = new PointF();
    private int pageIndex;

    public TextAnnotContent(PointF p, int pageIndex) {
        this.p.set(p.x, p.y);
        this.pageIndex = pageIndex;
    }

    @Override
    public int getPageIndex() {
        return pageIndex;
    }

    @Override
    public int getType() {
        return Annot.e_annotNote;
    }

    @Override
    public String getNM() {
        return null;
    }

    @Override
    public RectF getBBox() {
        return new RectF(p.x, p.y, p.x, p.y);
    }

    @Override
    public int getColor() {
        return 0;
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public float getLineWidth() {
        return 0;
    }

    @Override
    public String getSubject() {
        return null;
    }

    @Override
    public DateTime getModifiedDate() {
        return null;
    }

    @Override
    public String getContents() {
        return null;
    }

    @Override
    public String getIntent() {
        return null;
    }

    @Override
    public String getIcon() {
        return "";
    }

    @Override
    public String getFromType() {
        return Module.MODULE_NAME_SELECTION;
    }

    @Override
    public String getParentNM() {
        return null;
    }
}
