package com.nepdeveloper.backgroundcamera.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.nepdeveloper.backgroundcamera.R;
import com.nepdeveloper.backgroundcamera.utility.Constant;
import com.nepdeveloper.backgroundcamera.utility.NewMessageNotification;

public class AgreementActivity extends AppCompatActivity {

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agreement);


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)
                && !getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            NewMessageNotification.notify(this, "Sorry, this app won't work without camera and microphone", NewMessageNotification.ERROR);
            finish();
            return;
        }

        preferences = getSharedPreferences(Constant.PREFERENCE_NAME, MODE_PRIVATE);

        if (preferences.getInt(Constant.PREFERENCE_VERSION, 0) != Constant.PREFERENCE_VERSION_CODE) {
            boolean agreementAccepted = preferences.getBoolean(Constant.AGREEMENT_ACCEPTED, false);
            preferences.edit().clear().apply();
            preferences.edit().putInt(Constant.PREFERENCE_VERSION, Constant.PREFERENCE_VERSION_CODE).apply();
            preferences.edit().putBoolean(Constant.AGREEMENT_ACCEPTED, agreementAccepted).apply();
        }

        if (!preferences.getBoolean(Constant.AGREEMENT_ACCEPTED, false)) {
            findViewById(R.id.accept).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(AgreementActivity.this, MainActivity.class));
                    preferences.edit().putBoolean(Constant.AGREEMENT_ACCEPTED, true).apply();
                    finish();
                }
            });
            findViewById(R.id.decline).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        } else {
            findViewById(R.id.agreement_layout).setVisibility(View.GONE);
            startActivity(new Intent(AgreementActivity.this, MainActivity.class));
            finish();
        }
    }
}
