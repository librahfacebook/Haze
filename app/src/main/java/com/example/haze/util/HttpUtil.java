package com.example.haze.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * 获取所有省市县的数据
 */
public class HttpUtil {
    public static void sendOkHttpRequest(String address,okhttp3.Callback callback){
        OkHttpClient client=new OkHttpClient();
        //传入请求地址
        Request request=new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }
}
