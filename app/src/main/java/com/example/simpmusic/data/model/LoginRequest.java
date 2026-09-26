package com.example.simpmusic.data.model;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("Email")
    private String email;

    @SerializedName("UserName")
    private String userName;
    
    @SerializedName("Password")
    private String password;

    public LoginRequest(String emailOrUser, String password) {
        this.email = emailOrUser;
        this.userName = emailOrUser;
        this.password = password;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
