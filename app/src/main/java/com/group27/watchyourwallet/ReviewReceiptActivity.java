package com.group27.watchyourwallet;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ReviewReceiptActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_receipt);

        EditText editStoreName = findViewById(R.id.editStoreName);
        EditText editAmount    = findViewById(R.id.editAmount);
        EditText editCategory  = findViewById(R.id.editCategory);
        EditText editDate      = findViewById(R.id.editDate);
        Button saveButton      = findViewById(R.id.saveButton);

        editStoreName.setText(getIntent().getStringExtra("storeName"));
        editAmount.setText(getIntent().getStringExtra("amount"));
        editCategory.setText(getIntent().getStringExtra("category"));
        editDate.setText(getIntent().getStringExtra("date"));

        saveButton.setOnClickListener(v -> {
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}