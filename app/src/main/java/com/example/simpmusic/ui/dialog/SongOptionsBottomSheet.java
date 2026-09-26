package com.example.simpmusic.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.Song;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Locale;

public class SongOptionsBottomSheet extends BottomSheetDialogFragment {

    private final Song song;
    private OnOptionClickListener listener;

    public interface OnOptionClickListener {
        void onAddToPlaylist(Song song);
        void onViewAlbum(Song song);
        void onShare(Song song);
    }

    public SongOptionsBottomSheet(Song song) {
        this.song = song;
    }

    public void setOnOptionClickListener(OnOptionClickListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_song_options, container, false);

        ImageView ivCover = view.findViewById(R.id.ivOptionCover);
        TextView tvTitle = view.findViewById(R.id.tvOptionTitle);
        TextView tvArtist = view.findViewById(R.id.tvOptionArtist);
        TextView tvViewCount = view.findViewById(R.id.tvOptionViewCount);

        LinearLayout btnAddToPlaylist = view.findViewById(R.id.btnOptionAddToPlaylist);
        LinearLayout btnViewAlbum = view.findViewById(R.id.btnOptionViewAlbum);
        LinearLayout btnShare = view.findViewById(R.id.btnOptionShare);

        if (song != null) {
            tvTitle.setText(song.getTitle());
            tvArtist.setText(song.getArtist());
            if (tvViewCount != null) {
                tvViewCount.setText(formatViewCount(song.getViewCount()) + " lượt xem");
            }
            Glide.with(this).load(RetrofitClient.getAbsoluteUrl(song.getCoverUrl()))
                    .placeholder(R.drawable.ic_album)
                    .into(ivCover);
        }

        btnAddToPlaylist.setOnClickListener(v -> {
            if (listener != null) listener.onAddToPlaylist(song);
            dismiss();
        });

        btnViewAlbum.setOnClickListener(v -> {
            if (listener != null) listener.onViewAlbum(song);
            dismiss();
        });

        btnShare.setOnClickListener(v -> {
            if (listener != null) listener.onShare(song);
            dismiss();
        });

        return view;
    }

    private String formatViewCount(long count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1000000) return String.format(Locale.getDefault(), "%.1fk", count / 1000.0);
        return String.format(Locale.getDefault(), "%.1fM", count / 1000000.0);
    }
}
