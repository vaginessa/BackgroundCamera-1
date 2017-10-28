package com.nepdeveloper.backgroundcamera.activity;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.nepdeveloper.backgroundcamera.utility.Log;

import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.nepdeveloper.backgroundcamera.R;
import com.nepdeveloper.backgroundcamera.utility.Constant;
import com.rey.material.widget.Switch;

import java.text.SimpleDateFormat;
import java.util.Locale;


public class Settings extends AppCompatActivity {

    private SharedPreferences preferences;
    // private DirectoryChooserFragment mDialog;
    private AdView adView;
    private int height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        adView = (AdView) findViewById(R.id.adView);
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


        preferences = getSharedPreferences(Constant.PREFERENCE_NAME, MODE_PRIVATE);

        if (getActionBar() != null) {
            getActionBar().setDisplayShowHomeEnabled(true);
        }

        setupUI();
    }

    private void setupUI() {
        ((Switch) findViewById(R.id.shutter_sound)).setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(Switch view, boolean checked) {
                if (!checked && !doNotDisturbAllowed()) {
                    ((Switch) findViewById(R.id.shutter_sound)).setChecked(true);
                    requestDoNotDisturbPermission();
                    return;
                }
                preferences.edit().putBoolean(Constant.SHUTTER_SOUND, checked).apply();
            }

        });

        ((RadioGroup) findViewById(R.id.video_stop)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                switch (id) {
                    case R.id.screen_off_on:
                        preferences.edit().putString(Constant.WHEN_TO_STOP_RECORDING, Constant.SCREEN_IS_TOGGLED_TO_STOP).apply();
                        break;
                    case R.id.manual:
                        preferences.edit().putString(Constant.WHEN_TO_STOP_RECORDING, Constant.MANUALLY_STOP).apply();
                        break;
                }
            }
        });

        ((Switch) findViewById(R.id.show_notification)).setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(Switch view, boolean checked) {
                preferences.edit().putBoolean(Constant.SHOW_NOTIFICATION, checked).apply();
            }
        });


        ((Spinner) findViewById(R.id.video_recording_quality)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                switch (position) {
                    case 0:
                        preferences.edit().putString(Constant.RECORDING_QUALITY, Constant.QUALITY_HIGH).apply();
                        break;
                    case 1:
                        preferences.edit().putString(Constant.RECORDING_QUALITY, Constant.QUALITY_MEDIUM).apply();
                        break;
                    case 2:
                        preferences.edit().putString(Constant.RECORDING_QUALITY, Constant.QUALITY_LOW).apply();
                        break;
                    default:
                        preferences.edit().putString(Constant.RECORDING_QUALITY, Constant.QUALITY_HIGH).apply();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ((RadioGroup) findViewById(R.id.video_stop)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                switch (id) {
                    case R.id.screen_off_on:
                        preferences.edit().putString(Constant.WHEN_TO_STOP_RECORDING, Constant.SCREEN_IS_TOGGLED_TO_STOP).apply();
                        break;
                    case R.id.manual:
                        preferences.edit().putString(Constant.WHEN_TO_STOP_RECORDING, Constant.MANUALLY_STOP).apply();
                        break;
                }
            }
        });

        ((RadioButton) findViewById(R.id.always_active_radio)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    preferences.edit().putBoolean(Constant.SERVICE_ALWAYS_ACTIVE, true).apply();

                    RelativeLayout layout = (RelativeLayout) findViewById(R.id.start_time_btn);
                    layout.setEnabled(false);
                    for (int i = 0; i < layout.getChildCount(); i++) {
                        View child = layout.getChildAt(i);
                        child.setEnabled(false);
                    }

                    layout = (RelativeLayout) findViewById(R.id.end_time_btn);
                    layout.setEnabled(false);
                    for (int i = 0; i < layout.getChildCount(); i++) {
                        View child = layout.getChildAt(i);
                        child.setEnabled(false);
                    }
                }
            }
        });

        ((RadioButton) findViewById(R.id.schedule_radio)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    preferences.edit().putBoolean(Constant.SERVICE_ALWAYS_ACTIVE, false).apply();


                    RelativeLayout layout = (RelativeLayout) findViewById(R.id.start_time_btn);
                    layout.setEnabled(true);
                    for (int i = 0; i < layout.getChildCount(); i++) {
                        View child = layout.getChildAt(i);
                        child.setEnabled(true);
                    }

                    layout = (RelativeLayout) findViewById(R.id.end_time_btn);
                    layout.setEnabled(true);
                    for (int i = 0; i < layout.getChildCount(); i++) {
                        View child = layout.getChildAt(i);
                        child.setEnabled(true);
                    }

                }
            }
        });

        final AppCompatButton showAdvancedSetting = (AppCompatButton) findViewById(R.id.show_advanced_setting);

        final ScrollView scroll = (android.widget.ScrollView) findViewById(R.id.scroll_view);

        showAdvancedSetting.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (showAdvancedSetting.getText().toString().equals("SHOW ADVANCED SETTING")) {
                    showAdvancedSetting.setEnabled(false);
                    findViewById(R.id.advanced_setting).setVisibility(View.VISIBLE);
                    scroll.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scroll.fullScroll(View.FOCUS_DOWN);
                            showAdvancedSetting.setEnabled(true);
                            showAdvancedSetting.setText(R.string.hide_advanced_setting);
                        }
                    }, 500);

                } else {
                    findViewById(R.id.advanced_setting).setVisibility(View.GONE);
                    showAdvancedSetting.setText(R.string.show_advanced_setting);
                }
            }
        });


        ((CheckBox) findViewById(R.id.checkbox)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                preferences.edit().putBoolean(Constant.LEAST_VOLUME_IS_ONE, !checked).apply();
                if (!checked) {
                    AudioManager audio = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
                    if (audio != null && audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, 1, 0);
                    }
                }
            }
        });

        //presetup
        if (preferences.getBoolean(Constant.SHUTTER_SOUND, true)) {
            ((Switch) findViewById(R.id.shutter_sound)).setChecked(true);
        } else {
            if (doNotDisturbAllowed()) {
                ((Switch) findViewById(R.id.shutter_sound)).setChecked(false);
            } else {
                ((Switch) findViewById(R.id.shutter_sound)).setChecked(true);
                preferences.edit().putBoolean(Constant.SHUTTER_SOUND, true).apply();
                requestDoNotDisturbPermission();
            }
        }

        if (preferences.getString(Constant.WHEN_TO_STOP_RECORDING, Constant.MANUALLY_STOP).equals(Constant.SCREEN_IS_TOGGLED_TO_STOP)) {
            ((RadioButton) findViewById(R.id.screen_off_on)).setChecked(true);
            ((RadioButton) findViewById(R.id.manual)).setChecked(false);
        } else if (preferences.getString(Constant.WHEN_TO_STOP_RECORDING, Constant.MANUALLY_STOP).equals(Constant.MANUALLY_STOP)) {
            ((RadioButton) findViewById(R.id.screen_off_on)).setChecked(false);
            ((RadioButton) findViewById(R.id.manual)).setChecked(true);
        }
        switch (preferences.getString(Constant.RECORDING_QUALITY, Constant.QUALITY_MEDIUM)) {
            case Constant.QUALITY_HIGH:
                ((Spinner) findViewById(R.id.video_recording_quality)).setSelection(0);
                break;
            case Constant.QUALITY_MEDIUM:
                ((Spinner) findViewById(R.id.video_recording_quality)).setSelection(1);
                break;
            case Constant.QUALITY_LOW:
                ((Spinner) findViewById(R.id.video_recording_quality)).setSelection(2);
                break;
            default:
                ((Spinner) findViewById(R.id.video_recording_quality)).setSelection(0);
        }

        //  ((TextView) findViewById(R.id.storage_location)).setText(preferences.getString(Constant.STORAGE_LOCATION, Constant.FILE.getAbsolutePath()));

        if (preferences.getString(Constant.WHEN_TO_STOP_RECORDING, Constant.MANUALLY_STOP).equals(Constant.SCREEN_IS_TOGGLED_TO_STOP)) {
            ((RadioButton) findViewById(R.id.screen_off_on)).setChecked(true);
            ((RadioButton) findViewById(R.id.manual)).setChecked(false);
        } else if (preferences.getString(Constant.WHEN_TO_STOP_RECORDING, Constant.MANUALLY_STOP).equals(Constant.MANUALLY_STOP)) {
            ((RadioButton) findViewById(R.id.screen_off_on)).setChecked(false);
            ((RadioButton) findViewById(R.id.manual)).setChecked(true);
        }

        ((Switch) findViewById(R.id.show_notification)).setChecked(preferences.getBoolean(Constant.SHOW_NOTIFICATION, true));

        boolean serviceAlwaysActive = preferences.getBoolean(Constant.SERVICE_ALWAYS_ACTIVE, false);

        RelativeLayout layout = (RelativeLayout) findViewById(R.id.start_time_btn);
        layout.setEnabled(!serviceAlwaysActive);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setEnabled(!serviceAlwaysActive);
        }

        layout = (RelativeLayout) findViewById(R.id.end_time_btn);
        layout.setEnabled(!serviceAlwaysActive);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setEnabled(!serviceAlwaysActive);
        }

        ((RadioButton) findViewById(R.id.always_active_radio)).setChecked(serviceAlwaysActive);
        ((RadioButton) findViewById(R.id.schedule_radio)).setChecked(!serviceAlwaysActive);


        final String startTime = preferences.getString(Constant.START_TIME, Constant.DEFAULT_START_TIME);
        final String endTime = preferences.getString(Constant.END_TIME, Constant.DEFAULT_END_TIME);

        try {
            ((TextView) findViewById(R.id.start_time)).setText(
                    new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new SimpleDateFormat("HH:mm", Locale.ENGLISH).parse(startTime))
            );
        } catch (Exception e) {
            ((TextView) findViewById(R.id.start_time)).setText(Constant.DEFAULT_START_TIME);
            e.printStackTrace();
        }
        try {
            ((TextView) findViewById(R.id.end_time)).setText(
                    new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new SimpleDateFormat("HH:mm", Locale.ENGLISH).parse(endTime))
            );
        } catch (Exception e) {
            ((TextView) findViewById(R.id.start_time)).setText(Constant.DEFAULT_END_TIME);
            e.printStackTrace();
        }


        ((CheckBox) findViewById(R.id.checkbox)).setChecked(!preferences.getBoolean(Constant.LEAST_VOLUME_IS_ONE, true));
    }

    private boolean doNotDisturbAllowed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        return notificationManager != null && notificationManager.isNotificationPolicyAccessGranted();
    }

    private void requestDoNotDisturbPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        new AlertDialog.Builder(this)
                .setMessage("To turn off shutter sound you need to provide this app permission to access 'Do not disturb mode'.")
                .setPositiveButton("GIVE ACCESS", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            return;
                        }

                        Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

                        startActivityForResult(intent, Constant.APP_PERMISSIONS);
                    }
                })
                .setNegativeButton("CANCEL", null)
                .create()
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constant.APP_PERMISSIONS) {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return;
            }

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null && notificationManager.isNotificationPolicyAccessGranted()) {
                ((Switch) findViewById(R.id.shutter_sound)).setChecked(false);
                preferences.edit().putBoolean(Constant.SHUTTER_SOUND, false).apply();
            }
        }
    }

    public void setStartTime(View view) {
        final String startTime = preferences.getString(Constant.START_TIME, Constant.DEFAULT_START_TIME);

        RadialTimePickerDialogFragment rtpd = new RadialTimePickerDialogFragment()
                .setOnTimeSetListener(new RadialTimePickerDialogFragment.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
                        try {
                            String newStartTime = new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(
                                    new SimpleDateFormat("HH:mm", Locale.ENGLISH).parse(hourOfDay + ":" + minute));
                            ((TextView) findViewById(R.id.start_time)).setText(newStartTime);
                            preferences.edit().putString(Constant.START_TIME, hourOfDay + ":" + minute).apply();
                        } catch (Exception e) {
                            Log.i("biky", e.getMessage());
                        }
                    }
                })
                .setStartTime(Integer.parseInt(startTime.split(":")[0]), Integer.parseInt(startTime.split(":")[1]))
                .setDoneText("OK")
                .setCancelText("CANCEL")
                .setForced12hFormat();
        rtpd.show(getSupportFragmentManager(), Constant.TIME_PICKER);
    }

    public void setEndTime(View view) {
        final String endTime = preferences.getString(Constant.END_TIME, Constant.DEFAULT_END_TIME);

        RadialTimePickerDialogFragment rtpd = new RadialTimePickerDialogFragment()
                .setOnTimeSetListener(new RadialTimePickerDialogFragment.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
                        try {
                            String newStartTime = new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(
                                    new SimpleDateFormat("HH:mm", Locale.ENGLISH).parse(hourOfDay + ":" + minute));
                            ((TextView) findViewById(R.id.end_time)).setText(newStartTime);
                            preferences.edit().putString(Constant.END_TIME, hourOfDay + ":" + minute).apply();
                        } catch (Exception e) {
                            Log.i("biky", e.getMessage());
                        }
                    }
                })
                .setStartTime(Integer.parseInt(endTime.split(":")[0]), Integer.parseInt(endTime.split(":")[1]))
                .setDoneText("OK")
                .setCancelText("CANCEL")
                .setForced12hFormat();
        rtpd.show(getSupportFragmentManager(), Constant.TIME_PICKER);
    }
/*
    public void openDirChooser(View v) {
        if (Util.isMyServiceRunning(this, FileTransferService.class)) {
            new AlertDialog.Builder(this)
                    .setMessage("Wait until files are moved to new storage location")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create()
                    .show();
            return;
        }
        String oldDir = preferences.getString(Constant.STORAGE_LOCATION, Constant.FILE.getAbsolutePath());
        preferences.edit().putString(Constant.STORAGE_LOCATION_OLD, oldDir).apply();
        mDialog = DirectoryChooserFragment
                .newInstance(oldDir);
        mDialog.setCancelable(false);
        mDialog.show(getFragmentManager(), null);
    }


    @Override
    public void onSelectDirectory(@NonNull String path) {
        Log.i("biky", "path = " + path);
        preferences.edit().putString(Constant.STORAGE_LOCATION, path).apply();
        ((TextView) findViewById(R.id.storage_location)).setText(path);
        mDialog.dismiss();
        transferFiles();
    }

    @Override
    public void onCancelChooser() {
        mDialog.dismiss();
    }

    private void transferFiles() {
        String oldDir = preferences.getString(Constant.STORAGE_LOCATION_OLD, Constant.FILE.getAbsolutePath());
        String newDir = preferences.getString(Constant.STORAGE_LOCATION, Constant.FILE.getAbsolutePath());

        if (oldDir.equals(newDir)) {
            return;
        }

        final Intent v = new Intent(this, FileTransferService.class);
        Log.i("biky", "file transfer service called");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(v);
        } else {
            startService(v);
        }
    }*/
}
