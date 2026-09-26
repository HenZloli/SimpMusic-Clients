package com.example.simpmusic.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.simpmusic.R;
import com.example.simpmusic.data.api.RetrofitClient;
import com.example.simpmusic.data.model.Album;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateAlbumActivity extends AppCompatActivity {

    private static final String TAG = "CreateAlbum";

    private TextInputEditText etName;
    private TextInputEditText etDescription;

    private MaterialButton btnCreate;
    private ImageButton btnBack;

    private ImageView ivAlbumCover;
    private MaterialCardView cvCover;

    private Uri selectedImageUri;


    // =========================
    // PICK IMAGE
    // =========================

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {

                            selectedImageUri =
                                    result.getData().getData();

                            if (selectedImageUri != null) {

                                ivAlbumCover.setImageURI(
                                        selectedImageUri
                                );

                                ivAlbumCover.setPadding(
                                        0,
                                        0,
                                        0,
                                        0
                                );

                                ivAlbumCover.setColorFilter(null);
                            }
                        }
                    }
            );


    // =========================
    // ON CREATE
    // =========================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_create_album
        );

        initViews();
        setupEvents();
    }


    // =========================
    // INIT VIEWS
    // =========================

    private void initViews() {

        etName =
                findViewById(R.id.etName);

        etDescription =
                findViewById(R.id.etDescription);

        btnCreate =
                findViewById(R.id.btnCreate);

        btnBack =
                findViewById(R.id.btnBack);

        ivAlbumCover =
                findViewById(R.id.ivAlbumCover);

        cvCover =
                findViewById(R.id.cvCover);
    }


    // =========================
    // EVENTS
    // =========================

    private void setupEvents() {

        btnBack.setOnClickListener(v ->
                finish()
        );


        cvCover.setOnClickListener(v -> {

            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            );

            pickImageLauncher.launch(intent);
        });


        btnCreate.setOnClickListener(v -> {

            String name =
                    etName.getText()
                            .toString()
                            .trim();

            String description =
                    etDescription.getText()
                            .toString()
                            .trim();


            // Validate title

            if (name.isEmpty()) {

                etName.setError(
                        "Vui lòng nhập tên album"
                );

                etName.requestFocus();

                return;
            }


            // Validate image

            if (selectedImageUri == null) {

                Toast.makeText(
                        this,
                        "Vui lòng chọn ảnh bìa cho album",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            performCreateAlbum(
                    name,
                    description
            );
        });
    }


    // =========================
    // CREATE ALBUM
    // =========================

    private void performCreateAlbum(
            String name,
            String description
    ) {

        setLoading(true);

        try {

            // =========================
            // TITLE
            // =========================

            RequestBody titleBody =
                    RequestBody.create(
                            MediaType.parse("text/plain"),
                            name
                    );


            // =========================
            // DESCRIPTION
            // =========================

            RequestBody descriptionBody =
                    RequestBody.create(
                            MediaType.parse("text/plain"),
                            description
                    );


            // =========================
            // MIME TYPE
            // =========================

            String mimeType =
                    getContentResolver()
                            .getType(selectedImageUri);

            if (mimeType == null) {
                mimeType = "image/jpeg";
            }


            // =========================
            // FILE EXTENSION
            // =========================

            String extension =
                    MimeTypeMap
                            .getSingleton()
                            .getExtensionFromMimeType(
                                    mimeType
                            );

            if (extension == null) {
                extension = "jpg";
            }


            // =========================
            // CREATE TEMP FILE
            // =========================

            File imageFile =
                    new File(
                            getCacheDir(),
                            "album_cover_"
                                    + System.currentTimeMillis()
                                    + "."
                                    + extension
                    );


            // =========================
            // COPY URI -> FILE
            // =========================

            try (
                    InputStream inputStream =
                            getContentResolver()
                                    .openInputStream(
                                            selectedImageUri
                                    );

                    FileOutputStream outputStream =
                            new FileOutputStream(
                                    imageFile
                            )
            ) {

                if (inputStream == null) {

                    throw new Exception(
                            "Không thể đọc ảnh"
                    );
                }


                byte[] buffer =
                        new byte[8192];

                int read;

                while (
                        (read =
                                inputStream.read(buffer))
                                != -1
                ) {

                    outputStream.write(
                            buffer,
                            0,
                            read
                    );
                }

                outputStream.flush();
            }


            // =========================
            // IMAGE REQUEST BODY
            // =========================

            RequestBody imageBody =
                    RequestBody.create(
                            MediaType.parse(mimeType),
                            imageFile
                    );


            // =========================
            // MULTIPART IMAGE
            // =========================
            //
            // Backend:
            //
            // IFormFile? CoverFile
            //
            // nên phải gửi:
            //
            // "CoverFile"
            //

            MultipartBody.Part coverPart =
                    MultipartBody.Part.createFormData(
                            "CoverFile",
                            imageFile.getName(),
                            imageBody
                    );


            // =========================
            // API
            // =========================

            RetrofitClient
                    .getApiService()
                    .createAlbum(
                            titleBody,
                            descriptionBody,
                            coverPart
                    )
                    .enqueue(
                            new Callback<Album>() {

                                @Override
                                public void onResponse(
                                        Call<Album> call,
                                        Response<Album> response
                                ) {

                                    setLoading(false);


                                    if (response.isSuccessful()
                                            && response.body() != null) {

                                        Log.d(
                                                TAG,
                                                "Create album success: "
                                                        + response.body()
                                                        .getTitle()
                                        );


                                        Toast.makeText(
                                                CreateAlbumActivity.this,
                                                "Đã tạo album thành công",
                                                Toast.LENGTH_SHORT
                                        ).show();


                                        finish();

                                        return;
                                    }


                                    // =========================
                                    // ERROR
                                    // =========================

                                    String errorMessage =
                                            "Không có thông tin lỗi";


                                    try {

                                        if (response.errorBody()
                                                != null) {

                                            errorMessage =
                                                    response.errorBody()
                                                            .string();
                                        }

                                    } catch (Exception e) {

                                        Log.e(
                                                TAG,
                                                "Read error failed",
                                                e
                                        );
                                    }


                                    Log.e(
                                            TAG,
                                            "HTTP "
                                                    + response.code()
                                                    + " : "
                                                    + errorMessage
                                    );


                                    Toast.makeText(
                                            CreateAlbumActivity.this,
                                            "Lỗi "
                                                    + response.code()
                                                    + ": "
                                                    + errorMessage,
                                            Toast.LENGTH_LONG
                                    ).show();
                                }


                                @Override
                                public void onFailure(
                                        Call<Album> call,
                                        Throwable t
                                ) {

                                    setLoading(false);


                                    Log.e(
                                            TAG,
                                            "Create album failed",
                                            t
                                    );


                                    Toast.makeText(
                                            CreateAlbumActivity.this,
                                            "Lỗi kết nối: "
                                                    + t.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }
                    );


        } catch (Exception e) {

            setLoading(false);


            Log.e(
                    TAG,
                    "Image processing failed",
                    e
            );


            Toast.makeText(
                    this,
                    "Lỗi xử lý ảnh: "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // =========================
    // LOADING
    // =========================

    private void setLoading(boolean loading) {

        btnCreate.setEnabled(!loading);

        if (loading) {

            btnCreate.setText(
                    "ĐANG TẠO..."
            );

        } else {

            btnCreate.setText(
                    "TẠO ALBUM"
            );
        }
    }
}