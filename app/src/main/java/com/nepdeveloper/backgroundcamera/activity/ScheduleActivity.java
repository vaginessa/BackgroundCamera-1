package com.nepdeveloper.backgroundcamera.activity;

import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.Preference;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;

import com.bikcrum.circularrangeslider.CircularRangeSlider;
import com.nepdeveloper.backgroundcamera.R;
import com.nepdeveloper.backgroundcamera.utility.Constant;
import com.nepdeveloper.backgroundcamera.utility.Log;
import com.rey.material.widget.Switch;

import android.widget.TextView;

import java.util.Calendar;

public class ScheduleActivity extends AppCompatActivity {

    private Handler handler = new Handler();
    private CircularRangeSlider circularRangeSlider;
    private SharedPreferences preferences;
    private TextView endHour;
    private TextView startHour;
    private String[] hours;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);


        if (getActionBar() != null) {
            getActionBar().setDisplayShowHomeEnabled(true);
        }

        preferences = getSharedPreferences(Constant.PREFERENCE_NAME, MODE_PRIVATE);
        circularRangeSlider = findViewById(R.id.circular_slider);
        endHour = findViewById(R.id.service_end_hour);
        startHour = findViewById(R.id.service_start_hour);

        hours = getResources().getStringArray(R.array.step_array);

        int starthour = preferences.getInt(Constant.START_HOUR, Constant.DEFAULT_START_HOUR);
        int endhour = preferences.getInt(Constant.END_HOUR, Constant.DEFAULT_END_HOUR);

        circularRangeSlider.setStartIndex(endhour);
        circularRangeSlider.setEndIndex(starthour);

        updateTime(starthour, endhour);

        circularRangeSlider.setOnRangeChangeListener(new CircularRangeSlider.OnRangeChangeListener() {
            @Override
            public void onRangePress(int startIndex, int endIndex) {

            }

            @Override
            public void onRangeChange(int startIndex, int endIndex) {
                updateTime(endIndex, startIndex);
                Log.d("biky", "range changing");
            }

            @Override
            public void onRangeRelease(int startIndex, int endIndex) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(Constant.END_HOUR, startIndex);
                editor.putInt(Constant.START_HOUR, endIndex);
                editor.apply();
            }
        });

        handler.post(new Runnable() {
            @Override
            public void run() {
                updateProgress();
                handler.postDelayed(this, 60000); //wait one minute for next update of progress
            }
        });
    }

    private void updateTime(int starthour, int endhour) {
        if (endhour >= 0 && endhour < hours.length) {
            endHour.setText(String.format("Service End Time: %s", hours[endhour]));
        }
        if (starthour >= 0 && starthour < hours.length) {
            startHour.setText(String.format("Service Start Time: %s", hours[starthour]));
        }
    }

    private void updateProgress() {
        Calendar rightNow = Calendar.getInstance();
        int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        int minute = rightNow.get(Calendar.MINUTE);

        circularRangeSlider.setProgress(hour + minute / 60f);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bar_schedule_service, menu);

        Switch scheduleService = menu.findItem(R.id.schedule_service_switch).getActionView().findViewById(R.id.switch_in_action_bar);

        scheduleService.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(Switch view, boolean checked) {
                circularRangeSlider.setEnabled(checked);
                preferences.edit().putBoolean(Constant.SCHEDULE_SERVICE, checked).apply();
            }
        });
        //presetup
        if (preferences != null) {
            if (preferences.getBoolean(Constant.SCHEDULE_SERVICE, true)) {
                scheduleService.setChecked(true);
            } else {
                scheduleService.setChecked(false);
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
