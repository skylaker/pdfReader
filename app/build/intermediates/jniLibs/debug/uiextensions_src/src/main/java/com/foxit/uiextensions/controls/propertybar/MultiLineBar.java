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

import android.view.View;

public interface MultiLineBar {
    public interface IML_ValueChangeListener {
        public void onValueChanged(int type, Object value);

        public void onDismiss();

        public int getType();
    }

    public static final int TYPE_LIGHT = 1;
    public static final int TYPE_DAYNIGHT = 2;
    public static final int TYPE_SYSLIGHT = 3;
    public static final int TYPE_SINGLEPAGE = 4;
    public static final int TYPE_THUMBNAIL = 5;
    public static final int TYPE_LOCKSCREEN = 6;
    public static final int TYPE_REFLOW = 7;
    public static final int TYPE_CROP = 8;

    public void setProperty(int property, Object value);

    public boolean isShowing();

    public void show();

    public void dismiss();

    public View getContentView();

    public void registerListener(IML_ValueChangeListener listener);

    public void unRegisterListener(IML_ValueChangeListener listener);
}
