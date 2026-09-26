package com.example.simpmusic.data.local;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "songs")
public class SongEntity {
    @PrimaryKey
    private int id;
    private String title;
    private String artist;
    private String coverUrl;
    private String audioUrl;
    private String localAudioPath;
    private String lyrics;
    private boolean isFavorite;

    public SongEntity(int id, String title, String artist, String coverUrl, String audioUrl, String localAudioPath, String lyrics, boolean isFavorite) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.coverUrl = coverUrl;
        this.audioUrl = audioUrl;
        this.localAudioPath = localAudioPath;
        this.lyrics = lyrics;
        this.isFavorite = isFavorite;
    }

    @Ignore
    public SongEntity(int id, String title, String artist, String coverUrl, String audioUrl, String localAudioPath, String lyrics) {
        this(id, title, artist, coverUrl, audioUrl, localAudioPath, lyrics, false);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public String getLocalAudioPath() { return localAudioPath; }
    public void setLocalAudioPath(String localAudioPath) { this.localAudioPath = localAudioPath; }
    public String getLyrics() { return lyrics; }
    public void setLyrics(String lyrics) { this.lyrics = lyrics; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}
