package com.evcam;

import android.app.Application;
import android.content.Context;
import androidx.multidex.MultiDex;

public class EVCamApplication extends Application {
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // 安装 Multidex 支持（Android 5.0 以下必需）
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 应用初始化逻辑
    }
}
