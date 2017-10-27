package com.nepdeveloper.backgroundcamera.Utility;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.nepdeveloper.backgroundcamera.Activity.AgreementActivity;
import com.nepdeveloper.backgroundcamera.Activity.GalleryGrid;
import com.nepdeveloper.backgroundcamera.R;

import java.util.Date;
import java.util.Locale;

public class NewMessageNotification {

    private static final String NOTIFICATION_TAG = "Notification";
    public static final int TRANSFER_COMPLETE = 0;
    public static final int IMAGE_CAPTURED = 1;
    public static final int ERROR = 2;
    public static final int RECORDING_COMPLETE = 4;
    public static final int PERMISSION_DENIED = 5;
    private static final CharSequence CHANNEL_NAME = "Background Recorder";
    public static final String CHANNEL_ID = "com.nepdeveloper.backgroundcamera";

    public static void notify(final Context context,
                              final String text, int flag) {

        if (!context.getSharedPreferences(Constant.PREFERENCE_NAME, Context.MODE_PRIVATE).getBoolean(Constant.SHOW_NOTIFICATION, true)
                && flag != ERROR) {
            return;
        }
        cancel(context);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setDefaults(0)
                    .setSound(null)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setTicker(text)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(text)
                            .setBigContentTitle(context.getString(R.string.app_name))
                            .setSummaryText(new java.text.SimpleDateFormat("h:mm:ss a", Locale.ENGLISH).format(new Date()))
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);
            switch (flag) {
                case NewMessageNotification.IMAGE_CAPTURED:
                    builder.setContentIntent(PendingIntent.getActivity(context, 0,
                            new Intent(context, GalleryGrid.class).putExtra(Constant.FROM_NOTIFICATION, true), PendingIntent.FLAG_CANCEL_CURRENT));
                    builder.setOngoing(false);
                    break;
                case NewMessageNotification.TRANSFER_COMPLETE:
                    builder.setContentIntent(PendingIntent.getActivity(context, 0,
                            new Intent(context, GalleryGrid.class).putExtra(Constant.FROM_NOTIFICATION, true), PendingIntent.FLAG_CANCEL_CURRENT));
                    builder.setOngoing(false);
                    break;
                case NewMessageNotification.PERMISSION_DENIED:
                    builder.setContentIntent(PendingIntent.getActivity(context, 0,
                            new Intent(context, AgreementActivity.class), PendingIntent.FLAG_CANCEL_CURRENT));
                    builder.setOngoing(false);
                    break;
                case NewMessageNotification.RECORDING_COMPLETE:
                    builder.setContentIntent(PendingIntent.getActivity(context, 0,
                            new Intent(context, GalleryGrid.class).putExtra(Constant.FROM_NOTIFICATION, true), PendingIntent.FLAG_CANCEL_CURRENT));
                    builder.setOngoing(false);
                    break;
            }
            notify(context, builder.build());
        } else {
            Notification.Builder builder = new Notification.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setTicker(text)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(text)
                    .setStyle(new Notification.BigTextStyle()
                            .bigText(text)
                            .setBigContentTitle(context.getString(R.string.app_name))
                            .setSummaryText(new java.text.SimpleDateFormat("h:mm:ss a", Locale.ENGLISH).format(new Date()))
                    )
                    .setAutoCancel(true);
            switch (flag) {
                case NewMessageNotification.IMAGE_CAPTURED:
                    builder.setContentIntent(PendingIntent.getActivity(context, 0,
                            new Intent(context, GalleryGrid.class).putExtra(Constant.FROM_NOTIFICATION, true), PendingIntent.FLAG_CANCEL_CURRENT));
                    builder.setOngoing(false);
                    break;
                case NewMessageNotification.PERMISSION_DENIED:
                    builder.setContentIntent(PendingIntent.getActivity(context, 0,
                            new Intent(context, AgreementActivity.class), PendingIntent.FLAG_CANCEL_CURRENT));
                    builder.setOngoing(false);
                    break;
                case NewMessageNotification.RECORDING_COMPLETE:
                    builder.setContentIntent(PendingIntent.getActivity(context, 0,
                            new Intent(context, GalleryGrid.class).putExtra(Constant.FROM_NOTIFICATION, true), PendingIntent.FLAG_CANCEL_CURRENT));
                    builder.setOngoing(false);
                    break;
            }
            notify(context, builder.build());
        }
    }


    private static void notify(final Context context, final Notification notification) {
        final NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        // create android channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel androidChannel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);

            androidChannel.enableLights(false);
            // Sets whether notification posted to this channel should vibrate.
            androidChannel.enableVibration(false);
            // Sets the notification light color for notifications posted to this channel
            androidChannel.setLightColor(Color.GREEN);
            // Sets whether notifications posted to this channel appear on the lockscreen or not
            androidChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            // Sets whether notifications posted to this channel should display notification lights
            nm.createNotificationChannel(androidChannel);
        }

        nm.notify(1, notification);
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    public static void cancel(final Context context) {
        final NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_TAG, 1);
    }
}
