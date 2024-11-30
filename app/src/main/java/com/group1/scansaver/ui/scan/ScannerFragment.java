package com.group1.scansaver.ui.scan;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.Manifest;

import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.group1.scansaver.AddItemActivity;
import com.group1.scansaver.R;
import com.group1.scansaver.api.UPCApiRequest;
import com.group1.scansaver.databasehelpers.FirestoreHandler;
import com.group1.scansaver.databinding.FragmentScannerBinding;
import com.group1.scansaver.dataobjects.Item;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ScannerFragment extends Fragment {

    private FragmentScannerBinding binding;

    private ExecutorService cameraExecutor;

    private boolean isProcessingBarcode = false;
    private String scanResult;

    private FirestoreHandler fs;

    private static final int CAMERA_REQUEST_CODE = 100;

    public static ScannerFragment newInstance() {
        return new ScannerFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        ScannerViewModel scannerViewModel =
                new ViewModelProvider(this).get(ScannerViewModel.class);

        binding = FragmentScannerBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            final TextView textView = binding.textScan;
            scannerViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
            binding.previewView.setVisibility(View.GONE);
        }else{
            checkCameraPermission();
            fs = new FirestoreHandler();
            binding.previewView.setVisibility(View.VISIBLE);
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdownNow();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null &&
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
            startCameraPreview();
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else {
            startCameraPreview();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraPreview();
            } else {
                Toast.makeText(getContext(), "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCameraPreview() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();

                PreviewView previewView = binding.previewView;
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                Camera camera = cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void analyzeImage(@NonNull ImageProxy imageProxy) {

        if (isProcessingBarcode) {
            imageProxy.close();
            return;
        }

        @SuppressWarnings("UnsafeOptInUsageError")
        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build();

        BarcodeScanning.getClient(options)
                .process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        isProcessingBarcode = true;
                        String rawValue = barcode.getRawValue();
                        scanResult = rawValue;
                        vibratePhone();
                        handleScannedBarcode(rawValue);

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            isProcessingBarcode = false;
                        }, 2000);

                        break;
                    }
                })
                .addOnFailureListener(e -> e.printStackTrace())
                .addOnCompleteListener(task -> {
                    imageProxy.close();
                });
    }

    private void vibratePhone() {
        if (getContext() == null){
            return;
        }

        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(200);
            }
        }
    }

    private void handleScannedBarcode(String barcode) {

        Toast.makeText(getContext(), "Scanned: " + barcode, Toast.LENGTH_SHORT).show();

        UPCApiRequest apiRequest = new UPCApiRequest();
        apiRequest.fetchProductDetails(barcode, new UPCApiRequest.UPCApiResponseCallback() {
            @Override
            public void onSuccess(String barcodeAPI, String title, String msrp, String store, String imgURL) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Title: " + title, Toast.LENGTH_SHORT).show();
                    Toast.makeText(getContext(), "MSRP: " + msrp, Toast.LENGTH_SHORT).show();

                    try{
                        Item newItem = new Item(title,barcodeAPI,Double.parseDouble(msrp),store,imgURL);
                        fs.insertItemIntoFirestore(newItem);

                    }catch(Exception e){
                        new Thread(() -> {
                            boolean exists = fs.doesItemExist(barcode);
                            if (exists) {
                                Log.d("ITEM_CHECK", "Item exists in Firestore.");

                            } else {
                                Log.d("ITEM_CHECK", "Item does not exist in Firestore.");
                                Intent intent = new Intent(getActivity(), AddItemActivity.class);
                                intent.putExtra("SCANNED_BARCODE", barcode);
                                startActivity(intent);
                            }
                        }).start();

                    }

                });
            }
            @Override
            public void onError(String error) {}
        });

    }

}