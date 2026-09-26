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
import com.example.simpmusic.data.model.Album;

import java.util.List;

public class AlbumAdapter
        extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {

    private List<Album> albums;

    private final OnAlbumClickListener listener;


    public interface OnAlbumClickListener {

        void onAlbumClick(Album album);
    }


    public AlbumAdapter(
            List<Album> albums,
            OnAlbumClickListener listener
    ) {

        this.albums = albums;
        this.listener = listener;
    }


    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_album_card,
                                parent,
                                false
                        );


        return new AlbumViewHolder(view);
    }


    @Override
    public void onBindViewHolder(
            @NonNull AlbumViewHolder holder,
            int position
    ) {

        Album album =
                albums.get(position);

        holder.bind(
                album,
                listener
        );
    }


    @Override
    public int getItemCount() {

        return albums == null
                ? 0
                : albums.size();
    }


    public void setAlbums(
            List<Album> albums
    ) {

        this.albums = albums;

        notifyDataSetChanged();
    }


    // =========================
    // VIEW HOLDER
    // =========================

    static class AlbumViewHolder
            extends RecyclerView.ViewHolder {

        private final ImageView ivAlbumCover;

        private final TextView tvAlbumTitle;

        private final TextView tvAlbumSubtitle;


        public AlbumViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);


            ivAlbumCover =
                    itemView.findViewById(
                            R.id.ivAlbumCover
                    );

            tvAlbumTitle =
                    itemView.findViewById(
                            R.id.tvAlbumTitle
                    );

            tvAlbumSubtitle =
                    itemView.findViewById(
                            R.id.tvAlbumSubtitle
                    );
        }


        public void bind(
                Album album,
                OnAlbumClickListener listener
        ) {

            // =========================
            // TITLE
            // =========================

            tvAlbumTitle.setText(
                    album.getTitle()
            );


            // =========================
            // DESCRIPTION
            // =========================

            String description =
                    album.getDescription();


            if (description == null
                    || description.isEmpty()) {

                tvAlbumSubtitle.setText(
                        "Album"
                );

            } else {

                tvAlbumSubtitle.setText(
                        description
                );
            }


            // =========================
            // COVER
            // =========================

            Glide.with(
                            itemView.getContext()
                    )
                    .load(
                            album.getCoverUrl()
                    )
                    .placeholder(
                            android.R.drawable.ic_menu_gallery
                    )
                    .error(
                            android.R.drawable.ic_menu_gallery
                    )
                    .into(
                            ivAlbumCover
                    );


            // =========================
            // CLICK
            // =========================

            itemView.setOnClickListener(v -> {

                if (listener != null) {

                    listener.onAlbumClick(
                            album
                    );
                }
            });
        }
    }
}