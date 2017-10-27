package com.nepdeveloper.backgroundcamera.Activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.nepdeveloper.backgroundcamera.Utility.Log;

import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.nepdeveloper.backgroundcamera.Adapter.FullScreenViewAdapter.RecyclerViewAdapter;
import com.nepdeveloper.backgroundcamera.Adapter.FullScreenViewAdapter.ViewPagerAdapter;
import com.nepdeveloper.backgroundcamera.R;
import com.nepdeveloper.backgroundcamera.Service.AudioRecorderService;
import com.nepdeveloper.backgroundcamera.Service.VideoRecorderService;
import com.nepdeveloper.backgroundcamera.Utility.Constant;
import com.nepdeveloper.backgroundcamera.Utility.Util;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class GalleryFullscreen extends AppCompatActivity {

    private ArrayList<File> files = new ArrayList<>();
    private ViewPagerAdapter viewPagerAdapter;
    private boolean selectionMode = false;
    private RecyclerViewAdapter recyclerViewAdapter;
    private ViewPager viewPager;

    private RecyclerView recyclerView;
    private Runnable runnable;
    private Handler handler;

    private AdView adView;
    private int height;
    private int current = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_fullscreen);

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

        handler = new Handler();

        viewPager = (ViewPager) findViewById(R.id.view_pager);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        final int currentItem = getIntent().getIntExtra(Constant.CURRENT_FILE_SELECTION, 0);
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
                } else if (!Util.isMyServiceRunning(this, VideoRecorderService.class)
                        && !Util.isMyServiceRunning(this, AudioRecorderService.class)) {
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
                Toast.makeText(GalleryFullscreen.this, "No image or video found in app folder", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(GalleryFullscreen.this, "No image or video found", Toast.LENGTH_SHORT).show();
            finish();
        }

        viewPagerAdapter = new ViewPagerAdapter(this, files);

        viewPager.setAdapter(viewPagerAdapter);

        viewPager.setCurrentItem(currentItem);
        if (currentItem >= 0 && currentItem < files.size()) {
            ((TextView) findViewById(R.id.file_info)).setText(Util.getFileInfo(files.get(currentItem)));
        }

        recyclerViewAdapter = new RecyclerViewAdapter(this, files);
        recyclerView.setAdapter(recyclerViewAdapter);

        layoutManager.scrollToPositionWithOffset((currentItem / 4) * 4, 0);

        recyclerViewAdapter.setOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (selectionMode) {

                    findViewById(R.id.delete).setVisibility(View.VISIBLE);
                    findViewById(R.id.share).setVisibility(View.GONE);

                    recyclerViewAdapter.selectView(position, !view.isSelected());

                    if (recyclerViewAdapter.getSelectedCount() == 0) {
                        selectionMode = false;
                        recyclerViewAdapter.removeSelection();
                        findViewById(R.id.delete).setVisibility(View.GONE);
                        findViewById(R.id.share).setVisibility(View.VISIBLE);
                    }
                }
                viewPager.setCurrentItem(position, true);
            }
        });

        recyclerViewAdapter.setOnItemLongClickListener(new RecyclerViewAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(View view, int position) {
                handler.removeCallbacks(runnable);
                recyclerView.setVisibility(View.VISIBLE);

                selectionMode = true;

                findViewById(R.id.delete).setVisibility(View.VISIBLE);
                findViewById(R.id.share).setVisibility(View.GONE);

                recyclerViewAdapter.selectView(position, !view.isSelected());

                if (recyclerViewAdapter.getSelectedCount() == 0) {
                    selectionMode = false;
                    recyclerViewAdapter.removeSelection();
                    findViewById(R.id.delete).setVisibility(View.GONE);
                    findViewById(R.id.share).setVisibility(View.VISIBLE);
                }

                //  Log.i("biky", "on item long click");
                return true;
            }
        });
        runnable = new Runnable() {
            @Override
            public void run() {
                if (!selectionMode) {
                    recyclerView.setVisibility(View.GONE);
                }
            }
        };

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                handler.removeCallbacks(runnable);
                recyclerView.setVisibility(View.VISIBLE);

                handler.postDelayed(runnable, 1000);

                Log.i("biky", "on page scrolled");
            }

            @Override
            public void onPageSelected(int position) {
                current = position;
                if (position >= 0 && position < files.size()) {
                    ((TextView) findViewById(R.id.file_info)).setText(Util.getFileInfo(files.get(position)));
                }
                Log.i("biky", "on page selected");
                layoutManager.scrollToPositionWithOffset((position / 4) * 4, 0);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Log.i("biky", "on page scroll state changed");
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                handler.removeCallbacks(runnable);
                recyclerView.setVisibility(View.VISIBLE);
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        registerReceiver(receiver, new IntentFilter(Constant.NEW_FILE_CREATED));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        unregisterReceiver(receiver);
    }


    @Override
    public void onBackPressed() {
        if (recyclerViewAdapter == null) return;
        if (selectionMode) {
            selectionMode = false;
            recyclerViewAdapter.removeSelection();
            findViewById(R.id.delete).setVisibility(View.GONE);
            findViewById(R.id.share).setVisibility(View.VISIBLE);
        } else {
            sendResultToCaller();
            super.onBackPressed();
        }
    }

    private ArrayList<Integer> positions = new ArrayList<>();

    public void deleteFile(View v) {
        if (recyclerViewAdapter == null) return;

        new AlertDialog.Builder(GalleryFullscreen.this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete selected files?")
                .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int id) {
                        SparseBooleanArray selected = recyclerViewAdapter.getSelectedPositions();
                        for (int i = selected.size() - 1; i >= 0; i--) {
                            if (selected.valueAt(i)) {
                                int position = selected.keyAt(i);
                                File file = recyclerViewAdapter.getItem(position);
                                recyclerViewAdapter.delete(file);
                                positions.add(position);
                            }
                        }

                        viewPagerAdapter.notifyDataSetChanged();
                        recyclerViewAdapter.notifyDataSetChanged();

                        selectionMode = false;

                        recyclerViewAdapter.removeSelection();

                        findViewById(R.id.delete).setVisibility(View.GONE);
                        findViewById(R.id.share).setVisibility(View.VISIBLE);

                        if (recyclerViewAdapter.getItemCount() == 0) {
                            sendResultToCaller();
                        }
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

    }

    private void sendResultToCaller() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(Constant.DELETED_POSITIONS, positions);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
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
                    recyclerViewAdapter.notifyDataSetChanged();
                    viewPagerAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    protected void onResume() {
        if (recyclerView != null) {
            recyclerView.setVisibility(View.VISIBLE);

            handler.postDelayed(runnable, 1000);

        }
        super.onResume();
    }

    public void shareFile(View view) {
        if (current >= 0 && current < files.size()) {
            new AlertDialog.Builder(this)
                    .setTitle("Important message")
                    .setMessage("Because of privacy, after sharing, the recipient must append extension (Eg. .mp4) to file name to read it.")
                    .setPositiveButton("SHARE", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                String sharePath = files.get(current).getAbsolutePath();
                                Uri uri = Uri.parse(sharePath);
                                Intent share = new Intent(Intent.ACTION_SEND);
                                share.putExtra(Intent.EXTRA_STREAM, uri);
                                share.setType("*/*");
                                startActivity(Intent.createChooser(share, "Share File"));
                                dialogInterface.dismiss();
                            } catch (Exception e) {
                                Log.i("biky", e.getMessage());
                                dialogInterface.dismiss();
                            }
                        }
                    })
                    .setNegativeButton("DON'T SHARE", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create()
                    .show();

        }
    }
}
