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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<Playlist> playlists;
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public PlaylistAdapter(List<Playlist> playlists, OnPlaylistClickListener listener) {
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_library, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.bind(playlist, listener);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
        notifyDataSetChanged();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle;
        TextView tvSubtitle;

        View layoutGrid;
        ImageView ivGrid1, ivGrid2, ivGrid3, ivGrid4;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivLibraryCover);
            tvTitle = itemView.findViewById(R.id.tvLibraryTitle);
            tvSubtitle = itemView.findViewById(R.id.tvLibrarySubtitle);

            layoutGrid = itemView.findViewById(R.id.layoutPlaylistCoverGrid);
            ivGrid1 = itemView.findViewById(R.id.ivGrid1);
            ivGrid2 = itemView.findViewById(R.id.ivGrid2);
            ivGrid3 = itemView.findViewById(R.id.ivGrid3);
            ivGrid4 = itemView.findViewById(R.id.ivGrid4);
        }

        public void bind(Playlist playlist, OnPlaylistClickListener listener) {
            final int currentId = playlist.getId();
            itemView.setTag(currentId);
            
            tvTitle.setText(playlist.getName());
            
            String desc = playlist.getDescription();
            if (desc == null || desc.trim().isEmpty()) {
                desc = "SimpMusic";
            }
            tvSubtitle.setText("Playlist • " + desc);
            
            updateCoverUI(playlist);

            // Fetch details if songs list is empty and no specific cover URL is set
            if ((playlist.getSongs() == null || playlist.getSongs().isEmpty()) && 
                (playlist.getCoverUrl() == null || playlist.getCoverUrl().trim().isEmpty())) {
                
                if (playlist.getId() > 0) {
                    RetrofitClient.getApiService().getPlaylistById(playlist.getId()).enqueue(new Callback<Playlist>() {
                        @Override
                        public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Playlist detailed = response.body();
                                playlist.setSongs(detailed.getSongs());
                                playlist.setCoverUrl(detailed.getCoverUrl());
                                
                                // Verify view is still bound to the same playlist to avoid recycling bugs
                                Object tag = itemView.getTag();
                                if (tag instanceof Integer && (Integer)tag == currentId) {
                                    updateCoverUI(playlist);
                                }
                            }
                        }
                        @Override public void onFailure(Call<Playlist> call, Throwable t) {}
                    });
                }
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistClick(playlist);
                }
            });
        }

        private void updateCoverUI(Playlist playlist) {
            List<Song> songs = playlist.getSongs();
            if (songs != null && songs.size() >= 4) {
                ivCover.setVisibility(View.GONE);
                layoutGrid.setVisibility(View.VISIBLE);

                loadGridImage(songs.get(0).getCoverUrl(), ivGrid1);
                loadGridImage(songs.get(1).getCoverUrl(), ivGrid2);
                loadGridImage(songs.get(2).getCoverUrl(), ivGrid3);
                loadGridImage(songs.get(3).getCoverUrl(), ivGrid4);
            } else {
                ivCover.setVisibility(View.VISIBLE);
                layoutGrid.setVisibility(View.GONE);

                String coverUrl = null;
                if (songs != null && !songs.isEmpty()) {
                    coverUrl = songs.get(0).getCoverUrl();
                }
                
                if (coverUrl == null || coverUrl.trim().isEmpty()) {
                    coverUrl = playlist.getCoverUrl();
                }

                Glide.with(itemView.getContext())
                        .load(RetrofitClient.getAbsoluteUrl(coverUrl))
                        .placeholder(R.drawable.ic_library)
                        .error(R.drawable.ic_library)
                        .centerCrop()
                        .into(ivCover);
            }
        }

        private void loadGridImage(String url, ImageView imageView) {
            Glide.with(itemView.getContext())
                    .load(RetrofitClient.getAbsoluteUrl(url))
                    .placeholder(R.drawable.ic_library)
                    .error(R.drawable.ic_library)
                    .centerCrop()
                    .into(imageView);
        }
    }
}
