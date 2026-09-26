package com.example.simpmusic.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Artist implements Serializable {
    @SerializedName(value = "id", alternate = {"Id"})
    private int id;

    @SerializedName(value = "name", alternate = {"Name"})
    private String name;

    @SerializedName(value = "avatarUrl", alternate = {"AvatarUrl", "avatar_url", "Avatar_url", "avatarur"})
    private String avatarUrl;

    @SerializedName(value = "bio", alternate = {"Bio", "description"})
    private String bio;

    @SerializedName(value = "isFollowed", alternate = {"IsFollowed", "followed", "isFollowing"})
    private boolean isFollowed;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public boolean isFollowed() { return isFollowed; }
    public void setFollowed(boolean followed) { isFollowed = followed; }
}
