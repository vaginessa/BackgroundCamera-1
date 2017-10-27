package com.nepdeveloper.backgroundcamera.Utility;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.nepdeveloper.backgroundcamera.Utility.Log;

import com.nepdeveloper.backgroundcamera.Service.AudioRecorderService;
import com.nepdeveloper.backgroundcamera.Service.MyService;
import com.nepdeveloper.backgroundcamera.Service.VideoRecorderService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Util {
    public static void vibrate(Context context, long delay) {
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(delay, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //noinspection deprecation
                ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(delay);
            }
        }
    }

    public static void vibrate(Context context, long[] pattern, int repeat) {
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createWaveform(pattern, repeat));
            } else {
                //noinspection deprecation
                ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(pattern, repeat);
            }
        }
    }

    public static String getSeekTime(int msec) {
        int seconds = msec / 1000;
        int minutes = seconds / 60;
        int hours = minutes / 60;

        seconds %= 60;
        minutes %= 60;

        return (hours <= 0 ? "" : hours + ":") +
                minutes + ":" +
                seconds;
    }

    //returns date in nice format parsed from its name
    public static String getFileInfo(File file) {
        String fileName = file.getName();
        String parcelableDate;
        if (fileName.contains("(")) {
            parcelableDate = fileName.substring(3, fileName.lastIndexOf('('));
        } else {
            parcelableDate = fileName.substring(3, fileName.length() - 1);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH);
        try {
            return new SimpleDateFormat("EEE, MMM d, h:mm:ss a", Locale.ENGLISH).format(sdf.parse(parcelableDate));
        } catch (Exception ex) {
            return parcelableDate;
        }
    }

    public static int getPixels(Context context, int dp) {

        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        //noinspection deprecation
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void stopRecordingVideo(Context context) {
        Log.i("biky", "video recorder service stop called");
        context.stopService(new Intent(context, VideoRecorderService.class));
    }

    public static void stopRecordingAudio(Context context) {
        Log.i("biky", "audio recorder service stop called");
        context.stopService(new Intent(context, AudioRecorderService.class));
    }

    public static void stopMainService(Context context) {
        context.stopService(new Intent(context, MyService.class));
    }

    public static Snackbar prepareSnackBar(View view, String msg) {
        Snackbar snackbar;
        snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_LONG);
        View snackBarView = snackbar.getView();
        TextView textView = (TextView) snackBarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        return snackbar;
    }
}

