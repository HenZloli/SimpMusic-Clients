package com.example.simpmusic.data.model;

public class Notification {
    private int id;
    private String title;
    private String message;
    private String type; // e.g., "VIOLATION", "APPROVAL", "SYSTEM"
    private long timestamp;
    private boolean isRead;

    public Notification(int id, String title, String message, String type, long timestamp) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
