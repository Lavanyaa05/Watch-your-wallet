package com.group27.watchyourwallet;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.group27.watchyourwallet.api.OpenAIService;
import com.group27.watchyourwallet.model.QueryIntent;
import com.group27.watchyourwallet.model.Transaction;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatbotActivity extends AppCompatActivity {

    private TextView chatHistory;
    private EditText userInput;
    private CardView sendButton;
    private ScrollView scrollView;
    private OpenAIService openAIService;
    private ExecutorService executorService;
    private List<Transaction> allTransactions = new ArrayList<>();

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

        String json = getIntent().getStringExtra("transactions");

        if (json != null) {
            Type type =
                    new TypeToken<List<Transaction>>() {}.getType();

            allTransactions = new Gson().fromJson(json, type);
        }

        if (allTransactions == null) {
            allTransactions = new ArrayList<>();
        }

        sendButton.setOnClickListener(v -> sendMessage());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chatbot");
        }
    }

    private boolean isDataQuestion(String message) {

        message = message.toLowerCase();

        return (message.contains("spend") || message.contains("spent"))
                && (
                message.contains("transport")
                        || message.contains("food")
                        || message.contains("shopping")
                        || message.contains("entertainment")
                        || message.contains("this month")
                        || message.contains("today")
                        || message.contains("total")
        );
    }

    private String extractCategory(String message) {

        message = message.toLowerCase();

        if (message.contains("transport")) return "Transport";
        if (message.contains("food")) return "Food & Dining";
        if (message.contains("shopping")) return "Shopping";
        if (message.contains("entertainment")) return "Entertainment";

        return null;
    }

    private String handleDataQuestion(String message) {

        String category = extractCategory(message);
        String period = null;

        double total = 0;

        HashMap<String, Double> breakdown = new HashMap<>();

        for (Transaction t : allTransactions) {

            LocalDate date = parseDate(t.date);
            if (date == null) continue;

            // CATEGORY FILTER
            boolean matchCategory =
                    category == null || t.category.equals(category);

            // TIME FILTER (DEFAULT = FALSE SAFE)
            boolean matchTime = false;

            // USE DETECTED PERIOD (NOT message.contains)
            if (period.equals("ALL_TIME")) {
                matchTime = true;
            }

            // YEAR FILTER
            else if (message.contains("2020")) {
                matchTime = (date.getYear() == 2020);
            }
            else if (message.contains("2025")) {
                matchTime = (date.getYear() == 2025);
            }

            // THIS MONTH
            else if (period.equals("THIS_MONTH")) {
                LocalDate now = LocalDate.now();
                matchTime =
                        date.getMonth() == now.getMonth()
                                && date.getYear() == now.getYear();
            }

            // LAST MONTH (IMPORTANT FIX)
            else if (period.equals("LAST_MONTH")) {

                LocalDate now = LocalDate.now();
                LocalDate lastMonth = now.minusMonths(1);

                matchTime =
                        date.getMonth() == lastMonth.getMonth()
                                && date.getYear() == lastMonth.getYear();
            }

            // FINAL COMBINATION
            if (matchCategory && matchTime) {
                total += t.amount;
                breakdown.put(
                        t.storeName,
                        breakdown.getOrDefault(t.storeName, 0.0) + t.amount
                );
            }
        }

        StringBuilder result = new StringBuilder();

        result.append("You spent $")
                .append(String.format("%.2f", total))
                .append("\n");

        result.append("Period: ").append(period).append("\n\n");

        result.append("Breakdown:\n");

        for (Map.Entry<String, Double> entry : breakdown.entrySet()) {
            result.append("- ")
                    .append(entry.getKey())
                    .append(": $")
                    .append(String.format("%.2f", entry.getValue()))
                    .append("\n");
        }

        return result.toString();
    }

    private void sendMessage() {
        String message = userInput.getText().toString().trim();

        if (message.isEmpty()) return;

        appendMessage("You", message);
        userInput.setText("");

        sendButton.setClickable(false);
        sendButton.setAlpha(0.5f);

        executorService.execute(() -> {

            String response = "";

            if (isDataQuestion(message)) {

                try {
                    String json = openAIService.extractIntent(message);

                    QueryIntent intent =
                            new Gson().fromJson(json, QueryIntent.class);

                    if (intent == null) intent = new QueryIntent();

                    response = handleDataQuestionWithIntent(intent);

                } catch (Exception e) {
                    response = "Sorry, I couldn't understand the query.";
                }
            }


            String finalResponse = response;
            runOnUiThread(() -> {
                appendMessage("Assistant", finalResponse);
                sendButton.setClickable(true);
                sendButton.setAlpha(1.0f);

                scrollView.post(() ->
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN));
            });
        });
    }

    private String handleDataQuestionWithIntent(QueryIntent intent) {

        double total = 0;
        HashMap<String, Double> breakdown = new HashMap<>();

        if (intent == null) {
            intent = new QueryIntent();
        }

        for (Transaction t : allTransactions) {
            LocalDate date = parseDate(t.date);
            if (date == null) continue;

            // CATEGORY FILTER
            String category = intent.category;

            boolean matchCategory =
                    category == null ||
                            category.trim().isEmpty() ||
                            t.category.equalsIgnoreCase(category.trim());

            // TIME FILTER
            boolean matchTime = true;

            if (intent.year != null) {

                matchTime = (date.getYear() == intent.year);

            } else if ("THIS_MONTH".equals(intent.period)) {

                LocalDate now = LocalDate.now();

                matchTime =
                        date.getMonth() == now.getMonth()
                                && date.getYear() == now.getYear();

            } else if ("LAST_MONTH".equals(intent.period)) {

                LocalDate now = LocalDate.now();
                LocalDate lastMonth = now.minusMonths(1);

                matchTime =
                        date.getMonth() == lastMonth.getMonth()
                                && date.getYear() == lastMonth.getYear();

            } else if ("THIS_YEAR".equals(intent.period)) {

                matchTime = (date.getYear() == LocalDate.now().getYear());
            }

            if (matchCategory && matchTime) {
                total += t.amount;
                breakdown.put(
                        t.storeName,
                        breakdown.getOrDefault(t.storeName, 0.0) + t.amount
                );
            }
        }

        StringBuilder result = new StringBuilder();

        result.append("You spent $")
                .append(String.format("%.2f", total))
                .append("\n\nBreakdown:\n");

        for (Map.Entry<String, Double> entry : breakdown.entrySet()) {
            result.append("- ")
                    .append(entry.getKey())
                    .append(": $")
                    .append(String.format("%.2f", entry.getValue()))
                    .append("\n");
        }

        return result.toString();
    }

    private void appendMessage(String sender, String message) {
        String current = chatHistory.getText().toString();
        String newMessage = current + sender + ": " + message + "\n\n";
        chatHistory.setText(newMessage);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private LocalDate parseDate(String dateStr) {

        dateStr = dateStr.trim();

        Locale locale = Locale.ENGLISH;

        DateTimeFormatter[] formats = new DateTimeFormatter[] {

                // 1. 14/03/26 or 08/11/2024
                DateTimeFormatter.ofPattern("d/M/yy", locale),
                DateTimeFormatter.ofPattern("d/M/yyyy", locale),

                // 2. 3/14/2026 (US format)
                DateTimeFormatter.ofPattern("M/d/yyyy", locale),

                // 3. 8 April 2026
                DateTimeFormatter.ofPattern("d MMMM yyyy", locale),

                // 4. 11 JAN 2026 / 08 Nov 2024
                DateTimeFormatter.ofPattern("d MMM yyyy", locale),

                // 5. fallback ISO (just in case)
                DateTimeFormatter.ISO_LOCAL_DATE
        };

        for (DateTimeFormatter f : formats) {
            try {
                return LocalDate.parse(dateStr, f);
            } catch (Exception ignored) {}
        }

        return null; // unknown format
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // returns to previous screen
        return true;
    }
}