package com.example.simpmusic.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.Song;
import java.util.List;
import java.util.Locale;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songs;
    private final OnSongClickListener listener;
    private final int layoutId;

    public interface OnSongClickListener {
        void onSongClick(Song song);
        default void onMoreClick(Song song) {}
        default void onArtistClick(Song song) {}
    }

    public SongAdapter(List<Song> songs, OnSongClickListener listener) {
        this(songs, listener, R.layout.item_song);
    }

    public SongAdapter(List<Song> songs, OnSongClickListener listener, int layoutId) {
        this.songs = songs;
        this.listener = listener;
        this.layoutId = layoutId;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.bind(song, listener);
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    public void updateSongs(List<Song> songs) {
        this.songs = songs;
        notifyDataSetChanged();
    }

    public List<Song> getSongs() {
        return songs;
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        private final ImageView songCover;
        private final TextView songTitle;
        private final TextView songArtist;
        private final ImageView ivArtistIcon;
        private final View artistContainer;
        private final TextView tvViewCount;
        private final ImageButton btnMoreAction;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            songCover = itemView.findViewById(R.id.songCover);
            songTitle = itemView.findViewById(R.id.songTitle);
            songArtist = itemView.findViewById(R.id.songArtist);
            ivArtistIcon = itemView.findViewById(R.id.ivArtistIcon);
            artistContainer = itemView.findViewById(R.id.artistContainer);
            tvViewCount = itemView.findViewById(R.id.tvViewCount);
            btnMoreAction = itemView.findViewById(R.id.btnMoreAction);
        }

        public void bind(Song song, OnSongClickListener listener) {
            songTitle.setText(song.getTitle());
            songArtist.setText(song.getArtist());
            
            if (tvViewCount != null) {
                tvViewCount.setText(formatViewCount(song.getViewCount()) + " lượt xem");
            }
            
            Glide.with(itemView.getContext())
                    .load(RetrofitClient.getAbsoluteUrl(song.getCoverUrl()))
                    .placeholder(R.drawable.ic_album)
                    .error(R.drawable.ic_album)
                    .into(songCover);

            if (ivArtistIcon != null) {
                Glide.with(itemView.getContext())
                        .load(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivArtistIcon);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onSongClick(song);
            });
            
            if (btnMoreAction != null) {
                btnMoreAction.setOnClickListener(v -> {
                    if (listener != null) listener.onMoreClick(song);
                });
            }
        }

        private String formatViewCount(long count) {
            if (count < 1000) return String.valueOf(count);
            if (count < 1000000) return String.format(Locale.getDefault(), "%.1fk", count / 1000.0);
            return String.format(Locale.getDefault(), "%.1fM", count / 1000000.0);
        }
    }
}
