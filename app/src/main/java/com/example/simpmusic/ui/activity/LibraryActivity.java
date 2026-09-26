package com.example.simpmusic.ui.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.AddSongRequest;
import com.example.simpmusic.data.model.Album;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.data.model.Song;
import com.example.simpmusic.service.MusicService;
import com.example.simpmusic.ui.adapter.AlbumAdapter;
import com.example.simpmusic.ui.adapter.PlaylistAdapter;
import com.example.simpmusic.ui.dialog.CreateBottomSheet;
import com.example.simpmusic.ui.dialog.PlaylistSelectionBottomSheet;
import com.example.simpmusic.utils.AuthManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryActivity extends AppCompatActivity implements PlaylistAdapter.OnPlaylistClickListener, AlbumAdapter.OnAlbumClickListener, PlaylistSelectionBottomSheet.OnPlaylistSelectedListener {

    private RecyclerView rvLibraryContent;
    private PlaylistAdapter playlistAdapter;
    private LibraryAlbumAdapter albumAdapter;
    private BottomNavigationView bottomNavigation;
    private Chip chipPlaylists, chipAlbums;
    private ImageButton btnAddLibrary, btnSearchLibrary;
    private ImageView ivUserAvatar;

    private MaterialCardView miniPlayerCard;
    private ImageView miniPlayerCover;
    private TextView miniPlayerTitle, miniPlayerArtist;
    private ImageButton miniPlayerPlayPause, miniPlayerAdd;
    private ProgressBar miniPlayerProgress;

    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    private Song currentPlayingSong;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        initViews();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AuthManager.getInstance(this).isLoggedIn()) {
            finish();
            return;
        }
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_library);
        }
        fetchLibraryData();
    }

    private void initViews() {
        rvLibraryContent = findViewById(R.id.rvLibraryContent);
        if (rvLibraryContent != null) {
            rvLibraryContent.setLayoutManager(new LinearLayoutManager(this));
            playlistAdapter = new PlaylistAdapter(new ArrayList<>(), this);
            albumAdapter = new LibraryAlbumAdapter(new ArrayList<>(), this);
            rvLibraryContent.setAdapter(new ConcatAdapter(playlistAdapter, albumAdapter));
        }

        chipPlaylists = findViewById(R.id.chipPlaylists);
        chipAlbums = findViewById(R.id.chipAlbums);
        btnAddLibrary = findViewById(R.id.btnAddLibrary);
        btnSearchLibrary = findViewById(R.id.btnSearchLibrary);
        ivUserAvatar = findViewById(R.id.ivUserAvatar);

        if (chipPlaylists != null) chipPlaylists.setOnCheckedChangeListener((bv, isChecked) -> updateFilter());
        if (chipAlbums != null) chipAlbums.setOnCheckedChangeListener((bv, isChecked) -> updateFilter());
        if (btnAddLibrary != null) btnAddLibrary.setOnClickListener(v -> showCreateBottomSheet());
        if (ivUserAvatar != null) ivUserAvatar.setOnClickListener(v -> handleProfileNavigation());

        miniPlayerCard = findViewById(R.id.miniPlayerCard);
        miniPlayerCover = findViewById(R.id.miniPlayerCover);
        miniPlayerTitle = findViewById(R.id.miniPlayerTitle);
        miniPlayerArtist = findViewById(R.id.miniPlayerArtist);
        miniPlayerPlayPause = findViewById(R.id.miniPlayerPlayPause);
        miniPlayerAdd = findViewById(R.id.miniPlayerAdd);
        miniPlayerProgress = findViewById(R.id.miniPlayerProgress);
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

    private void showCreateBottomSheet() {
        if (!AuthManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        new CreateBottomSheet().show(getSupportFragmentManager(), "CreateBottomSheet");
    }

    private void handleProfileNavigation() {
        if (!AuthManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            startActivity(new Intent(this, ProfileActivity.class));
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_library);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_search) {
                    startActivity(new Intent(this, SearchActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_create) {
                    showCreateBottomSheet();
                    return false;
                } else if (id == R.id.nav_profile) {
                    handleProfileNavigation();
                    return true;
                }
                return id == R.id.nav_library;
            });
        }
    }

    private void fetchLibraryData() {
        RetrofitClient.getApiService().getPlaylists().enqueue(new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                if (response.isSuccessful() && response.body() != null && playlistAdapter != null) {
                    playlistAdapter.setPlaylists(response.body());
                }
            }
            @Override public void onFailure(Call<List<Playlist>> call, Throwable t) {}
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

    @Override
    protected void onStart() {
        super.onStart();
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MusicService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try { mediaController = controllerFuture.get(); setupController(); } catch (Exception e) {}
        }, MoreExecutors.directExecutor());
    }

    private void setupController() {
        if (mediaController == null) return;
        updateMiniPlayerUI();
        mediaController.addListener(new Player.Listener() {
            @Override public void onMediaItemTransition(MediaItem item, int reason) { updateMiniPlayerUI(); }
            @Override public void onIsPlayingChanged(boolean isPlaying) {
                if (miniPlayerPlayPause != null) miniPlayerPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                if (isPlaying) startProgressUpdate(); else stopProgressUpdate();
            }
        });
        if (miniPlayerPlayPause != null) miniPlayerPlayPause.setOnClickListener(v -> { if (mediaController.isPlaying()) mediaController.pause(); else mediaController.play(); });
    }

    private void updateMiniPlayerUI() {
        if (mediaController != null && mediaController.getCurrentMediaItem() != null) {
            MediaItem item = mediaController.getCurrentMediaItem();
            if (miniPlayerCard != null) miniPlayerCard.setVisibility(View.VISIBLE);
            if (miniPlayerTitle != null) miniPlayerTitle.setText(item.mediaMetadata.title);
            if (miniPlayerArtist != null) miniPlayerArtist.setText(item.mediaMetadata.artist);
            if (miniPlayerCover != null && item.mediaMetadata.artworkUri != null) Glide.with(this).load(item.mediaMetadata.artworkUri).into(miniPlayerCover);
        } else if (miniPlayerCard != null) miniPlayerCard.setVisibility(View.GONE);
    }

    private void startProgressUpdate() { progressHandler.post(updateProgressRunnable); }
    private void stopProgressUpdate() { progressHandler.removeCallbacks(updateProgressRunnable); }
    private final Runnable updateProgressRunnable = new Runnable() {
        @Override public void run() {
            if (mediaController != null && mediaController.isPlaying()) {
                if (miniPlayerProgress != null) {
                    long duration = mediaController.getDuration();
                    if (duration > 0) { miniPlayerProgress.setMax((int) duration); miniPlayerProgress.setProgress((int) mediaController.getCurrentPosition()); }
                }
                progressHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override protected void onStop() { super.onStop(); stopProgressUpdate(); if (controllerFuture != null) MediaController.releaseFuture(controllerFuture); }

    @Override public void onPlaylistClick(Playlist playlist) {
        Intent intent = new Intent(this, PlaylistDetailActivity.class);
        intent.putExtra("playlist", playlist);
        startActivity(intent);
    }

    @Override public void onAlbumClick(Album album) {
        Intent intent = new Intent(this, AlbumDetailActivity.class);
        intent.putExtra("album", album);
        startActivity(intent);
    }

    @Override public void onPlaylistSelected(Playlist p, Song s) { addSongToPlaylist(p.getId(), s); }
    @Override public void onCreateNewPlaylist(Song s) {}
    
    private void addSongToPlaylist(int id, Song s) {
        RetrofitClient.getApiService().addSongToPlaylist(id, new AddSongRequest(s.getId())).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> r) { if (r.isSuccessful()) Toast.makeText(LibraryActivity.this, "Đã thêm vào playlist", Toast.LENGTH_SHORT).show(); }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private class LibraryAlbumAdapter extends RecyclerView.Adapter<LibraryAlbumAdapter.ViewHolder> {
        private List<Album> albums;
        private AlbumAdapter.OnAlbumClickListener listener;
        public LibraryAlbumAdapter(List<Album> albums, AlbumAdapter.OnAlbumClickListener listener) { this.albums = albums; this.listener = listener; }
        public void setAlbums(List<Album> albums) { this.albums = albums != null ? albums : new ArrayList<>(); notifyDataSetChanged(); }
        @Override public ViewHolder onCreateViewHolder(android.view.ViewGroup p, int t) {
            View v = android.view.LayoutInflater.from(p.getContext()).inflate(R.layout.item_library, p, false);
            return new ViewHolder(v);
        }
        @Override public void onBindViewHolder(ViewHolder h, int p) {
            Album a = albums.get(p);
            h.tvTitle.setText(a.getTitle());
            h.tvSubtitle.setText("Album • " + (a.getDescription() != null ? a.getDescription() : "SimpMusic"));
            
            // Đảm bảo ẩn Grid và hiện Cover đơn cho Album
            h.layoutGrid.setVisibility(View.GONE);
            h.ivCover.setVisibility(View.VISIBLE);
            
            Glide.with(h.itemView.getContext())
                .load(RetrofitClient.getAbsoluteUrl(a.getCoverUrl()))
                .placeholder(R.drawable.ic_album).into(h.ivCover);
            h.itemView.setOnClickListener(v -> listener.onAlbumClick(a));
        }
        @Override public int getItemCount() { return albums != null ? albums.size() : 0; }
        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivCover; TextView tvTitle, tvSubtitle; View layoutGrid;
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
