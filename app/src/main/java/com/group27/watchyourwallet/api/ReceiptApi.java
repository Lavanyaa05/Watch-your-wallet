package com.group27.watchyourwallet.api;

import com.group27.watchyourwallet.model.Receipt;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import java.util.List;
import java.util.Map;

public interface ReceiptApi {

    // GET all receipts for a user
    @GET("receipts/user_1")
    Call<List<Receipt>> getReceipts();


    // POST a new receipt
    @POST("receipts")
    Call<Void> saveReceipt(@Body Receipt receipt);

    @GET("categoryTotals/user_1")
    Call<Map<String, Double>> getCategoryTotals();

    @GET("categoryTotal/user_1/{category}")
    Call<Map<String, Object>> getCategoryTotal(@Path("category") String category);

    // POST a question to chatbot
    //@POST("receipts/filter")
    //Call<List<Receipt>> filterReceipts(@Body Map<String, String> filters);
}