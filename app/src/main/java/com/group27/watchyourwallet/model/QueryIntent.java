package com.group27.watchyourwallet.model;

public class QueryIntent {
    // "DATA_QUERY" or "GENERAL_ADVICE"
    public String type;

    // e.g. "Food", "Shopping", "Transport" — matches t.category
    public String category;

    // e.g. "THIS_MONTH", "LAST_MONTH", "THIS_YEAR", or null for all time
    public String period;

    // e.g. "Sephora", "Starbucks" — matches t.storeName (case-insensitive)
    public String merchant;
}