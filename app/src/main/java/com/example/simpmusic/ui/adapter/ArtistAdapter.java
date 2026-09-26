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
import com.example.simpmusic.data.model.Artist;
import java.util.List;

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder> {

    private List<Artist> artists;
    private final OnArtistClickListener listener;

    public interface OnArtistClickListener {
        void onArtistClick(Artist artist);
    }

    public ArtistAdapter(List<Artist> artists, OnArtistClickListener listener) {
        this.artists = artists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist_circle, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        Artist artist = artists.get(position);
        holder.bind(artist, listener);
    }

    @Override
    public int getItemCount() {
        return artists != null ? artists.size() : 0;
    }

    public void setArtists(List<Artist> artists) {
        this.artists = artists;
        notifyDataSetChanged();
    }

    static class ArtistViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivArtistAvatar;
        private final TextView tvArtistName;

        public ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArtistAvatar = itemView.findViewById(R.id.ivArtistAvatar);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
        }

        public void bind(Artist artist, OnArtistClickListener listener) {
            tvArtistName.setText(artist.getName());
            
            String avatarUrl = artist.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                if (!avatarUrl.startsWith("http")) {
                    avatarUrl = "http://10.0.2.2:5081/" + avatarUrl.replace("\\", "/");
                }
            }

            Glide.with(itemView.getContext())
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivArtistAvatar);

            itemView.setOnClickListener(v -> listener.onArtistClick(artist));
        }
    }
}
