package com.example.simpmusic.data.model;

import com.google.gson.annotations.SerializedName;

public class CreateArtistRequest {
    @SerializedName("artistName")
    private String artistName;

    @SerializedName("realName")
    private String realName;

    @SerializedName("genre")
    private String genre;

    @SerializedName("bio")
    private String bio;

    @SerializedName("youtubeUrl")
    private String youtubeUrl;

    @SerializedName("facebookUrl")
    private String facebookUrl;

    @SerializedName("tikTokUrl")
    private String tikTokUrl;

    public CreateArtistRequest(String artistName, String realName, String genre, String bio, String youtubeUrl, String facebookUrl, String tikTokUrl) {
        this.artistName = artistName;
        this.realName = realName;
        this.genre = genre;
        this.bio = bio;
        this.youtubeUrl = youtubeUrl;
        this.facebookUrl = facebookUrl;
        this.tikTokUrl = tikTokUrl;
    }
}
