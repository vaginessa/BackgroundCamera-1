package com.nepdeveloper.backgroundcamera.Service;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import com.nepdeveloper.backgroundcamera.Utility.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.nepdeveloper.backgroundcamera.R;
import com.nepdeveloper.backgroundcamera.Utility.Constant;
import com.nepdeveloper.backgroundcamera.Utility.NewMessageNotification;
import com.nepdeveloper.backgroundcamera.Utility.Util;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VideoRecorderService extends Service implements SurfaceHolder.Callback {

    private WindowManager windowManager;
    private SurfaceView surfaceView;
    @SuppressWarnings("deprecation")
    private Camera camera = null;
    private MediaRecorder mediaRecorder = null;
    private SharedPreferences preferences;
    private File videoFile = null;
    private AudioManager audio;

    private boolean permissionIsGranted() {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        //noinspection deprecation
        if (Camera.getNumberOfCameras() <= 0) {
            Log.i("biky", "Recording Failed. No camera in this device");
            NewMessageNotification.notify(this, "Recording Failed. Your device doesn't have a camera", NewMessageNotification.ERROR);
            return false;
        }
        if (!Settings.canDrawOverlays(this)) {
            Log.i("biky", "Recording Failed. Allow this app permission to draw over apps");
            NewMessageNotification.notify(this, "Recording Failed. Allow this app permission to draw over apps", NewMessageNotification.PERMISSION_DENIED);
            return false;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.i("biky", "Recording Failed. You have not permitted this app to use camera");
            NewMessageNotification.notify(this, "Recording Failed. You have not permitted this app to use camera", NewMessageNotification.PERMISSION_DENIED);
            return false;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.i("biky", "Recording Failed. You have not permitted this app to record audio");
            NewMessageNotification.notify(this, "Recording Failed. You have not permitted this app to record audio", NewMessageNotification.ERROR);
            return false;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("biky", "Recording Failed. Allow this app write permission");
            NewMessageNotification.notify(this, "Recording Failed. Allow this app write permission", NewMessageNotification.PERMISSION_DENIED);
            return false;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("biky", "Recording Failed. Allow this app read storage permission");
            NewMessageNotification.notify(this, "Recording Failed. Allow this app read storage permission", NewMessageNotification.PERMISSION_DENIED);
            return false;
        }
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        preferences = getSharedPreferences(Constant.PREFERENCE_NAME, MODE_PRIVATE);

        if (!permissionIsGranted()) {
            Log.i("biky", "permission is not granted");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null && Constant.ACTION_STOP_SELF.equals(intent.getAction())) {
            Log.i("biky", "stopped from notification");
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            //noinspection deprecation
            camera = Camera.open();
        } catch (Exception e) {
            NewMessageNotification.notify(this, "Recording Failed. Other application might be using camera/mic", NewMessageNotification.ERROR);
            stopSelf();
            return START_NOT_STICKY;
        }

        Log.i("biky", "video recorder service created");
        // Create new SurfaceView, set its size to 1x1, move it to the top left corner and set this service as a callback
        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        surfaceView = new SurfaceView(this);

        WindowManager.LayoutParams layoutParams;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams = new WindowManager.LayoutParams(
                    1, 1,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
        } else {
            //noinspection deprecation
            layoutParams = new WindowManager.LayoutParams(
                    1, 1,
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
        }

        layoutParams.gravity = Gravity.START | Gravity.TOP;
        windowManager.addView(surfaceView, layoutParams);
        surfaceView.getHolder().addCallback(this);

        String text = "Recording video";
        Intent stopSelf = new Intent(this, VideoRecorderService.class);
        stopSelf.setAction(Constant.ACTION_STOP_SELF);
        PendingIntent pStopSelf = PendingIntent.getService(this, 0, stopSelf, 0);

        Notification notification;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setDefaults(0)
                    .setSound(null)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(text)
                            .setBigContentTitle(getString(R.string.app_name))
                            .setSummaryText(new java.text.SimpleDateFormat("h:mm:ss a", Locale.ENGLISH).format(new Date()))
                    )
                    .addAction(R.drawable.ic_stop, "Stop", pStopSelf)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            if (preferences.getBoolean(Constant.SHOW_NOTIFICATION, true)) {
                builder.setSmallIcon(R.drawable.ic_stat);
                builder.setTicker(text);
            }

            notification = builder.build();

            startForeground(Constant.NOTIFICATION_ID_VIDEO_RECORD, notification);
        } else {
            @SuppressWarnings("deprecation") Notification.Builder builder = new Notification.Builder(this, NewMessageNotification.CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(text)
                    .setStyle(new Notification.BigTextStyle()
                            .bigText(text)
                            .setBigContentTitle(getString(R.string.app_name))
                            .setSummaryText(new java.text.SimpleDateFormat("h:mm:ss a", Locale.ENGLISH).format(new Date()))
                    )
                    .addAction(R.drawable.ic_stop, "Stop", pStopSelf)
                    .setAutoCancel(true);

            if (preferences.getBoolean(Constant.SHOW_NOTIFICATION, true)) {
                builder.setSmallIcon(R.drawable.ic_stat);
                builder.setTicker(text);
            }

            notification = builder.build();

            startForeground(Constant.NOTIFICATION_ID_VIDEO_RECORD, notification);
        }
        return START_NOT_STICKY;
    }

    // Method called right after Surface created (initializing and starting MediaRecorder)
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            camera.setDisplayOrientation(90);
            camera.unlock();
        } catch (Exception e) {
            e.printStackTrace();
            NewMessageNotification.notify(this, "Recording Failed. Other application might be using camera/mic", NewMessageNotification.ERROR);
            stopSelf();
            return;
        }
        mediaRecorder = new MediaRecorder();
        try {

            mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
            //noinspection deprecation
            mediaRecorder.setCamera(camera);

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            switch (preferences.getString(Constant.RECORDING_QUALITY, Constant.QUALITY_MEDIUM)) {
                case Constant.QUALITY_HIGH:
                    mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
                    Log.i("biky", "quality high");
                    break;
                case Constant.QUALITY_MEDIUM:
                    if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
                        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
                        Log.i("biky", "quality 480p");
                    } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_CIF)) {
                        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_CIF));
                        Log.i("biky", "quality cif");
                    } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
                        Log.i("biky", "quality 720p");
                        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
                    } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_1080P)) {
                        Log.i("biky", "quality 1080p");
                        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_1080P));
                    } else {
                        Log.i("biky", "quality high");
                        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
                    }
                    break;
                case Constant.QUALITY_LOW:
                    mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
                    Log.i("biky", "quality low");
                    break;
                default:
                    mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
                    Log.i("biky", "quality high");
            }

            mediaRecorder.setOrientationHint(90);
        } catch (Exception e) {
            e.printStackTrace();
            NewMessageNotification.notify(this, "Recording Failed. Can't setup video recorder", NewMessageNotification.ERROR);
            stopSelf();
            return;
        }

        // File directory = new File(preferences.getString(Constant.STORAGE_LOCATION, Constant.FILE.getAbsolutePath()));
        File directory = Constant.FILE;

        if (!directory.exists() || !directory.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String videoFileName = Constant.FILE_PREFIX + timeStamp;

        videoFile = new File(directory, videoFileName + Constant.VIDEO_FILE_EXTENSION);

        int i = 1;
        while (videoFile.exists()) {
            videoFile = new File(directory, videoFileName + Constant.VIDEO_FILE_EXTENSION + "(" + i + ")");
            i++;
            //ensure it doesnot repeat forever
            if (i > 10000) {
                break;
            }
        }

        mediaRecorder.setOutputFile(videoFile.getPath() + ".temp");

        Log.i("biky", "media recorder path =  " + videoFile.getPath());

        final int ringerMode = audio.getRingerMode();

        try {
            mediaRecorder.prepare();
            if (!preferences.getBoolean(Constant.SHUTTER_SOUND, true)) {
                audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            }
            mediaRecorder.start();

            Util.vibrate(this, Constant.VIBRATE_PATTERN, -1);

            if (!preferences.getBoolean(Constant.SHUTTER_SOUND, true)) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Log.i("biky", "on complete " + e.getMessage());
                }
                if (audio != null) {
                    audio.setRingerMode(ringerMode);
                }
            }
            sendBroadcast(new Intent().
                    setAction(Constant.RECORDING_VIDEO)
            );
        } catch (Exception e) {
            videoFile = null;
            if (!preferences.getBoolean(Constant.SHUTTER_SOUND, true)) {
                audio.setRingerMode(ringerMode);
            }
            Log.i("biky", "media recorder failed to start " + e.getMessage());
            e.printStackTrace();
            NewMessageNotification.notify(this, "Recording Failed. Other application might be using camera/mic", NewMessageNotification.ERROR);
            stopSelf();
        }
    }

    // Stop recording and remove SurfaceView
    @Override
    public void onDestroy() {
        try {
            onComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("biky", "on destroy");
        stopForeground(true);
        super.onDestroy();
    }

    private void onComplete() throws Exception {
        int ringerMode = audio.getRingerMode();

        if (preferences != null && !preferences.getBoolean(Constant.SHUTTER_SOUND, true) && audio != null) {
            audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        }

        if (mediaRecorder != null) {
            try {
                mediaRecorder.reset();
                mediaRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (windowManager != null) {
            try {
                windowManager.removeView(surfaceView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (camera != null) {
            try {
                camera.lock();
                camera.release();
                Log.i("biky", "camera locked and released");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (videoFile != null) {
            File tempFile = new File(videoFile.getPath() + ".temp");
            if (tempFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                if (tempFile.renameTo(videoFile)) {
                    sendBroadcast(new Intent().
                            setAction(Constant.NEW_FILE_CREATED).
                            putExtra(Constant.FILE_PATH_NAME, videoFile.getAbsolutePath())
                    );
                }
                NewMessageNotification.notify(this, "Recording completed", NewMessageNotification.RECORDING_COMPLETE);
            }
        }

        if (preferences != null && !preferences.getBoolean(Constant.SHUTTER_SOUND, true) && audio != null) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                Log.i("biky", "on complete " + e.getMessage());
            }
        }

        if (preferences != null && !preferences.getBoolean(Constant.SHUTTER_SOUND, true) && audio != null) {
            audio.setRingerMode(ringerMode);
        }

        Log.i("biky", "media recorder reset and released");

        Util.vibrate(this, Constant.VIBRATE_PATTERN, -1);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}