package com.group27.watchyourwallet.model;

public class Receipt {
    private String receiptID;
    private String storeName;
    private double amount;
    private String category;
    private String date;
    private String rawText;
    private String userId;

    // full constructor
    private Receipt (String storeName, double amount, String category,
                     String date, String rawText) {
        this.storeName = storeName;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.rawText = rawText;
    }
    
    public static Receipt fromInputs(String storeName, String amountStr,
                                     String category, String date,
                                     String rawText) {

        double amount = 0;
        try {
            amount = Double.parseDouble(amountStr.replace("$", ""));
        } catch (Exception ignored) {}

        Receipt receipt = new Receipt(storeName, amount, category, date, rawText);

        receipt.setUserId("user_1");
        return receipt;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
