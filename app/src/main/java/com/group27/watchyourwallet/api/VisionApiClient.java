package com.group27.watchyourwallet.api;

import com.group27.watchyourwallet.BuildConfig;
import android.graphics.Bitmap;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VisionApiClient {
    private static final String API_KEY = BuildConfig.VISION_API_KEY;
    private static final String API_URL = "https://vision.googleapis.com/v1/images:annotate?key=" + API_KEY;
    private OkHttpClient client;

    public VisionApiClient() {
        client = new OkHttpClient();
    }

    //android stores photos as a Bitmap object in memory
    public String extractTextFromImage(Bitmap bitmap) throws Exception {
        //convert image to base64 text, we pass bitmap into it
        String base64Image = bitmapToBase64(bitmap);
        //wraps it in a way vision api expects
        String requestBody = buildRequestJson(base64Image);

        //jsonType is the object that represents the format of data that's being sent
        MediaType jsonType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(requestBody, jsonType);
        //construct a request
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        // sends the request to vision api and waits for a reply
        Response response = client.newCall(request).execute();
        //gets reply
        String responseBody = response.body().string();

        android.util.Log.d("VisionAPI", "Response: " + responseBody);

        return parseResponse(responseBody);

    }

    private String bitmapToBase64(Bitmap bitmap) {
        //creates empty container to hold image bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //converts the bitmap into JPEG format at 90% quality for balance between image quality and file size and puts it in the container
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        //takes the bytes out of the container
        byte[] imageBytes = outputStream.toByteArray();
        //converts those bytes into a base64 text string, no_wrap ensures no break in string and returns it
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }

    private String buildRequestJson(String base64Image) throws Exception {
        //building image object
        JSONObject image = new JSONObject();
        image.put("content", base64Image);

        //building feature object
        JSONObject feature = new JSONObject();
        feature.put("type", "TEXT_DETECTION");

        //put feature into an array
        JSONArray features = new JSONArray();
        features.put(feature);

        //build request item by combining image and the feature into one
        JSONObject requestItem = new JSONObject();
        requestItem.put("image", image);
        requestItem.put("features", features);

        //putting request item into an array
        JSONArray requests = new JSONArray();
        requests.put(requestItem);

        //building final body
        JSONObject body = new JSONObject();
        body.put("requests", requests);

        return body.toString();

    }

    private String parseResponse(String responseBody) throws Exception {
        // Convert raw API response string into JSON object (like a dictionary)
        JSONObject json = new JSONObject(responseBody);

        // "responses" is the main array returned by Google Vision API
        JSONArray responses = json.getJSONArray("responses");

        // Since we only sent ONE image, we take the first response
        JSONObject firstResponse = responses.getJSONObject(0);

        // Sometimes Vision API may NOT detect any text
        // So we check if "textAnnotations" exists first to avoid crashing
        if (!firstResponse.has("textAnnotations")) {
            return "No text detected";
        }

        // Get the array of detected text blocks
        JSONArray textAnnotations = firstResponse.getJSONArray("textAnnotations");

        //  check if array is empty
        if (textAnnotations.length() == 0) {
            return "No text detected";
        }

        // The FIRST item (index 0) always contains the FULL extracted text
        JSONObject firstAnnotation = textAnnotations.getJSONObject(0);

        // "description" field = the actual OCR text from the receipt
        return firstAnnotation.getString("description");
    }

}
