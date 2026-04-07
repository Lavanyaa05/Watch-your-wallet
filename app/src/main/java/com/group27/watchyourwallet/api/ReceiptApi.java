package com.group27.watchyourwallet.api;

import com.group27.watchyourwallet.model.Receipt;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import java.util.List;

public interface ReceiptApi {

    // GET all receipts for a user
    @GET("receipts/{userId}")
    Call<List<Receipt>> getReceipts(@Path("userId") String userId);

    // POST a new receipt
    @POST("receipts")
    Call<Void> saveReceipt(@Body Receipt receipt);
}