package com.example.simpmusic.ui.activity;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.Album;
import com.example.simpmusic.data.model.Song;
import com.example.simpmusic.utils.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadSongActivity extends AppCompatActivity {

    private TextInputEditText etSongTitle, etSongLyrics;
    private Spinner spinnerAlbums;
    private MaterialButton btnSelectAudio, btnSelectCover, btnUploadSong;
    private TextView tvAudioFileName, tvCoverFileName;
    private ProgressBar pbUpload;

    private Uri audioUri, coverUri;
    private List<Album> artistAlbums = new ArrayList<>();
    private long audioDurationSeconds = 0;

    private final ActivityResultLauncher<String> audioPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    audioUri = uri;
                    tvAudioFileName.setText(getFileName(uri));
                    audioDurationSeconds = getAudioDuration(uri);
                }
            }
    );

    private final ActivityResultLauncher<String> coverPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    coverUri = uri;
                    tvCoverFileName.setText(getFileName(uri));
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_song);

        initViews();
        fetchMyAlbums();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbarUploadSong);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etSongTitle = findViewById(R.id.etSongTitle);
        etSongLyrics = findViewById(R.id.etSongLyrics);
        spinnerAlbums = findViewById(R.id.spinnerAlbums);
        btnSelectAudio = findViewById(R.id.btnSelectAudio);
        btnSelectCover = findViewById(R.id.btnSelectCover);
        btnUploadSong = findViewById(R.id.btnUploadSong);
        tvAudioFileName = findViewById(R.id.tvAudioFileName);
        tvCoverFileName = findViewById(R.id.tvCoverFileName);
        pbUpload = findViewById(R.id.pbUpload);

        btnSelectAudio.setOnClickListener(v -> audioPickerLauncher.launch("audio/*"));
        btnSelectCover.setOnClickListener(v -> coverPickerLauncher.launch("image/*"));
        btnUploadSong.setOnClickListener(v -> validateAndUpload());
    }

    private void fetchMyAlbums() {
        int userId = AuthManager.getInstance(this).getUserId();
        RetrofitClient.getApiService().getAlbumsByArtist(userId).enqueue(new Callback<List<Album>>() {
            @Override
            public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    artistAlbums = response.body();
                    List<String> albumTitles = new ArrayList<>();
                    for (Album album : artistAlbums) {
                        albumTitles.add(album.getTitle());
                    }
                    // Sử dụng layout tùy chỉnh cho Spinner
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(UploadSongActivity.this,
                            R.layout.item_spinner_album, albumTitles);
                    adapter.setDropDownViewResource(R.layout.item_spinner_album);
                    spinnerAlbums.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<Album>> call, Throwable t) {
                Toast.makeText(UploadSongActivity.this, "Không thể tải danh sách album", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void validateAndUpload() {
        String title = etSongTitle.getText().toString().trim();
        String lyrics = etSongLyrics.getText().toString().trim();
        
        if (title.isEmpty()) {
            etSongTitle.setError("Vui lòng nhập tên bài hát");
            return;
        }
        if (audioUri == null) {
            Toast.makeText(this, "Vui lòng chọn tệp âm thanh", Toast.LENGTH_SHORT).show();
            return;
        }
        if (coverUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh bìa", Toast.LENGTH_SHORT).show();
            return;
        }
        if (artistAlbums.isEmpty()) {
            Toast.makeText(this, "Bạn cần tạo album trước khi tải bài hát", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadSong(title, lyrics);
    }

    private void uploadSong(String title, String lyrics) {
        pbUpload.setVisibility(View.VISIBLE);
        btnUploadSong.setEnabled(false);

        int selectedAlbumId = artistAlbums.get(spinnerAlbums.getSelectedItemPosition()).getId();

        RequestBody titlePart = RequestBody.create(MediaType.parse("text/plain"), title);
        RequestBody albumIdPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(selectedAlbumId));
        RequestBody durationPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(audioDurationSeconds));
        RequestBody lyricsPart = RequestBody.create(MediaType.parse("text/plain"), lyrics);

        MultipartBody.Part audioFilePart = prepareFilePart("AudioFile", audioUri);
        MultipartBody.Part coverFilePart = prepareFilePart("CoverFile", coverUri);

        RetrofitClient.getApiService().uploadSong(titlePart, albumIdPart, durationPart, lyricsPart, audioFilePart, coverFilePart)
                .enqueue(new Callback<Song>() {
                    @Override
                    public void onResponse(Call<Song> call, Response<Song> response) {
                        pbUpload.setVisibility(View.GONE);
                        btnUploadSong.setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(UploadSongActivity.this, "Tải lên thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(UploadSongActivity.this, "Lỗi khi tải lên: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Song> call, Throwable t) {
                        pbUpload.setVisibility(View.GONE);
                        btnUploadSong.setEnabled(true);
                        Toast.makeText(UploadSongActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        try {
            File file = new File(getCacheDir(), getFileName(fileUri));
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(fileUri)), file);
            return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private long getAudioDuration(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long timeInMillis = Long.parseLong(time);
            return timeInMillis / 1000;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
    }
}
