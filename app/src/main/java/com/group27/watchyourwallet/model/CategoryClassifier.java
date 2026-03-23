package com.group27.watchyourwallet.model;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CategoryClassifier {

    private JSONArray categories;

    // Constructor that loads the JSON file when created
    public CategoryClassifier(Context context) {
        try {
            InputStream is = context.getAssets().open("categories.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(json);
            categories = obj.getJSONArray("categories");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Takes a store name and returns the matching category
    public String classify(String storeName) {
        if (storeName == null || storeName.isEmpty()) {
            return "Uncategorized";
        }

        String name = storeName.toLowerCase();

        try {
            // Loop through each category in the JSON file
            for (int i = 0; i < categories.length(); i++) {
                JSONObject category = categories.getJSONObject(i);
                String categoryName = category.getString("name");
                JSONArray keywords = category.getJSONArray("keywords");

                // Check if store name contains any keyword
                for (int j = 0; j < keywords.length(); j++) {
                    if (name.contains(keywords.getString(j).toLowerCase())) {
                        return categoryName;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Uncategorized";
    }
}