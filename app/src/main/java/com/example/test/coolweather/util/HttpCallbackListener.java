package com.example.test.coolweather.util;

/**
 * Created by Administrator on 2016/7/27.
 */
public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);
}
