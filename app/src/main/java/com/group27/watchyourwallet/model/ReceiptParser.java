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
            if (!line.isEmpty() && line.length() > 2 && line.matches(".*[a-zA-Z].*")) {
                return line;
            }
        }
        return "Unknown";
    }

    public String getTotal() {
        Pattern totalPattern = Pattern.compile(
                "(?i)(grand total|total due|amount due|balance due|total)[^\\d]*(\\d+\\.\\d{2})"
        );
        Matcher totalMatcher = totalPattern.matcher(rawText);
        double lastMatch = -1;
        while (totalMatcher.find()) {
            lastMatch = Double.parseDouble(totalMatcher.group(2));
        }

        if (lastMatch != -1) {
            return "$" + lastMatch;
        }

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
        Pattern numericDate = Pattern.compile(
                "\\b(\\d{1,4}[/-]\\d{1,2}[/-]\\d{2,4})\\b"
        );

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