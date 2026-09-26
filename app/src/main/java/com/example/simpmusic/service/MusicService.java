package com.example.simpmusic.service;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.ViewCountResponse;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MusicService extends MediaSessionService {
    private ExoPlayer player;
    private MediaSession mediaSession;
    private String lastViewedSongId = null;

    @Override
    @UnstableApi
    public void onCreate() {
        super.onCreate();
        
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build();
        
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e("MusicService", "Lỗi phát nhạc: " + error.getMessage(), error);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(MusicService.this, "Không thể phát bài hát này. Vui lòng kiểm tra kết nối mạng.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                if (mediaItem != null && mediaItem.mediaId != null) {
                    String currentSongId = mediaItem.mediaId;
                    if (!currentSongId.equals(lastViewedSongId)) {
                        lastViewedSongId = currentSongId;
                        try {
                            int songId = Integer.parseInt(currentSongId);
                            RetrofitClient.getApiService().increaseViewCount(songId).enqueue(new Callback<ViewCountResponse>() {
                                @Override public void onResponse(@NonNull Call<ViewCountResponse> call, @NonNull Response<ViewCountResponse> response) {}
                                @Override public void onFailure(@NonNull Call<ViewCountResponse> call, @NonNull Throwable t) {}
                            });
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        });

        MediaSession.Callback callback = new MediaSession.Callback() {
            @NonNull
            @Override
            public ListenableFuture<List<MediaItem>> onAddMediaItems(@NonNull MediaSession mediaSession, @NonNull MediaSession.ControllerInfo controller, @NonNull List<MediaItem> mediaItems) {
                List<MediaItem> resolvedItems = new ArrayList<>();
                for (MediaItem item : mediaItems) {
                    MediaItem.Builder builder = item.buildUpon();
                    // Media3 stripping URI workaround: Reconstruct URI from metadata extras
                    if (item.localConfiguration == null || item.localConfiguration.uri == null) {
                        Bundle extras = item.mediaMetadata.extras;
                        if (extras != null) {
                            String audioUrl = extras.getString("audioUrl");
                            if (audioUrl != null) {
                                builder.setUri(Uri.parse(audioUrl));
                            }
                        }
                    }
                    resolvedItems.add(builder.build());
                }
                return Futures.immediateFuture(resolvedItems);
            }

            @UnstableApi
            @NonNull
            @Override
            public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onPlaybackResumption(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller) {
                return Futures.immediateFuture(new MediaSession.MediaItemsWithStartPosition(
                        Collections.emptyList(),
                        0,
                        0
                ));
            }
        };

        mediaSession = new MediaSession.Builder(this, player)
                .setCallback(callback)
                .build();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
