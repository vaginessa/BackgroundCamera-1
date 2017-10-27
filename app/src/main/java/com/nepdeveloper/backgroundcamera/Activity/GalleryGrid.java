package com.nepdeveloper.backgroundcamera.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.nepdeveloper.backgroundcamera.Utility.Log;

import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.nepdeveloper.backgroundcamera.Adapter.GridViewAdapter;
import com.nepdeveloper.backgroundcamera.R;
import com.nepdeveloper.backgroundcamera.Service.AudioRecorderService;
import com.nepdeveloper.backgroundcamera.Service.VideoRecorderService;
import com.nepdeveloper.backgroundcamera.Utility.Constant;
import com.nepdeveloper.backgroundcamera.Utility.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class GalleryGrid extends AppCompatActivity {
    private GridView gridView;
    public ArrayList<File> files = new ArrayList<>();
    private GridViewAdapter adapter;

    private AdView adView;
    private int height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        if (getActionBar() != null) {
            getActionBar().setDisplayShowHomeEnabled(true);
        }

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


        gridView = (GridView) findViewById(R.id.image_gallery);
/*
        File directory = new File(getSharedPreferences(Constant.PREFERENCE_NAME, MODE_PRIVATE)
                .getString(Constant.STORAGE_LOCATION, Constant.FILE.getAbsolutePath()));
*/
        File directory = Constant.FILE;
        if (directory.isDirectory()) {
            File[] fileList = directory.listFiles();

            for (File file : fileList) {
                if (file.getName().endsWith(Constant.IMAGE_FILE_EXTENSION)
                        || file.getName().endsWith(Constant.VIDEO_FILE_EXTENSION)
                        || file.getName().endsWith(Constant.AUDIO_FILE_EXTENSION)) {
                    files.add(file);
                } else if (!Util.isMyServiceRunning(this, VideoRecorderService.class) &&
                        !Util.isMyServiceRunning(this, AudioRecorderService.class)) {
                    Log.i("biky", "matchless file deleted");
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
            if (!files.isEmpty()) {
                //sort in descending order
                Collections.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return f2.getName().compareTo(f1.getName());
                    }
                });
            } else {
                Toast.makeText(GalleryGrid.this, "No image or video found in app folder", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(GalleryGrid.this, "No image or video found in app folder", Toast.LENGTH_SHORT).show();
            finish();
        }
        adapter = new GridViewAdapter(this, R.layout.single_grid, files);

        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                startActivityForResult(new Intent(GalleryGrid.this,
                        GalleryFullscreen.class).putExtra(Constant.CURRENT_FILE_SELECTION, position), Constant.REQUEST_DELETED_POSITIONS);
            }
        });


        gridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);

        gridView.setMultiChoiceModeListener(new GridView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                adapter.selectView(position, checked);
                mode.setTitle(adapter.getSelectedCount() + " selected");
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.grid_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.delete:
                        new AlertDialog.Builder(GalleryGrid.this)
                                .setTitle("Confirm Delete")
                                .setMessage("Are you sure you want to delete selected files?")
                                .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int id) {
                                        SparseBooleanArray selected = adapter.getSelectedPositions();
                                        for (int i = selected.size() - 1; i >= 0; i--) {
                                            if (selected.valueAt(i)) {
                                                int position = selected.keyAt(i);
                                                File selectedFile = adapter.getItem(position);
                                                adapter.delete(selectedFile);
                                            }
                                        }
                                        adapter.notifyDataSetChanged();
                                        mode.finish();
                                        if (adapter.getCount() == 0) {
                                            finish();
                                        }
                                        dialogInterface.dismiss();
                                    }
                                })
                                .setNegativeButton("DON'T DELETE", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                                .create()
                                .show();
                        return true;
                    case R.id.select_all:
                        if (adapter.getSelectedCount() == adapter.getCount()) {
                            return true;//if all items are selected return
                        }
                        //select all items
                        for (int i = adapter.getCount() - 1; i >= 0; i--) {
                            gridView.setItemChecked(i, true);
                            adapter.selectView(i, true);
                        }
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                adapter.removeSelection();
            }
        });
        registerReceiver(receiver, new IntentFilter(Constant.NEW_FILE_CREATED));
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.NEW_FILE_CREATED.equals(intent.getAction())) {
                File file = new File(intent.getStringExtra(Constant.FILE_PATH_NAME));
                Log.i("biky", "new image captured or video recorded, file path = " + file.getAbsolutePath());
                if (file.getName().endsWith(Constant.IMAGE_FILE_EXTENSION)
                        || file.getName().endsWith(Constant.VIDEO_FILE_EXTENSION)
                        || file.getName().endsWith(Constant.AUDIO_FILE_EXTENSION)) {
                    if (files.contains(file)) {
                        return;
                    }
                    files.add(0, file);
                    adapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("biky", "request code = " + requestCode + " result code = " + resultCode);
        if (requestCode == Constant.REQUEST_DELETED_POSITIONS && resultCode == RESULT_OK) {
            ArrayList<Integer> positions = data.getIntegerArrayListExtra(Constant.DELETED_POSITIONS);
            for (int position : positions) {
                if (position >= 0 && position < files.size()) {
                    adapter.delete(files.get(position));
                }
            }
            if (files.isEmpty()) {
                finish();
            }
            adapter.notifyDataSetChanged();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra(Constant.FROM_NOTIFICATION, false)) {
            switch (item.getItemId()) {
                case android.R.id.home:
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra(Constant.FROM_NOTIFICATION, false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            super.onBackPressed();
        }
    }
}
