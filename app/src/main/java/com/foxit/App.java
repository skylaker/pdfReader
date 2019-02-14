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
package com.foxit;

import android.content.Context;

import com.foxit.sdk.common.Library;
import com.foxit.sdk.common.PDFError;
import com.foxit.sdk.common.PDFException;
import com.foxit.uiextensions.home.local.LocalModule;
import com.foxit.uiextensions.utils.UIToast;

public class App {
    static {
        System.loadLibrary("rdk");
    }

    private static String sn = "XqKhhztYxS8P56M6hH9KjZyIEUcjMW4MxpFJO8mZaCEAUBnD6LMTjA==";
    private static String key = "ezJvj1/GvGh39zvoP2Xsb3l6zn2JOVSnX07nABUcENPzCkt3zCVq70nWC+6ly9m7lXESBztFG/Qv9WGVaRUWjDZe1IhD5hKC2jhe+k2aTDPLR5eaPWiEaOtMO9OyuYIIGD5IBBlbeum2H0i3esvrcwvw7X3ti+XlQ1DwJS/A1BlVIdkbN/s+3M6SEXFXSunQKkwBc56+4T9VdIpQpda2GpKluwAaU+QuEwbbCImErZ9OptXkZjEy6Fm5YnlBvgXyAQLVEfsVZFU7FKTSATecXP4YK2ciDxN38oaM0WW/T+tstcKv1dVQqQCr5r+eGYKyZfMGAGWAPxRiHtkTaMHkMwZxlMiviN5A3TcJ4uxCVN9W/ou1kYYk9pUTh2iG2Hl0mujgN09MzGzwWu7Kdp7jxH8Obg0MP1jdvTQlK4EXotD00beGHuQIzcaPYYBT/rZ9ooKGebZxACWfgaueEGfqmDUHJRfKOShvS7BEgp09Ra5whQAC8Imy+rHqzV0uD8VYXX19xBeY/a+pT4Ia3z/7SUb4sMwl6fryRPlboC41r/UZMcVRD1alJBjgd208YEwZ3nsD9EtXqHPujI0P1/qL65GNMoeXyYVE5bjnOoqfh9i+9FK14c64l270JnXd+c1uOi+GTckWxbFRkHIu/wRuXXDHDk9WJP3M0OZiVxegIpxrSwiu53Sdm8LhFPNpdq+ikAew3H1qA+gXklNawIJOEgUuuFJH2eV5ttlE8B3cmwa2EYNkezwEbUdBGmi454FHQMI7TEnKXsXHb1eLEzjfbvLqkc48T4KG0RKe5QUah2Eni9r+GdKwv8dGa5kp3AQzaDdKgONvzI+M3SCQfNXUTWTDJf4h/zAU+lQ5dkhMaV48BZK+49FpU5K+5y8rAQoiLbeElzHOPDic7uro90Fwk5TJ4+NR84zWFaMwRQPbP/XgXyhPJF/1lOQhcfU/YiRWDR6ihYrT6oXXaaleugC9TXw4bQ4ANQlEucIpqXus5E6dQrkczuIMMff//iQrRNkyfQn0cfUgErIaQxBuqXavQ+O5EyrIN/PKg43/bGDg39HgWQdMK7+5P+Y3QiErgE+dw8/TTGD96KR/tjnhjcp8bQqjFAUuheHELFq5TwI4lujALDyEAMNO/txPdFvPIl5eTuBFyF3uxAKeOsdPysI69I+DloQGmlmMbxHvA3lC/hggRD90Woj64WMXRUG8yy61HwT3+cu/DWo2AcLrLENV";

    private Context mContext;
    private int errCode = PDFError.NO_ERROR.getCode();
    private static App INSTANCE = new App();
    public static App instance() {
        return INSTANCE;
    }

    private App(){
        try {
            Library.init(sn, key);
        } catch (PDFException e) {
            errCode = e.getLastError();
        }
    }

    public boolean checkLicense(){
        switch (PDFError.valueOf(errCode)) {
            case NO_ERROR:
                break ;
            case LICENSE_INVALID:
                UIToast.getInstance(mContext).show("The License is invalid!");
                return false;
            default:
                UIToast.getInstance(mContext).show("Failed to initialize the library!");
                return false;
        }
        return true;
    }

    public void setApplicationContext(Context context) {
        mContext = context;
    }

    public Context getApplicationContext() {
        return mContext;
    }

    LocalModule mLocalModule = null;
    public LocalModule getLocalModule() {
        if (mLocalModule == null) {
            mLocalModule = new LocalModule(mContext);
            mLocalModule.loadModule();
        }
        return mLocalModule;
    }

    public void onDestroy() {
        if (mLocalModule != null) {
            mLocalModule.unloadModule();
            mLocalModule = null;
        }
    }
}
