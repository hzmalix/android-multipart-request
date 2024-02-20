package com.hzmalix.multipartreq;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {

    String url = "https://subdomain.ngrok.io/endpoint"; // TODO: Replace URL

    OkHttpClient client = new OkHttpClient();
    Button btnSelect, btnUpload;
    ImageView imgView;
    TextView txtResult;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSelect = findViewById(R.id.btnSelect);
        btnUpload = findViewById(R.id.btnUpload);
        txtResult = findViewById(R.id.txtResult);
        imgView = findViewById(R.id.imgView);

        btnSelect.setOnClickListener(this::selectImage);
        btnUpload.setOnClickListener(this::uploadImage);

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (result.getData() != null && result.getData().getExtras() != null) {
                            Bundle extras = result.getData().getExtras();
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            imgView.setImageBitmap(imageBitmap);
                        }

                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        imgView.setImageURI(uri);
                    }
                });
    }

    public static byte[] bitmapToBytes(Bitmap bitmap) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
            return bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Bitmap getResizedBitmap(Bitmap bitmap) {
        int maxSize = 400;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private void selectImage(View v) {
        String ACTION_CAPTURE = getString(R.string.action_capture);
        String ACTION_CHOOSE = getString(R.string.action_choose);
        String ACTION_CANCEL = getString(R.string.action_cancel);
        final CharSequence[] options = {ACTION_CAPTURE, ACTION_CHOOSE, ACTION_CANCEL};
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this);
        builder.setTitle(getString(R.string.app_name)).setCancelable(true);
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals(ACTION_CAPTURE)) {
                Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if (takePicture.resolveActivity(getPackageManager()) != null) {
                    cameraLauncher.launch(takePicture);
                } else {
                    Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
                }
            } else if (options[item].equals(ACTION_CHOOSE)) {
                galleryLauncher.launch("image/*");
            } else {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void uploadImage(View v) {
        Bitmap bitmap = ((BitmapDrawable) imgView.getDrawable()).getBitmap();
        bitmap = getResizedBitmap(bitmap);

        byte[] bytes = bitmapToBytes(bitmap);
        if (bytes != null) {
            RequestBody fileBody = RequestBody.create(bytes, MediaType.parse("image/png"));
            MultipartBody multipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "temp_.png", fileBody)
                    .build();

            Request request = new Request.Builder().url(url).post(multipartBody).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response)
                        throws IOException {
                    ResponseBody body = response.body();
                    if (body != null) {
                        String res = body.string();
                        Log.v("RESPONSE", res);
                    } else {
                        Log.e("ERROR", "Failed to upload");
                    }
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("ERROR", "Failed to upload");
                    if (e.getMessage() != null) {
                        Log.e("ERROR_MSG", e.getMessage());
                    }
                }
            });

        }
    }
}
