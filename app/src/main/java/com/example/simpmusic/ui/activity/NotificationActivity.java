package com.example.simpmusic.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.ApiResponse;
import com.example.simpmusic.data.model.Notification;
import com.example.simpmusic.ui.adapter.NotificationAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private LinearLayout llEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        rvNotifications = findViewById(R.id.rvNotifications);
        llEmpty = findViewById(R.id.llEmptyNotifications);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(new ArrayList<>());
        rvNotifications.setAdapter(adapter);

        fetchNotifications();
    }

    private void fetchNotifications() {
        RetrofitClient.getApiService().getNotifications().enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(Call<List<Notification>> call, Response<List<Notification>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Notification> notifications = response.body();
                    if (notifications.isEmpty()) {
                        rvNotifications.setVisibility(View.GONE);
                        llEmpty.setVisibility(View.VISIBLE);
                    } else {
                        rvNotifications.setVisibility(View.VISIBLE);
                        llEmpty.setVisibility(View.GONE);
                        adapter.setNotifications(notifications);
                        markAllAsRead();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Notification>> call, Throwable t) {
                Toast.makeText(NotificationActivity.this, "Không thể tải thông báo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAllAsRead() {
        RetrofitClient.getApiService().markAllAsRead().enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                // Background update, no need to show toast unless error
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                // Silent failure
            }
        });
    }
}
