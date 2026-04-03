package com.group27.watchyourwallet.model;

public class Receipt {
    private String receiptID;
    private String storeName;
    private double amount;
    private String category;
    private String date;
    private String userID;
    private String rawText;

    // default empty constructor
    public Receipt() {}

    // full constructor
    public Receipt (String storeName, double amount, String category, String date, String userID, String rawText) {
        this.storeName = storeName;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.userID = userID;
        this.rawText = rawText;
    }

    // getters and setters
    public String getReceiptID() { return receiptID; }
    public void setReceiptID(String receiptID) { this.receiptID = receiptID; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getUserId() { return userID; }
    public void setUserId(String userId) { this.userID = userId; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
}
