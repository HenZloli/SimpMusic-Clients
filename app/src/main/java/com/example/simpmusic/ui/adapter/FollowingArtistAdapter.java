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

public class FollowingArtistAdapter extends RecyclerView.Adapter<FollowingArtistAdapter.ViewHolder> {

    private List<Artist> artists;
    private final OnArtistClickListener listener;

    public interface OnArtistClickListener {
        void onArtistClick(Artist artist);
    }

    public FollowingArtistAdapter(List<Artist> artists, OnArtistClickListener listener) {
        this.artists = artists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist_follow, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivArtistAvatar;
        private final TextView tvArtistName;
        private final TextView tvArtistFollowers;
        private final ImageView ivFollowStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArtistAvatar = itemView.findViewById(R.id.ivArtistAvatar);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            tvArtistFollowers = itemView.findViewById(R.id.tvArtistFollowers);
            ivFollowStatus = itemView.findViewById(R.id.ivFollowStatus);
        }

        public void bind(Artist artist, OnArtistClickListener listener) {
            tvArtistName.setText(artist.getName());
            // Assuming Artist model doesn't have follower count yet based on previous read, 
            // but item_artist_follow.xml has tvArtistFollowers. I'll just hide it or set a placeholder.
            tvArtistFollowers.setText("Nghệ sĩ"); 

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
            
            // ivFollowStatus is visible by default in layout with green tint
            ivFollowStatus.setVisibility(artist.isFollowed() ? View.VISIBLE : View.GONE);
        }
    }
}
