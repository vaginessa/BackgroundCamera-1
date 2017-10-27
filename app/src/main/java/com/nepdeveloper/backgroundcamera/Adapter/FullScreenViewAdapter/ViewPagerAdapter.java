package com.nepdeveloper.backgroundcamera.Adapter.FullScreenViewAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nepdeveloper.backgroundcamera.Activity.MediaPlayer;
import com.nepdeveloper.backgroundcamera.R;
import com.nepdeveloper.backgroundcamera.Utility.Constant;
import com.nepdeveloper.backgroundcamera.Utility.TouchImageView;
import com.nepdeveloper.backgroundcamera.Utility.Util;

import java.io.File;
import java.util.ArrayList;


public class ViewPagerAdapter extends PagerAdapter {
    private ArrayList<File> files;
    private Context context;

    public ViewPagerAdapter(Context context, ArrayList<File> files) {
        this.context = context;
        this.files = files;
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        FrameLayout frameLayout = (FrameLayout) object;
        return view == frameLayout;
    }

    @Override
    public Object instantiateItem(final ViewGroup container, final int position) {
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();

        View view = inflater.inflate(R.layout.single_view, container,
                false);

        TouchImageView image = view.findViewById(R.id.image);

        if (files.get(position).getAbsolutePath().endsWith(Constant.IMAGE_FILE_EXTENSION)) {
            Glide.with(context).load(files.get(position)).into(image);
            view.findViewById(R.id.overlay).setVisibility(View.GONE);

        } else if (files.get(position).getAbsolutePath().endsWith(Constant.AUDIO_FILE_EXTENSION)
                || files.get(position).getAbsolutePath().endsWith(Constant.VIDEO_FILE_EXTENSION)) {

            view.findViewById(R.id.overlay).setVisibility(View.VISIBLE);

            if (files.get(position).getAbsolutePath().endsWith(Constant.AUDIO_FILE_EXTENSION)) {
                image.setImageResource(R.color.bpblack);
                Glide.with(context).load(R.drawable.ic_mic).into(((ImageView) view.findViewById(R.id.small_image)));
            } else {
                Glide.with(context).load(files.get(position)).into(image);
                Glide.with(context).load(R.drawable.ic_video_camera).into(((ImageView) view.findViewById(R.id.small_image)));
            }

            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(context, Uri.fromFile(files.get(position)));
                String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

                retriever.release();
                view.findViewById(R.id.media_len).setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.media_len)).setText(Util.getSeekTime(Integer.parseInt(time)));
            } catch (Exception e) {
                e.printStackTrace();
                view.findViewById(R.id.media_len).setVisibility(View.GONE);
            }

            view.findViewById(R.id.overlay).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    context.startActivity(new Intent(context, MediaPlayer.class).putExtra(Constant.FILE_PATH_NAME, files.get(position).getPath()));
                }
            });
        }
        ViewPager pager = (ViewPager) container;
        pager.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ViewPager pager = (ViewPager) container;
        pager.removeView((FrameLayout) object);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}
