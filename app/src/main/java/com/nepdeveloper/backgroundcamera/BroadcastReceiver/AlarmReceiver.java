package com.nepdeveloper.backgroundcamera.BroadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.media.MediaPlayer;
import com.nepdeveloper.backgroundcamera.Utility.Log;
import android.widget.Toast;

import com.nepdeveloper.backgroundcamera.R;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("biky", "alarm..");
        Toast.makeText(context, "Alarm....", Toast.LENGTH_LONG).show();

        MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.silence01s);

        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }
    }

}