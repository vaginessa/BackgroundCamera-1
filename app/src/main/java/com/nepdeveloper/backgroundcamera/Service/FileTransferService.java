
package com.nepdeveloper.backgroundcamera.Service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import com.nepdeveloper.backgroundcamera.Utility.Log;

import com.nepdeveloper.backgroundcamera.R;
import com.nepdeveloper.backgroundcamera.Utility.Constant;
import com.nepdeveloper.backgroundcamera.Utility.NewMessageNotification;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

public class FileTransferService extends Service {
    private Notification notification;

    public FileTransferService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("biky", "on start command file transfer service");

        if (intent != null && Constant.ACTION_STOP_SELF.equals(intent.getAction())) {
            Log.i("biky", "stopped from notification");
            stopSelf();
            return START_NOT_STICKY;
        }

        SharedPreferences preferences = getSharedPreferences(Constant.PREFERENCE_NAME, MODE_PRIVATE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            notification = builder.build();
            startForeground(Constant.NOTIFICATION_ID_TRANSFERRING_FILES, notification);
        } else {
            Notification.Builder builder = new Notification.Builder(this, NewMessageNotification.CHANNEL_ID);
            notification = builder.build();
            startForeground(Constant.NOTIFICATION_ID_TRANSFERRING_FILES, notification);
        }
/*TODO
        String srcDir = preferences.getString(Constant.STORAGE_LOCATION_OLD, Constant.FILE.getAbsolutePath());
        String desDir = preferences.getString(Constant.STORAGE_LOCATION, Constant.FILE.getAbsolutePath());
*/
        String srcDir = preferences.getString(Constant.STORAGE_LOCATION, Constant.FILE.getAbsolutePath());
        String desDir = Constant.FILE.getAbsolutePath();

        if (srcDir.equals(desDir)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        new FileTransfer().execute(srcDir, desDir);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        Log.i("biky", "on destroy file transfer service");
        super.onDestroy();
    }

    private class FileTransfer extends AsyncTask<String, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            NewMessageNotification.cancel(FileTransferService.this);
        }

        protected Integer doInBackground(String... strings) {
            File srcDir = new File(strings[0]);
            File desDir = new File(strings[1]);

            File[] srcFiles = srcDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().endsWith(Constant.IMAGE_FILE_EXTENSION)
                            || file.getName().endsWith(Constant.VIDEO_FILE_EXTENSION)
                            || file.getName().endsWith(Constant.AUDIO_FILE_EXTENSION);
                }
            });

            if (srcFiles == null) {
                return 0;
            }

            int totalFiles = srcFiles.length;

            if (totalFiles == 0) {
                return 0;
            }

            int totalFilesMoved = 0;

            for (int i = 0; i < totalFiles; i++) {
                try {
                    FileUtils.moveFileToDirectory(srcFiles[i], desDir, true);
                    publishProgress(totalFiles, i + 1);
                    totalFilesMoved++;
                } catch (FileExistsException e) {
                    e.printStackTrace();
                    try {
                        FileUtils.forceDelete(srcFiles[i]);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } catch (Exception e) {
                    Log.i("biky", e.getMessage());
                    return -1;
                }
            }
            return totalFilesMoved;
        }

        protected void onProgressUpdate(Integer... progress) {
            setProgressPercent(progress[0], progress[1]);
        }

        @Override
        protected void onPostExecute(Integer totalFiles) {
            if (totalFiles == -1) {
                NewMessageNotification.notify(FileTransferService.this, "Error in moving files to new storage location", NewMessageNotification.ERROR);
            } else if (totalFiles > 0) {
                NewMessageNotification.notify(FileTransferService.this, totalFiles + (totalFiles == 1 ? " file" : " files") + " moved to new storage location", NewMessageNotification.TRANSFER_COMPLETE);
            }
            stopSelf();
        }
    }


    private void setProgressPercent(Integer max, Integer progress) {

        NotificationManager manager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));

        String text = "Moving files to new storage location";
        String descrip = String.format(Locale.ENGLISH, "Moved %1$d/%2$d file%3$s",
                progress, max, progress > 1 ? "s" : "");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(FileTransferService.this)
                    .setDefaults(0)
                    .setSound(null)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(text)
                    .setProgress(max, progress, true)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(descrip)
                            .setBigContentTitle(getString(R.string.app_name))
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            builder.setSmallIcon(R.drawable.ic_stat);
            builder.setTicker(text);
            notification = builder.build();

            manager.notify(Constant.NOTIFICATION_ID_TRANSFERRING_FILES, notification);
        } else {
            Notification.Builder builder = new Notification.Builder(FileTransferService.this, NewMessageNotification.CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(text)
                    .setProgress(max, progress, true)
                    .setStyle(new Notification.BigTextStyle()
                            .bigText(descrip)
                            .setBigContentTitle(getString(R.string.app_name))
                    )
                    .setAutoCancel(true);

            builder.setSmallIcon(R.drawable.ic_stat);
            builder.setTicker(text);

            notification = builder.build();

            manager.notify(Constant.NOTIFICATION_ID_TRANSFERRING_FILES, notification);
        }
    }
}
