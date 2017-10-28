package com.nepdeveloper.backgroundcamera.service;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import com.nepdeveloper.backgroundcamera.utility.Log;

import com.nepdeveloper.backgroundcamera.hiddenCameraPackage.CameraConfig;
import com.nepdeveloper.backgroundcamera.hiddenCameraPackage.CameraError;
import com.nepdeveloper.backgroundcamera.hiddenCameraPackage.HiddenCameraService;
import com.nepdeveloper.backgroundcamera.hiddenCameraPackage.HiddenCameraUtils;
import com.nepdeveloper.backgroundcamera.hiddenCameraPackage.config.CameraFacing;
import com.nepdeveloper.backgroundcamera.hiddenCameraPackage.config.CameraImageFormat;
import com.nepdeveloper.backgroundcamera.hiddenCameraPackage.config.CameraResolution;
import com.nepdeveloper.backgroundcamera.hiddenCameraPackage.config.CameraRotation;
import com.nepdeveloper.backgroundcamera.R;
import com.nepdeveloper.backgroundcamera.utility.Constant;
import com.nepdeveloper.backgroundcamera.utility.NewMessageNotification;
import com.nepdeveloper.backgroundcamera.utility.Util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageCaptureService extends HiddenCameraService {
    private MediaPlayer cameraSound;
    private Intent intent;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("biky", "image service cam. on start command");
        this.intent = intent;

        cameraSound = MediaPlayer.create(this, R.raw.camera_shutter);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.
                CAMERA) == PackageManager.PERMISSION_GRANTED) {

            if (HiddenCameraUtils.canOverDrawOtherApps(this)) {
                File imageFile = createImageFile();
                CameraConfig cameraConfig = new CameraConfig()
                        .getBuilder(this)
                        .setCameraFacing(intent.getBooleanExtra(Constant.CAPTURE_PHOTO_BACK_CAM, true) ?
                                CameraFacing.REAR_FACING_CAMERA : CameraFacing.FRONT_FACING_CAMERA)
                        .setCameraResolution(CameraResolution.MEDIUM_RESOLUTION)
                        .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                        .setImageFile(imageFile)
                        .setImageRotation(intent.getBooleanExtra(Constant.CAPTURE_PHOTO_BACK_CAM, true) ?
                                CameraRotation.ROTATION_90 : CameraRotation.ROTATION_270)
                        //.setFlash(intent.getBooleanExtra(Constant.FLASH_ON, false))
                        .build();

                startCamera(cameraConfig);

                new android.os.Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            takePicture();
                        } catch (Exception e) {
                            Log.i("biky", "camera not initialized, " + e.getMessage());
                        }
                    }
                });
            } else {
                NewMessageNotification.notify(this, "Capture Failed. Allow this app permission to draw over apps", NewMessageNotification.PERMISSION_DENIED);
                Log.i("biky", "Allow this app permission to draw over apps");
                stopSelf();
            }
        } else {
            Log.i("biky", "image capture serive cam. Camera permission not available");
            NewMessageNotification.notify(this, "Capture Failed. You have not permitted this app to use camera", NewMessageNotification.PERMISSION_DENIED);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String imageFileName = Constant.FILE_PREFIX + timeStamp;

        //    File directory = new File(getSharedPreferences(Constant.PREFERENCE_NAME, MODE_PRIVATE).getString(Constant.STORAGE_LOCATION, Constant.FILE.getAbsolutePath()));

        File directory = Constant.FILE;

        if (!directory.exists() || !directory.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }

        File imageFile = new File(directory, imageFileName + Constant.IMAGE_FILE_EXTENSION);
        int i = 1;
        while (imageFile.exists()) {
            imageFile = new File(directory.getAbsolutePath(), imageFileName + Constant.IMAGE_FILE_EXTENSION + "(" + i + ")");
            i++;
            //ensure it doesnot repeat forever
            if (i > 10000) {
                break;
            }
        }
        return imageFile;
    }

    @Override
    public void onImageCapture(@NonNull File imageFile) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        if (getSharedPreferences(Constant.PREFERENCE_NAME, MODE_PRIVATE).getBoolean(Constant.SHUTTER_SOUND, true)) {
            final AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 3 / 4, 0);
            }
            cameraSound.start();
            cameraSound.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    if (am != null) {
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, 1, 0);
                    }
                }
            });
        }


        Log.i("biky", "image captured by  cam " + imageFile.length() + ", image absolute path = " + imageFile.getAbsolutePath());

        sendBroadcast(new Intent().
                setAction(Constant.NEW_FILE_CREATED).
                putExtra(Constant.FILE_PATH_NAME, imageFile.getAbsolutePath())
        );

        Util.vibrate(this, Constant.VIBRATE_PATTERN, -1);
        NewMessageNotification.notify(this, "Image Captured", NewMessageNotification.IMAGE_CAPTURED);
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopSelf();
        //start service again if image needs to capture by both camera
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
                && intent.getBooleanExtra(Constant.CAPTURE_PHOTO_BACK_CAM, true)
                && intent.getBooleanExtra(Constant.CAPTURE_PHOTO_FRONT_CAM, false)) {
            Intent i = new Intent(this, ImageCaptureService.class);
            i.putExtra(Constant.CAPTURE_PHOTO_BACK_CAM, false);
            i.putExtra(Constant.CAPTURE_PHOTO_FRONT_CAM, true);
            startService(i);
        }
    }

    @Override
    public void onCameraError(@CameraError.CameraErrorCodes int errorCode) {
        switch (errorCode) {
            case CameraError.ERROR_CAMERA_OPEN_FAILED:
                //Camera open failed. Probably because another application
                //is using the camera
                Log.i("biky", "Cannot open camera.");
                NewMessageNotification.notify(this, "Capture Failed. Other application might be using camera", NewMessageNotification.ERROR);
                break;
            case CameraError.ERROR_IMAGE_WRITE_FAILED:
                //Image write failed. Please check if you have provided WRITE_EXTERNAL_STORAGE permission
                Log.i("biky", "Cannot write image captured by camera.");
                NewMessageNotification.notify(this, "Capture Failed. Allow this app write permission", NewMessageNotification.PERMISSION_DENIED);
                break;
            case CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE:
                //camera permission is not available
                //Ask for the camera permission before initializing it.
                NewMessageNotification.notify(this, "Capture Failed. You have not permitted this app to use camera", NewMessageNotification.PERMISSION_DENIED);

                Log.i("biky", "Camera permission not available.");
                break;
            case CameraError.ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION:
                NewMessageNotification.notify(this, "Capture Failed. Allow this app permission to draw over apps", NewMessageNotification.PERMISSION_DENIED);
                Log.i("biky", "Allow this app permission to draw over apps");
                break;
            case CameraError.ERROR_DOES_NOT_HAVE_FRONT_CAMERA:
                NewMessageNotification.notify(this, "Front camera capture failed. Your device doesn't have front camera", NewMessageNotification.ERROR);
                Log.i("biky", "Your device does not have front camera.");
                break;
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("biky", "on destroy. image capture service  cam");
    }
}