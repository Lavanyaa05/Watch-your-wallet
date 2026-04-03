package com.group27.watchyourwallet.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParser {

    public static String extractStoreName(String rawText) {
        if (rawText == null || rawText.isEmpty()) return "Unknown Store";

        String[] lines = rawText.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && line.length() > 2) {
                return line;
            }
        }
        return "Unknown Store";
    }

    public static double extractAmount(String rawText) {
        if (rawText == null || rawText.isEmpty()) return 0.0;

        // First look for "total" keyword
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

        Pattern pattern = Pattern.compile(
                "\\d{1,2}[/\\-.}]\\d{1,2}[/\\-.}]\\d{2,4}"
        );
        Matcher matcher = pattern.matcher(rawText);
        if (matcher.find()) return matcher.group();

        return "Unknown Date";
    }

    public static Receipt parseReceipt(String rawText, String userId,
                                       CategoryClassifier classifier) {
        String storeName = extractStoreName(rawText);
        double amount = extractAmount(rawText);
        String date = extractDate(rawText);
        String category = classifier.classify(storeName);

        return new Receipt(storeName, amount, category, date, userId);
    }
}