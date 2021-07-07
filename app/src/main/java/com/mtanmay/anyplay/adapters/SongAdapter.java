package com.mtanmay.anyplay.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.mtanmay.anyplay.R;
import com.mtanmay.anyplay.song.Song;
import com.mtanmay.anyplay.ui.library.LibraryFragment;
import com.mtanmay.anyplay.utils.EncryptDecryptUtils;
import com.mtanmay.anyplay.utils.FileUtils;
import com.mtanmay.anyplay.utils.NetworkUtils;
import com.mtanmay.anyplay.utils.ToastUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import static com.mtanmay.anyplay.Constants.AES;
import static com.mtanmay.anyplay.Constants.DIR_CACHE;
import static com.mtanmay.anyplay.Constants.DIR_IMAGES;

public class SongAdapter extends ListAdapter<Song, SongAdapter.ViewHolder> {

    private final Context mContext;
    private OnItemClickListener listener;

    public SongAdapter(Context context) {
        super(DIFF_CALLBACK);
        this.mContext = context;
    }

    private static final DiffUtil.ItemCallback<Song> DIFF_CALLBACK = new DiffUtil.ItemCallback<Song>() {

        @Override
        public boolean areItemsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
            return oldItem.getSongId().equals(newItem.getSongId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
            return (oldItem.getCName().equals(newItem.getCName()) &&
                    oldItem.getArtist().equals(newItem.getArtist()) &&
                    oldItem.getThumbnailUrl().equals(newItem.getThumbnailUrl()));
        }
    };

    public Song getSongAt(int position) {
        return getItem(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View songView = LayoutInflater.from(mContext)
                                      .inflate(R.layout.song_item, parent, false);

        return new ViewHolder(songView);
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Song currentSong = getItem(position);

        holder.songName.setText(currentSong.getCName());
        holder.artistName.setText(currentSong.getArtist());

        // loading the image storage
        File imgFile = FileUtils.getFile(mContext, DIR_IMAGES, currentSong.getSongId());
        if(imgFile != null && imgFile.exists()) {

            try {
                byte[] encryptedImgBytes = FileUtils.readFile(imgFile);
                byte[] secretKeyBytes = Base64.decode(currentSong.getSecretKey(), Base64.NO_WRAP);
                SecretKey secretKey = new SecretKeySpec(secretKeyBytes, 0, secretKeyBytes.length, AES);
                byte[] decryptedImgBytes = EncryptDecryptUtils.decode(secretKey, encryptedImgBytes);
                File tempImgFile = FileUtils.getTempFile(mContext, "img", decryptedImgBytes, currentSong.getSongId());
                if(tempImgFile != null) {

                    Glide.with(mContext)
                            .asBitmap()
                            .load(Uri.fromFile(tempImgFile))
                            .placeholder(R.drawable.ic_placeholder_song)
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    Palette palette = Palette.from(resource).generate();
                                    Palette.Swatch swatch = palette.getDominantSwatch();
                                    holder.songThumbnail.setImageBitmap(resource);
                                    if (swatch != null) {
                                        holder.songThumbnail.setBackgroundColor(swatch.getRgb());
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) { }
                            });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {

            // re-downloading image in background if image file is not found
            if(NetworkUtils.isConnected(mContext)) {
                Glide.with(mContext)
                        .asBitmap()
                        .load(currentSong.getThumbnailUrl())
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                                File imgFile = FileUtils.getFile(mContext, DIR_CACHE, "temp.png");
                                try {
                                    OutputStream op = new FileOutputStream(imgFile);
                                    resource.compress(Bitmap.CompressFormat.PNG, 100, op);
                                    op.close();

                                    byte[] imgFileBytes = FileUtils.readFile(imgFile);
                                    byte[] imgData = EncryptDecryptUtils.encode(Base64.decode(currentSong.getSecretKey(), Base64.NO_WRAP), imgFileBytes);
                                    boolean saved = FileUtils.saveFile(mContext, DIR_IMAGES, imgData, currentSong.getSongId());
                                    if (!saved)
                                        ToastUtils.create(mContext, "Unable to download image");

                                    if (LibraryFragment.adapter.get() != null)
                                        LibraryFragment.adapter.get().notifyDataSetChanged();

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {}
                        });
            }
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView songName, artistName;
        public ImageView songThumbnail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.songName = itemView.findViewById(R.id.song_name);
            this.artistName = itemView.findViewById(R.id.artist_name);
            this.songThumbnail = itemView.findViewById(R.id.song_thumbnail);

            itemView.setOnClickListener(v -> listener.onItemClick(getItem(getPosition())));

        }
    }

    public interface OnItemClickListener {
        void onItemClick(Song song);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}
