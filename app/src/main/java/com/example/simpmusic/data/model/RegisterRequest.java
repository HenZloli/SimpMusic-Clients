package com.example.simpmusic.data.model;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName(value = "username", alternate = {"Username", "UserName"})
    private String username;
    
    @SerializedName(value = "email", alternate = {"Email"})
    private String email;
    
    @SerializedName(value = "password", alternate = {"Password"})
    private String password;

    public RegisterRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
}
