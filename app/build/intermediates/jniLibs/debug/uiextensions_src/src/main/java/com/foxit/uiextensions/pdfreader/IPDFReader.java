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

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.DocumentManager;

public interface IPDFReader {
    public void init();

    public boolean registerLifecycleListener(ILifecycleEventListener listener);

    public boolean unregisterLifecycleListener(ILifecycleEventListener listener);

    public boolean registerStateChangeListener(IStateChangeListener listener);

    public boolean unregisterStateChangeListener(IStateChangeListener listener);

    IMainFrame getMainFrame();

    PDFViewCtrl.UIExtensionsManager getUIExtensionsManager();

    DocumentManager getDocMgr();

    PDFViewCtrl getDocViewer();

    public int getState();

    public void changeState(int state);

    public void backToPrevActivity();
}
