package com.example.simpmusic.data.api;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.simpmusic.ui.activity.LoginActivity;
import com.example.simpmusic.utils.AuthManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    public static final String BASE_URL = "http://10.0.2.2:5081/";
    private static Retrofit retrofit = null;
    private static Context appContext;
    private static boolean isRedirecting = false;
    private static final String TAG = "RetrofitClient";

    public static void init(Context context) {
        if (appContext == null && context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public static void reset() {
        Log.d(TAG, "Resetting Retrofit instance...");
        retrofit = null;
        isRedirecting = false;
    }

    public static String getAbsoluteUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty() || relativeUrl.startsWith("http") || relativeUrl.startsWith("/") || relativeUrl.startsWith("file")) {
            return relativeUrl;
        }
        return BASE_URL + relativeUrl.replace("\\", "/");
    }

    public static MusicApiService getApiService() {
        if (retrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS);
            
            httpClient.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder();

                    String path = original.url().encodedPath().toLowerCase();
                    boolean isAuthApi = path.contains("/api/auth/");
                    
                    String token = "";
                    if (appContext != null) {
                        token = AuthManager.getInstance(appContext).getToken();
                        if (!isAuthApi && token != null && !token.isEmpty()) {
                            requestBuilder.header("Authorization", "Bearer " + token);
                        }
                    }

                    requestBuilder.header("Accept", "application/json");
                    Request request = requestBuilder.build();
                    
                    // Log request details for debugging
                    Log.d(TAG, "--> " + request.method() + " " + request.url());
                    if (request.header("Authorization") != null) {
                        Log.d(TAG, "Authorization: Bearer " + (token.length() > 10 ? token.substring(0, 10) + "..." : token));
                    }

                    Response response = chain.proceed(request);
                    
                    Log.d(TAG, "<-- " + response.code() + " " + request.url());

                    // Xử lý 401 Unauthorized
                    if (response.code() == 401 && !isAuthApi && appContext != null) {
                        synchronized (RetrofitClient.class) {
                            if (!isRedirecting) {
                                String currentToken = AuthManager.getInstance(appContext).getToken();
                                String requestAuthHeader = request.header("Authorization");
                                
                                // Chỉ xử lý nếu request này được gửi với Token hiện tại
                                if (currentToken != null && !currentToken.isEmpty() && 
                                    ("Bearer " + currentToken).equals(requestAuthHeader)) {
                                    
                                    Log.e(TAG, "Received 401 for a valid-looking token. Redirecting...");
                                    isRedirecting = true;
                                    handleUnauthorized();
                                } else {
                                    Log.w(TAG, "Received 401 but token was already changed or empty. Ignoring redirect.");
                                }
                            }
                        }
                    }

                    return response;
                }
            });

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(MusicApiService.class);
    }

    private static void handleUnauthorized() {
        if (appContext == null) return;
        
        // Xóa dữ liệu ngay lập tức
        AuthManager.getInstance(appContext).clear();

        new Handler(Looper.getMainLooper()).post(() -> {
            Log.e(TAG, "Session Expired. Clearing data and redirecting to LoginActivity");
            Intent intent = new Intent(appContext, LoginActivity.class);
            intent.putExtra("SESSION_EXPIRED", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            appContext.startActivity(intent);
        });
    }
}
