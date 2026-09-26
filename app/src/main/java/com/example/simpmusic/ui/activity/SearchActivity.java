package com.example.simpmusic.ui.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.AddSongRequest;
import com.example.simpmusic.data.model.Album;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.data.model.Song;
import com.example.simpmusic.data.model.ViewCountResponse;
import com.example.simpmusic.service.MusicService;
import com.example.simpmusic.ui.adapter.SongAdapter;
import com.example.simpmusic.ui.dialog.CreateBottomSheet;
import com.example.simpmusic.ui.dialog.PlaylistSelectionBottomSheet;
import com.example.simpmusic.ui.dialog.SongOptionsBottomSheet;
import com.example.simpmusic.utils.AuthManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener, SongOptionsBottomSheet.OnOptionClickListener, PlaylistSelectionBottomSheet.OnPlaylistSelectedListener {

    private SearchView searchView;
    private RecyclerView rvSearchResults, rvSearchHistory, rvTrending;
    private SongAdapter searchAdapter, historyAdapter, trendingAdapter;
    private LinearLayout llNoResults, llInitialView, llTrending;
    private ProgressBar pbLoading;
    
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
    
    private static final String PREF_NAME = "search_prefs";
    private static final String KEY_HISTORY = "search_history";
    private static final int MAX_HISTORY_SIZE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        setupSearch();
        setupBottomNavigation();
        loadSearchHistory();
        fetchTrendingSongs();
    }

    private void initViews() {
        searchView = findViewById(R.id.searchView);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        rvTrending = findViewById(R.id.rvTrending);
        llInitialView = findViewById(R.id.llInitialView);
        llTrending = findViewById(R.id.llTrending);
        llNoResults = findViewById(R.id.llNoResults);
        pbLoading = findViewById(R.id.pbLoading);

        // Mini Player views
        miniPlayerCard = findViewById(R.id.miniPlayerCard);
        miniPlayerCover = findViewById(R.id.miniPlayerCover);
        miniPlayerTitle = findViewById(R.id.miniPlayerTitle);
        miniPlayerArtist = findViewById(R.id.miniPlayerArtist);
        miniPlayerPlayPause = findViewById(R.id.miniPlayerPlayPause);
        miniPlayerProgress = findViewById(R.id.miniPlayerProgress);

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new SongAdapter(new ArrayList<>(), this);
        rvSearchResults.setAdapter(searchAdapter);

        rvTrending.setLayoutManager(new LinearLayoutManager(this));
        trendingAdapter = new SongAdapter(new ArrayList<>(), this);
        rvTrending.setAdapter(trendingAdapter);

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
            bottomNavigation.setSelectedItemId(R.id.nav_search);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
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
                return id == R.id.nav_search;
            });
        }
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.trim().isEmpty()) performSearch(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty()) showInitialState();
                else hideInitialState();
                return false;
            }
        });
    }

    private void showInitialState() {
        llInitialView.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
        llNoResults.setVisibility(View.GONE);
        pbLoading.setVisibility(View.GONE);
    }

    private void hideInitialState() {
        llInitialView.setVisibility(View.GONE);
        rvSearchResults.setVisibility(View.VISIBLE);
    }

    private void fetchTrendingSongs() {
        RetrofitClient.getApiService().getAllSongs().enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Song> songs = new ArrayList<>(response.body());
                    Collections.sort(songs, (s1, s2) -> Long.compare(s2.getViewCount(), s1.getViewCount()));
                    List<Song> topSongs = songs.subList(0, Math.min(5, songs.size()));
                    trendingAdapter.updateSongs(topSongs);
                }
            }
            @Override public void onFailure(Call<List<Song>> call, Throwable t) {}
        });
    }

    private void performSearch(String keyword) {
        pbLoading.setVisibility(View.VISIBLE);
        llNoResults.setVisibility(View.GONE);
        rvSearchResults.setVisibility(View.GONE);

        RetrofitClient.getApiService().searchSongs(keyword).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                pbLoading.setVisibility(View.GONE);
                rvSearchResults.setVisibility(View.VISIBLE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Song> songs = response.body();
                    searchAdapter.updateSongs(songs);
                    llNoResults.setVisibility(songs.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void onFailure(Call<List<Song>> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                rvSearchResults.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onSongClick(Song song) {
        saveToHistory(song);
        if (mediaController != null) {
            String audioUrl = RetrofitClient.getAbsoluteUrl(song.getAudioUrl());
            String coverUrl = RetrofitClient.getAbsoluteUrl(song.getCoverUrl());

            MediaMetadata metadata = new MediaMetadata.Builder()
                    .setTitle(song.getTitle()).setArtist(song.getArtist())
                    .setArtworkUri(Uri.parse(coverUrl)).build();

            MediaItem mediaItem = new MediaItem.Builder()
                    .setMediaId(String.valueOf(song.getId())).setUri(Uri.parse(audioUrl))
                    .setMediaMetadata(metadata).build();

            mediaController.setMediaItem(mediaItem);
            mediaController.prepare();
            mediaController.play();
        }
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("song", song);
        startActivity(intent);
    }

    @Override public void onArtistClick(Song song) {}

    private void saveToHistory(Song song) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(KEY_HISTORY, null);
        List<Song> historyList = json == null ? new ArrayList<>() : gson.fromJson(json, new TypeToken<List<Song>>(){}.getType());
        historyList.removeIf(s -> s.getId() == song.getId());
        historyList.add(0, song);
        if (historyList.size() > MAX_HISTORY_SIZE) historyList = historyList.subList(0, MAX_HISTORY_SIZE);
        prefs.edit().putString(KEY_HISTORY, gson.toJson(historyList)).apply();
    }

    private void loadSearchHistory() {}

    @Override protected void onStart() {
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

    @Override protected void onStop() {
        super.onStop();
        stopProgressUpdate();
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
    }

    @Override public void onMoreClick(Song song) { new SongOptionsBottomSheet(song).show(getSupportFragmentManager(), "SongOptions"); }
    @Override public void onAddToPlaylist(Song song) { new PlaylistSelectionBottomSheet(song).show(getSupportFragmentManager(), "PlaylistSelection"); }
    @Override public void onViewAlbum(Song song) {}
    @Override public void onShare(Song song) {}
    @Override public void onPlaylistSelected(Playlist playlist, Song song) {}
    @Override public void onCreateNewPlaylist(Song song) {}
}
