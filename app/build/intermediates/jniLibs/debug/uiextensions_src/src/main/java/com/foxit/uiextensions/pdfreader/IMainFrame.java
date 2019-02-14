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
package com.foxit.uiextensions.pdfreader;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.widget.RelativeLayout;

import com.foxit.uiextensions.controls.propertybar.MoreTools;
import com.foxit.uiextensions.controls.propertybar.MultiLineBar;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.modules.panel.IPanelManager;

public interface IMainFrame {
    void onConfigurationChanged(Configuration newConfig);

    Activity getAttachedActivity();

    Context getContext();

    RelativeLayout getContentView();

    void showToolbars();

    void hideToolbars();

    boolean isToolbarsVisible();

    BaseBar getAnnotCustomTopBar();

    BaseBar getAnnotCustomBottomBar();

    BaseBar getTopToolbar();

    BaseBar getBottomToolbar();

//    PanelHost getPanel();
//
//    void showPanel();
//
//    void showPanel(int TabTag);
//
//    void hidePanel();

    IPanelManager getPanelManager();

    PropertyBar getPropertyBar();

    MultiLineBar getSettingBar();

    void hideSettingBar();

    BaseBar getEditBar();

    BaseBar getEditDoneBar();

    MoreTools getMoreToolsBar();

    BaseBar getToolSetBar();

    void showMaskView();

    void hideMaskView();

    boolean isMaskViewShowing();

    boolean isEditBarShowing();

    void enableTopToolbar(boolean isEnabled);
    void enableBottomToolbar(boolean isEnabled);
}
