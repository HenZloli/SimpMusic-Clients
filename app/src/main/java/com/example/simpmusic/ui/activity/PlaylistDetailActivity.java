package com.example.simpmusic.ui.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.local.AppDatabase;
import com.example.simpmusic.data.local.OfflinePlaylistEntity;
import com.example.simpmusic.data.local.PlaylistSongCrossRef;
import com.example.simpmusic.data.local.SongEntity;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.data.model.Song;
import com.example.simpmusic.data.model.ViewCountResponse;
import com.example.simpmusic.service.MusicService;
import com.example.simpmusic.ui.adapter.SongAdapter;
import com.example.simpmusic.ui.dialog.SongOptionsBottomSheet;
import com.example.simpmusic.utils.AuthManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistDetailActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener, SongOptionsBottomSheet.OnOptionClickListener {

    private View rootPlaylistDetail;
    private ImageView ivPlaylistCoverLarge, ivOwnerAvatar;
    private View layoutPlaylistGridLarge;
    private ImageView ivGridLarge1, ivGridLarge2, ivGridLarge3, ivGridLarge4;
    private TextView tvPlaylistNameDetail, tvPlaylistDescriptionDetail, tvOwnerName;
    private RecyclerView rvPlaylistSongs;
    private SongAdapter songAdapter;
    private FloatingActionButton fabPlayPlaylist;
    private ImageButton btnDownloadPlaylist;
    private Playlist playlist;

    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    private boolean isOfflineMode = false;
    private static final int FAVORITE_PLAYLIST_ID = -1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlist = (Playlist) getIntent().getSerializableExtra("playlist");
        if (playlist == null) {
            finish();
            return;
        }

        isOfflineMode = getIntent().getBooleanExtra("isOffline", false);
        Log.d("PlaylistDetail", "Opening playlist: " + playlist.getName() + " (ID: " + playlist.getId() + "), Offline: " + isOfflineMode);

        initViews();
        displayPlaylistInfo();
        
        if (isOfflineMode) {
            loadOfflineSongsForPlaylist(playlist.getId());
            if (btnDownloadPlaylist != null) {
                btnDownloadPlaylist.setVisibility(View.GONE);
            }
        } else {
            fetchPlaylistDetails();
        }
    }

    private void loadOfflineSongsForPlaylist(int playlistId) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<SongEntity> entities;
            if (playlistId == FAVORITE_PLAYLIST_ID) { 
                entities = db.songDao().getFavoriteSongs();
            } else {
                entities = db.offlinePlaylistDao().getSongsForPlaylist(playlistId);
            }

            List<Song> songs = new ArrayList<>();
            if (entities != null) {
                for (SongEntity entity : entities) {
                    String audioUrl = entity.getAudioUrl();
                    if (entity.getLocalAudioPath() != null && !entity.getLocalAudioPath().isEmpty()) {
                        File file = new File(entity.getLocalAudioPath());
                        if (file.exists()) {
                            audioUrl = Uri.fromFile(file).toString();
                        }
                    }
                    songs.add(new Song(
                            entity.getId(),
                            entity.getTitle(),
                            entity.getArtist(),
                            entity.getCoverUrl(),
                            audioUrl,
                            entity.getLyrics(),
                            0
                    ));
                }
            }
            final List<Song> finalSongs = songs;
            runOnUiThread(() -> {
                songAdapter.updateSongs(finalSongs);
                // Ensure name is correct
                if (playlistId == FAVORITE_PLAYLIST_ID) {
                    tvPlaylistNameDetail.setText("Bài hát đã thích");
                } else {
                    tvPlaylistNameDetail.setText(playlist.getName());
                }
                tvPlaylistDescriptionDetail.setText(finalSongs.size() + " bài hát offline");
                
                if (!finalSongs.isEmpty()) {
                    String firstCover = finalSongs.get(0).getCoverUrl();
                    loadCoverAndSetBackground(RetrofitClient.getAbsoluteUrl(firstCover), ivPlaylistCoverLarge);
                }
            });
        }).start();
    }

    private void initViews() {
        rootPlaylistDetail = findViewById(R.id.rootPlaylistDetail);
        ivPlaylistCoverLarge = findViewById(R.id.ivPlaylistCoverLarge);
        layoutPlaylistGridLarge = findViewById(R.id.layoutPlaylistGridLarge);
        ivGridLarge1 = findViewById(R.id.ivGridLarge1);
        ivGridLarge2 = findViewById(R.id.ivGridLarge2);
        ivGridLarge3 = findViewById(R.id.ivGridLarge3);
        ivGridLarge4 = findViewById(R.id.ivGridLarge4);
        
        ivOwnerAvatar = findViewById(R.id.ivOwnerAvatar);
        tvOwnerName = findViewById(R.id.tvOwnerName);
        
        tvPlaylistNameDetail = findViewById(R.id.tvPlaylistNameDetail);
        tvPlaylistDescriptionDetail = findViewById(R.id.tvPlaylistDescriptionDetail);
        rvPlaylistSongs = findViewById(R.id.rvPlaylistSongs);
        fabPlayPlaylist = findViewById(R.id.fabPlayPlaylist);
        btnDownloadPlaylist = findViewById(R.id.btnDownloadPlaylist);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvPlaylistSongs.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(new ArrayList<>(), this);
        rvPlaylistSongs.setAdapter(songAdapter);

        fabPlayPlaylist.setOnClickListener(v -> {
            if (songAdapter.getItemCount() > 0) {
                onSongClick(songAdapter.getSongs().get(0));
            }
        });

        if (btnDownloadPlaylist != null) {
            btnDownloadPlaylist.setOnClickListener(v -> downloadEntirePlaylist());
        }
    }

    private void downloadEntirePlaylist() {
        List<Song> songs = songAdapter.getSongs();
        if (songs == null || songs.isEmpty()) {
            Toast.makeText(this, "Playlist không có bài hát nào để tải", Toast.LENGTH_SHORT).show();
            return;
        }

        if (playlist.getId() == 0) {
            Toast.makeText(this, "Lỗi: ID Playlist bằng 0. Không thể lưu trữ offline chính xác.", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Đang tải playlist '" + playlist.getName() + "'...", Toast.LENGTH_SHORT).show();
        btnDownloadPlaylist.setEnabled(false);
        btnDownloadPlaylist.setAlpha(0.5f);

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(PlaylistDetailActivity.this);
            
            // Save Playlist Metadata
            OfflinePlaylistEntity offlinePlaylist = new OfflinePlaylistEntity(
                playlist.getId(),
                playlist.getName(),
                playlist.getDescription(),
                playlist.getCoverUrl()
            );
            db.offlinePlaylistDao().insertPlaylist(offlinePlaylist);
            db.offlinePlaylistDao().deleteCrossRefsForPlaylist(playlist.getId());

            int successCount = 0;
            File downloadDir = new File(getExternalFilesDir(null), "downloads");
            if (!downloadDir.exists()) downloadDir.mkdirs();

            String token = AuthManager.getInstance(PlaylistDetailActivity.this).getToken();

            for (Song song : songs) {
                try {
                    String audioUrl = RetrofitClient.getAbsoluteUrl(song.getAudioUrl());
                    String fileName = "song_" + song.getId() + ".mp3";
                    File localFile = new File(downloadDir, fileName);

                    if (!localFile.exists()) {
                        URL url = new URL(audioUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        if (token != null && !token.isEmpty()) {
                            connection.setRequestProperty("Authorization", "Bearer " + token);
                        }
                        connection.setConnectTimeout(10000);
                        connection.connect();

                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            InputStream input = connection.getInputStream();
                            FileOutputStream output = new FileOutputStream(localFile);
                            byte[] data = new byte[8192];
                            int count;
                            while ((count = input.read(data)) != -1) {
                                output.write(data, 0, count);
                            }
                            output.flush();
                            output.close();
                            input.close();
                        }
                        connection.disconnect();
                    }

                    // Save Song Metadata and Link to Playlist
                    SongEntity existing = db.songDao().getSongById(song.getId());
                    boolean isFav = (existing != null && existing.isFavorite());
                    
                    SongEntity entity = new SongEntity(
                        song.getId(),
                        song.getTitle(),
                        song.getArtist(),
                        song.getCoverUrl(),
                        Uri.fromFile(localFile).toString(),
                        localFile.getAbsolutePath(),
                        song.getLyrics(),
                        isFav
                    );
                    db.songDao().insertSong(entity);
                    db.offlinePlaylistDao().insertPlaylistSongCrossRef(new PlaylistSongCrossRef(playlist.getId(), song.getId()));
                    successCount++;
                } catch (Exception e) {
                    Log.e("PlaylistDetail", "Error downloading song: " + song.getTitle(), e);
                }
            }
            final int finalSuccessCount = successCount;
            runOnUiThread(() -> {
                btnDownloadPlaylist.setEnabled(true);
                btnDownloadPlaylist.setAlpha(1.0f);
                btnDownloadPlaylist.setColorFilter(ContextCompat.getColor(PlaylistDetailActivity.this, R.color.spotify_green));
                btnDownloadPlaylist.setImageResource(android.R.drawable.stat_sys_download_done);
                Toast.makeText(PlaylistDetailActivity.this, "Đã tải xong '" + playlist.getName() + "' (" + finalSuccessCount + " bài)", Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private void displayPlaylistInfo() {
        if (playlist == null) return;
        tvPlaylistNameDetail.setText(playlist.getName());

        String username = AuthManager.getInstance(this).getUsername();
        if (username == null || username.isEmpty()) {
            username = AuthManager.getInstance(this).getEmail();
            if (username != null && username.contains("@")) username = username.split("@")[0];
        }
        tvOwnerName.setText(username != null ? username : "Người dùng");

        List<Song> songs = playlist.getSongs();
        if (songs != null) {
            tvPlaylistDescriptionDetail.setText(songs.size() + " bài hát");
        } else {
            tvPlaylistDescriptionDetail.setText("0 bài hát");
        }

        String colorSourceUrl = (songs != null && !songs.isEmpty()) ? songs.get(0).getCoverUrl() : playlist.getCoverUrl();

        if (songs != null && songs.size() >= 4) {
            ivPlaylistCoverLarge.setVisibility(View.GONE);
            layoutPlaylistGridLarge.setVisibility(View.VISIBLE);
            loadLargeGridImage(songs.get(0).getCoverUrl(), ivGridLarge1, true);
            loadLargeGridImage(songs.get(1).getCoverUrl(), ivGridLarge2, false);
            loadLargeGridImage(songs.get(2).getCoverUrl(), ivGridLarge3, false);
            loadLargeGridImage(songs.get(3).getCoverUrl(), ivGridLarge4, false);
        } else {
            ivPlaylistCoverLarge.setVisibility(View.VISIBLE);
            layoutPlaylistGridLarge.setVisibility(View.GONE);
            loadCoverAndSetBackground(RetrofitClient.getAbsoluteUrl(colorSourceUrl), ivPlaylistCoverLarge);
        }
    }

    private void loadLargeGridImage(String url, ImageView imageView, boolean isFirst) {
        String absoluteUrl = RetrofitClient.getAbsoluteUrl(url);
        if (isFirst) {
            loadCoverAndSetBackground(absoluteUrl, imageView);
        } else {
            Glide.with(this).load(absoluteUrl).centerCrop().into(imageView);
        }
    }

    private void loadCoverAndSetBackground(String url, ImageView imageView) {
        Glide.with(this)
                .asBitmap()
                .load(url)
                .placeholder(R.drawable.ic_album)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        imageView.setImageBitmap(resource);
                        applyDynamicColor(resource);
                    }
                    @Override public void onLoadCleared(@Nullable android.graphics.drawable.Drawable p) { }
                });
    }

    private void applyDynamicColor(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette != null) {
                int color = palette.getVibrantColor(palette.getDominantColor(Color.parseColor("#333333")));
                float[] hsv = new float[3];
                Color.colorToHSV(color, hsv);
                
                // Giảm nhẹ độ sáng để phần dưới hòa trộn mượt mà với nền đen, tránh bất đồng bộ màu sắc
                hsv[1] *= 0.8f; // bão hòa nhẹ
                hsv[2] *= 0.25f; // làm sẫm rõ rệt để hòa trộn mượt
                int darkColor = Color.HSVToColor(hsv);

                GradientDrawable gradient = new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{darkColor, Color.parseColor("#121212")}
                );
                rootPlaylistDetail.setBackground(gradient);
            }
        });
    }

    private void fetchPlaylistDetails() {
        RetrofitClient.getApiService().getPlaylistById(playlist.getId()).enqueue(new Callback<Playlist>() {
            @Override
            public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                if (response.isSuccessful() && response.body() != null) {
                    playlist = response.body();
                    if (playlist.getSongs() != null) songAdapter.updateSongs(playlist.getSongs());
                    displayPlaylistInfo();
                }
            }
            @Override public void onFailure(Call<Playlist> call, Throwable t) { }
        });
    }

    @Override
    public void onSongClick(Song clickedSong) {
        if (mediaController != null) {
            List<Song> currentSongs = songAdapter.getSongs();
            List<MediaItem> mediaItems = new ArrayList<>();
            int startIndex = 0;

            for (int i = 0; i < currentSongs.size(); i++) {
                Song song = currentSongs.get(i);
                if (song.getId() == clickedSong.getId()) startIndex = i;

                String songUri = song.getAudioUrl();
                if (!songUri.startsWith("file") && !songUri.startsWith("content")) {
                    songUri = RetrofitClient.getAbsoluteUrl(songUri);
                }

                Bundle bundle = new Bundle();
                bundle.putString("lyrics", song.getLyrics());
                bundle.putString("audioUrl", songUri);

                MediaMetadata metadata = new MediaMetadata.Builder()
                        .setTitle(song.getTitle())
                        .setArtist(song.getArtist())
                        .setArtworkUri(Uri.parse(RetrofitClient.getAbsoluteUrl(song.getCoverUrl())))
                        .setExtras(bundle)
                        .build();

                MediaItem mediaItem = new MediaItem.Builder()
                        .setMediaId(String.valueOf(song.getId()))
                        .setUri(Uri.parse(songUri))
                        .setMediaMetadata(metadata)
                        .build();
                mediaItems.add(mediaItem);
            }

            mediaController.setMediaItems(mediaItems, startIndex, 0);
            mediaController.prepare();
            mediaController.play();
            
            if (playlist.getId() != FAVORITE_PLAYLIST_ID && !isOfflineMode) {
                RetrofitClient.getApiService().increaseViewCount(clickedSong.getId()).enqueue(new Callback<ViewCountResponse>() {
                    @Override public void onResponse(Call<ViewCountResponse> call, Response<ViewCountResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            clickedSong.setViewCount(response.body().getViewCount());
                            songAdapter.notifyDataSetChanged();
                        }
                    }
                    @Override public void onFailure(Call<ViewCountResponse> call, Throwable t) {}
                });
            }
        }
        startActivity(new Intent(this, PlayerActivity.class).putExtra("song", clickedSong));
    }

    @Override
    protected void onStart() {
        super.onStart();
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MusicService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try { mediaController = controllerFuture.get(); } catch (Exception e) { }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
    }

    @Override public void onArtistClick(Song song) {
        if (song.getArtistId() > 0) {
            startActivity(new Intent(this, ArtistDetailActivity.class)
                    .putExtra("artistId", song.getArtistId())
                    .putExtra("artistName", song.getArtist()));
        }
    }

    @Override public void onMoreClick(Song song) {
        SongOptionsBottomSheet sheet = new SongOptionsBottomSheet(song);
        sheet.setOnOptionClickListener(this);
        sheet.show(getSupportFragmentManager(), "SongOptions");
    }

    @Override public void onAddToPlaylist(Song song) { }
    @Override public void onViewAlbum(Song song) { }
    @Override
    public void onShare(Song song) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Đang nghe: " + song.getTitle() + " - " + song.getArtist());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, null));
    }
}
