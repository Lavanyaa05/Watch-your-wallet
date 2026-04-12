package com.group27.watchyourwallet;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.group27.watchyourwallet.model.Receipt;
import com.group27.watchyourwallet.repository.DataRefreshManager;
import com.group27.watchyourwallet.repository.ReceiptRepository;

public class ReviewReceiptActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_receipt);

        EditText editStoreName = findViewById(R.id.editStoreName);
        EditText editAmount = findViewById(R.id.editAmount);
        Spinner editCategory = findViewById(R.id.editCategory);
        EditText editDate = findViewById(R.id.editDate);
        Button saveButton = findViewById(R.id.saveButton);

        String[] categories = {
                "Food & Dining",
                "Transport",
                "Shopping",
                "Entertainment",
                "Uncategorised"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editCategory.setAdapter(adapter);

        String aiCategory = getIntent().getStringExtra("category");
        if (aiCategory != null) {
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equalsIgnoreCase(aiCategory)) {
                    editCategory.setSelection(i);
                    break;
                }
            }
        }

        editStoreName.setText(getIntent().getStringExtra("storeName"));
        editAmount.setText(getIntent().getStringExtra("amount"));
        editDate.setText(getIntent().getStringExtra("date"));

        saveButton.setOnClickListener(v -> {
            String storeName = editStoreName.getText().toString().trim();
            String amount = editAmount.getText().toString().trim();
            String category = editCategory.getSelectedItem().toString();
            String date = editDate.getText().toString().trim();

            if (storeName.isEmpty() || amount.isEmpty() || date.isEmpty()) {
                Toast.makeText(ReviewReceiptActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            ReceiptRepository repository = new ReceiptRepository("");
            String rawText = getIntent().getStringExtra("rawText");

            Receipt receipt = Receipt.fromInputs(
                    storeName,
                    amount,
                    category,
                    date,
                    rawText
            );

            receipt.setUserId("user_1");

            repository.saveReceipt(receipt, new ReceiptRepository.OnCompleteListener() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        Toast.makeText(ReviewReceiptActivity.this, "Receipt saved!", Toast.LENGTH_SHORT).show();

                        // TRIGGER DASHBOARD REFRESH
                        DataRefreshManager.notifyDataChanged();

                        finish();
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() ->
                            Toast.makeText(
                                    ReviewReceiptActivity.this,
                                    "Failed to save: " + error,
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
                }
            });
        });
    }
}