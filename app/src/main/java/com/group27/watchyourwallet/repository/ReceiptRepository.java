package com.group27.watchyourwallet.repository;

import com.group27.watchyourwallet.model.Receipt;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;
import java.util.Map;

import com.group27.watchyourwallet.api.RetrofitClient;
import com.group27.watchyourwallet.api.ReceiptApi;

public class ReceiptRepository {

    private ReceiptApi apiService;

    public ReceiptRepository(String mongodbUri) {
        apiService = RetrofitClient.getClient().create(ReceiptApi.class);
    }

    public ReceiptRepository() {
        apiService = RetrofitClient.getClient().create(ReceiptApi.class);
    }

    public void saveReceipt(Receipt receipt, OnCompleteListener listener) {
        apiService.saveReceipt(receipt).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    if (listener != null) listener.onSuccess();
                } else {
                    if (listener != null) listener.onFailure("Error code: "
                            + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (listener != null) listener.onFailure(t.getMessage());
            }
        });
    }

    public void getReceipts(String userId, OnReceiptsLoadedListener listener) {
        apiService.getReceipts(userId).enqueue(new Callback<List<Receipt>>() {
            @Override
            public void onResponse(Call<List<Receipt>> call, Response<List<Receipt>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (listener != null) listener.onReceiptsLoaded(response.body());
                } else {
                    if (listener != null) listener.onReceiptsLoaded(null);
                }
            }

            @Override
            public void onFailure(Call<List<Receipt>> call, Throwable t) {
                if (listener != null) listener.onReceiptsLoaded(null);
            }
        });
    }

    public interface CategoryCallback {
        void onSuccess(Map<String, Double> data);
        void onFailure(Throwable t);
    }


    public void getCategoryTotals(CategoryCallback callback) {

        apiService.getCategoryTotals().enqueue(new Callback<Map<String, Double>>() {

            @Override
            public void onResponse(Call<Map<String, Double>> call,
                                   Response<Map<String, Double>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure(new Exception("Failed"));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Double>> call, Throwable t) {
                callback.onFailure(t);
            }
        });
    }


    /*
    public void filterReceipts(Map<String, String> filters, OnReceiptsLoadedListener listener) {
        apiService.filterReceipts(filters).enqueue(new Callback<List<Receipt>>() {
            @Override
            public void onResponse(Call<List<Receipt>> call, Response<List<Receipt>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listener.onReceiptsLoaded(response.body());
                } else {
                    listener.onReceiptsLoaded(null);
                }
            }

            @Override
            public void onFailure(Call<List<Receipt>> call, Throwable t) {
                listener.onReceiptsLoaded(null);
            }
        });
    }

     */


    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnReceiptsLoadedListener {
        void onReceiptsLoaded(List<Receipt> receipts);
    }
}