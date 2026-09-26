package com.example.simpmusic.ui.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.Album;
import com.example.simpmusic.data.model.Artist;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.data.model.Song;
import com.example.simpmusic.ui.activity.FollowingActivity;
import com.example.simpmusic.ui.activity.MainActivity;
import com.example.simpmusic.ui.activity.PlaylistDetailActivity;
import com.example.simpmusic.ui.activity.AlbumDetailActivity;
import com.example.simpmusic.ui.activity.PlayerActivity;
import com.example.simpmusic.ui.adapter.AlbumAdapter;
import com.example.simpmusic.ui.adapter.PlaylistAdapter;
import com.example.simpmusic.ui.adapter.SongAdapter;
import com.example.simpmusic.utils.AuthManager;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment implements 
        PlaylistAdapter.OnPlaylistClickListener, 
        AlbumAdapter.OnAlbumClickListener, 
        SongAdapter.OnSongClickListener {

    private TextView tvProfileName, tvFollowersCount, tvFollowingCount;
    private ImageView ivProfileLarge;
    private MaterialButton btnEditProfile;
    private ImageButton btnBack, btnLogoutTop;
    private View btnManagePlaylists, profileRootLayout;
    
    // Tab Layout Views
    private View layoutProfileTabs;
    private TextView tabCreatedContent, tabSavedContent;
    private View containerCreatedContent, containerSavedContent;
    private TextView tvCreatedEmptyState;

    private View layoutArtistAlbums, layoutArtistSongs;
    private RecyclerView rvMyPlaylists, rvArtistAlbums, rvArtistSongs;

    private PlaylistAdapter playlistAdapter;
    private AlbumAdapter albumAdapter;
    private SongAdapter songAdapter;

    private String currentAvatarUrl;
    private String currentBio;
    private int currentArtistId = -1;

    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null) {

                            selectedImageUri = result.getData().getData();

                            if (selectedImageUri != null) {
                                checkRoleAndPerformUpdate();
                            }
                        }
                    }
            );

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(
                R.layout.activity_profile,
                container,
                false
        );

        initViews(view);
        loadProfileData();

        return view;
    }

    private void initViews(View view) {

        profileRootLayout = view.findViewById(R.id.profileRootLayout);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvFollowersCount = view.findViewById(R.id.tvFollowersCount);
        tvFollowingCount = view.findViewById(R.id.tvFollowingCount);

        ivProfileLarge = view.findViewById(R.id.ivProfileLarge);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        btnBack = view.findViewById(R.id.btnBackProfile);
        btnLogoutTop = view.findViewById(R.id.btnLogoutTop);
        btnManagePlaylists = view.findViewById(R.id.btnManagePlaylists);

        // Tab Views
        layoutProfileTabs = view.findViewById(R.id.layoutProfileTabs);
        tabCreatedContent = view.findViewById(R.id.tabCreatedContent);
        tabSavedContent = view.findViewById(R.id.tabSavedContent);
        containerCreatedContent = view.findViewById(R.id.containerCreatedContent);
        containerSavedContent = view.findViewById(R.id.containerSavedContent);
        tvCreatedEmptyState = view.findViewById(R.id.tvCreatedEmptyState);

        // Subsections inside containers
        layoutArtistAlbums = view.findViewById(R.id.layoutArtistAlbums);
        layoutArtistSongs = view.findViewById(R.id.layoutArtistSongs);

        // RecyclerViews
        rvMyPlaylists = view.findViewById(R.id.rvMyPlaylists);
        rvArtistAlbums = view.findViewById(R.id.rvArtistAlbums);
        rvArtistSongs = view.findViewById(R.id.rvArtistSongs);

        rvMyPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
        playlistAdapter = new PlaylistAdapter(new ArrayList<>(), this);
        rvMyPlaylists.setAdapter(playlistAdapter);

        rvArtistAlbums.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        albumAdapter = new AlbumAdapter(new ArrayList<>(), this);
        rvArtistAlbums.setAdapter(albumAdapter);

        rvArtistSongs.setLayoutManager(new LinearLayoutManager(getContext()));
        songAdapter = new SongAdapter(new ArrayList<>(), this);
        rvArtistSongs.setAdapter(songAdapter);

        // Tab Logic
        tabCreatedContent.setOnClickListener(v -> switchTab(true));
        tabSavedContent.setOnClickListener(v -> switchTab(false));

        // Click Avatar to view full screen
        ivProfileLarge.setOnClickListener(v -> showFullScreenAvatar());

        // Edit profile
        btnEditProfile.setOnClickListener(v -> handleEditAction());

        // Back
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        // Logout
        if (btnLogoutTop != null) {
            btnLogoutTop.setOnClickListener(v -> showLogoutConfirmation());
        }

        // Following
        if (tvFollowingCount != null) {
            tvFollowingCount.setOnClickListener(v -> startActivity(
                    new Intent(getActivity(), FollowingActivity.class)
            ));
        }

        // Manage playlists
        if (btnManagePlaylists != null) {
            btnManagePlaylists.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToLibrary();
                }
            });
        }
    }

    private void switchTab(boolean showCreated) {
        if (showCreated) {
            containerCreatedContent.setVisibility(View.VISIBLE);
            containerSavedContent.setVisibility(View.GONE);
            tabCreatedContent.setAlpha(1.0f);
            tabSavedContent.setAlpha(0.5f);
        } else {
            containerCreatedContent.setVisibility(View.GONE);
            containerSavedContent.setVisibility(View.VISIBLE);
            tabCreatedContent.setAlpha(0.5f);
            tabSavedContent.setAlpha(1.0f);
        }
    }

    private void showFullScreenAvatar() {
        if (getContext() == null) {
            return;
        }

        Dialog dialog = new Dialog(
                requireContext(),
                android.R.style.Theme_Black_NoTitleBar_Fullscreen
        );

        ImageView imageView = new ImageView(requireContext());
        imageView.setBackgroundColor(Color.BLACK);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        Object imageSource = (currentAvatarUrl != null && !currentAvatarUrl.isEmpty())
                ? RetrofitClient.getAbsoluteUrl(currentAvatarUrl)
                : R.drawable.ic_person;

        Glide.with(this)
                .load(imageSource)
                .into(imageView);

        dialog.setContentView(imageView);
        imageView.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private boolean isArtist() {
        AuthManager authManager = AuthManager.getInstance(requireContext());
        String role = authManager.getRole();
        if (role == null) {
            return false;
        }
        role = role.toLowerCase().trim();
        return role.equals("artist") || role.equals("1");
    }

    private void handleEditAction() {
        if (isArtist()) {
            showArtistEditDialog();
        } else {
            openGallery();
        }
    }

    private void showArtistEditDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_artist_profile, null);

        EditText etName = dialogView.findViewById(R.id.etArtistName);
        EditText etDesc = dialogView.findViewById(R.id.etArtistDescription);

        String currentName = tvProfileName.getText() != null ? tvProfileName.getText().toString() : "";
        etName.setText(currentName);

        if (currentBio != null) {
            etDesc.setText(currentBio);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Chỉnh sửa thông tin Nghệ sĩ")
                .setView(dialogView)
                .setPositiveButton("CẬP NHẬT", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    updateArtistProfile(name, desc, null);
                })
                .setNeutralButton("ĐỔI ẢNH", (dialog, which) -> openGallery())
                .setNegativeButton("HỦY", null)
                .show();
    }

    private void checkRoleAndPerformUpdate() {
        if (isArtist()) {
            String name = tvProfileName.getText() != null ? tvProfileName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                AuthManager authManager = AuthManager.getInstance(requireContext());
                name = authManager.getUsername();
                if (name == null) name = "";
            }
            String desc = currentBio != null ? currentBio : "";
            updateArtistProfile(name, desc, selectedImageUri);
        } else {
            updateUserProfile(selectedImageUri);
        }
    }

    private void updateArtistProfile(String name, String desc, Uri imageUri) {
        RequestBody rbName = RequestBody.create(MediaType.parse("text/plain"), name != null ? name : "");
        RequestBody rbDesc = RequestBody.create(MediaType.parse("text/plain"), desc != null ? desc : "");
        MultipartBody.Part avatarPart = prepareFilePart("Avatar", imageUri);

        RetrofitClient.getApiService().updateArtistProfile(rbName, rbDesc, avatarPart)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                            if (name != null && !name.isEmpty()) {
                                tvProfileName.setText(name);
                            }
                            currentBio = desc;
                            loadArtistProfile();
                        } else {
                            Toast.makeText(requireContext(), "Cập nhật thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserProfile(Uri imageUri) {
        MultipartBody.Part avatarPart = prepareFilePart("Avatar", imageUri);
        if (avatarPart == null) return;

        RetrofitClient.getApiService().updateUserProfile(avatarPart)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Đổi ảnh đại diện thành công", Toast.LENGTH_SHORT).show();
                            loadUserProfile();
                        } else {
                            Toast.makeText(requireContext(), "Đổi ảnh thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        if (fileUri == null) return null;
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(fileUri);
            if (inputStream == null) return null;

            File tempFile = new File(requireContext().getCacheDir(), "temp_avatar.jpg");
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.close();
            inputStream.close();

            String mimeType = requireContext().getContentResolver().getType(fileUri);
            if (mimeType == null) mimeType = "image/jpeg";

            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), tempFile);
            return MultipartBody.Part.createFormData(partName, tempFile.getName(), requestFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadProfileData() {
        if (isArtist()) {
            layoutProfileTabs.setVisibility(View.VISIBLE);
            tvCreatedEmptyState.setVisibility(View.GONE);
            layoutArtistAlbums.setVisibility(View.VISIBLE);
            layoutArtistSongs.setVisibility(View.VISIBLE);
            switchTab(true); // Default to My Works for Artists
            loadArtistProfile();
        } else {
            // For regular users, we might just hide the "My Works" tab entirely or show it as empty
            layoutProfileTabs.setVisibility(View.GONE); // Hide tabs for normal users, just show Library
            switchTab(false); // Default to Library/Saved for normal users
            loadUserProfile();
        }
    }

    private void loadUserProfile() {
        if (!isAdded()) return;
        AuthManager authManager = AuthManager.getInstance(requireContext());
        String name = authManager.getUsername();
        if (name == null || name.isEmpty()) {
            name = authManager.getEmail();
            if (name != null && name.contains("@")) {
                name = name.split("@")[0];
            }
        }
        if (name != null && !name.isEmpty()) {
            tvProfileName.setText(name);
        }

        RetrofitClient.getApiService().getUserProfile().enqueue(new Callback<Artist>() {
            @Override
            public void onResponse(Call<Artist> call, Response<Artist> response) {
                if (!isAdded() || ivProfileLarge == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    Artist profile = response.body();
                    currentAvatarUrl = profile.getAvatarUrl();
                    currentBio = profile.getBio();
                    if (profile.getName() != null && !profile.getName().isEmpty()) {
                        tvProfileName.setText(profile.getName());
                    }
                    displayAvatar(currentAvatarUrl);
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

    private void loadArtistProfile() {
        if (!isAdded()) return;
        RetrofitClient.getApiService().getUserProfile().enqueue(new Callback<Artist>() {
            @Override
            public void onResponse(Call<Artist> call, Response<Artist> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Artist identity = response.body();
                    int artistId = identity.getId();
                    if (artistId <= 0) {
                        Toast.makeText(requireContext(), "Không lấy được Artist ID", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    currentArtistId = artistId;
                    getArtistDetail(artistId);
                    fetchArtistContent(artistId);
                } else {
                    Toast.makeText(requireContext(), "Không lấy được thông tin Artist", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Artist> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Lỗi tải Artist: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getArtistDetail(int artistId) {
        RetrofitClient.getApiService().getArtistById(artistId).enqueue(new Callback<Artist>() {
            @Override
            public void onResponse(Call<Artist> call, Response<Artist> response) {
                if (!isAdded() || ivProfileLarge == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    Artist artist = response.body();
                    currentArtistId = artist.getId();
                    currentAvatarUrl = artist.getAvatarUrl();
                    currentBio = artist.getBio();
                    if (artist.getName() != null && !artist.getName().isEmpty()) {
                        tvProfileName.setText(artist.getName());
                    }
                    displayAvatar(currentAvatarUrl);
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

    private void fetchArtistContent(int artistId) {
        // Fetch Artist Albums
        RetrofitClient.getApiService().getAlbumsByArtist(artistId).enqueue(new Callback<List<Album>>() {
            @Override
            public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    albumAdapter.setAlbums(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Album>> call, Throwable t) {}
        });

        // Fetch Artist Songs
        RetrofitClient.getApiService().getSongsByArtist(artistId).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    songAdapter.updateSongs(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {}
        });
    }

    private void displayAvatar(String avatarUrl) {
        if (!isAdded() || ivProfileLarge == null) return;
        ivProfileLarge.setImageTintList(null);

        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            String fullUrl = RetrofitClient.getAbsoluteUrl(avatarUrl);
            Glide.with(ProfileFragment.this)
                    .asBitmap()
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            if (!isAdded() || ivProfileLarge == null) return;
                            ivProfileLarge.setImageBitmap(resource);
                            applyDynamicBackground(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {}
                    });
        } else {
            displayDefaultAvatar();
        }
    }

    private void applyDynamicBackground(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette != null && isAdded()) {
                int dominantColor = palette.getDominantColor(0xFF282828);
                float[] hsv = new float[3];
                Color.colorToHSV(dominantColor, hsv);
                hsv[2] *= 0.35f; 
                int darkDominantColor = Color.HSVToColor(hsv);

                GradientDrawable gd = new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{darkDominantColor, 0xFF121212}
                );
                if (profileRootLayout != null) {
                    profileRootLayout.setBackground(gd);
                }
            }
        });
    }

    private void displayDefaultAvatar() {
        if (!isAdded() || ivProfileLarge == null) return;
        currentAvatarUrl = null;

        Glide.with(ProfileFragment.this)
                .load(R.drawable.ic_person)
                .circleCrop()
                .into(ivProfileLarge);

        if (profileRootLayout != null) {
            profileRootLayout.setBackgroundColor(Color.parseColor("#121212"));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileData();
        fetchMyPlaylists();
        updateFollowCounts();
    }

    private void updateFollowCounts() {
        RetrofitClient.getApiService().getFollowingArtists().enqueue(new Callback<List<Artist>>() {
            @Override
            public void onResponse(Call<List<Artist>> call, Response<List<Artist>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && tvFollowingCount != null) {
                    tvFollowingCount.setText("Đang theo dõi " + response.body().size());
                }
            }

            @Override
            public void onFailure(Call<List<Artist>> call, Throwable t) {}
        });
    }

    private void fetchMyPlaylists() {
        RetrofitClient.getApiService().getPlaylists().enqueue(new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    playlistAdapter.setPlaylists(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {}
        });
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("ĐĂNG XUẤT", (dialog, which) -> performLogout())
                .setNegativeButton("HỦY", null)
                .show();
    }

    private void performLogout() {
        AuthManager.getInstance(requireContext()).clear();
        RetrofitClient.reset();

        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onPlaylistClick(Playlist playlist) {
        Intent intent = new Intent(getActivity(), PlaylistDetailActivity.class);
        intent.putExtra("playlist", playlist);
        startActivity(intent);
    }

    @Override
    public void onAlbumClick(Album album) {
        Intent intent = new Intent(getActivity(), AlbumDetailActivity.class);
        intent.putExtra("album", album);
        startActivity(intent);
    }

    @Override
    public void onSongClick(Song song) {
        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra("song", song);
        startActivity(intent);
    }
}