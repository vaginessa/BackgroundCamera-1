package com.nepdeveloper.backgroundcamera.utility;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.nepdeveloper.backgroundcamera.service.AudioRecorderService;
import com.nepdeveloper.backgroundcamera.service.ImageCaptureService;
import com.nepdeveloper.backgroundcamera.service.MyService;
import com.nepdeveloper.backgroundcamera.service.VideoRecorderService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Util {
    public static void vibrate(Context context, long delay) {
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vibrator != null) {
                    vibrator.vibrate(VibrationEffect.createOneShot(delay, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            } else {
                if (vibrator != null) {
                    //noinspection deprecation
                    vibrator.vibrate(delay);
                }
            }
        }
    }

    public static void vibrate(Context context, long[] pattern, int repeat) {
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vibrator != null) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat));
                }
            } else {
                if (vibrator != null) {
                    //noinspection deprecation
                    vibrator.vibrate(pattern, repeat);
                }
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
        if (manager != null) {
            //noinspection deprecation
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
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

    public static void stopCapturingImage(Context context) {
        Log.i("biky", "image capture service stop called");
        context.stopService(new Intent(context, ImageCaptureService.class));
    }

    public static void stopMainService(Context context) {
        context.stopService(new Intent(context, MyService.class));
    }

    public static Snackbar prepareSnackBar(View view, String msg) {
        Snackbar snackbar;
        snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_LONG);
        View snackBarView = snackbar.getView();
        TextView textView = snackBarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        return snackbar;
    }

    public static boolean permissionIsGranted(Context context, String flag) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        String task;
        if (Constant.CAPTURE_PHOTO.equals(flag)) {
            task = "Capture";
        } else {
            task = "Recording";
        }
        //noinspection deprecation
        if (Constant.RECORD_VIDEO.equals(flag) || Constant.CAPTURE_PHOTO.equals(flag)) {
            if (Camera.getNumberOfCameras() <= 0) {
                Log.i("biky", task + " Failed. No camera in this device");
                NewMessageNotification.notify(context, task + " Failed. Your device doesn't have a camera", NewMessageNotification.ERROR);
                return false;
            }
            if (!Settings.canDrawOverlays(context)) {
                Log.i("biky", task + " Failed. Allow this app permission to draw over apps");
                NewMessageNotification.notify(context, task + " Failed. Allow this app permission to draw over apps", NewMessageNotification.PERMISSION_DENIED);
                return false;
            }

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.i("biky", task + " Failed. You have not permitted this app to use camera");
                NewMessageNotification.notify(context, task + " Failed. You have not permitted this app to use camera", NewMessageNotification.PERMISSION_DENIED);
                return false;
            }
        }

        if (!Constant.CAPTURE_PHOTO.equals(flag) && ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.i("biky", task + " Failed. You have not permitted this app to record audio");
            NewMessageNotification.notify(context, task + " Failed. You have not permitted this app to record audio", NewMessageNotification.ERROR);
            return false;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("biky", task + " Failed. Allow this app write permission");
            NewMessageNotification.notify(context, task + " Failed. Allow this app write permission", NewMessageNotification.PERMISSION_DENIED);
            return false;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("biky", task + " Failed. Allow this app read storage permission");
            NewMessageNotification.notify(context, task + " Failed. Allow this app read storage permission", NewMessageNotification.PERMISSION_DENIED);
            return false;
        }
        return true;
    }

}

