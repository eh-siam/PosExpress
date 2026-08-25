package com.example.posexpress.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.posexpress.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QrScannerActivity extends AppCompatActivity {

    private static final String TAG = "QrScannerActivity";
    private static final int PERMISSION_CAMERA_REQUEST = 1001;

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private BarcodeScanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        previewView = findViewById(R.id.previewView);
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        cameraExecutor = Executors.newSingleThreadExecutor();
        scanner = BarcodeScanning.getClient();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA_REQUEST);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null) {
                            handleQrResult(rawValue);
                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Barcode scan failed", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void handleQrResult(String result) {
        // Logic to extract number from bKash QR if needed. 
        // Typically bKash personal QR raw value is the phone number itself.
        // Merchant QR might be a URL, we can extract the number from it.
        String extractedNumber = extractNumber(result);
        
        Intent data = new Intent();
        data.putExtra("scanned_number", extractedNumber);
        setResult(RESULT_OK, data);
        finish();
    }

    private String extractNumber(String raw) {
        if (raw == null || raw.isEmpty()) return "";

        Log.d(TAG, "Raw QR value: " + raw);
        String result = raw;

        // 1. Handle bKash URL format: https://www.bkash.com/app/s/?s=01712345678
        if (raw.contains("s=")) {
            int startIndex = raw.indexOf("s=") + 2;
            int endIndex = raw.indexOf("&", startIndex);
            if (endIndex == -1) {
                result = raw.substring(startIndex);
            } else {
                result = raw.substring(startIndex, endIndex);
            }
            Log.d(TAG, "Extracted from URL: " + result);
        }

        // 2. Remove all non-digit characters (spaces, dashes, etc.)
        result = result.replaceAll("\\D", "");

        // 3. Handle country code 880 (e.g., 8801712345678 -> 01712345678)
        if (result.startsWith("880") && result.length() == 13) {
            result = result.substring(2);
        }

        // 4. Ensure it's 11 digits starting with 01
        if (result.length() > 11) {
            // Take last 11 digits if it's still long
            result = result.substring(result.length() - 11);
        }

        Log.d(TAG, "Final processed number: " + result);
        return result;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CAMERA_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        scanner.close();
    }
}
