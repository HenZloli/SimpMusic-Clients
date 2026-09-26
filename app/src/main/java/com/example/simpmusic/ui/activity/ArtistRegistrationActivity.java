package com.example.simpmusic.ui.activity;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.Artist;
import com.example.simpmusic.data.model.ArtistAccountRequest;
import com.example.simpmusic.data.model.CreateArtistRequest;
import com.example.simpmusic.utils.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArtistRegistrationActivity extends AppCompatActivity {

    private static final String TAG = "ArtistRegistration";
    private EditText etArtistName, etRealName, etGenre, etBio, etYoutube, etFacebook, etTikTok;
    private MaterialButton btnSubmitRequest;
    private MaterialCardView cardStatusPending, cardStatusApproved, cardStatusRejected, layoutRegistrationForm;
    private View layoutIntroHeader;
    private TextView tvRejectReason;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_registration);

        initViews();
        fetchMyRequest();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(""); // Dùng title trong Toolbar nếu muốn
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etArtistName = findViewById(R.id.etArtistName);
        etRealName = findViewById(R.id.etRealName);
        etGenre = findViewById(R.id.etGenre);
        etBio = findViewById(R.id.etBio);
        etYoutube = findViewById(R.id.etYoutube);
        etFacebook = findViewById(R.id.etFacebook);
        etTikTok = findViewById(R.id.etTikTok);

        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);
        cardStatusPending = findViewById(R.id.cardStatusPending);
        cardStatusApproved = findViewById(R.id.cardStatusApproved);
        cardStatusRejected = findViewById(R.id.cardStatusRejected);
        layoutRegistrationForm = findViewById(R.id.layoutRegistrationForm);
        layoutIntroHeader = findViewById(R.id.layoutIntroHeader);
        tvRejectReason = findViewById(R.id.tvRejectReason);
        progressBar = findViewById(R.id.progressBar);

        btnSubmitRequest.setOnClickListener(v -> submitRequest());
    }

    private void fetchMyRequest() {
        showLoading(true);
        RetrofitClient.getApiService().getMyArtistRequest().enqueue(new Callback<ArtistAccountRequest>() {
            @Override
            public void onResponse(Call<ArtistAccountRequest> call, Response<ArtistAccountRequest> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    updateUIBasedOnRequest(response.body());
                } else if (response.code() == 404) {
                    showInitialForm();
                } else {
                    showInitialForm();
                }
            }

            @Override
            public void onFailure(Call<ArtistAccountRequest> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Fetch failed", t);
                showInitialForm();
            }
        });
    }

    private void updateUIBasedOnRequest(ArtistAccountRequest request) {
        String status = request.getStatus();
        
        // Reset visibility
        cardStatusPending.setVisibility(View.GONE);
        cardStatusApproved.setVisibility(View.GONE);
        cardStatusRejected.setVisibility(View.GONE);
        layoutRegistrationForm.setVisibility(View.GONE);
        layoutIntroHeader.setVisibility(View.VISIBLE);

        if ("Pending".equalsIgnoreCase(status)) {
            cardStatusPending.setVisibility(View.VISIBLE);
            layoutIntroHeader.setVisibility(View.GONE); // Giảm bớt nội dung khi đang chờ
        } else if ("Approved".equalsIgnoreCase(status)) {
            cardStatusApproved.setVisibility(View.VISIBLE);
            layoutIntroHeader.setVisibility(View.GONE);
            updateLocalRoleToArtist();
        } else if ("Rejected".equalsIgnoreCase(status)) {
            cardStatusRejected.setVisibility(View.VISIBLE);
            tvRejectReason.setText("Lý do: " + (request.getAdminNote() != null ? request.getAdminNote() : "Không có lý do cụ thể"));
            layoutRegistrationForm.setVisibility(View.VISIBLE); // Cho phép sửa để gửi lại
            fillForm(request);
        } else {
            showInitialForm();
        }
    }

    private void showInitialForm() {
        layoutRegistrationForm.setVisibility(View.VISIBLE);
        layoutIntroHeader.setVisibility(View.VISIBLE);
    }

    private void updateLocalRoleToArtist() {
        AuthManager authManager = AuthManager.getInstance(this);
        if (!"Artist".equalsIgnoreCase(authManager.getRole())) {
            authManager.setRole("Artist");
            syncUserProfile();
        }
    }

    private void syncUserProfile() {
        RetrofitClient.getApiService().getUserProfile().enqueue(new Callback<Artist>() {
            @Override
            public void onResponse(Call<Artist> call, Response<Artist> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Artist p = response.body();
                    AuthManager.getInstance(ArtistRegistrationActivity.this).saveAuthData(
                            AuthManager.getInstance(ArtistRegistrationActivity.this).getToken(),
                            p.getId(),
                            AuthManager.getInstance(ArtistRegistrationActivity.this).getEmail(),
                            p.getName(),
                            "Artist"
                    );
                }
            }
            @Override public void onFailure(Call<Artist> call, Throwable t) {}
        });
    }

    private void fillForm(ArtistAccountRequest request) {
        etArtistName.setText(request.getArtistName());
        etRealName.setText(request.getRealName());
        etGenre.setText(request.getGenre());
        etBio.setText(request.getBio());
        etYoutube.setText(request.getYoutubeUrl());
        etFacebook.setText(request.getFacebookUrl());
        etTikTok.setText(request.getTikTokUrl());
    }

    private void submitRequest() {
        String artistName = etArtistName.getText().toString().trim();
        String realName = etRealName.getText().toString().trim();
        String genre = etGenre.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        
        String youtube = etYoutube.getText().toString().trim();
        if (!youtube.isEmpty() && !Patterns.WEB_URL.matcher(youtube).matches()) {
            etYoutube.setError("Link không hợp lệ");
            return;
        }
        
        String facebook = etFacebook.getText().toString().trim();
        if (!facebook.isEmpty() && !Patterns.WEB_URL.matcher(facebook).matches()) {
            etFacebook.setError("Link không hợp lệ");
            return;
        }
        
        String tikTok = etTikTok.getText().toString().trim();
        if (!tikTok.isEmpty() && !Patterns.WEB_URL.matcher(tikTok).matches()) {
            etTikTok.setError("Link không hợp lệ");
            return;
        }

        if (artistName.isEmpty() || realName.isEmpty() || genre.isEmpty() || bio.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ các thông tin có dấu (*)", Toast.LENGTH_SHORT).show();
            return;
        }

        CreateArtistRequest request = new CreateArtistRequest(
            artistName, realName, genre, bio, 
            youtube.isEmpty() ? null : youtube, 
            facebook.isEmpty() ? null : facebook, 
            tikTok.isEmpty() ? null : tikTok
        );
        
        showLoading(true);
        RetrofitClient.getApiService().createArtistRequest(request).enqueue(new Callback<ArtistAccountRequest>() {
            @Override
            public void onResponse(Call<ArtistAccountRequest> call, Response<ArtistAccountRequest> response) {
                showLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(ArtistRegistrationActivity.this, "Đã gửi yêu cầu xét duyệt!", Toast.LENGTH_SHORT).show();
                    fetchMyRequest();
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<ArtistAccountRequest> call, Throwable t) {
                showLoading(false);
                Toast.makeText(ArtistRegistrationActivity.this, "Lỗi mạng, vui lòng thử lại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleErrorResponse(Response<?> response) {
        String msg = "Gửi thất bại (" + response.code() + ")";
        try {
            JSONObject obj = new JSONObject(response.errorBody().string());
            if (obj.has("message")) msg = obj.getString("message");
        } catch (Exception ignored) {}
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSubmitRequest.setEnabled(!show);
        btnSubmitRequest.setAlpha(show ? 0.5f : 1.0f);
    }
}
