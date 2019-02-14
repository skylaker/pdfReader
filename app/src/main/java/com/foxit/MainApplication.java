package com.foxit;

import android.app.Application;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        App.instance().setApplicationContext(this);
        if(!App.instance().checkLicense()) {
            return;
        }
    }
}
