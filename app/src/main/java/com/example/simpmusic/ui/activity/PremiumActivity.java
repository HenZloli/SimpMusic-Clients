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

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.bumptech.glide.Glide;
import com.example.simpmusic.R;
import com.example.simpmusic.service.MusicService;
import com.example.simpmusic.ui.dialog.CreateBottomSheet;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

public class PremiumActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_premium);

        initViews();
        setupBottomNavigation();
    }

    private void initViews() {
        MaterialButton btnBackLarge = findViewById(R.id.btnPremiumBack);
        if (btnBackLarge != null) btnBackLarge.setOnClickListener(v -> finish());

        miniPlayerCard = findViewById(R.id.miniPlayerCard);
        miniPlayerCover = findViewById(R.id.miniPlayerCover);
        miniPlayerTitle = findViewById(R.id.miniPlayerTitle);
        miniPlayerArtist = findViewById(R.id.miniPlayerArtist);
        miniPlayerPlayPause = findViewById(R.id.miniPlayerPlayPause);
        miniPlayerProgress = findViewById(R.id.miniPlayerProgress);

        if (miniPlayerCard != null) {
            miniPlayerCard.setOnClickListener(v -> {
                if (mediaController != null && mediaController.getCurrentMediaItem() != null) {
                    startActivity(new Intent(this, PlayerActivity.class));
                }
            });
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile); // Premium sits at Profile/Premium tab
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
                } else if (id == R.id.nav_create) {
                    new CreateBottomSheet().show(getSupportFragmentManager(), "CreateBottomSheet");
                    return false;
                }
                return id == R.id.nav_profile;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMiniPlayerUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MusicService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                setupController();
            } catch (Exception e) {}
        }, MoreExecutors.directExecutor());
    }

    private void setupController() {
        if (mediaController == null) return;
        updateMiniPlayerUI();
        mediaController.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(MediaItem item, int r) {
                updateMiniPlayerUI();
            }
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (miniPlayerPlayPause != null) {
                    miniPlayerPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                }
                if (isPlaying) startProgressUpdate(); else stopProgressUpdate();
            }
        });
        if (miniPlayerPlayPause != null) {
            miniPlayerPlayPause.setOnClickListener(v -> {
                if (mediaController.isPlaying()) mediaController.pause(); else mediaController.play();
            });
        }
    }

    private void updateMiniPlayerUI() {
        if (mediaController != null && mediaController.getCurrentMediaItem() != null) {
            MediaItem item = mediaController.getCurrentMediaItem();
            if (miniPlayerCard != null) miniPlayerCard.setVisibility(View.VISIBLE);
            if (miniPlayerTitle != null) miniPlayerTitle.setText(item.mediaMetadata.title);
            if (miniPlayerArtist != null) miniPlayerArtist.setText(item.mediaMetadata.artist);
            if (miniPlayerCover != null && item.mediaMetadata.artworkUri != null) {
                Glide.with(this).load(item.mediaMetadata.artworkUri).into(miniPlayerCover);
            }
            if (mediaController.isPlaying()) startProgressUpdate();
        } else if (miniPlayerCard != null) {
            miniPlayerCard.setVisibility(View.GONE);
        }
    }

    private void startProgressUpdate() { progressHandler.post(updateProgressRunnable); }
    private void stopProgressUpdate() { progressHandler.removeCallbacks(updateProgressRunnable); }
    private final Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaController != null && mediaController.isPlaying()) {
                if (miniPlayerProgress != null) {
                    long duration = mediaController.getDuration();
                    if (duration > 0) {
                        miniPlayerProgress.setMax((int) duration);
                        miniPlayerProgress.setProgress((int) mediaController.getCurrentPosition());
                    }
                }
                progressHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        stopProgressUpdate();
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
    }
}
