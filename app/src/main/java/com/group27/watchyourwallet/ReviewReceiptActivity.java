package com.group27.watchyourwallet;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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
        String[] categories = {"Food & Dining", "Transport", "Shopping", "Entertainment", "Beauty & Wellness", "Uncategorised"};
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

            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}