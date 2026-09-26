package com.example.simpmusic.ui.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import com.example.simpmusic.data.model.AddSongRequest;
import com.example.simpmusic.data.model.Album;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.data.model.Song;
import com.example.simpmusic.data.model.ViewCountResponse;
import com.example.simpmusic.service.MusicService;
import com.example.simpmusic.ui.adapter.SongAdapter;
import com.example.simpmusic.ui.dialog.PlaylistSelectionBottomSheet;
import com.example.simpmusic.ui.dialog.SongOptionsBottomSheet;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlbumDetailActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener, SongOptionsBottomSheet.OnOptionClickListener, PlaylistSelectionBottomSheet.OnPlaylistSelectedListener {

    private View rootAlbumDetail;
    private ImageView ivAlbumCoverLarge;
    private TextView tvAlbumTitleDetail, tvAlbumDescriptionDetail;
    private RecyclerView rvAlbumSongs;
    private SongAdapter songAdapter;
    private ImageButton btnSaveAlbum;
    private FloatingActionButton fabPlayAlbum;
    private Album album;
    private boolean isSaved = false;

    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        album = (Album) getIntent().getSerializableExtra("album");
        if (album == null) {
            finish();
            return;
        }

        initViews();
        displayAlbumInfo();
        fetchAlbumDetails();
        checkSaveStatus();
    }

    private void initViews() {
        rootAlbumDetail = findViewById(R.id.rootAlbumDetail);
        ivAlbumCoverLarge = findViewById(R.id.ivAlbumCoverLarge);
        tvAlbumTitleDetail = findViewById(R.id.tvAlbumTitleDetail);
        tvAlbumDescriptionDetail = findViewById(R.id.tvAlbumDescriptionDetail);
        rvAlbumSongs = findViewById(R.id.rvAlbumSongs);
        btnSaveAlbum = findViewById(R.id.btnSaveAlbum);
        fabPlayAlbum = findViewById(R.id.fabPlayAlbum);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvAlbumSongs.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(new ArrayList<>(), this);
        rvAlbumSongs.setAdapter(songAdapter);

        btnSaveAlbum.setOnClickListener(v -> toggleSaveStatus());
        
        fabPlayAlbum.setOnClickListener(v -> {
            if (songAdapter.getItemCount() > 0) {
                onSongClick(songAdapter.getSongs().get(0));
            }
        });
    }

    private void displayAlbumInfo() {
        tvAlbumTitleDetail.setText(album.getTitle());
        tvAlbumDescriptionDetail.setText(album.getDescription() != null ? album.getDescription() : "Album");

        // Dynamic Background based on Album Cover
        String coverUrl = RetrofitClient.getAbsoluteUrl(album.getCoverUrl());
        Glide.with(this)
                .asBitmap()
                .load(coverUrl)
                .placeholder(R.drawable.ic_album)
                .error(R.drawable.ic_album)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        ivAlbumCoverLarge.setImageBitmap(resource);
                        applyDynamicColor(resource);
                    }
                    @Override public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) { }
                });
    }

    private void applyDynamicColor(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette != null) {
                int color = palette.getVibrantColor(palette.getDominantColor(Color.parseColor("#121212")));
                float[] hsv = new float[3];
                Color.colorToHSV(color, hsv);
                hsv[1] *= 0.8f; hsv[2] *= 0.4f; 
                int darkColor = Color.HSVToColor(hsv);
                GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{darkColor, Color.parseColor("#121212")});
                if (rootAlbumDetail != null) rootAlbumDetail.setBackground(gradient);
            }
        });
    }

    private void fetchAlbumDetails() {
        RetrofitClient.getApiService().getAlbumById(album.getId()).enqueue(new Callback<Album>() {
            @Override
            public void onResponse(Call<Album> call, Response<Album> response) {
                if (response.isSuccessful() && response.body() != null) {
                    album = response.body();
                    displayAlbumInfo();
                }
            }
            @Override public void onFailure(Call<Album> call, Throwable t) {}
        });

        RetrofitClient.getApiService().getSongsByAlbum(album.getId()).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Song> songs = response.body();
                    songAdapter.updateSongs(songs);
                    tvAlbumDescriptionDetail.setText(album.getDescription() + " • " + songs.size() + " bài hát");
                }
            }
            @Override public void onFailure(Call<List<Song>> call, Throwable t) {
                Toast.makeText(AlbumDetailActivity.this, "Không thể tải danh sách nhạc", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkSaveStatus() {
        RetrofitClient.getApiService().checkIfAlbumSaved(album.getId()).enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (response.isSuccessful() && response.body() != null) {
                    isSaved = response.body();
                    updateSaveButtonUI();
                }
            }
            @Override public void onFailure(Call<Boolean> call, Throwable t) {
                isSaved = false;
                updateSaveButtonUI();
            }
        });
    }

    private void toggleSaveStatus() {
        btnSaveAlbum.setEnabled(false);
        if (isSaved) {
            RetrofitClient.getApiService().unsaveAlbum(album.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    btnSaveAlbum.setEnabled(true);
                    if (response.isSuccessful()) {
                        isSaved = false;
                        updateSaveButtonUI();
                        Toast.makeText(AlbumDetailActivity.this, "Đã xóa khỏi thư viện", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<Void> call, Throwable t) { btnSaveAlbum.setEnabled(true); }
            });
        } else {
            RetrofitClient.getApiService().saveAlbum(album.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    btnSaveAlbum.setEnabled(true);
                    if (response.isSuccessful()) {
                        isSaved = true;
                        updateSaveButtonUI();
                        Toast.makeText(AlbumDetailActivity.this, "Đã lưu vào thư viện", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<Void> call, Throwable t) { btnSaveAlbum.setEnabled(true); }
            });
        }
    }

    private void updateSaveButtonUI() {
        if (isSaved) {
            btnSaveAlbum.setImageResource(R.drawable.ic_check);
            btnSaveAlbum.setColorFilter(getResources().getColor(R.color.spotify_green));
        } else {
            btnSaveAlbum.setImageResource(R.drawable.ic_add);
            btnSaveAlbum.setColorFilter(getResources().getColor(R.color.white));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MusicService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
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

                Bundle extras = new Bundle();
                extras.putString("lyrics", song.getLyrics());
                extras.putLong("viewCount", song.getViewCount());
                extras.putInt("artistId", song.getArtistId());

                MediaMetadata metadata = new MediaMetadata.Builder()
                        .setTitle(song.getTitle())
                        .setArtist(song.getArtist())
                        .setArtworkUri(Uri.parse(RetrofitClient.getAbsoluteUrl(song.getCoverUrl())))
                        .setExtras(extras)
                        .build();

                MediaItem mediaItem = new MediaItem.Builder()
                        .setMediaId(String.valueOf(song.getId()))
                        .setUri(Uri.parse(RetrofitClient.getAbsoluteUrl(song.getAudioUrl())))
                        .setMediaMetadata(metadata)
                        .build();

                mediaItems.add(mediaItem);
            }

            mediaController.setMediaItems(mediaItems, startIndex, 0);
            mediaController.prepare();
            mediaController.play();

            // Increase view count
            RetrofitClient.getApiService().increaseViewCount(clickedSong.getId()).enqueue(new Callback<ViewCountResponse>() {
                @Override
                public void onResponse(Call<ViewCountResponse> call, Response<ViewCountResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        clickedSong.setViewCount(response.body().getViewCount());
                        songAdapter.notifyDataSetChanged();
                    }
                }
                @Override public void onFailure(Call<ViewCountResponse> call, Throwable t) {}
            });
        }

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("song", clickedSong);
        startActivity(intent);
    }

    @Override
    public void onArtistClick(Song song) {
        if (song.getArtistId() > 0) {
            Intent intent = new Intent(this, ArtistDetailActivity.class);
            intent.putExtra("artistId", song.getArtistId());
            intent.putExtra("artistName", song.getArtist());
            startActivity(intent);
        }
    }

    @Override
    public void onMoreClick(Song song) {
        SongOptionsBottomSheet bottomSheet = new SongOptionsBottomSheet(song);
        bottomSheet.setOnOptionClickListener(this);
        bottomSheet.show(getSupportFragmentManager(), "SongOptions");
    }

    @Override
    public void onAddToPlaylist(Song song) {
        PlaylistSelectionBottomSheet selectionSheet = new PlaylistSelectionBottomSheet(song);
        selectionSheet.setOnPlaylistSelectedListener(this);
        selectionSheet.show(getSupportFragmentManager(), "PlaylistSelection");
    }

    @Override
    public void onPlaylistSelected(Playlist playlist, Song song) {
        addSongToPlaylist(playlist.getId(), song);
    }

    @Override
    public void onCreateNewPlaylist(Song song) {
        showCreatePlaylistDialog(song);
    }

    @Override
    public void onViewAlbum(Song song) {
        Toast.makeText(this, "Bạn đang xem album này", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onShare(Song song) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Đang nghe: " + song.getTitle() + " - " + song.getArtist());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, null));
    }

    private void showCreatePlaylistDialog(Song song) {
        View view = getLayoutInflater().inflate(R.layout.dialog_create_playlist_simple, null);
        TextInputEditText etName = view.findViewById(R.id.etPlaylistName);
        new AlertDialog.Builder(this)
                .setTitle("Tạo playlist mới")
                .setView(view)
                .setPositiveButton("Tạo", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) createNewPlaylist(name, song);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void createNewPlaylist(String name, Song song) {
        RetrofitClient.getApiService().createPlaylist(new Playlist(name, "", "")).enqueue(new Callback<Playlist>() {
            @Override
            public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                if (response.isSuccessful() && response.body() != null) {
                    addSongToPlaylist(response.body().getId(), song);
                }
            }
            @Override public void onFailure(Call<Playlist> call, Throwable t) {}
        });
    }

    private void addSongToPlaylist(int playlistId, Song song) {
        RetrofitClient.getApiService().addSongToPlaylist(playlistId, new AddSongRequest(song.getId())).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AlbumDetailActivity.this, "Đã thêm vào playlist", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }
}
