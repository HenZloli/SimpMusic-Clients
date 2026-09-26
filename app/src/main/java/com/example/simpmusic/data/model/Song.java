package com.example.simpmusic.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Song implements Serializable {
    @SerializedName(value = "id", alternate = {"Id"})
    private int id;

    @SerializedName(value = "title", alternate = {"Title"})
    private String title;

    @SerializedName(value = "artist", alternate = {"Artist", "artistName", "ArtistName"})
    private String artist;

    @SerializedName(value = "artistId", alternate = {"ArtistId", "artist_id", "Artist_Id"})
    private int artistId;

    @SerializedName(value = "coverUrl", alternate = {"CoverUrl", "cover_url", "Cover_Url"})
    private String coverUrl;

    @SerializedName(value = "audioUrl", alternate = {"AudioUrl", "audio_url", "Audio_Url"})
    private String audioUrl;

    @SerializedName(value = "lyrics", alternate = {"Lyrics"})
    private String lyrics;

    @SerializedName(value = "viewCount", alternate = {"ViewCount", "views", "Views", "view_count", "View_Count", "count", "view", "View"})
    private long viewCount;

    public Song(int id, String title, String artist, String coverUrl, String audioUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.coverUrl = coverUrl;
        this.audioUrl = audioUrl;
    }

    public Song(int id, String title, String artist, String coverUrl, String audioUrl, String lyrics) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.coverUrl = coverUrl;
        this.audioUrl = audioUrl;
        this.lyrics = lyrics;
    }

    public Song(int id, String title, String artist, String coverUrl, String audioUrl, String lyrics, long viewCount) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.coverUrl = coverUrl;
        this.audioUrl = audioUrl;
        this.lyrics = lyrics;
        this.viewCount = viewCount;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public int getArtistId() { return artistId; }
    public void setArtistId(int artistId) { this.artistId = artistId; }
    public String getCoverUrl() { return coverUrl; }
    public String getAudioUrl() { return audioUrl; }
    public String getLyrics() { return lyrics; }
    public void setLyrics(String lyrics) { this.lyrics = lyrics; }
    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }
}
