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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.Artist;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.data.model.Song;
import com.example.simpmusic.service.MusicService;
import com.example.simpmusic.ui.adapter.PlaylistAdapter;
import com.example.simpmusic.ui.dialog.CreateBottomSheet;
import com.example.simpmusic.utils.AuthManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity implements PlaylistAdapter.OnPlaylistClickListener {
    private TextView tvProfileName, tvFollowersCount, tvFollowingCount;
    private ImageView ivProfileLarge;
    private MaterialButton btnEditProfile;
    private ImageButton btnBack, btnLogoutTop;
    private View btnManagePlaylists;
    private RecyclerView rvMyPlaylists;
    private PlaylistAdapter playlistAdapter;

    // Mini Player & Nav
    private MaterialCardView miniPlayerCard;
    private ImageView miniPlayerCover;
    private TextView miniPlayerTitle, miniPlayerArtist;
    private ImageButton miniPlayerPlayPause;
    private ProgressBar miniPlayerProgress;
    private BottomNavigationView bottomNavigation;

    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupBottomNavigation();
        loadUserData();
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
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
                } else if (id == R.id.nav_library) {
                    startActivity(new Intent(this, LibraryActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, PremiumActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_create) {
                    new CreateBottomSheet().show(getSupportFragmentManager(), "CreateBottomSheet");
                    return false;
                }
                return false;
            });
        }
    }

    private void initViews() {
        tvProfileName = findViewById(R.id.tvProfileName);
        tvFollowersCount = findViewById(R.id.tvFollowersCount);
        tvFollowingCount = findViewById(R.id.tvFollowingCount);
        ivProfileLarge = findViewById(R.id.ivProfileLarge);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnBack = findViewById(R.id.btnBackProfile);
        btnLogoutTop = findViewById(R.id.btnLogoutTop);
        btnManagePlaylists = findViewById(R.id.btnManagePlaylists);
        rvMyPlaylists = findViewById(R.id.rvMyPlaylists);

        // Mini Player views
        miniPlayerCard = findViewById(R.id.miniPlayerCard);
        miniPlayerCover = findViewById(R.id.miniPlayerCover);
        miniPlayerTitle = findViewById(R.id.miniPlayerTitle);
        miniPlayerArtist = findViewById(R.id.miniPlayerArtist);
        miniPlayerPlayPause = findViewById(R.id.miniPlayerPlayPause);
        miniPlayerProgress = findViewById(R.id.miniPlayerProgress);

        rvMyPlaylists.setLayoutManager(new LinearLayoutManager(this));
        playlistAdapter = new PlaylistAdapter(new ArrayList<>(), this);
        rvMyPlaylists.setAdapter(playlistAdapter);

        btnBack.setOnClickListener(v -> finish());
        if (btnLogoutTop != null) btnLogoutTop.setOnClickListener(v -> showLogoutConfirmation());

        tvFollowingCount.setOnClickListener(v -> startActivity(new Intent(this, FollowingActivity.class)));
        btnManagePlaylists.setOnClickListener(v -> startActivity(new Intent(this, LibraryActivity.class)));

        if (miniPlayerCard != null) {
            miniPlayerCard.setOnClickListener(v -> {
                if (mediaController != null && mediaController.getCurrentMediaItem() != null) {
                    Intent intent = new Intent(this, PlayerActivity.class);
                    // Lấy thông tin bài hát hiện tại từ MediaController nếu cần truyền qua Intent
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchMyPlaylists();
        updateFollowCounts();
        updateMiniPlayerUI();
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
            @Override public void onMediaItemTransition(MediaItem item, int r) { updateMiniPlayerUI(); }
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
            if (mediaController.isPlaying()) startProgressUpdate();
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

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this).setTitle("Đăng xuất").setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("ĐĂNG XUẤT", (dialog, which) -> performLogout())
                .setNegativeButton("HỦY", null).show();
    }

    private void performLogout() {
        AuthManager.getInstance(this).clear();
        RetrofitClient.reset();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadUserData() {
        AuthManager authManager = AuthManager.getInstance(this);
        String name = authManager.getUsername();
        if (name == null || name.isEmpty()) {
            name = authManager.getEmail();
            if (name != null && name.contains("@")) name = name.split("@")[0];
        }
        tvProfileName.setText(name != null ? name : "Người dùng");
        Glide.with(this).load(R.drawable.ic_person).circleCrop().into(ivProfileLarge);
    }

    private void updateFollowCounts() {
        RetrofitClient.getApiService().getFollowingArtists().enqueue(new Callback<List<Artist>>() {
            @Override public void onResponse(Call<List<Artist>> call, Response<List<Artist>> response) {
                if (response.isSuccessful() && response.body() != null) tvFollowingCount.setText("Đang theo dõi " + response.body().size());
            }
            @Override public void onFailure(Call<List<Artist>> call, Throwable t) {}
        });
    }

    private void fetchMyPlaylists() {
        RetrofitClient.getApiService().getPlaylists().enqueue(new Callback<List<Playlist>>() {
            @Override public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                if (response.isSuccessful() && response.body() != null) playlistAdapter.setPlaylists(response.body());
            }
            @Override public void onFailure(Call<List<Playlist>> call, Throwable t) {}
        });
    }

    @Override public void onPlaylistClick(Playlist playlist) {
        Intent intent = new Intent(this, PlaylistDetailActivity.class);
        intent.putExtra("playlist", playlist);
        startActivity(intent);
    }
}
