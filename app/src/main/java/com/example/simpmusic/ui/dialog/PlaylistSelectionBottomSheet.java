package com.example.simpmusic.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.data.model.Song;
import com.example.simpmusic.ui.adapter.PlaylistSelectionAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistSelectionBottomSheet extends BottomSheetDialogFragment {

    private final Song song;
    private OnPlaylistSelectedListener listener;
    private PlaylistSelectionAdapter adapter;

    public interface OnPlaylistSelectedListener {
        void onPlaylistSelected(Playlist playlist, Song song);
        void onCreateNewPlaylist(Song song);
    }

    public PlaylistSelectionBottomSheet(Song song) {
        this.song = song;
    }

    public void setOnPlaylistSelectedListener(OnPlaylistSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_playlist_selection, container, false);

        RecyclerView rvPlaylists = view.findViewById(R.id.rvSelectionPlaylists);
        MaterialButton btnCreateNew = view.findViewById(R.id.btnCreateNewInSelection);

        rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PlaylistSelectionAdapter(new ArrayList<>(), playlist -> {
            if (listener != null) {
                listener.onPlaylistSelected(playlist, song);
            }
            dismiss();
        });
        rvPlaylists.setAdapter(adapter);

        btnCreateNew.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCreateNewPlaylist(song);
            }
            dismiss();
        });

        loadPlaylists();

        return view;
    }

    private void loadPlaylists() {
        RetrofitClient.getApiService().getPlaylists().enqueue(new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setPlaylists(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                Toast.makeText(getContext(), "Không thể tải playlist", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
