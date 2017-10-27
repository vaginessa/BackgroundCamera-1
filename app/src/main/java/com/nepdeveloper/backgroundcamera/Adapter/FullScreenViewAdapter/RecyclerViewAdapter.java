package com.nepdeveloper.backgroundcamera.Adapter.FullScreenViewAdapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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

/**
 * An array adapter that knows how to render views when given File classes
 */
public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {

    private Context context;
    private ArrayList<File> files;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private SparseBooleanArray selectedPositions;


    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemCLickListener) {
        this.onItemClickListener = onItemCLickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongCLickListener) {
        this.onItemLongClickListener = onItemLongCLickListener;
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private SquareImageView image;
        private ImageView smallImage;
        private TextView mediaSize;
        private TextView mediaLength;

        MyViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.img);
            smallImage = itemView.findViewById(R.id.small_image);
            mediaSize = itemView.findViewById(R.id.media_size);
            mediaLength = itemView.findViewById(R.id.media_len);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onItemClickListener != null)
                        onItemClickListener.onItemClick(view, getAdapterPosition());
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    return onItemLongClickListener != null && onItemLongClickListener.onItemLongClick(view, getAdapterPosition());
                }
            });
        }
    }

    public RecyclerViewAdapter(Context context, ArrayList<File> files) {
        this.context = context;
        this.files = files;
        selectedPositions = new SparseBooleanArray();

    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_grid, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        double sizeInKB = files.get(position).length() / 1024f;

        if (sizeInKB > 1024) {
            holder.mediaSize.setText(String.format(Locale.ENGLISH,
                    "%.1f MB", sizeInKB / 1024f));
        } else {
            holder.mediaSize.setText(String.format(Locale.ENGLISH,
                    "%.0f KB", sizeInKB));
        }

        if (files.get(position).getName().endsWith(Constant.VIDEO_FILE_EXTENSION) ||
                files.get(position).getName().endsWith(Constant.AUDIO_FILE_EXTENSION)) {

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
            holder.smallImage.setVisibility(View.GONE);
            holder.mediaLength.setVisibility(View.GONE);
        }
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(metrics.widthPixels / 4, metrics.widthPixels / 4);

        holder.itemView.setLayoutParams(layoutParams);

        int dps = Util.getPixels(context, 1);

        holder.itemView.setPadding(dps, dps, dps, dps);

        holder.itemView.setTag(files.get(position));

        FrameLayout frameLayout = ((FrameLayout) holder.itemView);
        if (selectedPositions.get(position)) {
            holder.itemView.setSelected(true);
            frameLayout.setForeground(new ColorDrawable(ContextCompat.getColor(context, R.color.whiteSelected)));
        } else {
            holder.itemView.setSelected(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                frameLayout.setForeground(context.getDrawable(R.drawable.selector));
            } else {
                //noinspection deprecation
                frameLayout.setForeground(context.getResources().getDrawable(R.drawable.selector));
            }
        }
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public File getItem(int position) {
        if (position >= 0 && position < files.size()) {
            return files.get(position);
        } else {
            return null;
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

    public void removeSelection() {
        selectedPositions = new SparseBooleanArray();
        notifyDataSetChanged();
    }


}