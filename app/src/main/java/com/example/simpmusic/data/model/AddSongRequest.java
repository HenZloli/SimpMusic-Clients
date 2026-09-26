package com.example.simpmusic.data.model;

public class AddSongRequest {
    private int songId;

    public AddSongRequest(int songId) {
        this.songId = songId;
    }

    public int getSongId() { return songId; }
    public void setSongId(int songId) { this.songId = songId; }
}
