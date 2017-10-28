package com.nepdeveloper.backgroundcamera.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.nepdeveloper.backgroundcamera.utility.Log;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.nepdeveloper.backgroundcamera.R;
import com.nepdeveloper.backgroundcamera.utility.Constant;
import com.nepdeveloper.backgroundcamera.utility.Util;

import android.widget.RelativeLayout;

public class MediaPlayer extends AppCompatActivity {

    private VideoView videoView;
    private ImageButton playBtn;
    private SeekBar seekBar;
    private TextView seekTime;
    private TextView totalTime;
    private ProgressBar progressBar;
    private RelativeLayout videoController;

    private GestureDetector gestureDetector;

    private Runnable runnable;
    private Handler handler;

    private Runnable goneVisibility;

    private boolean isVideo;

    private InterstitialAd mInterstitialAd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.adUnitId));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        if (getActionBar() != null) {
            getActionBar().setDisplayShowHomeEnabled(true);
        }

        final String videoViewPath;
        try {
            videoViewPath = getIntent().getStringExtra(Constant.FILE_PATH_NAME);
            if (videoViewPath == null) {
                Log.i("biky", "video/audio file is null");
                finish();
                return;
            }
            isVideo = videoViewPath.endsWith(Constant.VIDEO_FILE_EXTENSION);
        } catch (Exception e) {
            Log.i("biky", "error " + e.getMessage());
            finish();
            return;
        }

        videoView = findViewById(R.id.video);
        playBtn = findViewById(R.id.play_btn);
        seekBar = findViewById(R.id.seek_time_bar);
        seekTime = findViewById(R.id.seek_time);
        totalTime = findViewById(R.id.total_time);
        progressBar = findViewById(R.id.progress);

        videoController = findViewById(R.id.videocontroller);

        videoController.setVisibility(View.GONE);

        goneVisibility = new Runnable() {
            @Override
            public void run() {
                if (isVideo) videoController.setVisibility(View.GONE);
            }
        };

        gestureDetector = new GestureDetector(this, new GestureListener());

        handler = new Handler();

        try {
            setupMediaPlayer(videoViewPath);
        } catch (Exception e) {
            Toast.makeText(this, "Sorry, media can't be played.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupMediaPlayer(String videoViewPath) throws Exception {
        videoView.setVideoPath(videoViewPath);

        runnable = new Runnable() {
            @Override
            public void run() {
                seekTime.setText(Util.getSeekTime(videoView.getCurrentPosition()));
                seekBar.setProgress(videoView.getCurrentPosition());
                handler.postDelayed(this, 100);
            }
        };

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handler.removeCallbacks(goneVisibility);
                if (videoView.isPlaying()) {
                    videoView.pause();
                    handler.removeCallbacks(runnable);
                    videoController.setVisibility(View.VISIBLE);
                    //noinspection deprecation
                    playBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));

                    if (mInterstitialAd.isLoaded()) {
                        mInterstitialAd.show();
                    } else {
                        Log.d("TAG", "The interstitial wasn't loaded yet.");
                    }
                } else {
                    videoView.start();
                    handler.postDelayed(goneVisibility, 2000);
                    handler.post(runnable);
                    //noinspection deprecation
                    playBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
                }
            }
        });

        videoView.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(android.media.MediaPlayer mediaPlayer) {
                AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                //if video is playing without no sound user would know that volume is zero but incase of audio playback we need to implicitly increase volume to 3rd quarter level
                if (audio != null) {
                    if (isVideo && audio.getStreamVolume(AudioManager.STREAM_MUSIC) <= audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 4) {
                        Toast.makeText(MediaPlayer.this, "Turn up volume", Toast.LENGTH_SHORT).show();
                    } else if (!isVideo && audio.getStreamVolume(AudioManager.STREAM_MUSIC) < audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 3 / 4) {
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 3 / 4, 0);
                    }
                }

                totalTime.setText(Util.getSeekTime(videoView.getDuration()));
                seekTime.setText(Util.getSeekTime(videoView.getCurrentPosition()));

                seekBar.setMax(videoView.getDuration());
                seekBar.setProgress(videoView.getCurrentPosition());

                progressBar.setVisibility(View.GONE);

                videoController.setVisibility(View.VISIBLE);

                videoView.start();
                handler.post(runnable);

                handler.postDelayed(goneVisibility, 500);
            }
        });

        videoView.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener()

        {
            @Override
            public void onCompletion(android.media.MediaPlayer mediaPlayer) {
                //noinspection deprecation
                playBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
                videoView.seekTo(100);
                seekBar.setProgress(videoView.getCurrentPosition());
                seekTime.setText(Util.getSeekTime(videoView.getCurrentPosition()));
                handler.removeCallbacks(runnable);
                handler.removeCallbacks(goneVisibility);
                videoController.setVisibility(View.VISIBLE);
                finish();
            }
        });

        videoView.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener()

        {
            @Override
            public boolean onError(android.media.MediaPlayer mediaPlayer, int i, int i1) {
                Log.i("biky", "play back error ");
                finish();
                return true;
            }
        });

        videoView.setOnTouchListener(new View.OnTouchListener()

        {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent);
            }
        });


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()

        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean fromUser) {
                if (fromUser) {
                    seekTime.setText(Util.getSeekTime(videoView.getCurrentPosition()));
                    videoView.seekTo(position);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(goneVisibility);
                videoController.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (videoView.isPlaying()) {
                    handler.postDelayed(goneVisibility, 2000);
                }
            }
        });
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.i("biky", "on single tap confirmed");
            handler.removeCallbacks(goneVisibility);
            if (videoController.getVisibility() == View.VISIBLE) {
                if (isVideo) videoController.setVisibility(View.GONE);
            } else {
                videoController.setVisibility(View.VISIBLE);
                if (videoView.isPlaying()) {
                    handler.postDelayed(goneVisibility, 2000);
                }
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            /*  Log.i("biky","x1,y1="+e1.getX()+","+e1.getY()+" "+"x2,y2="+e2.getX()+","+e2.getY());
            float delx = e1.getX() - e2.getX();
            float dely = e1.getY() - e2.getY();
            double dist = Math.sqrt(delx * delx + dely * dely);

            Log.i("biky", "cal dist=" + delx + " actual dist = " + distanceX);*/
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(runnable);
        handler.removeCallbacks(goneVisibility);
        super.onDestroy();
    }
}
