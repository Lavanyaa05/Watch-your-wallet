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
    private static final String API_URL = "https://api.openai.com/v1/responses";
    private static final String MODEL = "gpt-4o-mini";

    private final String apiKey;

    public OpenAIService() {
        this.apiKey = BuildConfig.OPENAI_API_KEY;
    }

    // Categorises full receipt text
    public String categorise(String rawReceiptText) {
        String prompt = "You are a receipt categorisation assistant. " +
                "Given the full text of a receipt, return ONLY one of these exact categories: " +
                "Food & Dining, Transport, Shopping, Entertainment, Uncategorised.\n\n" +
                "Receipt text: " + rawReceiptText + "\n\n" +
                "Reply with ONLY the category name from the list. Do not add anything else.";

        try {
            return makeRequest(prompt, 20, 0.0);
        } catch (Exception e) {
            Log.e(TAG, "Categorisation failed: " + e.getMessage());
            return "Uncategorised";
        }
    }

    private String makeRequest(String userMessage, int maxTokens, double temperature) throws Exception {

        // Build request JSON
        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("input", userMessage);
        body.put("max_output_tokens", maxTokens);
        body.put("temperature", temperature);

        // Setup connection
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        // Send request
        OutputStream os = conn.getOutputStream();
        os.write(body.toString().getBytes());
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        Log.d(TAG, "Response code: " + responseCode);

        Scanner scanner;

        if (responseCode >= 200 && responseCode < 300) {
            scanner = new Scanner(conn.getInputStream());
        } else {
            scanner = new Scanner(conn.getErrorStream());
        }

        StringBuilder response = new StringBuilder();
        while (scanner.hasNext()) {
            response.append(scanner.nextLine());
        }
        scanner.close();

        Log.d(TAG, "OpenAI response: " + response.toString());

        if (responseCode < 200 || responseCode >= 300) {
            return "Uncategorised";
        }

        // Parse response safely
        JSONObject json = new JSONObject(response.toString());

        JSONArray output = json.optJSONArray("output");
        if (output != null && output.length() > 0) {
            JSONObject firstOutput = output.getJSONObject(0);
            JSONArray content = firstOutput.optJSONArray("content");

            if (content != null && content.length() > 0) {
                JSONObject textObj = content.getJSONObject(0);
                String text = textObj.optString("text", "").trim();

                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        return "Uncategorised";
    }

    public String extractFilters(String userQuery) {

        String prompt = "You are a financial assistant. " +
                "Extract filters from this user query and return ONLY JSON. " +
                "Fields: storeName, category, date. " +

                "Categories must be one of: Food & Dining, Transport, Beauty & Wellness, Groceries, Shopping, Entertainment. " +

                "If not present, return null.\n\n" +

                "Examples:\n" +
                "Input: Show me Starbucks purchases\n" +
                "Output: {\"storeName\":\"Starbucks\",\"category\":null,\"date\":null}\n\n" +

                "Input: food last week\n" +
                "Output: {\"storeName\":null,\"category\":\"Food & Dining\",\"date\":\"last_week\"}\n\n" +

                "User query: " + userQuery;

        try {
            return makeRequest(prompt, 100, 0.0);
        } catch (Exception e) {
            return "{}";
        }
    }
}