package com.group27.watchyourwallet;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "WYWPrefs";
    public static final String KEY_MONTHLY_BUDGET = "monthly_budget";

    private EditText etMonthlyBudget;
    private CardView btnSaveBudget;
    private TextView tvName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etMonthlyBudget = findViewById(R.id.etMonthlyBudget);
        btnSaveBudget   = findViewById(R.id.btnSaveBudget);
        tvName          = findViewById(R.id.tvName);

        // Load saved budget
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        double savedBudget = Double.longBitsToDouble(
                prefs.getLong(KEY_MONTHLY_BUDGET, Double.doubleToLongBits(0.0)));

        if (savedBudget > 0) {
            etMonthlyBudget.setText(String.format("%.2f", savedBudget));
        }

        // Load saved name
        String savedName = prefs.getString("user_name", "Steve");
        tvName.setText(savedName);

        // Save on button click
        btnSaveBudget.setOnClickListener(v -> saveBudget());

        // Save on keyboard "Done"
        etMonthlyBudget.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveBudget();
                return true;
            }
            return false;
        });

        // Edit name
        findViewById(R.id.btnEditName).setOnClickListener(v -> {
            // Simple inline editing — show an AlertDialog
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Edit Name");

            final EditText input = new EditText(this);
            input.setText(tvName.getText());
            input.setSelection(input.getText().length());
            builder.setView(input);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    tvName.setText(newName);
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putString("user_name", newName)
                            .apply();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });
    }

    private void saveBudget() {
        String input = etMonthlyBudget.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter a budget amount", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double budget = Double.parseDouble(input);

            // Save to SharedPreferences
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_MONTHLY_BUDGET, Double.doubleToLongBits(budget))
                    .apply();

            Toast.makeText(this, "Budget saved!", Toast.LENGTH_SHORT).show();

            // Hide keyboard
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etMonthlyBudget.getWindowToken(), 0);
            }

            // Return result so HomeActivity knows to refresh
            setResult(RESULT_OK);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
        }
    }
}