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

import com.group27.watchyourwallet.api.VisionApiClient;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Button scanButton;
    private TextView resultTextView;
    private VisionApiClient visionApiClient;
    private ExecutorService executorService;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processSelectedImage(uri);
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
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

        scanButton = findViewById(R.id.buttonToTakePhoto);
        resultTextView = findViewById(R.id.textViewResult);

        //creates the helper
        visionApiClient = new VisionApiClient();
        executorService = Executors.newSingleThreadExecutor();

        //wait for button click
        scanButton.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });
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
                    //MainActivity connects to your OCR class
                    String extractedText = visionApiClient.extractTextFromImage(finalBitmap);

                    runOnUiThread(() -> resultTextView.setText(extractedText));

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