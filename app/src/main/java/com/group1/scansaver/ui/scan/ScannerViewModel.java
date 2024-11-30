package com.group1.scansaver.ui.scan;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ScannerViewModel extends ViewModel {
    private final MutableLiveData<String> mText;

    public ScannerViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Please Login To Scan Barcodes");
    }

    public LiveData<String> getText() {
        return mText;
    }
}