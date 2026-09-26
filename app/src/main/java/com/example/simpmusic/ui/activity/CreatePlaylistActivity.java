package com.example.simpmusic.ui.activity;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.utils.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatePlaylistActivity extends AppCompatActivity {

    private TextInputEditText etName, etDescription;
    private MaterialButton btnCreate;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_playlist);

        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        btnCreate = findViewById(R.id.btnCreate);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên playlist", Toast.LENGTH_SHORT).show();
                return;
            }

            performCreatePlaylist(name, description);
        });
    }

    private void performCreatePlaylist(String name, String description) {
        btnCreate.setEnabled(false);
        btnCreate.setText("Đang tạo...");

        Playlist playlist = new Playlist(name, description, "");

        RetrofitClient.getApiService().createPlaylist(playlist).enqueue(new Callback<Playlist>() {
            @Override
            public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                btnCreate.setEnabled(true);
                btnCreate.setText("TẠO");

                if (response.isSuccessful()) {
                    Toast.makeText(CreatePlaylistActivity.this, "Đã tạo playlist thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CreatePlaylistActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Playlist> call, Throwable t) {
                btnCreate.setEnabled(true);
                btnCreate.setText("TẠO");
                Toast.makeText(CreatePlaylistActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
