package com.example.simpmusic.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "offline_playlists")
public class OfflinePlaylistEntity {
    @PrimaryKey
    private int id;
    private String name;
    private String description;
    private String coverUrl;

    public OfflinePlaylistEntity(int id, String name, String description, String coverUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.coverUrl = coverUrl;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCoverUrl() { return coverUrl; }
}
