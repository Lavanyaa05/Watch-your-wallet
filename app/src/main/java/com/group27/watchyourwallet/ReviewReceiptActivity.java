package com.group27.watchyourwallet;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.group27.watchyourwallet.model.Receipt;
import com.group27.watchyourwallet.repository.ReceiptRepository;

public class ReviewReceiptActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_receipt);

        EditText editStoreName = findViewById(R.id.editStoreName);
        EditText editAmount    = findViewById(R.id.editAmount);
        Spinner editCategory   = findViewById(R.id.editCategory);
        EditText editDate      = findViewById(R.id.editDate);
        Button saveButton      = findViewById(R.id.saveButton);

        // set up dropdown options
        String[] categories = {"Food & Dining", "Transport", "Shopping", "Entertainment", "Beauty & Wellness"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editCategory.setAdapter(adapter);

        // pre-select the AI category
        String aiCategory = getIntent().getStringExtra("category");
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equalsIgnoreCase(aiCategory)) {
                editCategory.setSelection(i);
                break;
            }
        }

        editStoreName.setText(getIntent().getStringExtra("storeName"));
        editAmount.setText(getIntent().getStringExtra("amount"));
        editDate.setText(getIntent().getStringExtra("date"));

        saveButton.setOnClickListener(v -> {
            String storeName = editStoreName.getText().toString();
            String amount    = editAmount.getText().toString();
            String category  = editCategory.getSelectedItem().toString();
            String date      = editDate.getText().toString();

            // Saving the data to MongoDB
            ReceiptRepository repository = new ReceiptRepository(BuildConfig.MONGODB_URI);
            String userId = "user_1";

            // Get raw OCR text passed from MainActivity
            String rawText = getIntent().getStringExtra("rawText");
            double finalAmount;
            try {
                finalAmount = Double.parseDouble(amount.replace("$", ""));
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            Receipt receipt = new Receipt(storeName, finalAmount, category, date, userId, rawText);

            repository.saveReceipt(receipt, new ReceiptRepository.OnCompleteListener() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        Toast.makeText(ReviewReceiptActivity.this, "Receipt saved!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> Toast.makeText(ReviewReceiptActivity.this, "Failed to save: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        });
    }
}