package com.example.haze;

import android.app.Application;

import com.avos.avoscloud.AVOSCloud;

public class MyLeanCloudApp extends Application{

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化参数依次为 this, AppId, AppKey
        AVOSCloud.initialize(this,"O5vAdJP2oEStLVKzrm7jKK68-gzGzoHsz","DcyztGAXHWKtvEGTo7nuwcCo");
    }
}
