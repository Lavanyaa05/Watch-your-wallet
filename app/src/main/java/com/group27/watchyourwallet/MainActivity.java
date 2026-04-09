package com.group27.watchyourwallet;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.group27.watchyourwallet.api.OpenAIService;
import com.group27.watchyourwallet.api.VisionApiClient;
import com.group27.watchyourwallet.model.ReceiptParser;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.group27.watchyourwallet.model.CameraHelper;
import com.group27.watchyourwallet.model.PermissionHelper;

public class MainActivity extends AppCompatActivity {

    private Button btnGallery;
    private Button btnTakePhoto;
    private TextView resultTextView;
    private VisionApiClient visionApiClient;
    private ExecutorService executorService;
    private CameraHelper cameraHelper;

    // Gallery picker
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processSelectedImage(uri);
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                }
            });

    // Camera launcher
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    processSelectedImage(cameraHelper.getPhotoUri());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnGallery = findViewById(R.id.btnGallery);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        resultTextView = findViewById(R.id.textViewResult);

        visionApiClient = new VisionApiClient();
        executorService = Executors.newSingleThreadExecutor();
        cameraHelper = new CameraHelper(this);

        btnGallery.setOnClickListener(v -> openGallery());
        btnTakePhoto.setOnClickListener(v -> openCamera());
    }

    private void openCamera() {
        if (PermissionHelper.hasCameraPermission(this)) {
            cameraHelper.launchCamera(cameraLauncher);
        } else {
            PermissionHelper.requestCameraPermission(this);
        }
    }

    private void openGallery() {
        if (PermissionHelper.hasGalleryPermission(this)) {
            pickImageLauncher.launch("image/*");
        } else {
            PermissionHelper.requestGalleryPermission(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                cameraHelper.launchCamera(cameraLauncher);
            } else {
                Toast.makeText(this, "Camera permission is needed to take photos", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PermissionHelper.GALLERY_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*");
            } else {
                Toast.makeText(this, "Gallery permission is needed to upload photos", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processSelectedImage(Uri imageUri) {
        try {
            Bitmap bitmap;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageUri);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            }

            resultTextView.setText("Scanning receipt...");

            Bitmap finalBitmap = bitmap;
            executorService.execute(() -> {
                try {
                    String extractedText = visionApiClient.extractTextFromImage(finalBitmap);

                    ReceiptParser parser = new ReceiptParser(extractedText);
                    OpenAIService categoryService = new OpenAIService();
                    String category = categoryService.categorise(extractedText);

                    String storeName = parser.getStoreName();
                    String amount = String.valueOf(parser.getTotal());
                    String date = parser.getDate();

                    runOnUiThread(() -> {
                        Intent intent = new Intent(MainActivity.this, ReviewReceiptActivity.class);
                        intent.putExtra("storeName", storeName);
                        intent.putExtra("amount", amount);
                        intent.putExtra("category", category);
                        intent.putExtra("date", date);
                        resultTextView.setText("Scan your receipt");
                        startActivity(intent);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            resultTextView.setText("OCR failed: " + e.getMessage()));
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            resultTextView.setText("Failed to load image");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}