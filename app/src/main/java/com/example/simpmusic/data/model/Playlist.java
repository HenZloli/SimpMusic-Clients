package com.example.simpmusic.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Playlist implements Serializable {
    @SerializedName(value = "id", alternate = {"Id", "playlistId", "PlaylistId", "idPlaylist"})
    private int id;

    @SerializedName(value = "name", alternate = {"Name", "title", "Title"})
    private String name;

    @SerializedName(value = "description", alternate = {"Description"})
    private String description;

    @SerializedName(value = "coverUrl", alternate = {"CoverUrl", "cover_url", "image", "Image"})
    private String coverUrl;

    @SerializedName(value = "songs", alternate = {"Songs", "listSongs"})
    private List<Song> songs;

    public Playlist(String name, String description, String coverUrl) {
        this.name = name;
        this.description = description;
        this.coverUrl = coverUrl;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public List<Song> getSongs() { return songs; }
    public void setSongs(List<Song> songs) { this.songs = songs; }
}
