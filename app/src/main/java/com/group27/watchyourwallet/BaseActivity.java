package com.group27.watchyourwallet;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setupFab();
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fab_chatbot);
        if (fab != null) {  // ← check FIRST
            fab.setImageTintList(ColorStateList.valueOf(Color.WHITE));  // ← then use it
            fab.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChatbotActivity.class);
                startActivity(intent);
            });
        }
    }
}