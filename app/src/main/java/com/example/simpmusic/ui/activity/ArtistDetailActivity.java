package com.example.simpmusic.ui.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.AddSongRequest;
import com.example.simpmusic.data.model.Album;
import com.example.simpmusic.data.model.Artist;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.data.model.Song;
import com.example.simpmusic.data.model.ViewCountResponse;
import com.example.simpmusic.service.MusicService;
import com.example.simpmusic.ui.adapter.AlbumAdapter;
import com.example.simpmusic.ui.adapter.SongAdapter;
import com.example.simpmusic.ui.dialog.PlaylistSelectionBottomSheet;
import com.example.simpmusic.ui.dialog.SongOptionsBottomSheet;
import com.example.simpmusic.utils.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArtistDetailActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener, AlbumAdapter.OnAlbumClickListener, SongOptionsBottomSheet.OnOptionClickListener, PlaylistSelectionBottomSheet.OnPlaylistSelectedListener {

    private ImageView ivArtistAvatarLarge;
    private TextView tvArtistNameDetail, tvArtistBioDetail;
    private MaterialButton btnFollowArtist;
    private RecyclerView rvArtistSongs, rvArtistAlbums;
    
    private SongAdapter songAdapter;
    private AlbumAdapter albumAdapter;
    
    private int artistId;
    private String artistName;
    private boolean isFollowed = false;
    private boolean isFollowStateChecked = false;

    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_detail);

        artistId = getIntent().getIntExtra("artistId", -1);
        artistName = getIntent().getStringExtra("artistName");

        if (artistId == -1) {
            finish();
            return;
        }

        initViews();
        fetchArtistInfo();
        fetchArtistSongsAndAlbums();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkFollowStatus();
    }

    private void initViews() {
        ivArtistAvatarLarge = findViewById(R.id.ivArtistAvatarLarge);
        tvArtistNameDetail = findViewById(R.id.tvArtistNameDetail);
        tvArtistBioDetail = findViewById(R.id.tvArtistBioDetail);
        btnFollowArtist = findViewById(R.id.btnFollowArtist);
        rvArtistSongs = findViewById(R.id.rvArtistSongs);
        rvArtistAlbums = findViewById(R.id.rvArtistAlbums);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        if (artistName != null) tvArtistNameDetail.setText(artistName);

        if (btnFollowArtist != null) {
            btnFollowArtist.setVisibility(View.INVISIBLE);
            
            // Ngăn chặn việc tự follow chính mình
            if (artistId == AuthManager.getInstance(this).getUserId()) {
                btnFollowArtist.setVisibility(View.GONE);
            } else {
                btnFollowArtist.setOnClickListener(v -> {
                    if (!AuthManager.getInstance(this).isLoggedIn()) {
                        startActivity(new Intent(this, LoginActivity.class));
                        return;
                    }
                    toggleFollowStatus();
                });
            }
        }

        rvArtistSongs.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(new ArrayList<>(), this);
        rvArtistSongs.setAdapter(songAdapter);

        rvArtistAlbums.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        albumAdapter = new AlbumAdapter(new ArrayList<>(), this);
        rvArtistAlbums.setAdapter(albumAdapter);
    }

    private void checkFollowStatus() {
        if (!AuthManager.getInstance(this).isLoggedIn() || artistId == AuthManager.getInstance(this).getUserId()) {
            isFollowed = false;
            isFollowStateChecked = true;
            if (artistId != AuthManager.getInstance(this).getUserId()) {
                updateFollowButtonUI();
            }
            return;
        }

        RetrofitClient.getApiService().getFollowingArtists().enqueue(new Callback<List<Artist>>() {
            @Override
            public void onResponse(Call<List<Artist>> call, Response<List<Artist>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean found = false;
                    for (Artist a : response.body()) {
                        if (a.getId() == artistId) {
                            found = true;
                            break;
                        }
                    }
                    isFollowed = found;
                    isFollowStateChecked = true;
                    updateFollowButtonUI();
                } else {
                    showButtonAnyway();
                }
            }
            @Override public void onFailure(Call<List<Artist>> call, Throwable t) { showButtonAnyway(); }
        });
    }

    private void showButtonAnyway() {
        isFollowStateChecked = true;
        if (btnFollowArtist != null && artistId != AuthManager.getInstance(this).getUserId()) {
            btnFollowArtist.setVisibility(View.VISIBLE);
        }
    }

    private void updateFollowButtonUI() {
        if (btnFollowArtist == null || artistId == AuthManager.getInstance(this).getUserId()) return;
        
        btnFollowArtist.setVisibility(View.VISIBLE);
        if (isFollowed) {
            btnFollowArtist.setText("Đang theo dõi");
            btnFollowArtist.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.spotify_green));
            btnFollowArtist.setTextColor(ContextCompat.getColor(this, R.color.spotify_black));
            btnFollowArtist.setStrokeColor(ContextCompat.getColorStateList(this, android.R.color.transparent));
        } else {
            btnFollowArtist.setText("Theo dõi");
            btnFollowArtist.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.spotify_dark_grey));
            btnFollowArtist.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnFollowArtist.setStrokeColor(ContextCompat.getColorStateList(this, R.color.white));
        }
    }

    private void toggleFollowStatus() {
        if (!isFollowStateChecked) return;

        btnFollowArtist.setEnabled(false);
        if (isFollowed) {
            RetrofitClient.getApiService().unfollowArtist(artistId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    btnFollowArtist.setEnabled(true);
                    if (response.isSuccessful()) {
                        isFollowed = false;
                        updateFollowButtonUI();
                        Toast.makeText(ArtistDetailActivity.this, "Đã bỏ theo dõi", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<Void> call, Throwable t) { btnFollowArtist.setEnabled(true); }
            });
        } else {
            RetrofitClient.getApiService().followArtist(artistId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    btnFollowArtist.setEnabled(true);
                    if (response.isSuccessful()) {
                        isFollowed = true;
                        updateFollowButtonUI();
                        Toast.makeText(ArtistDetailActivity.this, "Đã theo dõi", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<Void> call, Throwable t) { btnFollowArtist.setEnabled(true); }
            });
        }
    }

    private void fetchArtistInfo() {
        RetrofitClient.getApiService().getArtistById(artistId).enqueue(new Callback<Artist>() {
            @Override
            public void onResponse(Call<Artist> call, Response<Artist> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Artist artist = response.body();
                    tvArtistNameDetail.setText(artist.getName());
                    if (artist.getBio() != null && !artist.getBio().isEmpty()) tvArtistBioDetail.setText(artist.getBio());
                    
                    if (ivArtistAvatarLarge != null) {
                        ivArtistAvatarLarge.setImageTintList(null);
                        
                        Glide.with(ArtistDetailActivity.this)
                                .load(artist.getAvatarUrl())
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(ivArtistAvatarLarge);
                    }
                }
            }
            @Override public void onFailure(Call<Artist> call, Throwable t) {}
        });
    }

    private void fetchArtistSongsAndAlbums() {
        RetrofitClient.getApiService().getSongsByArtist(artistId).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) songAdapter.updateSongs(response.body());
            }
            @Override public void onFailure(Call<List<Song>> call, Throwable t) {}
        });

        RetrofitClient.getApiService().getAlbumsByArtist(artistId).enqueue(new Callback<List<Album>>() {
            @Override
            public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                if (response.isSuccessful() && response.body() != null) albumAdapter.setAlbums(response.body());
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
            try { mediaController = controllerFuture.get(); } catch (Exception ignored) {}
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
    }

    @Override
    public void onSongClick(Song song) {
        if (mediaController != null) {
            RetrofitClient.getApiService().increaseViewCount(song.getId()).enqueue(new Callback<ViewCountResponse>() {
                @Override 
                public void onResponse(Call<ViewCountResponse> call, Response<ViewCountResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        song.setViewCount(response.body().getViewCount());
                        if (songAdapter != null) songAdapter.notifyDataSetChanged();
                    }
                }
                @Override public void onFailure(Call<ViewCountResponse> call, Throwable t) {}
            });

            MediaMetadata metadata = new MediaMetadata.Builder()
                    .setTitle(song.getTitle()).setArtist(song.getArtist())
                    .setArtworkUri(Uri.parse(RetrofitClient.getAbsoluteUrl(song.getCoverUrl())))
                    .build();

            MediaItem mediaItem = new MediaItem.Builder()
                    .setMediaId(String.valueOf(song.getId())).setUri(Uri.parse(RetrofitClient.getAbsoluteUrl(song.getAudioUrl())))
                    .setMediaMetadata(metadata).build();

            mediaController.setMediaItem(mediaItem);
            mediaController.prepare();
            mediaController.play();
        }
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("song", song);
        startActivity(intent);
    }

    @Override public void onAlbumClick(Album album) {
        Intent intent = new Intent(this, AlbumDetailActivity.class);
        intent.putExtra("album", album);
        startActivity(intent);
    }

    @Override public void onMoreClick(Song song) {
        SongOptionsBottomSheet bottomSheet = new SongOptionsBottomSheet(song);
        bottomSheet.setOnOptionClickListener(this);
        bottomSheet.show(getSupportFragmentManager(), "SongOptions");
    }

    @Override public void onAddToPlaylist(Song song) {
        PlaylistSelectionBottomSheet selectionSheet = new PlaylistSelectionBottomSheet(song);
        selectionSheet.setOnPlaylistSelectedListener(this);
        selectionSheet.show(getSupportFragmentManager(), "PlaylistSelection");
    }

    @Override public void onPlaylistSelected(Playlist playlist, Song song) { addSongToPlaylist(playlist.getId(), song); }

    @Override public void onCreateNewPlaylist(Song song) {
        View view = getLayoutInflater().inflate(R.layout.dialog_create_playlist_simple, null);
        TextInputEditText etName = view.findViewById(R.id.etPlaylistName);
        new AlertDialog.Builder(this)
                .setTitle("Tạo playlist mới").setView(view)
                .setPositiveButton("Tạo", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) createNewPlaylist(name, song);
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void createNewPlaylist(String name, Song song) {
        RetrofitClient.getApiService().createPlaylist(new Playlist(name, "", "")).enqueue(new Callback<Playlist>() {
            @Override
            public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                if (response.isSuccessful() && response.body() != null) addSongToPlaylist(response.body().getId(), song);
            }
            @Override public void onFailure(Call<Playlist> call, Throwable t) {}
        });
    }

    private void addSongToPlaylist(int playlistId, Song song) {
        RetrofitClient.getApiService().addSongToPlaylist(playlistId, new AddSongRequest(song.getId())).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) Toast.makeText(ArtistDetailActivity.this, "Đã thêm vào playlist", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    @Override public void onViewAlbum(Song song) {}
    @Override public void onShare(Song song) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Đang nghe: " + song.getTitle() + " - " + song.getArtist());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, null));
    }
}
