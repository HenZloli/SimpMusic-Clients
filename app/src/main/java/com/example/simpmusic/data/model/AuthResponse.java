package com.example.simpmusic.data.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName(value = "token", alternate = {"Token", "accessToken"})
    private String token;
    
    private String message;
    
    @SerializedName(value = "user", alternate = {"UserData", "account", "data"})
    private UserData user;

    @SerializedName(value = "userId", alternate = {"Id", "id", "UserId"})
    private Integer topLevelUserId;

    @SerializedName(value = "role", alternate = {"Role", "userRole", "RoleName"})
    private String topLevelRole;

    @SerializedName(value = "username", alternate = {"UserName", "name", "DisplayName"})
    private String topLevelUsername;

    public String getToken() { return token; }
    public String getMessage() { return message; }

    public int getUserId() {
        if (user != null) return user.getId();
        if (topLevelUserId != null) return topLevelUserId;
        return -1;
    }

    public String getRole() {
        if (user != null && user.getRole() != null) return user.getRole();
        if (topLevelRole != null) return topLevelRole;
        return "";
    }

    public String getUsername() {
        if (user != null && user.getUsername() != null) return user.getUsername();
        if (topLevelUsername != null) return topLevelUsername;
        return null;
    }

    public static class UserData {
        @SerializedName(value = "id", alternate = {"Id", "userId"})
        private int id;
        
        @SerializedName(value = "role", alternate = {"Role", "userRole", "RoleName"})
        private String role;

        @SerializedName(value = "username", alternate = {"UserName", "name", "DisplayName"})
        private String username;
        
        public int getId() { return id; }
        public String getRole() { return role; }
        public String getUsername() { return username; }
    }
}
