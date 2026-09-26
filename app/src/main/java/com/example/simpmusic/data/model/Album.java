package com.example.simpmusic.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Album implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName(value = "title", alternate = {"Title"})
    private String title;

    @SerializedName(value = "description", alternate = {"Description"})
    private String description;

    @SerializedName(value = "coverUrl", alternate = {"CoverUrl"})
    private String coverUrl;

    @SerializedName(value = "songs", alternate = {"Songs"})
    private List<Song> songs;

    public Album(int id, String title, String description, String coverUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.coverUrl = coverUrl;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public List<Song> getSongs() { return songs; }
    public void setSongs(List<Song> songs) { this.songs = songs; }
}
