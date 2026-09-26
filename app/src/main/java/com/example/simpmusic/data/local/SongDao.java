package com.example.simpmusic.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SongDao {
    @Query("SELECT * FROM songs")
    List<SongEntity> getAllSongs();

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    List<SongEntity> getFavoriteSongs();

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    SongEntity getSongById(int songId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSong(SongEntity song);

    @Delete
    void deleteSong(SongEntity song);

    @Query("DELETE FROM songs WHERE id = :songId")
    void deleteById(int songId);

    @Query("UPDATE songs SET isFavorite = :isFav WHERE id = :songId")
    void updateFavoriteStatus(int songId, boolean isFav);
}
