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
                "You are an intent classifier for a personal finance app. Today's date is " +
                        java.time.LocalDate.now().toString() + ".\n" +
                        "Given a user message, respond ONLY with a valid JSON object. No explanation, no markdown.\n\n" +

                        "JSON fields:\n" +
                        "- type: \"DATA_QUERY\" if asking about their own spending, otherwise \"GENERAL_ADVICE\"\n" +
                        "- category: spending category if mentioned (\"Food & Dining\", \"Shopping\", \"Transport\", " +
                        "  \"Entertainment\", \"Uncategorised\") — null if not mentioned (null = all categories)\n" +
                        "- merchant: specific store/brand name if mentioned — null if not mentioned\n" +
                        "- period: one of THIS_WEEK, LAST_WEEK, THIS_MONTH, LAST_MONTH, THIS_YEAR, LAST_YEAR, " +
                        "  SPECIFIC_MONTH, SPECIFIC_YEAR, ALL_TIME — null means ALL_TIME\n" +
                        "- month: integer 1-12 if a specific month is mentioned, else 0\n" +
                        "- year: 4-digit year if a specific year is mentioned, else 0\n\n" +

                        "Rules:\n" +
                        "- \"this year\" or \"so far this year\" or \"in 2026\" (current year) → THIS_YEAR\n" +
                        "- \"last year\" → LAST_YEAR\n" +
                        "- \"in 2025\" or any past/future year → SPECIFIC_YEAR, year=that year\n" +
                        "- \"in March\" → SPECIFIC_MONTH, month=3, year=0\n" +
                        "- \"in March 2025\" → SPECIFIC_MONTH, month=3, year=2025\n" +
                        "- \"this week\" → THIS_WEEK\n" +
                        "- \"last week\" → LAST_WEEK\n" +
                        "- \"this month\" → THIS_MONTH\n" +
                        "- \"last month\" → LAST_MONTH\n" +
                        "- No time mentioned → null (ALL_TIME)\n" +
                        "- No category mentioned → null (means ALL categories, not just one)\n\n" +

                        "Examples:\n" +
                        "{\"type\":\"DATA_QUERY\",\"category\":null,\"merchant\":null,\"period\":\"THIS_YEAR\",\"month\":0,\"year\":0}\n" +
                        "← for: \"how much did i spend in 2026 so far\"\n\n" +
                        "{\"type\":\"DATA_QUERY\",\"category\":null,\"merchant\":null,\"period\":\"LAST_WEEK\",\"month\":0,\"year\":0}\n" +
                        "← for: \"what did i spend last week\"\n\n" +
                        "{\"type\":\"DATA_QUERY\",\"category\":null,\"merchant\":null,\"period\":\"SPECIFIC_MONTH\",\"month\":3,\"year\":2025}\n" +
                        "← for: \"how much did i spend in March 2025\"\n\n" +
                        "{\"type\":\"DATA_QUERY\",\"category\":null,\"merchant\":null,\"period\":\"SPECIFIC_YEAR\",\"month\":0,\"year\":2025}\n" +
                        "← for: \"how much did i spend in 2025\"\n\n" +
                        "{\"type\":\"DATA_QUERY\",\"category\":\"Shopping\",\"merchant\":null,\"period\":\"LAST_MONTH\",\"month\":0,\"year\":0}\n" +
                        "← for: \"how much did i spend on shopping last month\"\n\n" +
                        "{\"type\":\"DATA_QUERY\",\"category\":null,\"merchant\":\"Sephora\",\"period\":null,\"month\":0,\"year\":0}\n" +
                        "← for: \"how much did i spend on sephora\"\n\n" +
                        "{\"type\":\"GENERAL_ADVICE\",\"category\":null,\"merchant\":null,\"period\":null,\"month\":0,\"year\":0}\n" +
                        "← for: \"how should i save more money\"\n\n" +

                        "Now classify this message:\n" +
                        "User: \"" + userMessage + "\"";

        try {
            String raw = makeRequest(prompt, 120, 0.0);
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