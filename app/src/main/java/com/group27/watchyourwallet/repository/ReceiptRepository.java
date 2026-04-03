package com.group27.watchyourwallet.repository;

import android.util.Log;
import com.group27.watchyourwallet.model.Receipt;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

public class ReceiptRepository {

    private static final String TAG = "ReceiptRepository";
    private MongoClient mongoClient;
    private MongoCollection<Document> receiptsCollection;

    public ReceiptRepository(String mongoUri) {
        try {
            // Connect synchronously so collection is ready immediately
            mongoClient = MongoClients.create(mongoUri);
            MongoDatabase database = mongoClient.getDatabase("watchyourwallet");
            receiptsCollection = database.getCollection("receipts");
            Log.d(TAG, "MongoDB connected successfully");
        } catch (Exception e) {
            Log.e(TAG, "MongoDB connection failed: " + e.getMessage());
        }
    }

    public void saveReceipt(Receipt receipt, OnCompleteListener listener) {
        new Thread(() -> {
            try {
                if (receiptsCollection == null) {
                    Log.e(TAG, "Collection is null — connection may have failed");
                    if (listener != null) listener.onFailure("Database not connected");
                    return;
                }

                Document doc = new Document()
                        .append("storeName", receipt.getStoreName())
                        .append("amount", receipt.getAmount())
                        .append("category", receipt.getCategory())
                        .append("date", receipt.getDate())
                        .append("userId", receipt.getUserId())
                        .append("rawText", receipt.getRawText());

                receiptsCollection.insertOne(doc);
                Log.d(TAG, "Receipt saved successfully");
                if (listener != null) listener.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Failed to save: " + e.getMessage());
                if (listener != null) listener.onFailure(e.getMessage());
            }
        }).start();
    }

    public void getReceipts(String userId, OnReceiptsLoadedListener listener) {
        new Thread(() -> {
            try {
                List<Receipt> receipts = new ArrayList<>();
                Document query = new Document("userId", userId);

                for (Document doc : receiptsCollection.find(query)) {
                    Receipt receipt = new Receipt();
                    receipt.setReceiptID(doc.getObjectId("_id").toString());
                    receipt.setStoreName(doc.getString("storeName"));
                    receipt.setAmount(doc.getDouble("amount"));
                    receipt.setCategory(doc.getString("category"));
                    receipt.setDate(doc.getString("date"));
                    receipt.setUserId(doc.getString("userId"));
                    receipt.setRawText(doc.getString("rawText"));
                    receipts.add(receipt);
                }

                Log.d(TAG, "Loaded " + receipts.size() + " receipts");
                if (listener != null) listener.onReceiptsLoaded(receipts);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load: " + e.getMessage());
            }
        }).start();
    }

    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnReceiptsLoadedListener {
        void onReceiptsLoaded(List<Receipt> receipts);
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}