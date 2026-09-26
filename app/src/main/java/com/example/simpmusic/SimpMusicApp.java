package com.example.simpmusic;

import android.app.Application;
import com.example.simpmusic.data.api.RetrofitClient;

public class SimpMusicApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Cung cấp context cho RetrofitClient ngay lập tức
        RetrofitClient.init(this);
    }
}
