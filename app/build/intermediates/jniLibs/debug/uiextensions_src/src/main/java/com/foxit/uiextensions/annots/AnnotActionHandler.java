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
package com.foxit.uiextensions.annots;

import android.content.Context;
import android.widget.Toast;

import com.foxit.sdk.common.ActionHandler;
import com.foxit.sdk.common.IdentityProperties;

public class AnnotActionHandler extends ActionHandler{
    private Context mContext;

    public AnnotActionHandler(Context context) {
        this.mContext = context;
    }

    @Override
    public int alert(String msg, String title, int type, int icon) {
        int ret = 0;
        Toast.makeText(mContext, "alert...." + msg, Toast.LENGTH_SHORT).show();
        return ret;
    }

    @Override
    public IdentityProperties getIdentityProperties() {
        IdentityProperties identityProperties = new IdentityProperties();
        identityProperties.setName("Foxit");

        return identityProperties;
    }
}
