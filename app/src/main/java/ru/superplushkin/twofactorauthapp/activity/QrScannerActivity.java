package ru.superplushkin.twofactorauthapp.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ru.superplushkin.twofactorauthapp.R;
import ru.superplushkin.twofactorauthapp.model.QRService;

public class QrScannerActivity extends MyActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private static final long SCAN_COOLDOWN_MS = 2000;
    private static final long SAME_QR_DELAY_MS = 4000;

    private ImageButton btnGallery;
    private TextView tvHint;
    private ImageView qrOverlay;
    private PreviewView cameraPreview;

    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;

    private ScheduledExecutorService cooldownExecutor;
    private ScheduledFuture<?> cooldownFuture;
    private BarcodeScanner barcodeScanner;
    private boolean isScanning = true;
    private boolean isResultProcessed = false;
    private boolean isGalleryPickerActive = false;
    private long lastScanTime = 0;

    private String lastScannedQr = "";
    private long lastSameQrScanTime = 0;

    private ActivityResultLauncher<Intent> pickImageGalleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_qr_scanner);

        initViews();
        initBarcodeScanner();
        setupClickListeners();
        setupBackPressedHandler();
        setupResultLaunchers();

        if (checkCameraPermission())
            startScanner();
    }

    private void initViews() {
        Toolbar toolBar = findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        btnGallery = findViewById(R.id.btnGallery);
        tvHint = findViewById(R.id.tvHint);
        qrOverlay = findViewById(R.id.qrOverlay);
        cameraPreview = findViewById(R.id.cameraPreview);

        startHintAnimation();
    }
    private void initBarcodeScanner() {
        var options = new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build();
        barcodeScanner = BarcodeScanning.getClient(options);

        cameraExecutor = Executors.newSingleThreadExecutor();
        cooldownExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    private void setupClickListeners() {
        btnGallery.setOnClickListener(v -> pickImageFromGallery());
        cameraPreview.setOnClickListener(new View.OnClickListener() {
            private long lastClickTime = -300;

            @Override
            public void onClick(View v) {
                long clickTime = System.currentTimeMillis();
                if (clickTime - lastClickTime < 300)
                    restartScanner();

                lastClickTime = clickTime;
            }
        });
    }
    private void setupBackPressedHandler() {
        var callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithCancel();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }
    private void setupResultLaunchers() {
        pickImageGalleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    isGalleryPickerActive = false;
                    isScanning = true;

                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null)
                            processImageFromGallery(imageUri);
                    }

                    resumeCamera();
                }
        );
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {
                Manifest.permission.CAMERA
            }, CAMERA_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    private void startScanner() {
        var cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                var imageAnalysis = new ImageAnalysis.Builder()
                        .setImageQueueDepth(1)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::processImageProxy);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Toast.makeText(this, R.string.camera_failed, Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    private void restartScanner() {
        isScanning = true;
        isResultProcessed = false;
        lastScannedQr = "";
        tvHint.setText(R.string.scan_qr_prompt);
        tvHint.setTextColor(ContextCompat.getColor(this, R.color.text_hard_on_background_primary));
        startHintAnimation();
        resumeCamera();
        Toast.makeText(this, R.string.restarting_scanner, Toast.LENGTH_SHORT).show();
    }
    private void handleScanResult(String qrContent) {
        if (isResultProcessed || !isScanning)
            return;

        long currentTime = System.currentTimeMillis();
        if (qrContent.equals(lastScannedQr) && (currentTime - lastSameQrScanTime < SAME_QR_DELAY_MS))
            return;

        if (currentTime - lastScanTime < SCAN_COOLDOWN_MS)
            return;

        lastScanTime = currentTime;

        lastScannedQr = qrContent;
        lastSameQrScanTime = currentTime;

        QRService service = processQrContent(qrContent);
        if (service == null) {
            startScanCooldown();
            return;
        }

        isResultProcessed = true;
        isScanning = false;
        cancelCooldown();

        tvHint.setText(R.string.qr_scanned_success);
        tvHint.setTextColor(Color.GREEN);
        tvHint.clearAnimation();

        startSuccessfulScanAnimation(service);
    }
    private QRService processQrContent(String qrContent) {
        try {
            Uri uri = Uri.parse(qrContent);
            if (!"otpauth".equals(uri.getScheme()) || !"totp".equals(uri.getHost())) {
                Toast.makeText(this, R.string.invalid_qr_format, Toast.LENGTH_SHORT).show();
                return null;
            }

            String path = uri.getPath();
            if (path != null && path.startsWith("/"))
                path = path.substring(1);

            String label = path != null ? path : "";
            String serviceName = "";
            String account = "";
            String secret = uri.getQueryParameter("secret");
            String issuer = uri.getQueryParameter("issuer");
            String algorithm = uri.getQueryParameter("algorithm");
            String digits = uri.getQueryParameter("digits");

            if (!label.isEmpty()) {
                if (label.contains(":")) {
                    String[] parts = label.split(":", 2);
                    serviceName = parts[0];
                    account = parts[1];
                } else {
                    serviceName = label;
                }
            }

            if (issuer == null || issuer.isEmpty()) {
                Toast.makeText(this, R.string.invalid_qr_format, Toast.LENGTH_SHORT).show();
                return null;
            }

            if (label.isEmpty())
                serviceName = issuer;

            if (algorithm == null || algorithm.isEmpty())
                algorithm = "SHA1";

            try{
                digits = String.valueOf(Short.parseShort(digits != null ? digits : "6"));
            } catch(NumberFormatException ex) {
                digits = "6";
            }

            return new QRService(serviceName, secret, account, issuer, algorithm, Short.parseShort(digits));

        } catch (Exception e) {
            Toast.makeText(this, R.string.failed_to_parse_qr, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void resumeCamera() {
        if (cameraProvider == null && checkCameraPermission())
            startScanner();
    }
    private void stopCamera() {
        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
                cameraProvider = null;
            } catch (Exception ignored) {}
        }
    }

    private void startScanCooldown() {
        if (cooldownFuture != null && !cooldownFuture.isDone())
            cooldownFuture.cancel(false);

        isScanning = false;
        tvHint.setText(R.string.scan_cooldown);
        tvHint.setTextColor(Color.RED);

        cooldownFuture = cooldownExecutor.schedule(() -> {
            runOnUiThread(() -> {
                isScanning = true;
                tvHint.setText(R.string.scan_qr_prompt);
                tvHint.setTextColor(ContextCompat.getColor(this, R.color.text_hard_on_background_primary));
            });
        }, SCAN_COOLDOWN_MS, TimeUnit.MILLISECONDS);
    }
    private void cancelCooldown() {
        if (cooldownFuture != null && !cooldownFuture.isDone())
            cooldownFuture.cancel(false);
    }

    private void pickImageFromGallery() {
        isGalleryPickerActive = true;
        isScanning = false;
        stopCamera();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            pickImageGalleryLauncher.launch(Intent.createChooser(intent, getString(R.string.select_image)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.no_gallery_app, Toast.LENGTH_SHORT).show();
            isGalleryPickerActive = false;
            isScanning = true;
            resumeCamera();
        }
    }
    private void processImageFromGallery(Uri imageUri) {
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                if (bitmap != null) {
                    String qrContent = decodeQRFromBitmap(bitmap);
                    runOnUiThread(() -> {
                        if (qrContent != null && !qrContent.isEmpty()) {
                            handleScanResult(qrContent);
                        } else {
                            Toast.makeText(this, R.string.no_qr_in_image, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, R.string.failed_to_load_image, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.error_processing_image, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageProxy(ImageProxy imageProxy) {
        if(imageProxy.getImage() == null){
            Toast.makeText(this, R.string.chosen_not_image, Toast.LENGTH_SHORT).show();
            imageProxy.close();
            return;
        }

        if (!isScanning || isResultProcessed || isGalleryPickerActive || cameraProvider == null) {
            imageProxy.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty() && isScanning && !isResultProcessed) {
                        final String rawValue = barcodes.get(0).getRawValue();
                        if (rawValue != null)
                            runOnUiThread(() -> handleScanResult(rawValue));
                    }
                })
                .addOnFailureListener(e -> {})
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private String decodeQRFromBitmap(Bitmap bitmap) {
        try {
            // Конвертируем Bitmap в формат, понятный ZXing
            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));

            return new MultiFormatReader().decode(binaryBitmap, hints).getText();
        } catch (Exception e) {
            return null;
        }
    }

    private void startSuccessfulScanAnimation(QRService service) {
        qrOverlay.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(200)
                .withEndAction(() -> qrOverlay.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .withEndAction(() -> qrOverlay.postDelayed(() -> finishWithData(service), 250)))
                .start();
    }
    private void startHintAnimation() {
        tvHint.animate()
                .alpha(0.7f)
                .setDuration(1000)
                .withEndAction(() -> tvHint.animate().alpha(1f).setDuration(1000))
                .start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finishWithCancel();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanner();
            } else {
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
                finishWithCancel();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isGalleryPickerActive = false;
        stopCamera();
        if (cameraExecutor != null)
            cameraExecutor.shutdown();
        if (cooldownExecutor != null)
            cooldownExecutor.shutdown();
        if (barcodeScanner != null)
            barcodeScanner.close();
    }

    private void finishWithData(QRService service) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("SCAN_RESULT_SERVICE", service);
        setResult(RESULT_OK, resultIntent);
        finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }
    private void finishWithCancel() {
        setResult(RESULT_CANCELED);
        finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }
}