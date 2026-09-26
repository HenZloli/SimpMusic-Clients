package com.example.simpmusic.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.Artist;
import com.example.simpmusic.ui.adapter.FollowingArtistAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowingActivity extends AppCompatActivity implements FollowingArtistAdapter.OnArtistClickListener {

    private RecyclerView rvFollowingList;
    private FollowingArtistAdapter adapter;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following);

        initViews();
        fetchFollowingArtists();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarFollowing);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Đang theo dõi");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvFollowingList = findViewById(R.id.rvFollowingList);
        rvFollowingList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FollowingArtistAdapter(new ArrayList<>(), this);
        rvFollowingList.setAdapter(adapter);
    }

    private void fetchFollowingArtists() {
        RetrofitClient.getApiService().getFollowingArtists().enqueue(new Callback<List<Artist>>() {
            @Override
            public void onResponse(Call<List<Artist>> call, Response<List<Artist>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setArtists(response.body());
                } else {
                    Toast.makeText(FollowingActivity.this, "Không thể tải danh sách", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Artist>> call, Throwable t) {
                Toast.makeText(FollowingActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onArtistClick(Artist artist) {
        Intent intent = new Intent(this, ArtistDetailActivity.class);
        intent.putExtra("artist", artist);
        startActivity(intent);
    }
}
