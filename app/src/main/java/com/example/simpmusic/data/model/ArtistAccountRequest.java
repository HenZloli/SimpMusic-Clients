package com.example.simpmusic.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ArtistAccountRequest implements Serializable {
    @SerializedName("id")
    private int id;

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

    @SerializedName("status")
    private String status;

    @SerializedName("adminNote")
    private String adminNote;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("reviewedAt")
    private String reviewedAt;

    // Constructor for creating a new request
    public ArtistAccountRequest(String artistName, String realName, String genre, String bio, String youtubeUrl, String facebookUrl, String tikTokUrl) {
        this.artistName = artistName;
        this.realName = realName;
        this.genre = genre;
        this.bio = bio;
        this.youtubeUrl = youtubeUrl;
        this.facebookUrl = facebookUrl;
        this.tikTokUrl = tikTokUrl;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }

    public String getFacebookUrl() { return facebookUrl; }
    public void setFacebookUrl(String facebookUrl) { this.facebookUrl = facebookUrl; }

    public String getTikTokUrl() { return tikTokUrl; }
    public void setTikTokUrl(String tikTokUrl) { this.tikTokUrl = tikTokUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(String reviewedAt) { this.reviewedAt = reviewedAt; }
}
