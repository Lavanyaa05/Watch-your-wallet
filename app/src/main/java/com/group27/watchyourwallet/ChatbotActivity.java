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
import java.time.DayOfWeek;
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
        LocalDate now = LocalDate.now();

        // Each row: { storeName, amount, date }
        List<Object[]> matchedRows = new ArrayList<>();

        for (Transaction t : allTransactions) {
            if (t == null) continue;

            LocalDate date = parseDate(t.date);
            if (date == null) continue;

            // ── Category filter (null = ALL categories) ───────────────────────
            boolean matchCategory = true;
            if (intent.category != null && !intent.category.isEmpty()) {
                if (t.category == null) {
                    matchCategory = false;
                } else {
                    String ic = intent.category.toLowerCase();
                    String tc = t.category.toLowerCase();
                    matchCategory = tc.contains(ic) || ic.contains(tc);
                }
            }

            // ── Merchant filter ───────────────────────────────────────────────
            boolean matchMerchant =
                    intent.merchant == null || intent.merchant.isEmpty() ||
                            (t.storeName != null &&
                                    t.storeName.toLowerCase().contains(intent.merchant.toLowerCase()));

            // ── Time filter ───────────────────────────────────────────────────
            boolean matchTime = true;
            String period = (intent.period == null) ? "ALL_TIME" : intent.period;

            switch (period) {
                case "THIS_WEEK": {
                    LocalDate monday = now.with(DayOfWeek.MONDAY);
                    LocalDate sunday = monday.plusDays(6);
                    matchTime = !date.isBefore(monday) && !date.isAfter(sunday);
                    break;
                }
                case "LAST_WEEK": {
                    LocalDate monday = now.with(DayOfWeek.MONDAY).minusWeeks(1);
                    LocalDate sunday = monday.plusDays(6);
                    matchTime = !date.isBefore(monday) && !date.isAfter(sunday);
                    break;
                }
                case "THIS_MONTH": {
                    matchTime = date.getMonth() == now.getMonth()
                            && date.getYear() == now.getYear();
                    break;
                }
                case "LAST_MONTH": {
                    LocalDate last = now.minusMonths(1);
                    matchTime = date.getMonth() == last.getMonth()
                            && date.getYear() == last.getYear();
                    break;
                }
                case "THIS_YEAR": {
                    matchTime = date.getYear() == now.getYear();
                    break;
                }
                case "LAST_YEAR": {
                    matchTime = date.getYear() == now.getYear() - 1;
                    break;
                }
                case "SPECIFIC_YEAR": {
                    if (intent.year > 0) matchTime = date.getYear() == intent.year;
                    break;
                }
                case "SPECIFIC_MONTH": {
                    boolean monthOk = (intent.month <= 0) || date.getMonthValue() == intent.month;
                    boolean yearOk  = (intent.year  <= 0) || date.getYear()       == intent.year;
                    matchTime = monthOk && yearOk;
                    break;
                }
                case "ALL_TIME":
                default:
                    matchTime = true;
                    break;
            }

            if (matchCategory && matchMerchant && matchTime) {
                total += t.amount;
                String store = (t.storeName == null || t.storeName.isEmpty())
                        ? "Unknown" : t.storeName;
                matchedRows.add(new Object[]{ store, t.amount, date });
            }
        }

        if (matchedRows.isEmpty()) {
            return "No matching transactions found.";
        }

        // Sort chronologically
        matchedRows.sort((a, b) -> ((LocalDate) a[2]).compareTo((LocalDate) b[2]));

        String period = (intent.period == null) ? "ALL_TIME" : intent.period;
        boolean groupByMonth = "THIS_YEAR".equals(period)
                || "LAST_YEAR".equals(period)
                || "SPECIFIC_YEAR".equals(period);

        boolean groupByDay = "THIS_MONTH".equals(period)
                || "LAST_MONTH".equals(period)
                || "SPECIFIC_MONTH".equals(period)
                || "THIS_WEEK".equals(period)
                || "LAST_WEEK".equals(period);

        StringBuilder sb = new StringBuilder();
        sb.append("You spent $").append(String.format("%.2f", total));

        if (groupByMonth) {
            // ── Group by month (e.g. year queries) ───────────────────────────
            DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
            LinkedHashMap<String, List<Object[]>> byMonth = new LinkedHashMap<>();

            for (Object[] row : matchedRows) {
                String key = ((LocalDate) row[2]).format(monthFmt);
                byMonth.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            }

            for (Map.Entry<String, List<Object[]>> entry : byMonth.entrySet()) {
                double monthTotal = 0;
                for (Object[] row : entry.getValue()) monthTotal += (double) row[1];
                sb.append("\n\n").append(entry.getKey())
                        .append(" ($").append(String.format("%.2f", monthTotal)).append("):");
                for (Object[] row : entry.getValue()) {
                    sb.append("\n  • ").append(row[0])
                            .append(":  $").append(String.format("%.2f", (double) row[1]));
                }
            }

        } else if (groupByDay) {
            // ── Group by date (e.g. month / week queries) ────────────────────
            DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
            LinkedHashMap<LocalDate, List<Object[]>> byDay = new LinkedHashMap<>();

            for (Object[] row : matchedRows) {
                LocalDate d = (LocalDate) row[2];
                byDay.computeIfAbsent(d, k -> new ArrayList<>()).add(row);
            }

            for (Map.Entry<LocalDate, List<Object[]>> entry : byDay.entrySet()) {
                double dayTotal = 0;
                for (Object[] row : entry.getValue()) dayTotal += (double) row[1];
                sb.append("\n\n").append(entry.getKey().format(dayFmt))
                        .append(" ($").append(String.format("%.2f", dayTotal)).append("):");
                for (Object[] row : entry.getValue()) {
                    sb.append("\n  • ").append(row[0])
                            .append(":  $").append(String.format("%.2f", (double) row[1]));
                }
            }

        } else {
            // ── Flat list for merchant / all-time queries ─────────────────────
            sb.append("\n\nBreakdown:");
            for (Object[] row : matchedRows) {
                sb.append("\n• ").append(row[0])
                        .append(":  $").append(String.format("%.2f", (double) row[1]));
            }
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
        // Handles "11 JAN 2026", "8 April 2026", "8 april 2026" etc.
        String normalized = dateStr.trim().toLowerCase();
        // Capitalize first letter of each word for month name matching
        normalized = java.util.Arrays.stream(normalized.split(" "))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));

        Locale locale = Locale.ENGLISH;
        DateTimeFormatter[] formats = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("d/M/yy",      locale),
                DateTimeFormatter.ofPattern("d/M/yyyy",    locale),
                DateTimeFormatter.ofPattern("d MMM yyyy",  locale),   // "8 Apr 2026", "11 Jan 2026"
                DateTimeFormatter.ofPattern("d MMMM yyyy", locale),   // "8 April 2026"
                DateTimeFormatter.ofPattern("M/d/yyyy",    locale),
                DateTimeFormatter.ISO_LOCAL_DATE
        };
        for (DateTimeFormatter f : formats) {
            try { return LocalDate.parse(normalized, f); }
            catch (Exception ignored) {}
        }
        return null;
    }
}