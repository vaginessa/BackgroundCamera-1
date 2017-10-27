package com.nepdeveloper.backgroundcamera.Adapter;

import android.app.Activity;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.nepdeveloper.backgroundcamera.Utility.Log;

import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nepdeveloper.backgroundcamera.R;
import com.nepdeveloper.backgroundcamera.Utility.Constant;
import com.nepdeveloper.backgroundcamera.Utility.SquareImageView;
import com.nepdeveloper.backgroundcamera.Utility.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class GridViewAdapter extends ArrayAdapter<File> {
    private Context context;
    private int layoutResourceId;
    private ArrayList<File> files = new ArrayList<>();
    private SparseBooleanArray selectedPositions;

    public GridViewAdapter(Context context, int layoutResourceId, ArrayList<File> files) {
        super(context, layoutResourceId, files);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.files = files;
        selectedPositions = new SparseBooleanArray();
    }

    @Override
    @NonNull
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        View row = convertView;
        ViewHolder holder;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new ViewHolder();

            holder.image = row.findViewById(R.id.img);
            holder.smallImage = row.findViewById(R.id.small_image);
            holder.mediaLength = row.findViewById(R.id.media_len);
            holder.mediaSize = row.findViewById(R.id.media_size);

            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        double sizeInKB = files.get(position).length() / 1024f;

        if (sizeInKB > 1024) {
            holder.mediaSize.setText(String.format(Locale.ENGLISH,
                    "%.1f MB", sizeInKB / 1024f));
        } else {
            holder.mediaSize.setText(String.format(Locale.ENGLISH,
                    "%.0f KB", sizeInKB));
        }

        if (files.get(position).getName().endsWith(Constant.VIDEO_FILE_EXTENSION)
                || files.get(position).getName().endsWith(Constant.AUDIO_FILE_EXTENSION)) {

            if (files.get(position).getName().endsWith(Constant.VIDEO_FILE_EXTENSION)) {
                Glide.with(context).load(R.drawable.ic_video_camera).into(holder.smallImage);
                Glide.with(context).load(files.get(position)).error(R.drawable.ic_error_placeholder_white).into(holder.image);
            } else {
                Glide.with(context).load(R.drawable.ic_mic).into(holder.smallImage);
                holder.image.setImageResource(R.color.bpblack);
            }

            holder.smallImage.setVisibility(View.VISIBLE);

            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(context, Uri.fromFile(files.get(position)));
                String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                retriever.release();


                holder.mediaLength.setText(Util.getSeekTime(Integer.parseInt(time)));
                holder.mediaLength.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
                holder.mediaLength.setVisibility(View.GONE);
            }

        } else if (files.get(position).getName().endsWith(Constant.IMAGE_FILE_EXTENSION)) {
            Glide.with(context).load(files.get(position)).error(R.drawable.ic_error_placeholder_white).into(holder.image);

            holder.mediaLength.setVisibility(View.GONE);
            holder.smallImage.setVisibility(View.GONE);
        }

        return row;
    }

    private static class ViewHolder {
        SquareImageView image;
        ImageView smallImage;
        TextView mediaLength;
        TextView mediaSize;
    }

    @Nullable
    @Override
    public File getItem(int position) {
        if (position >= 0 && position < files.size()) {
            return files.get(position);
        } else {
            return null;
        }
    }

    @Override
    public void remove(File file) {
        if (file != null && file.delete()) {
            files.remove(file);
            notifyDataSetChanged();
        }
    }

    public void delete(File file) {
        if (file != null) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            files.remove(file);
            notifyDataSetChanged();
        }
    }


    public void removeSelection() {
        selectedPositions = new SparseBooleanArray();
        notifyDataSetChanged();
    }


    public void selectView(int position, boolean value) {
        if (value) {
            selectedPositions.put(position, true);
        } else {
            selectedPositions.delete(position);
        }
        notifyDataSetChanged();
    }


    public int getSelectedCount() {
        return selectedPositions.size();
    }

    public SparseBooleanArray getSelectedPositions() {
        return selectedPositions;
    }
}