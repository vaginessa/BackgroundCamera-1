package com.nepdeveloper.backgroundcamera.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.nepdeveloper.backgroundcamera.R;
import com.nepdeveloper.backgroundcamera.service.AudioRecorderService;
import com.nepdeveloper.backgroundcamera.service.FileTransferService;
import com.nepdeveloper.backgroundcamera.service.MyService;
import com.nepdeveloper.backgroundcamera.service.VideoRecorderService;
import com.nepdeveloper.backgroundcamera.utility.AppRater;
import com.nepdeveloper.backgroundcamera.utility.Constant;
import com.nepdeveloper.backgroundcamera.utility.Log;
import com.nepdeveloper.backgroundcamera.utility.NewMessageNotification;
import com.nepdeveloper.backgroundcamera.utility.Util;
import com.rey.material.widget.Switch;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private MyService myService;
    private boolean isBound;

    private SharedPreferences preferences;

    private AdView adView;
    private int height;
    private Switch serviceSwitch = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(Constant.PREFERENCE_NAME, MODE_PRIVATE);

        NewMessageNotification.cancel(this);

        AppRater.appLaunched(this);

        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        height = adView.getLayoutParams().height;
        adView.getLayoutParams().height = 0;

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Log.i("Ads", "onAdLoaded");
                adView.getLayoutParams().height = height;
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                Log.i("Ads", "onAdFailedToLoad");
                //   adView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,0));
                adView.getLayoutParams().height = 0;
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
                Log.i("Ads", "onAdOpened");
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
                Log.i("Ads", "onAdLeftApplication");
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the user is about to return
                // to the app after tapping on an ad.
                Log.i("Ads", "onAdClosed");
            }
        });

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.i("biky", "asking permission onStart()");
            if (checkPermission()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
                    requestOverlayPermission();
                } else {
                    allPermissionGranted();
                }
            } else {
                requestPermission();
            }
        }

        setupUI();
/*
        Calendar calendar = Calendar.getInstance();


        calendar.set(Calendar.HOUR_OF_DAY, 23); // For 1 PM or 2 PM
        calendar.set(Calendar.MINUTE, 33);
        calendar.set(Calendar.SECOND, 0);
        PendingIntent pi = PendingIntent.getService(this, 0,
                new Intent(this, MyService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pi);

        Log.i("biky", "alarm time = " + calendar.getTimeInMillis() + ", now = " + System.currentTimeMillis());
*/
        if (preferences.contains(Constant.STORAGE_LOCATION)) {

            String srcDir = preferences.getString(Constant.STORAGE_LOCATION, Constant.FILE.getAbsolutePath());
            String desDir = Constant.FILE.getAbsolutePath();

            if (!srcDir.equals(desDir) && !Util.isMyServiceRunning(this, FileTransferService.class)) {
                Intent i = new Intent(this, FileTransferService.class);
                startService(i);
            }
            preferences.edit().remove(Constant.STORAGE_LOCATION).apply();
        }
        if (preferences.contains(Constant.STORAGE_LOCATION_OLD)) {
            preferences.edit().remove(Constant.STORAGE_LOCATION_OLD).apply();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constant.NEW_FILE_CREATED);
        filter.addAction(Constant.RECORDING_AUDIO);
        filter.addAction(Constant.RECORDING_VIDEO);
        registerReceiver(receiver, filter);
    }

    private void setupUI() {
        final RadioButton recordVideo = findViewById(R.id.record_video);
        final RadioButton capturePhoto = findViewById(R.id.capture_photo);
        final RadioButton recordAudio = findViewById(R.id.record_audio);
        final CheckBox capturePhotoBackCamera = findViewById(R.id.capture_photo_back_camera);
        final CheckBox capturePhotoFrontCamera = findViewById(R.id.capture_photo_front_camera);
        final AppCompatButton moreBtn = findViewById(R.id.more_btn);

        findViewById(R.id.stop_recording).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Util.stopRecordingAudio(MainActivity.this);
                Util.stopRecordingVideo(MainActivity.this);
            }
        });

        recordVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Util.stopRecordingAudio(MainActivity.this);
                Util.stopCapturingImage(MainActivity.this);

                recordVideo.setChecked(true);
                preferences.edit().putBoolean(Constant.RECORD_VIDEO, true).apply();

                capturePhoto.setChecked(false);
                preferences.edit().putBoolean(Constant.CAPTURE_PHOTO, false).apply();

                capturePhotoBackCamera.setEnabled(false);
                capturePhotoFrontCamera.setEnabled(false);

                recordAudio.setChecked(false);
            }
        });

        capturePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Util.stopRecordingVideo(MainActivity.this);
                Util.stopRecordingAudio(MainActivity.this);

                recordVideo.setChecked(false);
                preferences.edit().putBoolean(Constant.RECORD_VIDEO, false).apply();

                capturePhoto.setChecked(true);
                preferences.edit().putBoolean(Constant.CAPTURE_PHOTO, true).apply();

                capturePhotoBackCamera.setEnabled(true);
                capturePhotoFrontCamera.setEnabled(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT));

                recordAudio.setChecked(false);
                preferences.edit().putBoolean(Constant.RECORD_AUDIO, false).apply();
            }
        });

        recordAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Util.stopRecordingVideo(MainActivity.this);
                Util.stopCapturingImage(MainActivity.this);

                recordVideo.setChecked(false);
                preferences.edit().putBoolean(Constant.RECORD_VIDEO, false).apply();

                capturePhoto.setChecked(false);
                preferences.edit().putBoolean(Constant.CAPTURE_PHOTO, false).apply();

                capturePhotoBackCamera.setEnabled(false);
                capturePhotoFrontCamera.setEnabled(false);

                recordAudio.setChecked(true);
                preferences.edit().putBoolean(Constant.RECORD_AUDIO, true).apply();
            }
        });

        capturePhotoFrontCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                preferences.edit().putBoolean(Constant.CAPTURE_PHOTO_FRONT_CAM, checked).apply();
            }
        });

        capturePhotoBackCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                preferences.edit().putBoolean(Constant.CAPTURE_PHOTO_BACK_CAM, checked).apply();
            }
        });


        final ScrollView scroll = findViewById(R.id.scroll_view);
        moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (moreBtn.getText().toString().equals("MORE")) {
                    moreBtn.setEnabled(false);
                    findViewById(R.id.more_opt).setVisibility(View.VISIBLE);
                    scroll.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scroll.fullScroll(View.FOCUS_DOWN);
                            moreBtn.setEnabled(true);
                            moreBtn.setText(R.string.less);
                            findViewById(R.id.fab_gallery).setVisibility(View.GONE);
                        }
                    }, 500);
                } else {
                    findViewById(R.id.more_opt).setVisibility(View.GONE);
                    findViewById(R.id.fab_gallery).setVisibility(View.VISIBLE);

                    moreBtn.setText(R.string.more);

                }
            }
        });

        findViewById(R.id.view_in_playstore).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppRater.openPlayStore(MainActivity.this);
            }
        });

        findViewById(R.id.rate_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppRater.openPlayStore(MainActivity.this);
            }
        });


        findViewById(R.id.share_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        "Hey check out my app at: " + "http://play.google.com/store/apps/details?id=" + getPackageName())
                ;
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            }
        });


        findViewById(R.id.about_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Developer")
                        .setMessage("Created in: Sept, 2017\nBikram Pandit\nComputer Engineer\nWebsite: bikcrum.blogspot.com\nEmail: bikcrum@gmail.com")
                        .setPositiveButton("DISMISS", null)
                        .show();
            }
        });


        //presetup
        if (Util.isMyServiceRunning(this, VideoRecorderService.class)) {
            findViewById(R.id.stop_recording).setVisibility(View.VISIBLE);
            ((AppCompatButton) findViewById(R.id.stop_recording)).setText(R.string.stop_recording_video);
        } else if (Util.isMyServiceRunning(this, AudioRecorderService.class)) {
            ((AppCompatButton) findViewById(R.id.stop_recording)).setText(R.string.stop_recording_audio);
            findViewById(R.id.stop_recording).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.stop_recording).setVisibility(View.GONE);
        }

        if (preferences.getBoolean(Constant.RECORD_VIDEO, true)) {
            recordVideo.setChecked(true);
            recordAudio.setChecked(false);
            capturePhoto.setChecked(false);
            capturePhotoBackCamera.setEnabled(false);
            capturePhotoFrontCamera.setEnabled(false);
        } else if (preferences.getBoolean(Constant.CAPTURE_PHOTO, false)) {
            recordVideo.setChecked(false);
            recordAudio.setChecked(false);
            capturePhoto.setChecked(true);
            capturePhotoBackCamera.setEnabled(true);
            capturePhotoFrontCamera.setEnabled(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT));
        } else if (preferences.getBoolean(Constant.RECORD_AUDIO, false)) {
            recordVideo.setChecked(false);
            recordAudio.setChecked(true);
            capturePhoto.setChecked(false);
            capturePhotoBackCamera.setEnabled(false);
            capturePhotoFrontCamera.setEnabled(false);
        }
        capturePhotoBackCamera.setChecked(preferences.getBoolean(Constant.CAPTURE_PHOTO_BACK_CAM, true));
        capturePhotoFrontCamera.setChecked(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT) &&
                preferences.getBoolean(Constant.CAPTURE_PHOTO_FRONT_CAM, false));

        findViewById(R.id.more_opt).setVisibility(View.GONE);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.NEW_FILE_CREATED.equals(intent.getAction())) {
                File file = new File(intent.getStringExtra(Constant.FILE_PATH_NAME));
                if (file.getName().endsWith(Constant.VIDEO_FILE_EXTENSION) || file.getName().endsWith(Constant.AUDIO_FILE_EXTENSION)) {
                    findViewById(R.id.stop_recording).setVisibility(View.GONE);
                }
            } else if (Constant.RECORDING_VIDEO.equals(intent.getAction())) {
                ((AppCompatButton) findViewById(R.id.stop_recording)).setText(R.string.stop_recording_video);
                findViewById(R.id.stop_recording).setVisibility(View.VISIBLE);
            } else if (Constant.RECORDING_AUDIO.equals(intent.getAction())) {
                ((AppCompatButton) findViewById(R.id.stop_recording)).setText(R.string.stop_recording_audio);
                findViewById(R.id.stop_recording).setVisibility(View.VISIBLE);
            }
        }
    };


    @Override
    protected void onStart() {
        super.onStart();
    }

    private void bindService() {
        Intent intent = new Intent(this, MyService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService();
    }

    private void unbindService() {
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        unbindService();
        super.onDestroy();
    }

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyService.MyBinder myBinder = (MyService.MyBinder) service;
            myService = myBinder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bar, menu);

        serviceSwitch = menu.findItem(R.id.service_switch).getActionView().findViewById(R.id.switch_in_action_bar);

        serviceSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(Switch view, boolean checked) {
                if (preferences != null) {
                    preferences.edit().putBoolean(Constant.SERVICE_ACTIVE, checked).apply();
                    if (checked) {
                        startService(new Intent(MainActivity.this, MyService.class));
                        bindService();
                    } else {
                        unbindService();
                        stopService(new Intent(MainActivity.this, MyService.class));
                        Util.stopCapturingImage(MainActivity.this);
                        Util.stopRecordingAudio(MainActivity.this);
                        Util.stopRecordingVideo(MainActivity.this);
                    }
                }
            }
        });
        //presetup
        if (preferences != null) {

            if (preferences.getBoolean(Constant.SERVICE_ACTIVE, true)) {
                serviceSwitch.setChecked(preferences.getBoolean(Constant.SERVICE_ACTIVE, true));
            } else {
                serviceSwitch.setChecked(preferences.getBoolean(Constant.SERVICE_ACTIVE, false));
                Util.prepareSnackBar(findViewById(R.id.coordinate_layout), "Service is turned off")
                        .setAction("TURN ON", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (serviceSwitch != null) {
                                    serviceSwitch.setChecked(true);
                                }
                            }
                        })
                        .show();
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.setting:
                Intent i = new Intent(this, Settings.class);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferences.getString(Constant.WHEN_TO_STOP_RECORDING, Constant.MANUALLY_STOP).equals(Constant.SCREEN_IS_TOGGLED_TO_STOP)) {
            ((TextView) findViewById(R.id.video_switch_text1)).setText(R.string.turn_screen_on_from_off_to_stop_recording);
            ((TextView) findViewById(R.id.audio_switch_text1)).setText(R.string.turn_screen_on_from_off_to_stop_recording);
        } else if (preferences.getString(Constant.WHEN_TO_STOP_RECORDING, Constant.MANUALLY_STOP).equals(Constant.MANUALLY_STOP)) {
            ((TextView) findViewById(R.id.video_switch_text1)).setText(R.string.stop_recording_from_app_or_notification);
            ((TextView) findViewById(R.id.audio_switch_text1)).setText(R.string.stop_recording_from_app_or_notification);
        }
    }

    private boolean checkPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);

    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        if (!checkPermission()) {
            preferences.edit().putBoolean(Constant.PERMISSION_ASKED_BEFORE, true).apply();

            ArrayList<String> permissions = new ArrayList<>();

            permissions.add(Manifest.permission.CAMERA);
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.RECORD_AUDIO);

            requestPermissions(permissions.toArray(new String[permissions.size()]), Constant.APP_PERMISSIONS);
        }
    }

    private void exitApp() {
        Util.stopMainService(this);
        Util.stopCapturingImage(this);
        Util.stopRecordingAudio(this);
        Util.stopRecordingVideo(this);
        finish();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.i("biky", "on request permission result");

        if (requestCode == Constant.APP_PERMISSIONS) {

            if (checkPermission()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
                    requestOverlayPermission();
                } else {
                    allPermissionGranted();
                }
            } else {
                showDialog();
            }

        }
    }

    private void requestOverlayPermission() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Allow Permission")
                .setMessage("You need to allow 'Display over other apps' permission inorder to function Camera in background.")
                .setPositiveButton("ALLOW PERMISSION", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
                            return;
                        }
                        Intent myIntent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        myIntent.setData(Uri.parse("package:" + getPackageName()));

                        startActivityForResult(myIntent, Constant.APP_PERMISSIONS);
                    }
                })
                .setNegativeButton("Exit App", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        exitApp();
                    }
                })
                .setCancelable(false)
                .create()
                .show();
    }

    public void showDialog() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;


        AlertDialog dialog;

        if (preferences.getBoolean(Constant.PERMISSION_ASKED_BEFORE, false) && !checkPermission()) {

            if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                    || !shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    || !shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
                    || !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {

                Log.i("biky", "never show again is checked");

                dialog = new AlertDialog.Builder(this)
                        .setTitle("Permission Denied")
                        .setMessage("You checked 'Never show again' while requesting permissions by app. Go to app setting to allow all of these permissions manually.")
                        .setPositiveButton("GO-TO APP SETTINGS", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                manualPermission();
                            }
                        })
                        .setNegativeButton("EXIT APP", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                exitApp();
                            }
                        })
                        .create();

                dialog.setCancelable(false);
                dialog.show();

                return;
            } else {
                Log.i("biky", "never show again is noooooooooooooooot checked");
            }
        }

        dialog = new AlertDialog.Builder(this)
                .setTitle("Permission Denied")
                .setMessage("This App can't run without allowing it permission")
                .setPositiveButton("ALLOW PERMISSIONS", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestPermission();
                    }
                })
                .setNegativeButton("EXIT APP", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        exitApp();
                    }
                })
                .create();

        dialog.setCancelable(false);
        dialog.show();
    }

    private void manualPermission() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, Constant.APP_PERMISSIONS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.i("biky", "on activity result");

        if (requestCode == Constant.APP_PERMISSIONS) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                if (checkPermission()) {
                    if (!android.provider.Settings.canDrawOverlays(this)) {
                        new AlertDialog.Builder(this)
                                .setTitle("Restart App")
                                .setMessage("Restart app to validate permission")
                                .setPositiveButton("RESTART APP NOW", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        exitApp();
                                        startActivity(new Intent(MainActivity.this, MainActivity.class));
                                    }
                                })
                                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        if (checkPermission()) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(MainActivity.this)) {
                                                requestOverlayPermission();
                                            } else {
                                                allPermissionGranted();
                                            }
                                        } else {
                                            showDialog();
                                        }
                                    }
                                })
                                .setCancelable(false)
                                .create()
                                .show();
                    } else {
                        allPermissionGranted();
                    }
                } else {
                    showDialog();
                }
                return;
            }


            if (checkPermission()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
                    requestOverlayPermission();
                } else {
                    allPermissionGranted();
                }
            } else {
                showDialog();
            }
        }
    }

    private void allPermissionGranted() {
        Log.i("biky","all permission is granted");
        if (preferences.getBoolean(Constant.SERVICE_ACTIVE, true)) {
            Intent i = new Intent(this, MyService.class);
            startService(i);
            bindService();
        }
    }


    public void openGallery(View v) {
        startActivity(new Intent(this, GalleryGrid.class));
    }

}

