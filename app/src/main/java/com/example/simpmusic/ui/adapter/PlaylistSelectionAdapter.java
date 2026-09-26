package com.example.simpmusic.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.data.model.Song;

import java.util.List;

public class PlaylistSelectionAdapter extends RecyclerView.Adapter<PlaylistSelectionAdapter.ViewHolder> {

    private List<Playlist> playlists;
    private final OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public PlaylistSelectionAdapter(List<Playlist> playlists, OnPlaylistClickListener listener) {
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.tvName.setText(playlist.getName());

        List<Song> songs = playlist.getSongs();
        if (songs != null && songs.size() >= 4) {
            holder.ivCover.setVisibility(View.GONE);
            holder.layoutGrid.setVisibility(View.VISIBLE);
            loadGridImage(songs.get(0).getCoverUrl(), holder.ivGrid1);
            loadGridImage(songs.get(1).getCoverUrl(), holder.ivGrid2);
            loadGridImage(songs.get(2).getCoverUrl(), holder.ivGrid3);
            loadGridImage(songs.get(3).getCoverUrl(), holder.ivGrid4);
        } else {
            holder.ivCover.setVisibility(View.VISIBLE);
            holder.layoutGrid.setVisibility(View.GONE);
            String coverUrl = (songs != null && !songs.isEmpty()) ? songs.get(0).getCoverUrl() : playlist.getCoverUrl();
            Glide.with(holder.itemView.getContext())
                    .load(RetrofitClient.getAbsoluteUrl(coverUrl))
                    .placeholder(R.drawable.ic_library)
                    .error(R.drawable.ic_library)
                    .centerCrop()
                    .into(holder.ivCover);
        }

        holder.itemView.setOnClickListener(v -> listener.onPlaylistClick(playlist));
    }

    private void loadGridImage(String url, ImageView imageView) {
        Glide.with(imageView.getContext())
                .load(RetrofitClient.getAbsoluteUrl(url))
                .placeholder(R.drawable.ic_library)
                .error(R.drawable.ic_library)
                .centerCrop()
                .into(imageView);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvName;
        View layoutGrid;
        ImageView ivGrid1, ivGrid2, ivGrid3, ivGrid4;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivSelectionPlaylistCover);
            tvName = itemView.findViewById(R.id.tvSelectionPlaylistName);
            layoutGrid = itemView.findViewById(R.id.layoutSelectionPlaylistCoverGrid);
            ivGrid1 = itemView.findViewById(R.id.ivSelectionGrid1);
            ivGrid2 = itemView.findViewById(R.id.ivSelectionGrid2);
            ivGrid3 = itemView.findViewById(R.id.ivSelectionGrid3);
            ivGrid4 = itemView.findViewById(R.id.ivSelectionGrid4);
        }
    }
}
