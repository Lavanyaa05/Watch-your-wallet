package com.group27.watchyourwallet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.ListenableFuture;
import com.group27.watchyourwallet.api.OpenAIService;
import com.group27.watchyourwallet.api.VisionApiClient;
import com.group27.watchyourwallet.model.PermissionHelper;
import com.group27.watchyourwallet.model.ReceiptParser;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PREVIEW_PERMISSION_REQUEST_CODE = 101;

    private Button btnGallery;
    private Button btnTakePhoto;
    private TextView resultTextView;
    private TextView processingTextView;
    private PreviewView previewView;

    private VisionApiClient visionApiClient;
    private ExecutorService executorService;
    private ImageCapture imageCapture;
    private boolean isProcessing = false;
    private Bitmap resizeBitmap(Bitmap original) {
        int maxWidth = 1200;

        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= maxWidth) {
            return original;
        }

        float ratio = (float) height / width;
        int newWidth = maxWidth;
        int newHeight = (int) (maxWidth * ratio);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    isProcessing = true;
                    showProcessingState();
                    processSelectedImage(uri);
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_scan);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(MainActivity.this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_scan) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            return false;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnGallery = findViewById(R.id.btnGallery);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        resultTextView = findViewById(R.id.textViewResult);
        processingTextView = findViewById(R.id.processingTextView);
        previewView = findViewById(R.id.previewView);

        visionApiClient = new VisionApiClient();
        executorService = Executors.newSingleThreadExecutor();

        btnGallery.setOnClickListener(v -> openGallery());
        btnTakePhoto.setOnClickListener(v -> capturePhoto());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCameraPreview();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PREVIEW_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isProcessing) {
            return;
        }

        resetScanUi();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCameraPreview();
        }
    }

    private void resetScanUi() {
        resultTextView.setVisibility(View.VISIBLE);
        previewView.setVisibility(View.VISIBLE);
        btnGallery.setVisibility(View.VISIBLE);
        btnTakePhoto.setVisibility(View.VISIBLE);
        processingTextView.setVisibility(View.GONE);
        resultTextView.setText("Scan your receipt");
    }

    private void showProcessingState() {
        previewView.setVisibility(View.GONE);
        btnGallery.setVisibility(View.GONE);
        btnTakePhoto.setVisibility(View.GONE);
        resultTextView.setVisibility(View.GONE);
        processingTextView.setVisibility(View.VISIBLE);
    }

    private void openGallery() {
        if (PermissionHelper.hasGalleryPermission(this)) {
            pickImageLauncher.launch("image/*");
        } else {
            PermissionHelper.requestGalleryPermission(this);
        }
    }

    private void startCameraPreview() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture
                );

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Failed to start camera preview", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        File photoFile = new File(
                getExternalCacheDir(),
                "receipt_" + System.currentTimeMillis() + ".jpg"
        );

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri imageUri = Uri.fromFile(photoFile);
                        isProcessing = true;
                        showProcessingState();
                        processSelectedImage(imageUri);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        resetScanUi();
                        Toast.makeText(MainActivity.this, "Capture failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PREVIEW_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraPreview();
            } else {
                Toast.makeText(this, "Camera permission is needed to scan receipts", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PermissionHelper.GALLERY_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
            bitmap = resizeBitmap(bitmap);
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
                        isProcessing = false;
                        Intent intent = new Intent(MainActivity.this, ReviewReceiptActivity.class);
                        intent.putExtra("storeName", storeName);
                        intent.putExtra("amount", amount);
                        intent.putExtra("category", category);
                        intent.putExtra("date", date);
                        intent.putExtra("rawText", extractedText);

                        startActivity(intent);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        isProcessing = false;
                        resetScanUi();
                        resultTextView.setText("OCR failed: " + e.getMessage());
                    });
                }
            });

        } catch (IOException e) {
            isProcessing = false;
            e.printStackTrace();
            resetScanUi();
            resultTextView.setText("Failed to load image");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}