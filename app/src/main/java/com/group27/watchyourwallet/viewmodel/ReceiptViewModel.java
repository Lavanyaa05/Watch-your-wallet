package com.group27.watchyourwallet.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.group27.watchyourwallet.model.Receipt;

public class ReceiptViewModel extends AndroidViewModel {

    private MutableLiveData<Receipt> scannedReceiptLiveData;
    private MutableLiveData<String> errorLiveData;
    private MutableLiveData<Boolean> loadingLiveData;

    public ReceiptViewModel(@NonNull Application application) {
        super(application);
        scannedReceiptLiveData = new MutableLiveData<>();
        errorLiveData = new MutableLiveData<>();
        loadingLiveData = new MutableLiveData<>();
    }

    // Called when OCR returns raw text
    public void processOcrResult(String rawText) {
        loadingLiveData.setValue(true);
        // TODO: parse rawText into Receipt and post to scannedReceiptLiveData
        loadingLiveData.setValue(false);
    }

    // Getters for LiveData — UI observes these
    public MutableLiveData<Receipt> getScannedReceiptLiveData() {
        return scannedReceiptLiveData;
    }

    public MutableLiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public MutableLiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }
}