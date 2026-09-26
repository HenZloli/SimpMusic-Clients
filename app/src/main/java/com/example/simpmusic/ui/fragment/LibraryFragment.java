package com.example.simpmusic.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.local.AppDatabase;
import com.example.simpmusic.data.local.OfflinePlaylistEntity;
import com.example.simpmusic.data.local.SongEntity;
import com.example.simpmusic.data.model.AddSongRequest;
import com.example.simpmusic.data.model.Album;
import com.example.simpmusic.data.model.Artist;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.data.model.Song;
import com.example.simpmusic.ui.activity.AlbumDetailActivity;
import com.example.simpmusic.ui.activity.CreatePlaylistActivity;
import com.example.simpmusic.ui.activity.LoginActivity;
import com.example.simpmusic.ui.activity.MainActivity;
import com.example.simpmusic.ui.activity.PlaylistDetailActivity;
import com.example.simpmusic.ui.activity.ProfileActivity;
import com.example.simpmusic.ui.adapter.AlbumAdapter;
import com.example.simpmusic.ui.adapter.PlaylistAdapter;
import com.example.simpmusic.ui.dialog.CreateBottomSheet;
import com.example.simpmusic.ui.dialog.PlaylistSelectionBottomSheet;
import com.example.simpmusic.utils.AuthManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryFragment extends Fragment implements PlaylistAdapter.OnPlaylistClickListener, AlbumAdapter.OnAlbumClickListener, PlaylistSelectionBottomSheet.OnPlaylistSelectedListener {

    private RecyclerView rvLibraryContent;
    private PlaylistAdapter playlistAdapter;
    private LibraryAlbumAdapter albumAdapter;
    private Chip chipPlaylists, chipAlbums;
    private View btnSearchLibrary, ivUserAvatar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_library, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchLibraryData();
        loadUserAvatar();
    }

    private void initViews(View view) {
        rvLibraryContent = view.findViewById(R.id.rvLibraryContent);
        if (rvLibraryContent != null) {
            rvLibraryContent.setLayoutManager(new LinearLayoutManager(getContext()));
            playlistAdapter = new PlaylistAdapter(new ArrayList<>(), this);
            albumAdapter = new LibraryAlbumAdapter(new ArrayList<>(), this);
            rvLibraryContent.setAdapter(new ConcatAdapter(playlistAdapter, albumAdapter));
        }

        chipPlaylists = view.findViewById(R.id.chipPlaylists);
        chipAlbums = view.findViewById(R.id.chipAlbums);
        btnSearchLibrary = view.findViewById(R.id.btnSearchLibrary);
        ivUserAvatar = view.findViewById(R.id.ivUserAvatar);

        // Hide + button as requested
        View btnAddLibrary = view.findViewById(R.id.btnAddLibrary);
        if (btnAddLibrary != null) btnAddLibrary.setVisibility(View.GONE);

        if (chipPlaylists != null) chipPlaylists.setOnCheckedChangeListener((bv, isChecked) -> updateFilter());
        if (chipAlbums != null) chipAlbums.setOnCheckedChangeListener((bv, isChecked) -> updateFilter());
        
        if (btnSearchLibrary != null) {
            btnSearchLibrary.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    BottomNavigationView nav = getActivity().findViewById(R.id.bottomNavigation);
                    if (nav != null) nav.setSelectedItemId(R.id.nav_search);
                }
            });
        }
        
        if (ivUserAvatar != null) ivUserAvatar.setOnClickListener(v -> handleProfileNavigation());
    }

    private void loadUserAvatar() {
        if (getContext() == null || ivUserAvatar == null) return;
        AuthManager authManager = AuthManager.getInstance(getContext());
        if (!authManager.isLoggedIn()) {
            ((ImageView)ivUserAvatar).setImageResource(R.drawable.ic_person);
            return;
        }

        RetrofitClient.getApiService().getUserProfile().enqueue(new Callback<Artist>() {
            @Override
            public void onResponse(Call<Artist> call, Response<Artist> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    String avatarUrl = response.body().getAvatarUrl();
                    Glide.with(LibraryFragment.this)
                            .load(RetrofitClient.getAbsoluteUrl(avatarUrl))
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into((ImageView) ivUserAvatar);
                }
            }
            @Override public void onFailure(Call<Artist> call, Throwable t) {}
        });
    }

    private void updateFilter() {
        if (rvLibraryContent == null) return;
        boolean pChecked = chipPlaylists != null && chipPlaylists.isChecked();
        boolean aChecked = chipAlbums != null && chipAlbums.isChecked();

        if (pChecked && !aChecked) {
            rvLibraryContent.setAdapter(playlistAdapter);
        } else if (aChecked && !pChecked) {
            rvLibraryContent.setAdapter(albumAdapter);
        } else {
            rvLibraryContent.setAdapter(new ConcatAdapter(playlistAdapter, albumAdapter));
        }
    }

    private void handleProfileNavigation() {
        if (getContext() == null) return;
        if (!AuthManager.getInstance(getContext()).isLoggedIn()) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
        } else {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToProfile();
            } else {
                startActivity(new Intent(getActivity(), ProfileActivity.class));
            }
        }
    }

    private void fetchLibraryData() {
        RetrofitClient.getApiService().getPlaylists().enqueue(new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                List<Playlist> networkPlaylists = (response.isSuccessful() && response.body() != null) 
                        ? new ArrayList<>(response.body()) : new ArrayList<>();
                loadOfflinePlaylistsAndMerge(networkPlaylists);
            }
            @Override public void onFailure(Call<List<Playlist>> call, Throwable t) {
                loadOfflinePlaylistsAndMerge(new ArrayList<>());
            }
        });

        RetrofitClient.getApiService().getSavedAlbums().enqueue(new Callback<List<Album>>() {
            @Override
            public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                if (response.isSuccessful() && response.body() != null && albumAdapter != null) {
                    albumAdapter.setAlbums(response.body());
                }
            }
            @Override public void onFailure(Call<List<Album>> call, Throwable t) {}
        });
    }

    private void loadOfflinePlaylistsAndMerge(List<Playlist> networkPlaylists) {
        if (getContext() == null) return;
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            
            List<OfflinePlaylistEntity> offlineEntities = db.offlinePlaylistDao().getAllPlaylists();
            List<Playlist> mergedList = new ArrayList<>(networkPlaylists);

            if (offlineEntities != null) {
                for (OfflinePlaylistEntity entity : offlineEntities) {
                    boolean alreadyInList = false;
                    for (Playlist p : mergedList) {
                        if (p.getId() == entity.getId()) {
                            alreadyInList = true;
                            if (p.getDescription() == null || !p.getDescription().contains("(Đã tải về)")) {
                                p.setDescription((p.getDescription() != null ? p.getDescription() : "") + " (Đã tải về)");
                            }
                            List<SongEntity> songEntities = db.offlinePlaylistDao().getSongsForPlaylist(p.getId());
                            if (songEntities != null && !songEntities.isEmpty()) {
                                List<Song> songs = new ArrayList<>();
                                for (SongEntity se : songEntities) {
                                    songs.add(new Song(se.getId(), se.getTitle(), se.getArtist(), se.getCoverUrl(), se.getAudioUrl(), se.getLyrics(), 0));
                                }
                                p.setSongs(songs);
                            }
                            break;
                        }
                    }

                    if (!alreadyInList) {
                        Playlist p = new Playlist(entity.getName(), entity.getDescription(), entity.getCoverUrl());
                        p.setId(entity.getId());
                        p.setDescription((p.getDescription() != null ? p.getDescription() : "") + " (Offline)");
                        
                        List<SongEntity> songEntities = db.offlinePlaylistDao().getSongsForPlaylist(p.getId());
                        if (songEntities != null && !songEntities.isEmpty()) {
                            List<Song> songs = new ArrayList<>();
                            for (SongEntity se : songEntities) {
                                songs.add(new Song(se.getId(), se.getTitle(), se.getArtist(), se.getCoverUrl(), se.getAudioUrl(), se.getLyrics(), 0));
                            }
                            p.setSongs(songs);
                        }
                        
                        mergedList.add(0, p);
                    }
                }
            }

            List<SongEntity> favoriteSongs = db.songDao().getFavoriteSongs();
            if (favoriteSongs != null && !favoriteSongs.isEmpty()) {
                Playlist liked = new Playlist("Bài hát đã thích", "Các bài hát bạn đã yêu thích", "");
                liked.setId(-1000);
                List<Song> songs = new ArrayList<>();
                for (SongEntity se : favoriteSongs) {
                    songs.add(new Song(se.getId(), se.getTitle(), se.getArtist(), se.getCoverUrl(), se.getAudioUrl(), se.getLyrics(), 0));
                }
                liked.setSongs(songs);
                mergedList.add(0, liked);
            }
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (playlistAdapter != null) {
                        playlistAdapter.setPlaylists(mergedList);
                    }
                });
            }
        }).start();
    }

    @Override public void onPlaylistClick(Playlist playlist) {
        Intent intent = new Intent(getActivity(), PlaylistDetailActivity.class);
        intent.putExtra("playlist", playlist);
        boolean isTrulyOffline = playlist.getId() == -1000 || 
                                 (playlist.getDescription() != null && 
                                  (playlist.getDescription().contains("(Offline)") || playlist.getDescription().contains("(Đã tải về)")));
        intent.putExtra("isOffline", isTrulyOffline);
        startActivity(intent);
    }

    @Override public void onAlbumClick(Album album) {
        Intent intent = new Intent(getActivity(), AlbumDetailActivity.class);
        intent.putExtra("album", album);
        startActivity(intent);
    }

    @Override public void onPlaylistSelected(Playlist p, Song s) { addSongToPlaylist(p.getId(), s); }

    @Override
    public void onCreateNewPlaylist(Song song) {
        startActivity(new Intent(getContext(), CreatePlaylistActivity.class));
    }
    
    private void addSongToPlaylist(int id, Song s) {
        RetrofitClient.getApiService().addSongToPlaylist(id, new AddSongRequest(s.getId())).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> r) { if (r.isSuccessful()) Toast.makeText(getContext(), "Đã thêm vào playlist", Toast.LENGTH_SHORT).show(); }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private class LibraryAlbumAdapter extends RecyclerView.Adapter<LibraryAlbumAdapter.ViewHolder> {
        private List<Album> albums;
        private AlbumAdapter.OnAlbumClickListener listener;
        public LibraryAlbumAdapter(List<Album> albums, AlbumAdapter.OnAlbumClickListener listener) { this.albums = albums; this.listener = listener; }
        public void setAlbums(List<Album> albums) { this.albums = albums != null ? albums : new ArrayList<>(); notifyDataSetChanged(); }
        @NonNull
        @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_library, p, false);
            return new ViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int p) {
            Album a = albums.get(p);
            h.tvTitle.setText(a.getTitle());
            h.tvSubtitle.setText("Album • " + (a.getDescription() != null ? a.getDescription() : "SimpMusic"));
            h.layoutGrid.setVisibility(View.GONE);
            h.ivCover.setVisibility(View.VISIBLE);
            com.bumptech.glide.Glide.with(h.itemView.getContext()).load(RetrofitClient.getAbsoluteUrl(a.getCoverUrl())).placeholder(R.drawable.ic_album).into(h.ivCover);
            h.itemView.setOnClickListener(v -> listener.onAlbumClick(a));
        }
        @Override public int getItemCount() { return albums != null ? albums.size() : 0; }
        class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.ImageView ivCover; android.widget.TextView tvTitle, tvSubtitle; View layoutGrid;
            public ViewHolder(View v) { 
                super(v); 
                ivCover = v.findViewById(R.id.ivLibraryCover); 
                tvTitle = v.findViewById(R.id.tvLibraryTitle); 
                tvSubtitle = v.findViewById(R.id.tvLibrarySubtitle); 
                layoutGrid = v.findViewById(R.id.layoutPlaylistCoverGrid);
            }
        }
    }
}
