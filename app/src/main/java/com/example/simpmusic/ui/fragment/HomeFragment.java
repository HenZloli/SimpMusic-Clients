package com.example.simpmusic.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.AddSongRequest;
import com.example.simpmusic.data.model.Album;
import com.example.simpmusic.data.model.Artist;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.data.model.Song;
import com.example.simpmusic.data.model.UnreadCountResponse;
import com.example.simpmusic.ui.activity.AlbumDetailActivity;
import com.example.simpmusic.ui.activity.ArtistDetailActivity;
import com.example.simpmusic.ui.activity.CreatePlaylistActivity;
import com.example.simpmusic.ui.activity.LoginActivity;
import com.example.simpmusic.ui.activity.MainActivity;
import com.example.simpmusic.ui.activity.NotificationActivity;
import com.example.simpmusic.ui.activity.ProfileActivity;
import com.example.simpmusic.ui.adapter.AlbumAdapter;
import com.example.simpmusic.ui.adapter.SongAdapter;
import com.example.simpmusic.ui.dialog.PlaylistSelectionBottomSheet;
import com.example.simpmusic.utils.AuthManager;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements
        SongAdapter.OnSongClickListener,
        AlbumAdapter.OnAlbumClickListener,
        PlaylistSelectionBottomSheet.OnPlaylistSelectedListener {

    private RecyclerView songsRecyclerView, rvAlbums, rvRadio;
    private SongAdapter songAdapter, radioAdapter;
    private AlbumAdapter albumAdapter;

    private ImageView ivProfileIcon;
    private TextView tvNotificationBadge;
    private View btnNotifications;
    private FrameLayout flNotifications;
    private Chip chipAll, chipMusic, chipPodcasts;

    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout layoutOffline;
    private LinearLayout layoutHomeContent;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initViews(view);
        setupChips(view);
        setupSwipeRefresh();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkConnectionAndLoadData();
    }

    private boolean isNetworkAvailable() {
        if (getContext() == null) return false;
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void checkConnectionAndLoadData() {
        if (!isNetworkAvailable()) {
            if (layoutOffline != null) layoutOffline.setVisibility(View.VISIBLE);
            if (layoutHomeContent != null) layoutHomeContent.setVisibility(View.GONE);
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            
            // Tải avatar local nếu có thể hoặc giữ nguyên
            loadProfileAvatar();
        } else {
            if (layoutOffline != null) layoutOffline.setVisibility(View.GONE);
            if (layoutHomeContent != null) layoutHomeContent.setVisibility(View.VISIBLE);
            
            loadProfileAvatar();
            checkArtistRole();
            fetchAlbums();
            fetchSongs();
        }
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                checkConnectionAndLoadData();
            });
            swipeRefreshLayout.setColorSchemeResources(R.color.spotify_green);
            swipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.spotify_dark_grey));
        }
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        layoutOffline = view.findViewById(R.id.layoutOffline);
        layoutHomeContent = view.findViewById(R.id.layoutHomeContent);

        songsRecyclerView = view.findViewById(R.id.songsRecyclerView);
        if (songsRecyclerView != null) {
            songsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            songsRecyclerView.setNestedScrollingEnabled(false);
            songAdapter = new SongAdapter(new ArrayList<>(), this, R.layout.item_song);
            songsRecyclerView.setAdapter(songAdapter);
        }

        rvAlbums = view.findViewById(R.id.rvAlbums);
        if (rvAlbums != null) {
            rvAlbums.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            albumAdapter = new AlbumAdapter(new ArrayList<>(), this);
            rvAlbums.setAdapter(albumAdapter);
        }

        rvRadio = view.findViewById(R.id.rvRadio);
        if (rvRadio != null) {
            rvRadio.setLayoutManager(new LinearLayoutManager(getContext()));
            rvRadio.setNestedScrollingEnabled(false);
            radioAdapter = new SongAdapter(new ArrayList<>(), this, R.layout.item_song);
            rvRadio.setAdapter(radioAdapter);
        }

        ivProfileIcon = view.findViewById(R.id.ivProfileIcon);
        if (ivProfileIcon != null) {
            ivProfileIcon.setOnClickListener(v -> handleProfileNavigation());
        }

        flNotifications = view.findViewById(R.id.flNotifications);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge);

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), NotificationActivity.class));
            });
        }
    }

    private void setupChips(View view) {
        chipAll = view.findViewById(R.id.chipAll);
        chipMusic = view.findViewById(R.id.chipMusic);
        chipPodcasts = view.findViewById(R.id.chipPodcasts);

        TextView tvAlbumsTitle = view.findViewById(R.id.tvAlbumsTitle);
        TextView tvSongsTitle = view.findViewById(R.id.tvSongsTitle);
        TextView tvRadioTitle = view.findViewById(R.id.tvRadioTitle);

        View.OnClickListener chipListener = v -> {
            int selectedId = v.getId();

            if (chipAll != null) {
                chipAll.setChipBackgroundColorResource(selectedId == R.id.chipAll ? R.color.spotify_green : R.color.spotify_dark_grey);
                chipAll.setTextColor(selectedId == R.id.chipAll ? getResources().getColor(R.color.spotify_black) : getResources().getColor(R.color.white));
            }
            if (chipMusic != null) {
                chipMusic.setChipBackgroundColorResource(selectedId == R.id.chipMusic ? R.color.spotify_green : R.color.spotify_dark_grey);
                chipMusic.setTextColor(selectedId == R.id.chipMusic ? getResources().getColor(R.color.spotify_black) : getResources().getColor(R.color.white));
            }
            if (chipPodcasts != null) {
                chipPodcasts.setChipBackgroundColorResource(selectedId == R.id.chipPodcasts ? R.color.spotify_green : R.color.spotify_dark_grey);
                chipPodcasts.setTextColor(selectedId == R.id.chipPodcasts ? getResources().getColor(R.color.spotify_black) : getResources().getColor(R.color.white));
            }

            if (selectedId == R.id.chipAll) {
                if (tvAlbumsTitle != null) tvAlbumsTitle.setVisibility(View.VISIBLE);
                if (rvAlbums != null) rvAlbums.setVisibility(View.VISIBLE);
                if (tvSongsTitle != null) tvSongsTitle.setVisibility(View.VISIBLE);
                if (songsRecyclerView != null) songsRecyclerView.setVisibility(View.VISIBLE);
                if (tvRadioTitle != null) tvRadioTitle.setVisibility(View.VISIBLE);
                if (rvRadio != null) rvRadio.setVisibility(View.VISIBLE);
            } else if (selectedId == R.id.chipMusic) {
                if (tvAlbumsTitle != null) tvAlbumsTitle.setVisibility(View.GONE);
                if (rvAlbums != null) rvAlbums.setVisibility(View.GONE);
                if (tvSongsTitle != null) tvSongsTitle.setVisibility(View.VISIBLE);
                if (songsRecyclerView != null) songsRecyclerView.setVisibility(View.VISIBLE);
                if (tvRadioTitle != null) tvRadioTitle.setVisibility(View.GONE);
                if (rvRadio != null) rvRadio.setVisibility(View.GONE);
            } else if (selectedId == R.id.chipPodcasts) {
                if (tvAlbumsTitle != null) tvAlbumsTitle.setVisibility(View.GONE);
                if (rvAlbums != null) rvAlbums.setVisibility(View.GONE);
                if (tvSongsTitle != null) tvSongsTitle.setVisibility(View.GONE);
                if (songsRecyclerView != null) songsRecyclerView.setVisibility(View.GONE);
                if (tvRadioTitle != null) tvRadioTitle.setVisibility(View.VISIBLE);
                if (rvRadio != null) rvRadio.setVisibility(View.VISIBLE);
            }
        };

        if (chipAll != null) chipAll.setOnClickListener(chipListener);
        if (chipMusic != null) chipMusic.setOnClickListener(chipListener);
        if (chipPodcasts != null) chipPodcasts.setOnClickListener(chipListener);
    }

    private void loadProfileAvatar() {
        if (getContext() == null) return;
        AuthManager authManager = AuthManager.getInstance(getContext());
        if (!authManager.isLoggedIn()) {
            displayDefaultAvatar();
            return;
        }
        String role = authManager.getRole();
        if (isArtistRole(role)) {
            loadArtistAvatar();
        } else {
            loadUserAvatar();
        }
    }

    private boolean isArtistRole(String role) {
        if (role == null) return false;
        role = role.trim();
        return role.equalsIgnoreCase("Artist") || role.equals("1");
    }

    private void loadUserAvatar() {
        if (!isNetworkAvailable()) { displayDefaultAvatar(); return; }
        RetrofitClient.getApiService().getUserProfile().enqueue(new Callback<Artist>() {
            @Override
            public void onResponse(Call<Artist> call, Response<Artist> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    displayAvatar(response.body().getAvatarUrl());
                } else {
                    displayDefaultAvatar();
                }
            }
            @Override
            public void onFailure(Call<Artist> call, Throwable t) {
                if (!isAdded()) return;
                displayDefaultAvatar();
            }
        });
    }

    private void loadArtistAvatar() {
        if (!isNetworkAvailable()) { displayDefaultAvatar(); return; }
        RetrofitClient.getApiService().getUserProfile().enqueue(new Callback<Artist>() {
            @Override
            public void onResponse(Call<Artist> call, Response<Artist> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    displayDefaultAvatar();
                    return;
                }
                int artistId = response.body().getId();
                if (artistId <= 0) {
                    displayDefaultAvatar();
                    return;
                }
                loadArtistAvatarById(artistId);
            }
            @Override
            public void onFailure(Call<Artist> call, Throwable t) {
                if (!isAdded()) return;
                displayDefaultAvatar();
            }
        });
    }

    private void loadArtistAvatarById(int artistId) {
        if (!isNetworkAvailable()) { displayDefaultAvatar(); return; }
        RetrofitClient.getApiService().getArtistById(artistId).enqueue(new Callback<Artist>() {
            @Override
            public void onResponse(Call<Artist> call, Response<Artist> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    displayAvatar(response.body().getAvatarUrl());
                } else {
                    displayDefaultAvatar();
                }
            }
            @Override
            public void onFailure(Call<Artist> call, Throwable t) {
                if (!isAdded()) return;
                displayDefaultAvatar();
            }
        });
    }

    private void displayAvatar(String avatarUrl) {
        if (!isAdded() || ivProfileIcon == null) return;
        ivProfileIcon.setImageTintList(null);
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            displayDefaultAvatar();
            return;
        }
        String finalUrl = RetrofitClient.getAbsoluteUrl(avatarUrl);
        Glide.with(HomeFragment.this)
                .load(finalUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(ivProfileIcon);
    }

    private void displayDefaultAvatar() {
        if (!isAdded() || ivProfileIcon == null) return;
        ivProfileIcon.setImageTintList(null);
        Glide.with(HomeFragment.this)
                .load(R.drawable.ic_person)
                .circleCrop()
                .into(ivProfileIcon);
    }

    private void checkArtistRole() {
        if (getActivity() == null) return;
        AuthManager authManager = AuthManager.getInstance(getActivity());
        if (authManager.isLoggedIn() && isArtistRole(authManager.getRole())) {
            if (flNotifications != null) flNotifications.setVisibility(View.VISIBLE);
            fetchUnreadNotificationCount();
        } else {
            if (flNotifications != null) flNotifications.setVisibility(View.GONE);
        }
    }

    private void fetchUnreadNotificationCount() {
        if (!isNetworkAvailable()) return;
        RetrofitClient.getApiService().getUnreadCount().enqueue(new Callback<UnreadCountResponse>() {
            @Override
            public void onResponse(Call<UnreadCountResponse> call, Response<UnreadCountResponse> response) {
                if (!isAdded() || tvNotificationBadge == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    int count = response.body().getUnreadCount();
                    if (count > 0) {
                        tvNotificationBadge.setText(String.valueOf(count));
                        tvNotificationBadge.setVisibility(View.VISIBLE);
                    } else {
                        tvNotificationBadge.setVisibility(View.GONE);
                    }
                }
            }
            @Override public void onFailure(Call<UnreadCountResponse> call, Throwable t) {}
        });
    }

    private void fetchAlbums() {
        RetrofitClient.getApiService().getAllAlbums().enqueue(new Callback<List<Album>>() {
            @Override
            public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && albumAdapter != null) {
                    albumAdapter.setAlbums(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<Album>> call, Throwable t) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void fetchSongs() {
        RetrofitClient.getApiService().getAllSongs().enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    if (songAdapter != null) songAdapter.updateSongs(response.body());
                    if (radioAdapter != null) radioAdapter.updateSongs(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void handleProfileNavigation() {
        if (getContext() == null) return;
        AuthManager authManager = AuthManager.getInstance(getContext());
        if (!authManager.isLoggedIn()) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            return;
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToProfile();
        } else {
            startActivity(new Intent(getActivity(), ProfileActivity.class));
        }
    }

    @Override
    public void onSongClick(Song song) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onSongClick(song);
        }
    }

    @Override
    public void onArtistClick(Song song) {
        if (song.getArtistId() > 0) {
            Intent intent = new Intent(getActivity(), ArtistDetailActivity.class);
            intent.putExtra("artistId", song.getArtistId());
            intent.putExtra("artistName", song.getArtist());
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Không tìm thấy thông tin nghệ sĩ", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAlbumClick(Album album) {
        Intent intent = new Intent(getActivity(), AlbumDetailActivity.class);
        intent.putExtra("album", album);
        startActivity(intent);
    }

    @Override
    public void onPlaylistSelected(Playlist playlist, Song song) {
        addSongToPlaylist(playlist.getId(), song);
    }

    @Override
    public void onCreateNewPlaylist(Song song) {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), CreatePlaylistActivity.class);
            startActivity(intent);
        }
    }

    private void addSongToPlaylist(int id, Song song) {
        RetrofitClient.getApiService().addSongToPlaylist(id, new AddSongRequest(song.getId())).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Đã thêm vào playlist", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }
}
