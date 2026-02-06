package com.kooo.evcam;

import android.app.Application;
import android.content.Context;
import androidx.multidex.MultiDex;

public class EVCamApplication extends Application {
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
