package com.group27.watchyourwallet.model;

import android.util.Log;
import com.group27.watchyourwallet.api.OpenAIService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParser {

    private static final String TAG = "ReceiptParser";
    private String rawText;
    private String[] lines;

    public ReceiptParser(String rawText) {
        this.rawText = rawText;
        this.lines = rawText.split("\n");
    }

    public String getStoreName() {
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.matches("\\d+:\\d+")) continue;
            if (line.matches("[\\d]+")) continue;
            if (line.length() < 3) continue;
            if (line.startsWith("$")) continue;
            if (line.matches(".*\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,5}.*")) continue;
            Log.d(TAG, "Store name found: " + line);
            return line;
        }
        return "Unknown Store";
    }

    public double getAmount() {
        for (String line : lines) {
            String lower = line.toLowerCase();
            if (lower.contains("total") || lower.contains("amount")
                    || lower.contains("subtotal")) {
                Pattern pattern = Pattern.compile("\\d+[.,]\\d{2}");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return Double.parseDouble(matcher.group().replace(",", "."));
                }
            }
        }

        double largest = 0.0;
        Pattern pattern = Pattern.compile("\\d+[.,]\\d{2}");
        Matcher matcher = pattern.matcher(rawText);
        while (matcher.find()) {
            double value = Double.parseDouble(matcher.group().replace(",", "."));
            if (value > largest) largest = value;
        }
        return largest;
    }

    public String getDate() {
        Pattern fullYear = Pattern.compile( "\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\b");
        Matcher matcher = fullYear.matcher(rawText);
        if (matcher.find()) return matcher.group();

        Pattern shortYear = Pattern.compile("\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2}");
        Matcher matcher2 = shortYear.matcher(rawText);
        if (matcher2.find()) return matcher2.group();

        return "Unknown Date";
    }

    public Receipt toReceipt(String userId, OpenAIService openAIService) {
        String storeName = getStoreName();
        double amount = getAmount();
        String date = getDate();

        Log.d(TAG, "Sending to OpenAI: " + storeName);
        String category = openAIService.categorise(storeName);
        Log.d(TAG, "Category received: " + category);

        Log.d(TAG, "Final - Store: " + storeName
                + " | Amount: " + amount
                + " | Date: " + date
                + " | Category: " + category);

        return new Receipt(storeName, amount, category, date, userId, rawText);
    }
}