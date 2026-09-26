package com.example.simpmusic.ui.dialog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.simpmusic.R;
import com.example.simpmusic.ui.activity.CreateAlbumActivity;
import com.example.simpmusic.ui.activity.CreatePlaylistActivity;
import com.example.simpmusic.ui.activity.LoginActivity;
import com.example.simpmusic.ui.activity.UploadSongActivity;
import com.example.simpmusic.utils.AuthManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class CreateBottomSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_create_menu, container, false);

        LinearLayout llCreatePlaylist = view.findViewById(R.id.llCreatePlaylist);
        LinearLayout llCreateAlbum = view.findViewById(R.id.llCreateAlbum);
        LinearLayout llUploadSong = view.findViewById(R.id.llUploadSong);

        AuthManager authManager = AuthManager.getInstance(requireContext());
        boolean isLoggedIn = authManager.isLoggedIn();
        String role = authManager.getRole().toLowerCase();

        // Kiểm tra quyền hiển thị các mục (Artist)
        if (role.equals("artist") || role.equals("1")) {
            llCreateAlbum.setVisibility(View.VISIBLE);
            llUploadSong.setVisibility(View.VISIBLE);
        } else {
            llCreateAlbum.setVisibility(View.GONE);
            llUploadSong.setVisibility(View.GONE);
        }

        llCreatePlaylist.setOnClickListener(v -> {
            if (!isLoggedIn) {
                startActivity(new Intent(getContext(), LoginActivity.class));
            } else {
                startActivity(new Intent(getContext(), CreatePlaylistActivity.class));
            }
            dismiss();
        });

        llCreateAlbum.setOnClickListener(v -> {
            if (!isLoggedIn) {
                startActivity(new Intent(getContext(), LoginActivity.class));
            } else {
                startActivity(new Intent(getContext(), CreateAlbumActivity.class));
            }
            dismiss();
        });

        llUploadSong.setOnClickListener(v -> {
            if (!isLoggedIn) {
                startActivity(new Intent(getContext(), LoginActivity.class));
            } else {
                startActivity(new Intent(getContext(), UploadSongActivity.class));
            }
            dismiss();
        });

        return view;
    }
}
