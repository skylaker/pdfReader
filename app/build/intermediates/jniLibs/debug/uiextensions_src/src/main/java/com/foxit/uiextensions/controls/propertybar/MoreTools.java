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
package com.foxit.uiextensions.controls.propertybar;

import android.graphics.RectF;
import android.view.View;

public interface MoreTools {
    interface IMT_MoreClickListener {
        void onMTClick(int type);

        int getType();
    }

    interface IMT_DismissListener {
        void onMTDismiss();
    }

    public static final int MT_TYPE_HIGHLIGHT = 1;
    public static final int MT_TYPE_ANNOTTEXT = 2;
    public static final int MT_TYPE_STRIKEOUT = 3;
    public static final int MT_TYPE_SQUIGGLY = 4;
    public static final int MT_TYPE_UNDERLINE = 5;
    public static final int MT_TYPE_CIRCLE = 6;
    public static final int MT_TYPE_SQUARE = 7;
    public static final int MT_TYPE_TYPEWRITER = 8;
    public static final int MT_TYPE_STAMP = 9;
    public static final int MT_TYPE_INSERTTEXT = 10;
    public static final int MT_TYPE_REPLACE = 11;
    public static final int MT_TYPE_ERASER = 12;
    public static final int MT_TYPE_INK = 13;
    public static final int MT_TYPE_LINE = 14;
    public static final int MT_TYPE_ARROW = 15;
    public static final int MT_TYPE_FILEATTACHMENT = 16;

    public boolean isShowing();

    public void show(RectF rectF, boolean showMask);

    public void update(RectF rectF);

    public void dismiss();

    public View getContentView();

    public void setButtonEnable(int buttonType, boolean enable);

    public void registerListener(IMT_MoreClickListener listener);

    public void unRegisterListener(IMT_MoreClickListener listener);

    public void setMTDismissListener(IMT_DismissListener dismissListener);
}
