package com.group27.watchyourwallet.model;

public class QueryIntent {
    // "DATA_QUERY" or "GENERAL_ADVICE"
    public String type;

    // e.g. "Food", "Shopping", "Transport" — matches t.category
    public String category;

    // One of: THIS_WEEK, LAST_WEEK, THIS_MONTH, LAST_MONTH,
    // THIS_YEAR, LAST_YEAR, SPECIFIC_MONTH, SPECIFIC_YEAR, ALL_TIME
    // null = ALL_TIME
    public String period;

    // e.g. "Sephora", "Starbucks" — matches t.storeName (case-insensitive)
    public String merchant;

    public int month;
    public int year;
}