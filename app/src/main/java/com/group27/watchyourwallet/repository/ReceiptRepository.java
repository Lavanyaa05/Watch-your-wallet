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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReceiptRepository {
    private static final String TAG = "ReceiptRepository";
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> receiptsCollection;
    private ExecutorService executor;

    public ReceiptRepository(String mongoUri) {
        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                mongoClient = MongoClients.create(mongoUri);
                database = mongoClient.getDatabase("watchyourwallet");
                receiptsCollection = database.getCollection("receipts");
                Log.d(TAG, "MongoDB connected successfully");
            } catch (Exception e) {
                Log.e(TAG, "MongoDB connection failed: " + e.getMessage());
            }
        });
    }

    // Save a receipt to MongoDB
    public void saveReceipt(Receipt receipt, OnCompleteListener listener) {
        executor.execute(() -> {
            try {
                Document doc = new Document()
                        .append("storeName", receipt.getStoreName())
                        .append("amount", receipt.getAmount())
                        .append("category", receipt.getCategory())
                        .append("date", receipt.getDate())
                        .append("userId", receipt.getUserId());

                receiptsCollection.insertOne(doc);
                Log.d(TAG, "Receipt saved successfully");
                if (listener != null) listener.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Failed to save receipt: " + e.getMessage());
                if (listener != null) listener.onFailure(e.getMessage());
            }
        });
    }

    // Get all receipts for a user
    public void getReceipts(String userId, OnReceiptsLoadedListener listener) {
        executor.execute(() -> {
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
                    receipts.add(receipt);
                }

                Log.d(TAG, "Loaded " + receipts.size() + " receipts");
                if (listener != null) listener.onReceiptsLoaded(receipts);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load receipts: " + e.getMessage());
            }
        });
    }

    // Callback interfaces
    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnReceiptsLoadedListener {
        void onReceiptsLoaded(List<Receipt> receipts);
    }

    // Close connection when done
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        executor.shutdown();
    }
}
