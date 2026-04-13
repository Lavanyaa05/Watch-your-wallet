package com.group27.watchyourwallet.model;

public class Transaction {

    public String storeName;
    public double amount;
    public String category;
    public String date;

    public Transaction(String name, double amount, String category, String date) {
        this.storeName = name;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

}
