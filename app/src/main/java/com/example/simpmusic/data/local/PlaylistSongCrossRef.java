package com.example.simpmusic.data.local;

import androidx.room.Entity;

@Entity(tableName = "playlist_song_cross_ref", primaryKeys = {"playlistId", "songId"})
public class PlaylistSongCrossRef {
    private int playlistId;
    private int songId;

    public PlaylistSongCrossRef(int playlistId, int songId) {
        this.playlistId = playlistId;
        this.songId = songId;
    }

    public int getPlaylistId() { return playlistId; }
    public int getSongId() { return songId; }
}
