package com.example.simpmusic.data.model;

import com.google.gson.annotations.SerializedName;

public class ViewCountResponse {
    @SerializedName(value = "viewCount", alternate = {"ViewCount", "views", "Views", "view_count", "View_Count", "count", "newViewCount", "newCount", "data"})
    private long viewCount;

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }
}
