package com.group27.watchyourwallet.model;

import java.util.regex.*;

public class ReceiptParser {

    private String[] lines;
    private String rawText;

    public ReceiptParser(String rawText) {
        this.rawText = rawText;
        this.lines = rawText.split("\n");
    }

    public String getStoreName() {
        for (String line : lines) {
            line = line.trim();
            // Must have more than 2 chars AND contain at least one letter
            if (!line.isEmpty() && line.length() > 2 && line.matches(".*[a-zA-Z].*")) {
                return line;
            }
        }
        return "Unknown";
    }

    public String getTotal() {
        // First try to find a line with "total" keyword
        Pattern totalPattern = Pattern.compile(
                "(?i)(grand total|total due|amount due|balance due|total)[^\\d]*(\\d+\\.\\d{2})"
        );
        Matcher totalMatcher = totalPattern.matcher(rawText);
        double lastMatch = -1;
        while (totalMatcher.find()) {
            lastMatch = Double.parseDouble(totalMatcher.group(2));
        }

        // If total keyword found, return it
        if (lastMatch != -1) {
            return "$" + lastMatch;
        }

        // Fallback: find the largest dollar amount on any line
        Pattern dollarPattern = Pattern.compile("\\$\\s*(\\d+\\.\\d{2})");
        Matcher dollarMatcher = dollarPattern.matcher(rawText);
        double largestAmount = -1;
        while (dollarMatcher.find()) {
            double amount = Double.parseDouble(dollarMatcher.group(1));
            if (amount > largestAmount) {
                largestAmount = amount;
            }
        }

        return largestAmount == -1 ? "Unknown" : "$" + largestAmount;
    }

    public String getDate() {
        // Format 1: 03/28/2026 or 03-28-2026
        Pattern numericDate = Pattern.compile(
                "\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\b"
        );

        // Format 2: 11 Jan 2026 or 11 January 2026
        Pattern writtenDate = Pattern.compile(
                "(?i)\\b(\\d{1,2}\\s+" +
                        "(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|" +
                        "jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)" +
                        "\\s+\\d{2,4})\\b"
        );

        Matcher m1 = numericDate.matcher(rawText);
        if (m1.find()) return m1.group(1);

        Matcher m2 = writtenDate.matcher(rawText);
        if (m2.find()) return m2.group(1);

        return "Unknown";
    }
}
