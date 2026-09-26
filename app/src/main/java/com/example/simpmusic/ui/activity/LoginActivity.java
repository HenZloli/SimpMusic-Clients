package com.example.simpmusic.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.AuthResponse;
import com.example.simpmusic.data.model.LoginRequest;
import com.example.simpmusic.utils.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private ImageButton btnBack;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnBack = findViewById(R.id.btnBackLogin);

        if (getIntent().getBooleanExtra("SESSION_EXPIRED", false)) {
            showSessionExpiredDialog();
        }

        btnBack.setOnClickListener(v -> finish());

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đủ email và mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            performLogin(email, password);
        });

        findViewById(R.id.tvGoToRegister).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void showSessionExpiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Phiên hết hạn")
                .setMessage("Phiên đăng nhập của bạn đã hết hạn hoặc không còn hiệu lực. Vui lòng đăng nhập lại.")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    getIntent().removeExtra("SESSION_EXPIRED");
                })
                .setCancelable(false)
                .show();
    }

    private void performLogin(String email, String password) {
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang xử lý...");

        LoginRequest request = new LoginRequest(email, password);
        RetrofitClient.getApiService().login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("ĐĂNG NHẬP");

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    String token = auth.getToken();
                    
                    if (token == null || token.isEmpty()) {
                        Log.e(TAG, "Login success but token is null");
                        Toast.makeText(LoginActivity.this, "Lỗi: Không nhận được mã truy cập từ máy chủ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Lưu dữ liệu vào AuthManager
                    AuthManager.getInstance(LoginActivity.this).saveAuthData(
                            token,
                            auth.getUserId(),
                            email,
                            auth.getUsername(),
                            auth.getRole()
                    );

                    // Reset Retrofit để đảm bảo các yêu cầu tiếp theo sử dụng token mới
                    RetrofitClient.reset();

                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Email hoặc mật khẩu không chính xác", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("ĐĂNG NHẬP");
                Toast.makeText(LoginActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
