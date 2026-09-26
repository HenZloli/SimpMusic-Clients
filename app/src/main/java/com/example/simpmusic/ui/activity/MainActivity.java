package com.example.simpmusic.ui.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.bumptech.glide.Glide;
import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.AddSongRequest;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.data.model.Song;
import com.example.simpmusic.service.MusicService;
import com.example.simpmusic.ui.fragment.HomeFragment;
import com.example.simpmusic.ui.fragment.LibraryFragment;
import com.example.simpmusic.ui.fragment.PremiumFragment;
import com.example.simpmusic.ui.fragment.ProfileFragment;
import com.example.simpmusic.ui.fragment.SearchFragment;
import com.example.simpmusic.ui.dialog.CreateBottomSheet;
import com.example.simpmusic.ui.dialog.PlaylistSelectionBottomSheet;
import com.example.simpmusic.utils.AuthManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements PlaylistSelectionBottomSheet.OnPlaylistSelectedListener {

    private MaterialCardView miniPlayerCard;
    private ImageView miniPlayerCover;
    private TextView miniPlayerTitle, miniPlayerArtist;
    private BottomNavigationView bottomNavigation;
    private ImageButton miniPlayerPlayPause, miniPlayerAdd;
    private ProgressBar miniPlayerProgress;

    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    private Song currentPlayingSong;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private int currentNavId = R.id.nav_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RetrofitClient.init(this);
        setContentView(R.layout.activity_main);
        
        initSharedViews();
        setupNavigation();

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), true);
        }
    }

    private void initSharedViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        miniPlayerCard = findViewById(R.id.miniPlayerCard);
        miniPlayerCover = findViewById(R.id.miniPlayerCover);
        miniPlayerTitle = findViewById(R.id.miniPlayerTitle);
        miniPlayerArtist = findViewById(R.id.miniPlayerArtist);
        miniPlayerPlayPause = findViewById(R.id.miniPlayerPlayPause);
        miniPlayerAdd = findViewById(R.id.miniPlayerAdd);
        miniPlayerProgress = findViewById(R.id.miniPlayerProgress);

        if (miniPlayerAdd != null) {
            miniPlayerAdd.setOnClickListener(v -> {
                if (currentPlayingSong != null) {
                    PlaylistSelectionBottomSheet sheet = new PlaylistSelectionBottomSheet(currentPlayingSong);
                    sheet.setOnPlaylistSelectedListener(this);
                    sheet.show(getSupportFragmentManager(), "PlaylistSelection");
                }
            });
        }
        
        if (miniPlayerCard != null) {
            miniPlayerCard.setOnClickListener(v -> {
                if (currentPlayingSong != null) {
                    Intent intent = new Intent(this, PlayerActivity.class);
                    intent.putExtra("song", currentPlayingSong);
                    startActivity(intent);
                }
            });
        }
    }

    private void setupNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == currentNavId) return true;

                // Xử lý nút Tạo: Nếu chưa đăng nhập thì nhảy sang trang Login
                if (id == R.id.nav_create) {
                    if (!AuthManager.getInstance(this).isLoggedIn()) {
                        startActivity(new Intent(this, LoginActivity.class));
                    } else {
                        new CreateBottomSheet().show(getSupportFragmentManager(), "CreateBottomSheet");
                    }
                    return false;
                }

                boolean slideForward = true;
                // Xác định hướng trượt PowerPoint
                if (id == R.id.nav_home) slideForward = false;
                else if (id == R.id.nav_search && currentNavId != R.id.nav_home) slideForward = false;
                else if (id == R.id.nav_library && (currentNavId == R.id.nav_profile)) slideForward = false;

                if (id == R.id.nav_home) {
                    currentNavId = id;
                    loadFragment(new HomeFragment(), slideForward);
                    return true;
                } else if (id == R.id.nav_search) {
                    currentNavId = id;
                    loadFragment(new SearchFragment(), slideForward);
                    return true;
                } else if (id == R.id.nav_library) {
                    if (!AuthManager.getInstance(this).isLoggedIn()) {
                        startActivity(new Intent(this, LoginActivity.class));
                        return false;
                    }
                    currentNavId = id;
                    loadFragment(new LibraryFragment(), slideForward);
                    return true;
                } else if (id == R.id.nav_profile) {
                    currentNavId = id;
                    loadFragment(new PremiumFragment(), slideForward);
                    return true;
                }
                return true;
            });
        }
    }

    public void navigateToProfile() {
        loadFragment(new ProfileFragment(), true);
    }

    public void navigateToLibrary() {
        bottomNavigation.setSelectedItemId(R.id.nav_library);
    }

    public void loadFragment(Fragment fragment, boolean slideForward) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (slideForward) {
            transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        } else {
            transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left);
        }
        transaction.replace(R.id.content_container, fragment);
        if (fragment instanceof ProfileFragment) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    @Override
    public void onPlaylistSelected(Playlist playlist, Song song) {
        addSongToPlaylist(playlist.getId(), song);
    }

    @Override
    public void onCreateNewPlaylist(Song song) {
        startActivity(new Intent(this, CreatePlaylistActivity.class));
    }

    private void addSongToPlaylist(int id, Song s) {
        RetrofitClient.getApiService().addSongToPlaylist(id, new AddSongRequest(s.getId())).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> r) { 
                if (r.isSuccessful()) Toast.makeText(MainActivity.this, "Đã thêm vào playlist", Toast.LENGTH_SHORT).show(); 
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    @Override protected void onStart() {
        super.onStart();
        SessionToken token = new SessionToken(this, new ComponentName(this, MusicService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try { mediaController = controllerFuture.get(); setupController(); } catch (Exception ignored) {}
        }, MoreExecutors.directExecutor());
    }

    private void setupController() {
        if (mediaController == null) return;
        syncMiniPlayer();
        mediaController.addListener(new Player.Listener() {
            @Override public void onMediaItemTransition(MediaItem item, int r) { syncMiniPlayer(); }
            @Override public void onIsPlayingChanged(boolean isPlaying) {
                if (miniPlayerPlayPause != null) miniPlayerPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                if (isPlaying) startProgressUpdate(); else stopProgressUpdate();
            }
        });
        if (miniPlayerPlayPause != null) miniPlayerPlayPause.setOnClickListener(v -> { if (mediaController.isPlaying()) mediaController.pause(); else mediaController.play(); });
    }

    private void syncMiniPlayer() {
        if (mediaController == null) return;
        MediaItem item = mediaController.getCurrentMediaItem();
        if (item != null && item.mediaMetadata != null) {
            miniPlayerCard.setVisibility(View.VISIBLE);
            miniPlayerTitle.setText(item.mediaMetadata.title);
            miniPlayerArtist.setText(item.mediaMetadata.artist);
            if (item.mediaMetadata.artworkUri != null) Glide.with(this).load(item.mediaMetadata.artworkUri).into(miniPlayerCover);
            
            int id = -1;
            try { id = Integer.parseInt(item.mediaId); } catch (Exception ignored) {}
            currentPlayingSong = new Song(id, item.mediaMetadata.title.toString(), item.mediaMetadata.artist.toString(), item.mediaMetadata.artworkUri.toString(), "", "", 0);
        } else {
            miniPlayerCard.setVisibility(View.GONE);
        }
    }

    private void startProgressUpdate() { progressHandler.post(updateProgressRunnable); }
    private void stopProgressUpdate() { progressHandler.removeCallbacks(updateProgressRunnable); }
    private final Runnable updateProgressRunnable = new Runnable() {
        @Override public void run() {
            if (mediaController != null && mediaController.isPlaying()) {
                long duration = mediaController.getDuration();
                if (duration > 0) {
                    miniPlayerProgress.setMax((int) duration);
                    miniPlayerProgress.setProgress((int) mediaController.getCurrentPosition());
                }
                progressHandler.postDelayed(this, 1000);
            }
        }
    };

    public void onSongClick(Song song) {
        if (mediaController == null) return;
        MediaMetadata metadata = new MediaMetadata.Builder().setTitle(song.getTitle()).setArtist(song.getArtist()).setArtworkUri(Uri.parse(RetrofitClient.getAbsoluteUrl(song.getCoverUrl()))).build();
        MediaItem mediaItem = new MediaItem.Builder().setMediaId(String.valueOf(song.getId())).setUri(Uri.parse(RetrofitClient.getAbsoluteUrl(song.getAudioUrl()))).setMediaMetadata(metadata).build();
        mediaController.setMediaItem(mediaItem);
        mediaController.prepare();
        mediaController.play();
        currentPlayingSong = song;
    }

    @Override protected void onStop() { super.onStop(); stopProgressUpdate(); if (controllerFuture != null) MediaController.releaseFuture(controllerFuture); }
}
