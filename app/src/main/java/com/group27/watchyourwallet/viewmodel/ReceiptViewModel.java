package com.group27.watchyourwallet.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.group27.watchyourwallet.BuildConfig;
import com.group27.watchyourwallet.model.CategoryClassifier;
import com.group27.watchyourwallet.model.Receipt;
// import com.group27.watchyourwallet.model.ReceiptParser;
import com.group27.watchyourwallet.repository.*;
import java.util.List;

public class ReceiptViewModel extends AndroidViewModel {

    private ReceiptRepository repository;
    private CategoryClassifier classifier;
    private MutableLiveData<List<Receipt>> receiptsLiveData;
    private MutableLiveData<Receipt> scannedReceiptLiveData;
    private MutableLiveData<String> errorLiveData;
    private MutableLiveData<Boolean> loadingLiveData;

    private static final String USER_ID = "user1"; // hardcoded for now

    public ReceiptViewModel(@NonNull Application application) {
        super(application);
        repository = new ReceiptRepository(BuildConfig.MONGODB_URI);
        classifier = new CategoryClassifier(application);
        receiptsLiveData = new MutableLiveData<>();
        scannedReceiptLiveData = new MutableLiveData<>();
        errorLiveData = new MutableLiveData<>();
        loadingLiveData = new MutableLiveData<>();
    }

    // Called when OCR returns raw text
    public void processOcrResult(String rawText) {
        loadingLiveData.setValue(true);

        // Parse the raw text into a Receipt object
        //Receipt receipt = ReceiptParser.parseReceipt(rawText, USER_ID, classifier);

        // Post to UI so user can review before saving
        //scannedReceiptLiveData.setValue(receipt);
        loadingLiveData.setValue(false);
    }

    // Called when user confirms and saves the receipt
    public void saveReceipt(Receipt receipt) {
        loadingLiveData.setValue(true);
        repository.saveReceipt(receipt, new ReceiptRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                loadingLiveData.postValue(false);
                // Refresh the list after saving
                loadReceipts();
            }

            @Override
            public void onFailure(String error) {
                loadingLiveData.postValue(false);
                errorLiveData.postValue("Failed to save: " + error);
            }
        });
    }

    // Load all receipts for this user
    public void loadReceipts() {
        loadingLiveData.setValue(true);
        repository.getReceipts(USER_ID, new ReceiptRepository.OnReceiptsLoadedListener() {
            @Override
            public void onReceiptsLoaded(List<Receipt> receipts) {
                receiptsLiveData.postValue(receipts);
                loadingLiveData.postValue(false);
            }
        });
    }

    // Getters for LiveData — UI observes these
    public MutableLiveData<List<Receipt>> getReceiptsLiveData() {
        return receiptsLiveData;
    }

    public MutableLiveData<Receipt> getScannedReceiptLiveData() {
        return scannedReceiptLiveData;
    }

    public MutableLiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public MutableLiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}