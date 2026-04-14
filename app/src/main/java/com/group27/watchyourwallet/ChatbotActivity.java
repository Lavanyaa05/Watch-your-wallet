package com.group27.watchyourwallet;

import android.os.Bundle;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.group27.watchyourwallet.api.OpenAIService;
import com.group27.watchyourwallet.model.ChatAdapter;
import com.group27.watchyourwallet.model.ChatMessage;
import com.group27.watchyourwallet.model.QueryIntent;
import com.group27.watchyourwallet.model.Transaction;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatbotActivity extends AppCompatActivity {

    private EditText userInput;
    private CardView sendButton;
    private OpenAIService openAIService;
    private ExecutorService executorService;

    private List<ChatMessage> messages = new ArrayList<>();
    private List<Transaction> allTransactions = new ArrayList<>();
    private ChatAdapter adapter;
    private RecyclerView chatRecycler;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        userInput = findViewById(R.id.userInput);
        sendButton = findViewById(R.id.sendButton);
        chatRecycler = findViewById(R.id.chatRecycler);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // newest messages at bottom
        chatRecycler.setLayoutManager(layoutManager);

        adapter = new ChatAdapter(messages);
        chatRecycler.setAdapter(adapter);

        String json = getIntent().getStringExtra("transactions");
        if (json != null) {
            Type type = new TypeToken<List<Transaction>>() {}.getType();
            allTransactions = gson.fromJson(json, type);
        }

        openAIService = new OpenAIService();
        executorService = Executors.newSingleThreadExecutor();

        // Welcome message
        appendMessage("Hi! I'm Mira 👋 Ask me anything about your spending and finances!", false);

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String message = userInput.getText().toString().trim();
        if (message.isEmpty()) return;

        appendMessage(message, true);
        userInput.setText("");

        sendButton.setClickable(false);
        sendButton.setAlpha(0.5f);

        executorService.execute(() -> {
            String response;
            try {
                String intentJson = openAIService.extractIntent(message);
                QueryIntent intent;

                try {
                    intent = gson.fromJson(intentJson, QueryIntent.class);
                } catch (Exception e) {
                    intent = new QueryIntent();
                    intent.type = "GENERAL_ADVICE";
                }

                if (intent == null) {
                    intent = new QueryIntent();
                    intent.type = "GENERAL_ADVICE";
                }

                if ("DATA_QUERY".equals(intent.type)) {
                    response = handleDataQuery(intent);
                } else {
                    response = openAIService.chat(message);
                }

            } catch (Exception e) {
                response = "Sorry, something went wrong. Please try again.";
            }

            String finalResponse = response;
            runOnUiThread(() -> {
                appendMessage(finalResponse, false);
                sendButton.setClickable(true);
                sendButton.setAlpha(1.0f);
            });
        });
    }

    private String handleDataQuery(QueryIntent intent) {
        double total = 0;
        LinkedHashMap<String, Double> breakdown = new LinkedHashMap<>();

        for (Transaction t : allTransactions) {
            if (t == null) continue;

            LocalDate date = parseDate(t.date);
            if (date == null) continue;

            // --- Category filter (fuzzy: "Food & Dining" matches "Food", "food", etc.) ---
            boolean matchCategory = true;
            if (intent.category != null && !intent.category.isEmpty() && t.category != null) {
                String intentCat = intent.category.toLowerCase();
                String transCat  = t.category.toLowerCase();
                // Match if either contains the other (handles "Food & Dining" vs "Food")
                matchCategory = transCat.contains(intentCat) || intentCat.contains(transCat);
            } else if (intent.category != null && !intent.category.isEmpty() && t.category == null) {
                matchCategory = false;
            }

            // --- Merchant / store filter ---
            boolean matchMerchant =
                    intent.merchant == null ||
                            intent.merchant.isEmpty() ||
                            (t.storeName != null &&
                                    t.storeName.toLowerCase().contains(intent.merchant.toLowerCase()));

            // --- Time period filter ---
            boolean matchTime = true;
            if ("THIS_MONTH".equals(intent.period)) {
                LocalDate now = LocalDate.now();
                matchTime = date.getMonth() == now.getMonth()
                        && date.getYear() == now.getYear();

            } else if ("LAST_MONTH".equals(intent.period)) {
                LocalDate now = LocalDate.now();
                LocalDate last = now.minusMonths(1);
                matchTime = date.getMonth() == last.getMonth()
                        && date.getYear() == last.getYear();

            } else if ("THIS_YEAR".equals(intent.period)) {
                matchTime = date.getYear() == LocalDate.now().getYear();
            }

            if (matchCategory && matchMerchant && matchTime) {
                total += t.amount;
                String store = (t.storeName == null || t.storeName.isEmpty())
                        ? "Unknown" : t.storeName;
                breakdown.merge(store, t.amount, Double::sum);
            }
        }

        if (breakdown.isEmpty()) {
            return "No matching transactions found.";
        }

        // Sort by amount descending
        List<Map.Entry<String, Double>> entries = new ArrayList<>(breakdown.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        StringBuilder sb = new StringBuilder();
        sb.append("You spent $")
                .append(String.format("%.2f", total))
                .append("\n\nBreakdown:");

        for (Map.Entry<String, Double> e : entries) {
            sb.append("\n• ")
                    .append(e.getKey())
                    .append(":  $")
                    .append(String.format("%.2f", e.getValue()));
        }

        return sb.toString();
    }

    private void appendMessage(String message, boolean isUser) {
        messages.add(new ChatMessage(message, isUser));
        adapter.notifyItemInserted(messages.size() - 1);
        chatRecycler.post(() -> chatRecycler.scrollToPosition(messages.size() - 1));
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null) return null;
        Locale locale = Locale.ENGLISH;
        DateTimeFormatter[] formats = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("d/M/yy", locale),
                DateTimeFormatter.ofPattern("d/M/yyyy", locale),
                DateTimeFormatter.ofPattern("d MMM yyyy", locale),
                DateTimeFormatter.ISO_LOCAL_DATE
        };
        for (DateTimeFormatter f : formats) {
            try { return LocalDate.parse(dateStr, f); }
            catch (Exception ignored) {}
        }
        return null;
    }
}