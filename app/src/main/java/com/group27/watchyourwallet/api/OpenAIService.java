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

    public String chat(String userMessage) {
        String prompt = "You are a helpful personal finance assistant. " +
                "Help the user with questions about their spending and expenses.KEEP YOUR RESPONSE TO A MAXIMUM OF 100 WORDS\n" +
                "User: " + userMessage;
        try {
            return makeRequest(prompt, 100, 0.0);
        } catch (Exception e) {
            Log.e(TAG, "Chat failed: " + e.getMessage());
            return "Sorry, I couldn't process your message. Please try again.";
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

    public String extractIntent(String userMessage) {

        final String prompt =
                "You are an intent classifier for a personal finance app. " +
                        "Given a user message, respond ONLY with a valid JSON object. No explanation, no markdown.\n\n" +

                        "JSON fields:\n" +
                        "- type: \"DATA_QUERY\" if the user is asking about their own spending/transactions, " +
                        "  otherwise \"GENERAL_ADVICE\"\n" +
                        "- category: spending category if mentioned (e.g. \"Food & Dining\", \"Shopping\", \"Transport\", " +
                        "  \"Entertainment\") — null if not mentioned\n" +
                        "- period: \"THIS_MONTH\", \"LAST_MONTH\", \"THIS_YEAR\", or null if no time period mentioned\n" +
                        "- merchant: specific store or brand name if mentioned (e.g. \"Sephora\", \"Starbucks\", " +
                        "  \"Netflix\") — null if not mentioned\n\n" +

                        "Examples:\n" +
                        "User: \"how much did i spend on shopping\" → " +
                        "{\"type\":\"DATA_QUERY\",\"category\":\"Shopping\",\"period\":null,\"merchant\":null}\n" +

                        "User: \"how much did i spend in 2026\" → " +
                        "{\"type\":\"DATA_QUERY\",\"category\":null,\"period\":\"THIS_YEAR\",\"merchant\":null}\n" +

                        "User: \"how much did i spend on sephora\" → " +
                        "{\"type\":\"DATA_QUERY\",\"category\":null,\"period\":null,\"merchant\":\"Sephora\"}\n" +

                        "User: \"how much did i spend on food this month\" → " +
                        "{\"type\":\"DATA_QUERY\",\"category\":\"Food & Dining\",\"period\":\"THIS_MONTH\",\"merchant\":null}\n" +

                        "User: \"how should i save more money\" → " +
                        "{\"type\":\"GENERAL_ADVICE\",\"category\":null,\"period\":null,\"merchant\":null}\n\n" +

                        // ✅ THE FIX: append the actual user message here
                        "Now classify this message:\n" +
                        "User: \"" + userMessage + "\"";

        try {
            String raw = makeRequest(prompt, 80, 0.0);

            // Strip markdown code fences if model wraps response in ```json ... ```
            raw = raw.trim();
            if (raw.startsWith("```")) {
                raw = raw.replaceAll("```json", "").replaceAll("```", "").trim();
            }

            return raw;

        } catch (Exception e) {
            Log.e(TAG, "extractIntent failed: " + e.getMessage());
            return "{}";
        }
    }
}