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
package com.foxit.uiextensions.modules.signature;

import android.content.Context;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;


public class SignatureMixListPopup {
    static void show(Context context, ViewGroup parent, final PDFViewCtrl pdfViewCtrl, SignatureFragment.SignatureInkCallback inkCallback) {
        final SignatureListPicker listPicker = new SignatureListPicker(context,parent, pdfViewCtrl, inkCallback);
        final Popup popup = new Popup(listPicker.getRootView());
        listPicker.init(new SignatureListPicker.ISignListPickerDismissCallback() {
            @Override
            public void onDismiss() {
                popup.dismiss();
            }
        });
        popup.showAtLocation(parent, Gravity.RIGHT, 0, 0);
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (true) {
                    if (listPicker.getBaseItemsSize() == 0) {
                        ((UIExtensionsManager)pdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                    }
                } else {
                    if (listPicker.getHandwritingItemsSize() == 0) {
                        ((UIExtensionsManager)pdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                    }
                }
                listPicker.dismiss();
            }
        });
    }

    static void show(Context context, ViewGroup parent, final PDFViewCtrl pdfViewCtrl, SignatureFragment.SignatureInkCallback inkCallback, final RectF menuRect) {
        final SignatureListPicker listPicker = new SignatureListPicker(context, parent, pdfViewCtrl, inkCallback);
        final Popup popup = new Popup(listPicker.getRootView());
        listPicker.init(new SignatureListPicker.ISignListPickerDismissCallback() {
            @Override
            public void onDismiss() {
                popup.dismiss();
            }
        });
        popup.showAtLocation(parent, Gravity.RIGHT, 0, 0);
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (true) {
                    if (listPicker.getBaseItemsSize() == 0) {
                        ((UIExtensionsManager)pdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                        pdfViewCtrl.invalidate();
                    }
                } else {
                    if (listPicker.getHandwritingItemsSize() == 0) {
                        ((UIExtensionsManager)pdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                    }
                }
                listPicker.dismiss();
                if (((UIExtensionsManager)pdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() instanceof SignatureToolHandler) {
//                    App.instance().getRead().getMainFrame().getAnnotMenu().show(menuRect);
                }
            }
        });
    }

    static class Popup extends PopupWindow {
        public Popup(View view) {
            super(view, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, true);
            setWindowLayoutMode(0, ViewGroup.LayoutParams.MATCH_PARENT);
            setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
            setBackgroundDrawable(new ColorDrawable(0xFFFFFFFF));
            setAnimationStyle(R.style.View_Animation_RtoL);
            setContentView(view);
            setFocusable(true);
            setOutsideTouchable(true);
        }
    }
}
