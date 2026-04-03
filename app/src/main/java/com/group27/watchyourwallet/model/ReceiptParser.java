package com.group27.watchyourwallet.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParser {

    public static String extractStoreName(String rawText) {
        if (rawText == null || rawText.isEmpty()) return "Unknown Store";

        String[] lines = rawText.split("\\n");
        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty()) continue;
            if (line.matches("\\d+:\\d+")) continue;
            if (line.matches("\\d+")) continue;
            if (line.length() < 3) continue;
            if (line.startsWith("$")) continue;
            if (line.matches(".*\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,5}.*")) continue;

            android.util.Log.d("Parser", "Store name found: " + line);
            return line;
        }
        return "Unknown Store";
    }

    public static double extractAmount(String rawText) {
        if (rawText == null || rawText.isEmpty()) return 0.0;

        String[] lines = rawText.toLowerCase().split("\\n");
        for (String line : lines) {
            if (line.contains("total") || line.contains("amount")
                    || line.contains("subtotal")) {
                Pattern pattern = Pattern.compile("\\d+[.,]\\d{2}");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String amount = matcher.group().replace(",", ".");
                    return Double.parseDouble(amount);
                }
            }
        }

        // Fallback: find the largest number on the receipt
        double largest = 0.0;
        Pattern pattern = Pattern.compile("\\d+[.,]\\d{2}");
        Matcher matcher = pattern.matcher(rawText);
        while (matcher.find()) {
            String match = matcher.group().replace(",", ".");
            double value = Double.parseDouble(match);
            if (value > largest) largest = value;
        }
        return largest;
    }

    public static String extractDate(String rawText) {
        if (rawText == null || rawText.isEmpty()) return "Unknown Date";

        // Try to find a date with 4-digit year first (more specific)
        Pattern fullYear = Pattern.compile("\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{4}");
        Matcher matcher = fullYear.matcher(rawText);
        if (matcher.find()) return matcher.group();

        // Fallback: 2-digit year
        Pattern shortYear = Pattern.compile("\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2}");
        Matcher matcher2 = shortYear.matcher(rawText);
        if (matcher2.find()) return matcher2.find() ? matcher2.group() : "Unknown Date";

        return "Unknown Date";
    }

    public static Receipt parseReceipt(String rawText, String userId,
                                       CategoryClassifier classifier) {
        String storeName = extractStoreName(rawText);
        double amount = extractAmount(rawText);
        String date = extractDate(rawText);
        String category = classifier.classify(storeName);

        android.util.Log.d("Parser", "Parsed - Store: " + storeName
                + " Amount: " + amount + " Date: " + date + " Category: " + category);

        return new Receipt(storeName, amount, category, date, userId);
    }
}