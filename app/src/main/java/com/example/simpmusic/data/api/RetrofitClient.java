package com.example.simpmusic.data.api;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

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

    public static void init(Context context) {
        if (appContext == null && context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public static void reset() {
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

                    String tokenSent = null;
                    if (appContext != null) {
                        tokenSent = AuthManager.getInstance(appContext).getToken();
                        if (tokenSent != null && !tokenSent.isEmpty()) {
                            requestBuilder.header("Authorization", "Bearer " + tokenSent);
                        }
                    }

                    requestBuilder.header("Accept", "application/json");
                    Request request = requestBuilder.build();
                    Response response = chain.proceed(request);

                    String path = request.url().encodedPath();
                    boolean isAuthApi = path.contains("/api/Auth/");

                    if (response.code() == 401 && !isAuthApi && appContext != null && !isRedirecting) {
                        String currentToken = AuthManager.getInstance(appContext).getToken();
                        if (currentToken != null && !currentToken.isEmpty()) {
                            isRedirecting = true;
                            handleUnauthorized();
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
        
        new Handler(Looper.getMainLooper()).post(() -> {
            AuthManager.getInstance(appContext).clear();
            reset();
            Intent intent = new Intent(appContext, LoginActivity.class);
            intent.putExtra("SESSION_EXPIRED", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            appContext.startActivity(intent);
        });
    }
}
