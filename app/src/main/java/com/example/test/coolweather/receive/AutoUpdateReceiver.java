package com.example.test.coolweather.receive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Administrator on 2016/7/28.
 */
public class AutoUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context,AutoUpdateService.class);
        context.startService(i);
    }
}
