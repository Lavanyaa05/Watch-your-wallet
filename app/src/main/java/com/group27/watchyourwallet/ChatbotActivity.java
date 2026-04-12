package com.group27.watchyourwallet;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.group27.watchyourwallet.api.OpenAIService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatbotActivity extends AppCompatActivity {

    private TextView chatHistory;
    private EditText userInput;
    private androidx.cardview.widget.CardView sendButton;
    private ScrollView scrollView;
    private OpenAIService openAIService;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        chatHistory = findViewById(R.id.chatHistory);
        userInput = findViewById(R.id.userInput);
        sendButton = findViewById(R.id.sendButton);
        scrollView = findViewById(R.id.scrollView);

        openAIService = new OpenAIService();
        executorService = Executors.newSingleThreadExecutor();

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String message = userInput.getText().toString().trim();

        if (message.isEmpty()) return;

        // Show user message
        appendMessage("You", message);
        userInput.setText("");
        sendButton.setClickable(false);
        sendButton.setAlpha(0.5f);  // dims it to show it's disabled



        // Send to OpenAI on background thread
        executorService.execute(() -> {
            String response = openAIService.chat(message);
            runOnUiThread(() -> {
                appendMessage("Assistant", response);
                sendButton.setClickable(true);
                sendButton.setAlpha(1.0f);  // restores full opacity sendButton.setClickable(true);
                // Auto scroll to bottom
                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
            });
        });
    }

    private void appendMessage(String sender, String message) {
        String current = chatHistory.getText().toString();
        String newMessage = current + sender + ": " + message + "\n\n";
        chatHistory.setText(newMessage);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}