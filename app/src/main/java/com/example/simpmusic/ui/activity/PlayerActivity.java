package com.example.simpmusic.ui.activity;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.local.AppDatabase;
import com.example.simpmusic.data.local.SongEntity;
import com.example.simpmusic.data.model.AddSongRequest;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.data.model.Song;
import com.example.simpmusic.data.model.ViewCountResponse;
import com.example.simpmusic.service.MusicService;
import com.example.simpmusic.utils.AuthManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerActivity extends AppCompatActivity {

    private ImageView ivCover, ivArtistPlayerIcon;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime, tvHeaderTitle, tvLyrics, tvViewCountPlayer;
    private SeekBar seekBar;
    private FloatingActionButton fabPlayPause;
    private ImageButton btnBack, btnPrevious, btnNext, btnShuffle, btnRepeat, btnFavorite, btnPlaylist, btnLyrics, btnMorePlayer;
    private MaterialCardView cvAlbumArt, cvLyrics;
    private View playerRootLayout, playerArtistContainer;

    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;
    private Song intentSong;
    private long currentDisplayViewCount = 0;
    private static final String BASE_URL = "http://10.0.2.2:5081/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_player);

        initViews();
        handleIntent(getIntent());
        setupClickListeners();
        setupInsets();
    }

    private void setupInsets() {
        if (playerRootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(playerRootLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
        if (mediaController != null) {
            checkAndPlayIntentSong();
        }
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("song")) {
            intentSong = (Song) intent.getSerializableExtra("song");
            if (intentSong != null) {
                if (intentSong.getViewCount() > 0) {
                    currentDisplayViewCount = intentSong.getViewCount();
                }
                updateStaticUI(intentSong);
            }
        }
    }

    private void initViews() {
        playerRootLayout = findViewById(R.id.playerRootLayout);
        btnBack = findViewById(R.id.btnBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        ivCover = findViewById(R.id.ivCover);
        tvTitle = findViewById(R.id.tvTitle);
        tvArtist = findViewById(R.id.tvArtist);
        ivArtistPlayerIcon = findViewById(R.id.ivArtistPlayerIcon);
        playerArtistContainer = findViewById(R.id.playerArtistContainer);
        tvViewCountPlayer = findViewById(R.id.tvViewCountPlayer);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnPlaylist = findViewById(R.id.btnPlaylist);
        btnLyrics = findViewById(R.id.btnLyrics);
        btnMorePlayer = findViewById(R.id.btnMorePlayer);
        cvAlbumArt = findViewById(R.id.cvAlbumArt);
        cvLyrics = findViewById(R.id.cvLyrics);
        tvLyrics = findViewById(R.id.tvLyrics);
        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnPrevious = findViewById(R.id.btnPrevious);
        fabPlayPause = findViewById(R.id.fabPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnRepeat = findViewById(R.id.btnRepeat);

        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && tvCurrentTime != null) {
                        tvCurrentTime.setText(formatTime(progress));
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { isUserSeeking = true; }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (mediaController != null) {
                        mediaController.seekTo(seekBar.getProgress());
                    }
                    isUserSeeking = false;
                }
            });
        }
    }

    private void setupClickListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        
        if (btnLyrics != null) {
            btnLyrics.setOnClickListener(v -> {
                if (cvLyrics != null && cvAlbumArt != null) {
                    boolean isVisible = cvLyrics.getVisibility() == View.VISIBLE;
                    cvLyrics.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                    cvAlbumArt.setVisibility(isVisible ? View.VISIBLE : View.GONE);
                    if (!isVisible && intentSong != null && tvLyrics != null) {
                        tvLyrics.setText(intentSong.getLyrics() != null && !intentSong.getLyrics().isEmpty()
                                ? intentSong.getLyrics() : "Không có lời bài hát.");
                    }
                }
            });
        }
        
        if (btnPlaylist != null) btnPlaylist.setOnClickListener(v -> showPlaylistDialog());
        if (btnMorePlayer != null) btnMorePlayer.setOnClickListener(v -> showMoreOptions());
        
        if (btnFavorite != null) {
            btnFavorite.setOnClickListener(v -> {
                if (intentSong == null) return;
                boolean targetState = !btnFavorite.isSelected();
                btnFavorite.setSelected(targetState);
                updateFavoriteIcon(targetState);

                new Thread(() -> {
                    AppDatabase db = AppDatabase.getInstance(PlayerActivity.this);
                    SongEntity existing = db.songDao().getSongById(intentSong.getId());
                    if (existing != null) {
                        db.songDao().updateFavoriteStatus(intentSong.getId(), targetState);
                    } else if (targetState) {
                        SongEntity entity = new SongEntity(
                            intentSong.getId(),
                            intentSong.getTitle(),
                            intentSong.getArtist(),
                            intentSong.getCoverUrl(),
                            intentSong.getAudioUrl(),
                            "",
                            intentSong.getLyrics(),
                            true
                        );
                        db.songDao().insertSong(entity);
                    }
                    
                    runOnUiThread(() -> {
                        String msg = targetState ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích";
                        Toast.makeText(PlayerActivity.this, msg, Toast.LENGTH_SHORT).show();
                    });
                }).start();
            });
        }

        if (playerArtistContainer != null) {
            playerArtistContainer.setOnClickListener(v -> {
                if (intentSong != null && intentSong.getArtistId() > 0) {
                    Intent intent = new Intent(this, ArtistDetailActivity.class);
                    intent.putExtra("artistId", intentSong.getArtistId());
                    intent.putExtra("artistName", intentSong.getArtist());
                    startActivity(intent);
                } else if (intentSong != null) {
                    Toast.makeText(this, "Không tìm thấy thông tin nghệ sĩ", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateStaticUI(Song song) {
        if (tvTitle != null) tvTitle.setText(song.getTitle());
        if (tvArtist != null) tvArtist.setText(song.getArtist());
        if (song.getViewCount() > 0) {
            currentDisplayViewCount = Math.max(currentDisplayViewCount, song.getViewCount());
        }
        if (tvViewCountPlayer != null) tvViewCountPlayer.setText(formatViewCount(currentDisplayViewCount));
        
        if (ivCover != null) {
            String coverUrl = getAbsoluteUrl(song.getCoverUrl());
            Glide.with(this).asBitmap().load(coverUrl)
                    .placeholder(R.drawable.ic_album)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            ivCover.setImageBitmap(resource);
                            applyDynamicBackground(resource);
                        }
                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {}
                    });
        }

        if (ivArtistPlayerIcon != null) {
            Glide.with(this)
                    .load(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivArtistPlayerIcon);
        }
        
        if (tvLyrics != null) {
            tvLyrics.setText(song.getLyrics() != null && !song.getLyrics().isEmpty() ? song.getLyrics() : "Không có lời bài hát.");
        }

        if (song.getId() > 0) {
            final int songId = song.getId();
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(PlayerActivity.this);
                SongEntity entity = db.songDao().getSongById(songId);
                final boolean isFav = (entity != null && entity.isFavorite());
                runOnUiThread(() -> {
                    if (intentSong != null && intentSong.getId() == songId && btnFavorite != null) {
                        btnFavorite.setSelected(isFav);
                        updateFavoriteIcon(isFav);
                    }
                });
            }).start();
        }
    }

    private void applyDynamicBackground(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette != null) {
                int dominantColor = palette.getDominantColor(0xFF282828);
                float[] hsv = new float[3];
                Color.colorToHSV(dominantColor, hsv);
                hsv[2] *= 0.35f; 
                int darkDominantColor = Color.HSVToColor(hsv);

                GradientDrawable gd = new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{darkDominantColor, 0xFF121212}
                );
                if (playerRootLayout != null) {
                    playerRootLayout.setBackground(gd);
                }

                WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
                if (controller != null) {
                    controller.setAppearanceLightStatusBars(hsv[2] > 0.75f);
                }
            }
        });
    }

    private void animateAlbumArt(boolean isPlaying) {
        if (cvAlbumArt == null) return;
        float scale = isPlaying ? 1.0f : 0.85f;
        float alpha = isPlaying ? 1.0f : 0.6f;
        
        cvAlbumArt.animate()
                .scaleX(scale)
                .scaleY(scale)
                .alpha(alpha)
                .setDuration(400)
                .start();
    }

    private String getAbsoluteUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty()) return "";
        if (relativeUrl.startsWith("http") || relativeUrl.startsWith("file") || relativeUrl.startsWith("content")) return relativeUrl;
        return BASE_URL + relativeUrl.replace("\\", "/");
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
                checkAndPlayIntentSong();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    private void checkAndPlayIntentSong() {
        if (mediaController == null || intentSong == null) return;
        MediaItem currentItem = mediaController.getCurrentMediaItem();
        String intentMediaId = String.valueOf(intentSong.getId());

        if (currentItem == null || !intentMediaId.equals(currentItem.mediaId)) {
            RetrofitClient.getApiService().increaseViewCount(intentSong.getId()).enqueue(new Callback<ViewCountResponse>() {
                @Override
                public void onResponse(Call<ViewCountResponse> call, Response<ViewCountResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        long count = response.body().getViewCount();
                        if (count > 0) {
                            currentDisplayViewCount = Math.max(currentDisplayViewCount, count);
                            intentSong.setViewCount(currentDisplayViewCount);
                            if (tvViewCountPlayer != null) {
                                tvViewCountPlayer.setText(formatViewCount(currentDisplayViewCount));
                            }
                        }
                    }
                }
                @Override public void onFailure(Call<ViewCountResponse> call, Throwable t) {}
            });
            updateStaticUI(intentSong);
            startPlaybackWithMetadata(currentDisplayViewCount > 0 ? currentDisplayViewCount : intentSong.getViewCount());
        } else {
            fetchViewCountFromServer(intentSong.getId());
            updateUIFromController();
        }
    }

    private void fetchViewCountFromServer(int songId) {
        RetrofitClient.getApiService().getSongById(songId).enqueue(new Callback<Song>() {
            @Override
            public void onResponse(Call<Song> call, Response<Song> response) {
                if (response.isSuccessful() && response.body() != null) {
                    long count = response.body().getViewCount();
                    if (count > 0) {
                        currentDisplayViewCount = Math.max(currentDisplayViewCount, count);
                        if (intentSong != null && intentSong.getId() == songId) {
                            intentSong.setViewCount(currentDisplayViewCount);
                        }
                        if (tvViewCountPlayer != null) {
                            tvViewCountPlayer.setText(formatViewCount(currentDisplayViewCount));
                        }
                    }
                }
            }
            @Override public void onFailure(Call<Song> call, Throwable t) {}
        });
    }

    private void startPlaybackWithMetadata(long viewCount) {
        if (mediaController == null || intentSong == null) return;
        
        String songUri = getAbsoluteUrl(intentSong.getAudioUrl());
        Bundle extras = new Bundle();
        extras.putString("lyrics", intentSong.getLyrics());
        extras.putLong("viewCount", viewCount);
        extras.putInt("artistId", intentSong.getArtistId());
        extras.putString("audioUrl", songUri); 
        
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(intentSong.getTitle())
                .setArtist(intentSong.getArtist())
                .setArtworkUri(Uri.parse(getAbsoluteUrl(intentSong.getCoverUrl())))
                .setExtras(extras)
                .build();
        
        MediaItem mediaItem = new MediaItem.Builder()
                .setMediaId(String.valueOf(intentSong.getId()))
                .setUri(Uri.parse(songUri))
                .setMediaMetadata(metadata)
                .build();
        
        mediaController.setMediaItem(mediaItem);
        mediaController.prepare();
        mediaController.play();
    }

    private void setupController() {
        if (mediaController == null) return;
        
        updateUIFromController();
        
        mediaController.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                updateUIFromController();
                if (mediaItem != null && mediaItem.mediaId != null) {
                    try {
                        int id = Integer.parseInt(mediaItem.mediaId);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> fetchViewCountFromServer(id), 300);
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseIcon(isPlaying);
                animateAlbumArt(isPlaying);
                if (isPlaying) startProgressUpdate(); else stopProgressUpdate();
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    updateDuration();
                }
            }
        });

        if (fabPlayPause != null) {
            fabPlayPause.setOnClickListener(v -> {
                if (mediaController.isPlaying()) mediaController.pause(); else mediaController.play();
            });
        }
        
        if (btnNext != null) btnNext.setOnClickListener(v -> mediaController.seekToNext());
        if (btnPrevious != null) btnPrevious.setOnClickListener(v -> mediaController.seekToPrevious());
        
        if (btnShuffle != null) {
            btnShuffle.setOnClickListener(v -> {
                boolean shuffleMode = !mediaController.getShuffleModeEnabled();
                mediaController.setShuffleModeEnabled(shuffleMode);
                updateShuffleIcon(shuffleMode);
            });
        }
        
        if (btnRepeat != null) {
            btnRepeat.setOnClickListener(v -> {
                int nextRepeatMode = (mediaController.getRepeatMode() + 1) % 3;
                mediaController.setRepeatMode(nextRepeatMode);
                updateRepeatIcon(nextRepeatMode);
            });
        }
    }

    private void updateDuration() {
        if (mediaController != null && seekBar != null) {
            long duration = mediaController.getDuration();
            if (duration > 0 && duration != Long.MAX_VALUE) {
                seekBar.setMax((int) duration);
                if (tvTotalTime != null) tvTotalTime.setText(formatTime((int) duration));
            }
        }
    }

    private void updateUIFromController() {
        if (mediaController == null) return;
        MediaItem item = mediaController.getCurrentMediaItem();
        if (item != null && item.mediaMetadata != null) {
            int id = -1;
            try { id = Integer.parseInt(item.mediaId); } catch (Exception ignored) {}

            if (tvTitle != null) tvTitle.setText(item.mediaMetadata.title != null ? item.mediaMetadata.title.toString() : "Unknown Title");
            if (tvArtist != null) tvArtist.setText(item.mediaMetadata.artist != null ? item.mediaMetadata.artist.toString() : "Unknown Artist");
            
            long viewCountFromMetadata = 0;
            int artistIdFromMetadata = -1;
            if (item.mediaMetadata.extras != null) {
                viewCountFromMetadata = item.mediaMetadata.extras.getLong("viewCount", 0);
                artistIdFromMetadata = item.mediaMetadata.extras.getInt("artistId", -1);
            }
            
            if (id != -1 && intentSong != null && intentSong.getId() != id) {
                currentDisplayViewCount = viewCountFromMetadata;
            } else {
                if (viewCountFromMetadata > 0) {
                    currentDisplayViewCount = Math.max(currentDisplayViewCount, viewCountFromMetadata);
                }
                if (intentSong != null && intentSong.getViewCount() > 0) {
                    currentDisplayViewCount = Math.max(currentDisplayViewCount, intentSong.getViewCount());
                }
            }
            
            if (tvViewCountPlayer != null) {
                tvViewCountPlayer.setText(formatViewCount(currentDisplayViewCount));
            }

            if (item.mediaMetadata.artworkUri != null) {
                Glide.with(this).asBitmap().load(item.mediaMetadata.artworkUri)
                        .placeholder(R.drawable.ic_album)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                if (ivCover != null) ivCover.setImageBitmap(resource);
                                applyDynamicBackground(resource);
                            }
                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {}
                        });
            }

            if (ivArtistPlayerIcon != null) {
                Glide.with(this)
                        .load(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivArtistPlayerIcon);
            }
            
            if (tvLyrics != null && item.mediaMetadata.extras != null) {
                String lyrics = item.mediaMetadata.extras.getString("lyrics", "");
                tvLyrics.setText(lyrics != null && !lyrics.isEmpty() ? lyrics : "Không có lời bài hát.");
            }
            
            if (id != -1) {
                if (intentSong != null && intentSong.getId() == id) {
                    intentSong.setViewCount(currentDisplayViewCount);
                    if (artistIdFromMetadata > 0) intentSong.setArtistId(artistIdFromMetadata);
                } else {
                    intentSong = new Song(id, 
                        item.mediaMetadata.title != null ? item.mediaMetadata.title.toString() : "", 
                        item.mediaMetadata.artist != null ? item.mediaMetadata.artist.toString() : "", 
                        item.mediaMetadata.artworkUri != null ? item.mediaMetadata.artworkUri.toString() : "", 
                        item.mediaMetadata.extras != null ? item.mediaMetadata.extras.getString("audioUrl", "") : "", 
                        item.mediaMetadata.extras != null ? item.mediaMetadata.extras.getString("lyrics", "") : "",
                        currentDisplayViewCount);
                    if (artistIdFromMetadata > 0) intentSong.setArtistId(artistIdFromMetadata);
                }

                final int currentSongId = id;
                new Thread(() -> {
                    AppDatabase db = AppDatabase.getInstance(PlayerActivity.this);
                    SongEntity entity = db.songDao().getSongById(currentSongId);
                    final boolean isFav = (entity != null && entity.isFavorite());
                    runOnUiThread(() -> {
                        if (intentSong != null && intentSong.getId() == currentSongId && btnFavorite != null) {
                            btnFavorite.setSelected(isFav);
                            updateFavoriteIcon(isFav);
                        }
                    });
                }).start();
            }
        }
        
        updatePlayPauseIcon(mediaController.isPlaying());
        animateAlbumArt(mediaController.isPlaying());
        updateRepeatIcon(mediaController.getRepeatMode());
        updateShuffleIcon(mediaController.getShuffleModeEnabled());
        updateDuration();
        
        if (mediaController.isPlaying()) {
            startProgressUpdate();
        }
    }

    private void updatePlayPauseIcon(boolean isPlaying) {
        if (fabPlayPause != null) {
            fabPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        }
    }

    private void updateRepeatIcon(int repeatMode) {
        if (btnRepeat == null) return;
        if (repeatMode == Player.REPEAT_MODE_OFF) {
            btnRepeat.setColorFilter(Color.WHITE);
            btnRepeat.setAlpha(0.6f);
        } else {
            btnRepeat.setColorFilter(ContextCompat.getColor(this, R.color.spotify_green));
            btnRepeat.setAlpha(1.0f);
        }
    }

    private void updateShuffleIcon(boolean shuffleMode) {
        if (btnShuffle == null) return;
        if (shuffleMode) {
            btnShuffle.setColorFilter(ContextCompat.getColor(this, R.color.spotify_green));
            btnShuffle.setAlpha(1.0f);
        } else {
            btnShuffle.setColorFilter(Color.WHITE);
            btnShuffle.setAlpha(0.6f);
        }
    }

    private void updateFavoriteIcon(boolean isFavorite) {
        if (btnFavorite == null) return;
        if (isFavorite) {
            btnFavorite.setColorFilter(ContextCompat.getColor(this, R.color.spotify_green));
        } else {
            btnFavorite.setColorFilter(Color.WHITE);
        }
    }

    private void showPlaylistDialog() {
        if (!AuthManager.getInstance(this).isLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        RetrofitClient.getApiService().getPlaylists().enqueue(new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isEmpty()) {
                        showCreatePlaylistDialog();
                    } else {
                        showSelectPlaylistDialog(response.body());
                    }
                }
            }
            @Override public void onFailure(Call<List<Playlist>> call, Throwable t) {}
        });
    }

    private void showSelectPlaylistDialog(List<Playlist> playlists) {
        String[] names = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) names[i] = playlists.get(i).getName();
        new AlertDialog.Builder(this)
                .setTitle("Thêm vào playlist")
                .setItems(names, (dialog, which) -> addSongToPlaylist(playlists.get(which).getId()))
                .setPositiveButton("Mới", (dialog, which) -> showCreatePlaylistDialog())
                .show();
    }

    private void showCreatePlaylistDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_create_playlist_simple, null);
        TextInputEditText etName = view.findViewById(R.id.etPlaylistName);
        new AlertDialog.Builder(this)
                .setTitle("Tạo playlist mới")
                .setView(view)
                .setPositiveButton("Tạo", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) createNewPlaylist(name);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void createNewPlaylist(String name) {
        RetrofitClient.getApiService().createPlaylist(new Playlist(name, "", "")).enqueue(new Callback<Playlist>() {
            @Override
            public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                if (response.isSuccessful() && response.body() != null) {
                    addSongToPlaylist(response.body().getId());
                }
            }
            @Override public void onFailure(Call<Playlist> call, Throwable t) {}
        });
    }

    private void addSongToPlaylist(int playlistId) {
        if (intentSong == null) return;
        RetrofitClient.getApiService().addSongToPlaylist(playlistId, new AddSongRequest(intentSong.getId())).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PlayerActivity.this, "Đã thêm vào playlist", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void showMoreOptions() {
        new AlertDialog.Builder(this).setItems(new String[]{"Chia sẻ", "Xem hàng đợi"}, (dialog, which) -> {
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        }).show();
    }

    private void startProgressUpdate() {
        stopProgressUpdate();
        progressHandler.post(updateProgressRunnable);
    }

    private void stopProgressUpdate() {
        progressHandler.removeCallbacks(updateProgressRunnable);
    }

    private final Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaController != null && mediaController.isPlaying() && !isUserSeeking) {
                long currentPos = mediaController.getCurrentPosition();
                long duration = mediaController.getDuration();
                
                if (seekBar != null) {
                    if (duration > 0 && duration != Long.MAX_VALUE && seekBar.getMax() != (int) duration) {
                        seekBar.setMax((int) duration);
                        if (tvTotalTime != null) tvTotalTime.setText(formatTime((int) duration));
                    }
                    seekBar.setProgress((int) currentPos);
                }
                
                if (tvCurrentTime != null) {
                    tvCurrentTime.setText(formatTime((int) currentPos));
                }

                progressHandler.postDelayed(this, 1000);
            }
        }
    };

    private String formatTime(int millis) {
        int sec = millis / 1000;
        return String.format(Locale.getDefault(), "%d:%02d", sec / 60, sec % 60);
    }

    private String formatViewCount(long count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1000000) return String.format(Locale.getDefault(), "%.1fk", count / 1000.0);
        return String.format(Locale.getDefault(), "%.1fM", count / 1000000.0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopProgressUpdate();
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            controllerFuture = null;
        }
        mediaController = null;
    }
}
