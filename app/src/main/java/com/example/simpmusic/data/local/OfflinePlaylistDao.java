package com.example.simpmusic.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface OfflinePlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlaylist(OfflinePlaylistEntity playlist);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlaylistSongCrossRef(PlaylistSongCrossRef crossRef);

    @Query("SELECT * FROM offline_playlists")
    List<OfflinePlaylistEntity> getAllPlaylists();

    @Query("SELECT s.* FROM songs s INNER JOIN playlist_song_cross_ref ref ON s.id = ref.songId WHERE ref.playlistId = :playlistId")
    List<SongEntity> getSongsForPlaylist(int playlistId);

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    void deleteCrossRefsForPlaylist(int playlistId);
}
