package com.nepdeveloper.backgroundcamera.utility;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AlertDialog;

public class AppRater {

    private final static int DAYS_UNTIL_PROMPT = 3;//Min number of days
    private final static int LAUNCHES_UNTIL_PROMPT = 3;//Min number of launches
    private static final String DONT_SHOW_AGAIN = "DONT_SHOW_AGAIN";
    private static final String LAUNCH_COUNT = "LAUNCH_COUNT";
    private static final String FIRST_LAUNCH_DATE = "FIRST_LAUNCH_DATE";

    public static void appLaunched(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constant.PREFERENCE_NAME, Context.MODE_PRIVATE);
        if (preferences.getBoolean(DONT_SHOW_AGAIN, false)) {
            return;
        }

        SharedPreferences.Editor editor = preferences.edit();

        // Increment launch counter
        long launch_count = preferences.getLong(LAUNCH_COUNT, 0) + 1;
        editor.putLong(LAUNCH_COUNT, launch_count);

        // Get date of first launch
        Long date_firstLaunch = preferences.getLong(FIRST_LAUNCH_DATE, 0);

        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong(FIRST_LAUNCH_DATE, date_firstLaunch);
        }

        // Wait at least n days before opening
        if (launch_count >= LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= date_firstLaunch +
                    (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                showRateDialog(context, editor);
            }
        }

        editor.apply();
    }

    private static void showRateDialog(final Context context, final SharedPreferences.Editor editor) {
        new AlertDialog.Builder(context)
                .setTitle("Rate this app")
                .setMessage("If you enjoy using this app, Please rate it.")
                .setPositiveButton("RATE IT", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        openPlayStore(context);
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton("LATER", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setNeutralButton("DON'T SHOW AGAIN", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        editor.putBoolean(DONT_SHOW_AGAIN, true).apply();
                    }
                })
                .setCancelable(false)
                .create()
                .show();
    }

    public static void openPlayStore(Context context) {
        Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            context.startActivity(goToMarket);
        } catch (Exception e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
        }
    }
}