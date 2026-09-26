package com.example.simpmusic.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SongEntity.class, OfflinePlaylistEntity.class, PlaylistSongCrossRef.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract SongDao songDao();
    public abstract OfflinePlaylistDao offlinePlaylistDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "simpmusic_db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
