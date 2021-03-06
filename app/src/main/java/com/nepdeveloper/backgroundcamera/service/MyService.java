package com.nepdeveloper.backgroundcamera.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import com.nepdeveloper.backgroundcamera.R;
import com.nepdeveloper.backgroundcamera.utility.Constant;
import com.nepdeveloper.backgroundcamera.utility.Log;
import com.nepdeveloper.backgroundcamera.utility.NewMessageNotification;
import com.nepdeveloper.backgroundcamera.utility.Util;


public class MyService extends Service {
    private IBinder binder = new MyBinder();
    //private boolean isBound = false;
    private MediaPlayer mediaPlayer;
    private SharedPreferences preferences;
    private AudioManager audio;
    private boolean dontTrigger = false;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            dontTrigger = true;
        }
    };

    public MyService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("biky", "on start command");

        preferences = getSharedPreferences(Constant.PREFERENCE_NAME, MODE_PRIVATE);

        registerReceiver();

        return START_STICKY;
    }

    public void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        filter.addAction(Intent.ACTION_SCREEN_ON);

        registerReceiver(broadcastReceiver, filter);

        mediaPlayer = MediaPlayer.create(this, R.raw.silence01s);

        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }

        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audio != null && audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0 && preferences.getBoolean(Constant.LEAST_VOLUME_IS_ONE, true)) {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, 1, 0);
        }
    }

    public void unregisterReceiver() {
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onRebind(Intent intent) {
        //isBound = true;
        Log.d("biky", "on rebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("biky", "on unbind");
        //isBound = false;
        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("biky", "on bind");
        //isBound = true;
        return binder;
    }

    public class MyBinder extends Binder {
        public MyService getService() {
            return MyService.this;
        }
    }

    long prevMillis = System.currentTimeMillis();
    long totalTime0 = 0;
    //long totalTime15 = 0;

    public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                if (preferences.getString(Constant.WHEN_TO_STOP_RECORDING, Constant.MANUALLY_STOP).equals(Constant.SCREEN_IS_TOGGLED_TO_STOP)) {
                    Util.stopRecordingVideo(MyService.this);
                    Util.stopRecordingAudio(MyService.this);
                }
            } else if ("android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) {
                int volume;
                try {
                    volume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 0);
                } catch (Exception e) {
                    return;
                }

                Log.i("biky", "volume = " + volume);
                long currMillis = System.currentTimeMillis();
                long delta = currMillis - prevMillis;
                Log.i("biky", "delta = " + delta);
                //if delta < 500 means button is pressed within half seconds gap. if more then its not taken in count
                if (delta < 500) {
                    if (volume == 0) {
                        totalTime0 += delta;
                        //totalTime15 = 0;
                    } else if (volume == 15) {
                        totalTime0 = 0;
                        // totalTime15 += delta;
                    }
                } else {
                    //button is released
                    totalTime0 = 0;
                    //  totalTime15 = 0;
                }

                Log.i("biky", "total time 0 = " + totalTime0);
                //    Log.i("biky", "total time 15 = " + totalTime15);

                //how much time should volume down button should be press to trigger the event is given by timej in seconds
                if (totalTime0 > Constant.HOLD_DOWN_TIME && preferences.getBoolean(Constant.SERVICE_ACTIVE, true)) {
                    trigger();
                }
                prevMillis = currMillis;

                if (volume == 0 && preferences.getBoolean(Constant.LEAST_VOLUME_IS_ONE, true)) {
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, 1, 0);
                }
            }
        }
    };

    public void trigger() {
        if (dontTrigger) {
            return;
        }
        if (preferences == null) {
            preferences = getSharedPreferences(Constant.PREFERENCE_NAME, MODE_PRIVATE);
        }
        if (preferences == null) {
            return;
        }
        if (preferences.getBoolean(Constant.CAPTURE_PHOTO, false) &&
                (preferences.getBoolean(Constant.CAPTURE_PHOTO_BACK_CAM, true)
                        ||
                        preferences.getBoolean(Constant.CAPTURE_PHOTO_FRONT_CAM, false))) {

            if (Util.permissionIsGranted(this, Constant.CAPTURE_PHOTO)) {

                if (!Util.isMyServiceRunning(this, ImageCaptureService.class)) {
                    Util.stopRecordingVideo(this);
                    Util.stopRecordingAudio(this);
                    Util.stopCapturingImage(this);

                    Util.vibrate(this, 100);

                    Intent i = new Intent(this, ImageCaptureService.class);
                    i.putExtra(Constant.CAPTURE_PHOTO_BACK_CAM, preferences.getBoolean(Constant.CAPTURE_PHOTO_BACK_CAM, true));
                    i.putExtra(Constant.CAPTURE_PHOTO_FRONT_CAM, getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
                            && preferences.getBoolean(Constant.CAPTURE_PHOTO_FRONT_CAM, false));

                    Log.i("biky", "image capture service called");
                    startService(i);
                } else {
                    Log.i("biky", "image capture service already running");
                }

            } else {
                dontTrigger = true;
                handler.removeCallbacks(runnable);
                handler.postDelayed(runnable, 2000);
            }
        } else if (preferences.getBoolean(Constant.RECORD_VIDEO, true)) {

            if (Util.permissionIsGranted(this, Constant.RECORD_VIDEO)) {

                if (!Util.isMyServiceRunning(this, VideoRecorderService.class)) {
                    Util.stopRecordingVideo(this);
                    Util.stopRecordingAudio(this);
                    Util.stopCapturingImage(this);

                    Util.vibrate(this, 100);

                    final Intent v = new Intent(this, VideoRecorderService.class);
                    Log.i("biky", "video recorder service called");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(v);
                    } else {
                        startService(v);
                    }
                } else {
                    Log.i("biky", "video recording service already running");
                }
            } else {
                dontTrigger = true;
                handler.removeCallbacks(runnable);
                handler.postDelayed(runnable, 2000);
            }
        } else if (preferences.getBoolean(Constant.RECORD_AUDIO, false)) {

            if (Util.permissionIsGranted(this, Constant.RECORD_AUDIO)) {

                if (!Util.isMyServiceRunning(this, AudioRecorderService.class)) {
                    Util.vibrate(this, 100);

                    Util.stopRecordingVideo(this);
                    Util.stopRecordingAudio(this);
                    Util.stopCapturingImage(this);

                    final Intent v = new Intent(this, AudioRecorderService.class);
                    Log.i("biky", "audio recorder service called");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(v);
                    } else {
                        startService(v);
                    }
                } else {
                    Log.i("biky", "audio recording service already running");
                }
            } else {
                dontTrigger = true;
                handler.removeCallbacks(runnable);
                handler.postDelayed(runnable, 2000);
            }
        }
    }


    @Override
    public void onDestroy() {
        Log.i("biky", "my service on destroy");

        unregisterReceiver();

        NewMessageNotification.cancel(this);

        super.onDestroy();
    }

}
