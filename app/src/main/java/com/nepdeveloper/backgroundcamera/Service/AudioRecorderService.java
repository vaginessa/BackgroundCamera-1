package com.nepdeveloper.backgroundcamera.Service;


import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import com.nepdeveloper.backgroundcamera.Utility.Log;

import com.nepdeveloper.backgroundcamera.R;
import com.nepdeveloper.backgroundcamera.Utility.Constant;
import com.nepdeveloper.backgroundcamera.Utility.NewMessageNotification;
import com.nepdeveloper.backgroundcamera.Utility.Util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AudioRecorderService extends Service {

    @SuppressWarnings("deprecation")
    private MediaRecorder mediaRecorder = null;
    private SharedPreferences preferences;
    private File audioFile = null;

    private boolean permissionIsGranted() {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.i("biky", "Recording Failed. You have not permitted this app to record audio");
            NewMessageNotification.notify(this, "Recording Failed. You have not permitted this app to record audio", NewMessageNotification.ERROR);
            return false;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("biky", "Recording Failed. Allow this app write storage permission");
            NewMessageNotification.notify(this, "Recording Failed. Allow this app write storage permission", NewMessageNotification.PERMISSION_DENIED);
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
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            NewMessageNotification.notify(this, "Sorry, your device doesn't have a microphone", NewMessageNotification.ERROR);
            stopSelf();
            return START_NOT_STICKY;
        }

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

        startRecording();

        Util.vibrate(this, Constant.VIBRATE_PATTERN, -1);

        String text = "Recording audio";
        Intent stopSelf = new Intent(this, AudioRecorderService.class);
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

            startForeground(Constant.NOTIFICATION_ID_AUDIO_RECORD, notification);
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

            startForeground(Constant.NOTIFICATION_ID_AUDIO_RECORD, notification);
        }
        return START_NOT_STICKY;
    }

    private void startRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

//        File directory = new File(preferences.getString(Constant.STORAGE_LOCATION, Constant.FILE.getAbsolutePath()));

        File directory = Constant.FILE;

        if (!directory.exists() || !directory.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String audioFileName = Constant.FILE_PREFIX + timeStamp;

        audioFile = new File(directory, audioFileName + Constant.AUDIO_FILE_EXTENSION);

        int i = 1;
        while (audioFile.exists()) {
            audioFile = new File(directory, audioFileName + Constant.AUDIO_FILE_EXTENSION + "(" + i + ")");
            i++;
            //ensure it doesnot repeat forever
            if (i > 10000) {
                break;
            }
        }

        Log.i("biky", "audio recording path = " + audioFile.getAbsolutePath());

        mediaRecorder.setOutputFile(audioFile.getPath() + ".temp");

        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            Log.e("biky", "prepare() failed");
            NewMessageNotification.notify(this, "Recording Failed. Other application might be using mic", NewMessageNotification.ERROR);
            stopSelf();
            return;
        }

        mediaRecorder.start();

        sendBroadcast(new Intent().
                setAction(Constant.RECORDING_AUDIO)
        );

        Log.i("biky", "recording audio");

    }

    private void stopRecording() {
        if (mediaRecorder == null) return;
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
        if (audioFile == null) {
            return;
        }
        NewMessageNotification.notify(this, "Recording completed", NewMessageNotification.RECORDING_COMPLETE);

        if (audioFile != null) {
            File tempFile = new File(audioFile.getPath() + ".temp");
            if (tempFile.exists()) {
                File newFile = new File(audioFile.getPath());
                //noinspection ResultOfMethodCallIgnored
                if (tempFile.renameTo(newFile)) {

                    sendBroadcast(new Intent().
                            setAction(Constant.NEW_FILE_CREATED).
                            putExtra(Constant.FILE_PATH_NAME, audioFile.getAbsolutePath())
                    );

                    NewMessageNotification.notify(this, "Recording completed", NewMessageNotification.RECORDING_COMPLETE);
                    Util.vibrate(this, Constant.VIBRATE_PATTERN, -1);
                }
            }
        }

    }

    // Stop recording and remove SurfaceView
    @Override
    public void onDestroy() {
        stopRecording();
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}