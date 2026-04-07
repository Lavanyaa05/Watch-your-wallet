package com.group27.watchyourwallet.api;

import android.util.Log;
import com.group27.watchyourwallet.BuildConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class OpenAIService {

    private static final String TAG = "OpenAIService";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";
    private final String apiKey;

    public OpenAIService() {
        this.apiKey = BuildConfig.OPENAI_API_KEY;
    }

    // Categorises a store name using OpenAI
    public String categorise(String storeName) {
        String prompt = "You are a receipt categorisation assistant. " +
                "Given a store name from a receipt, return ONLY one of these exact categories: " +
                "Food & Dining, Transport, Beauty & Wellness, Groceries, Shopping, Entertainment, Uncategorised. " +
                "Store name: \"" + storeName + "\". " +
                "Reply with just the category name, nothing else. No punctuation, no explanation.";

        try {
            return makeRequest(prompt, 20, 0.0);
        } catch (Exception e) {
            Log.e(TAG, "Categorisation failed: " + e.getMessage());
            return "Uncategorised";
        }
    }
    private String makeRequest(String userMessage, int maxTokens, double temperature) throws Exception {
        // Build message
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", userMessage);

        JSONArray messages = new JSONArray();
        messages.put(message);

        // Build request body
        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("messages", messages);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);

        // Make HTTP connection
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        // Send request
        OutputStream os = conn.getOutputStream();
        os.write(body.toString().getBytes());
        os.flush();
        os.close();

        // Check response code
        int responseCode = conn.getResponseCode();
        Log.d(TAG, "Response code: " + responseCode);

        if (responseCode != 200) {
            Scanner errorScanner = new Scanner(conn.getErrorStream());
            StringBuilder errorBody = new StringBuilder();
            while (errorScanner.hasNext()) errorBody.append(errorScanner.nextLine());
            errorScanner.close();
            Log.e(TAG, "Error response: " + errorBody);
            return "Uncategorised";
        }

        // Read response
        Scanner scanner = new Scanner(conn.getInputStream());
        StringBuilder response = new StringBuilder();
        while (scanner.hasNext()) response.append(scanner.nextLine());
        scanner.close();

        Log.d(TAG, "OpenAI response: " + response);

        // Parse and return just the content
        JSONObject json = new JSONObject(response.toString());
        return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
    }
}