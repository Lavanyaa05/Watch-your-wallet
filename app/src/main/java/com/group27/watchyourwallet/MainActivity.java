package com.group27.watchyourwallet;

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
import com.group27.watchyourwallet.model.ReceiptParser;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Button btnGallery;
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

        btnGallery = findViewById(R.id.btnGallery);
        resultTextView = findViewById(R.id.textViewResult);

        visionApiClient = new VisionApiClient();
        executorService = Executors.newSingleThreadExecutor();

        btnGallery.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
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

                    runOnUiThread(() -> {
                        ReceiptParser parser = new ReceiptParser(extractedText);
                        String result = "Store: " + parser.getStoreName() + "\n" +
                                "Date: "  + parser.getDate()      + "\n" +
                                "Total: " + parser.getTotal();
                        resultTextView.setText(result);
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