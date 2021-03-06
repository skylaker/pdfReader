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

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;


public class SignatureSignListBar extends TopBarImpl {
    public SignatureSignListBar(Context context) {
        super(context);
        mRightSideInterval = (int) context.getResources().getDimension(R.dimen.ux_horz_right_margin_pad);
    }
}
