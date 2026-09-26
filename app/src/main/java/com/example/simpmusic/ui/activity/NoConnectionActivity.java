package com.example.simpmusic.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simpmusic.R;
import com.example.simpmusic.data.local.AppDatabase;
import com.example.simpmusic.data.local.OfflinePlaylistEntity;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.ui.adapter.PlaylistAdapter;

import java.util.ArrayList;
import java.util.List;

public class NoConnectionActivity extends AppCompatActivity implements PlaylistAdapter.OnPlaylistClickListener {

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private PlaylistAdapter playlistAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_connection);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        Button btnRetry = findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(v -> {
            if (isConnected()) {
                finish();
            } else {
                Toast.makeText(this, "Vẫn chưa có kết nối Internet", Toast.LENGTH_SHORT).show();
            }
        });

        // Thiết lập RecyclerView hiển thị playlist đã tải
        RecyclerView rvDownloadedPlaylists = findViewById(R.id.rvDownloadedPlaylists);
        rvDownloadedPlaylists.setLayoutManager(new LinearLayoutManager(this));
        playlistAdapter = new PlaylistAdapter(new ArrayList<>(), this);
        rvDownloadedPlaylists.setAdapter(playlistAdapter);

        loadDownloadedPlaylists();
        setupNetworkCallback();
    }

    private void loadDownloadedPlaylists() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<OfflinePlaylistEntity> entities = db.offlinePlaylistDao().getAllPlaylists();
            List<Playlist> playlists = new ArrayList<>();
            
            if (entities != null) {
                for (OfflinePlaylistEntity entity : entities) {
                    Playlist playlist = new Playlist(entity.getName(), entity.getDescription(), entity.getCoverUrl());
                    playlist.setId(entity.getId());
                    playlists.add(playlist);
                }
            }

            runOnUiThread(() -> playlistAdapter.setPlaylists(playlists));
        }).start();
    }

    @Override
    public void onPlaylistClick(Playlist playlist) {
        Intent intent = new Intent(this, PlaylistDetailActivity.class);
        intent.putExtra("playlist", playlist);
        intent.putExtra("isOffline", true);
        startActivity(intent);
    }

    private boolean isConnected() {
        if (connectivityManager == null) return false;
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    private void setupNetworkCallback() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                runOnUiThread(() -> {
                    Toast.makeText(NoConnectionActivity.this, "Đã khôi phục kết nối internet", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        };

        if (connectivityManager != null) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onBackPressed() {
        if (isConnected()) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, "Vui lòng kết nối Internet để tiếp tục", Toast.LENGTH_SHORT).show();
        }
    }
}
