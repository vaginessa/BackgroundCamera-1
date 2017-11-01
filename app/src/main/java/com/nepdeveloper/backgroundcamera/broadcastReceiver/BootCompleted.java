package com.nepdeveloper.backgroundcamera.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.nepdeveloper.backgroundcamera.service.MyService;
import com.nepdeveloper.backgroundcamera.utility.Constant;

public class BootCompleted extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (context.getSharedPreferences(Constant.PREFERENCE_NAME, Context.MODE_PRIVATE)
                    .getBoolean(Constant.SERVICE_ACTIVE, true)) {
                Intent serviceIntent = new Intent(context, MyService.class);
                context.startService(serviceIntent);
            }
        }
    }
}
