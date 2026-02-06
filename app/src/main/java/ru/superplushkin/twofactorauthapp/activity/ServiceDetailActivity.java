package ru.superplushkin.twofactorauthapp.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrInterface;
import com.r0adkll.slidr.model.SlidrListener;
import com.r0adkll.slidr.util.ViewDragHelper;

import ru.superplushkin.twofactorauthapp.db.DatabaseHelper;
import ru.superplushkin.twofactorauthapp.R;
import ru.superplushkin.twofactorauthapp.subclasses.SlidrConfigHelper;
import ru.superplushkin.twofactorauthapp.subclasses.TOTPGenerator;
import ru.superplushkin.twofactorauthapp.model.Service;

public class ServiceDetailActivity extends MyActivity implements ServiceDetailsBottomSheet.OnServiceBottomSheetClickListener {

    private Service service;
    private DatabaseHelper dbHelper;

    private TextView tvServiceName, tvAccount, tvCode;
    private ProgressBar progressBar;
    private ImageView ivServiceIcon, ivFavorite, ivCopyCode;
    private View favoriteContainer;
    private MaterialButton btnMoreDetails;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;
    private SlidrInterface slidrInterface;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().setAttributes(getWindow().getAttributes());

        setContentView(R.layout.activity_service_details);
        overridePendingTransition(R.anim.slide_in_right, 0);

        long serviceId = getIntent().getLongExtra("SERVICE_ID", -1);
        if (serviceId == -1) {
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);

        service = dbHelper.getService(serviceId);
        if (service == null) {
            finish();
            return;
        }

        initViews();
        setupViews();
        setupClickListeners();
        setupSwipeToGoBack();
        startCodeUpdates();
        setupBackPressedHandler();
    }

    private void initViews() {
        MaterialToolbar toolBar = findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        tvServiceName = findViewById(R.id.tvServiceName);
        tvAccount = findViewById(R.id.tvAccount);
        tvCode = findViewById(R.id.tvCode);

        progressBar = findViewById(R.id.progressBar);
        ivServiceIcon = findViewById(R.id.ivServiceIcon);
        ivFavorite = findViewById(R.id.ivFavorite);
        ivCopyCode = findViewById(R.id.ivCopyCode);

        favoriteContainer = findViewById(R.id.favoriteContainer);

        btnMoreDetails = findViewById(R.id.btnMoreDetails);
    }

    private void setupViews() {
        tvServiceName.setText(service.getServiceName());

        String accountOrIssuer = service.getAccount();
        if (accountOrIssuer == null || accountOrIssuer.isEmpty())
            accountOrIssuer = service.getIssuer();

        if (accountOrIssuer == null || accountOrIssuer.isEmpty())
            accountOrIssuer = "";

        tvAccount.setVisibility(accountOrIssuer.isEmpty() ? View.GONE : View.VISIBLE);
        tvAccount.setText(accountOrIssuer);

        tvCode.setTextSize(calculateTextSize(service.getDigits()));

        updateFavoriteIcon(service.isFavorite());
        updateCode();
    }
    private void setupClickListeners() {
        ivCopyCode.setOnClickListener(v -> copyCodeToClipboard());
        tvCode.setOnClickListener(v -> copyCodeToClipboard());
        ivFavorite.setOnClickListener(v -> toggleFavorite());
        btnMoreDetails.setOnClickListener(v -> showDetailsBottomSheet());
    }
    private void setupSwipeToGoBack() {
        slidrInterface = Slidr.attach(this, SlidrConfigHelper.createRightEdgeConfig(new SlidrListener() {
            @Override
            public boolean onSlideClosed() {
                finishActivityWithResult(false, false);
                return true;
            }
            @Override public void onSlideStateChanged(int state) {
                if (state == ViewDragHelper.STATE_DRAGGING)
                    handler.removeCallbacks(updateRunnable);
                else
                    handler.post(updateRunnable);
            }
            @Override public void onSlideChange(float percent) {}
            @Override public void onSlideOpened() {}
        }));
        slidrInterface.unlock();
    }
    private void setupBackPressedHandler() {
        var callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishActivityWithResult(false, true);
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void startCodeUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateCode();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateRunnable);
    }

    private void showDetailsBottomSheet() {
        var bottomSheet = ServiceDetailsBottomSheet.newInstance(service);
        bottomSheet.setClickListener(this);
        bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
    }
    private void showQRCodeBottomSheet() {
        var qrBottomSheet = QRCodeBottomSheet.newInstance(service);
        qrBottomSheet.show(getSupportFragmentManager(), qrBottomSheet.getTag());
    }

    private void updateCode() {
        String algorithm = service.getAlgorithm() != null ? service.getAlgorithm() : "SHA1";
        short digits = service.getDigits() > 0 ? service.getDigits() : 6;

        String code = TOTPGenerator.generateCode(service.getSecretKey(), algorithm, digits);
        tvCode.setText(code.length() == digits ? TOTPGenerator.formatCode(code) : code);
        updateTimer();
    }
    private void updateFavoriteIcon(boolean isFavorite) {
        if (isFavorite) {
            ivFavorite.setImageResource(R.drawable.ic_star);
            ivFavorite.setColorFilter(ContextCompat.getColor(this, R.color.favorite_star_background));
            favoriteContainer.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(150)
                .withEndAction(() -> favoriteContainer.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start())
                .start();
        } else {
            ivFavorite.setImageResource(R.drawable.ic_star_only_border);
            ivFavorite.setColorFilter(ContextCompat.getColor(this, R.color.not_favorite_star_background));
            favoriteContainer.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(150)
                .withEndAction(() -> favoriteContainer.animate()
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .setDuration(150)
                        .start())
                .start();
        }
    }
    private void updateTimer() {
        progressBar.setProgress(TOTPGenerator.getTimerProgress());

        if (TOTPGenerator.getTimeRemainingFromGenerator() <= 5) {
            tvCode.setTextColor(ContextCompat.getColor(this, R.color.attention_color));
            progressBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.attention_color)));
        } else {
            tvCode.setTextColor(ContextCompat.getColor(this, R.color.text_hard_on_background_primary));
            progressBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_hard_on_background_primary)));
        }
    }

    private void toggleFavorite() {
        boolean newState = !service.isFavorite();
        service.setFavorite(newState);

        new Thread(() -> dbHelper.toggleFavorite(service.getId())).start();

        updateFavoriteIcon(newState);

        Toast.makeText(this, newState ? R.string.added_to_favorites : R.string.removed_from_favorites, Toast.LENGTH_SHORT).show();
    }

    private void copyCodeToClipboard() {
        try {
            String code = tvCode.getText().toString().replace(" ", "");
            ClipData clip = ClipData.newPlainText("2FA Code", code);
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(clip);

            ivCopyCode.setColorFilter(ContextCompat.getColor(this, R.color.text_hard_on_background_primary));
            handler.postDelayed(() -> ivCopyCode.setColorFilter(ContextCompat.getColor(ServiceDetailActivity.this, R.color.text_medium_on_background_primary)), 150);

            dbHelper.incrementUsageCount(service.getId());
            service.incrementUsageCount();

        } catch (Exception e) {
            Toast.makeText(this, R.string.copy_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    @Override
    public void onQRClick(Service service) {
        showQRCodeBottomSheet();
    }

    @Override
    public void onEditClick(Service service) {
        // TODO: Реализовать редактирование
        Toast.makeText(this, R.string.soon_button, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(Service serviceToDelete) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_service)
                .setMessage(String.format(getString(R.string.delete_service_this_action_cannot_be_undone), serviceToDelete.getServiceName()))
                .setPositiveButton(R.string.service_delete_button, (dialog, which) -> finishActivityWithResult(true, true))
                .setNegativeButton(R.string.cancel_button, null)
                .setIcon(R.drawable.ic_warning)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(updateRunnable);
        handler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
    }

    private float calculateTextSize(int digits) {
        float baseSize = 48;    // размер для 6 символов
        int baseDigits = 6;      // базовое количество символов
        float sizeChange = 5;   // изменение размера
        int digitChange = 2;     // изменение количества символов

        return baseSize - (digits - baseDigits) * (sizeChange / digitChange);
    }

    private void finishActivityWithResult(boolean serviceNeedToDelete, boolean activityNeedToAnimate){
        if (slidrInterface != null)
            slidrInterface.lock();

        Intent intent = new Intent(ServiceDetailActivity.this, MainActivity.class);
        intent.putExtra("service_id", service.getId());
        intent.putExtra("serviceNeedToDelete", serviceNeedToDelete);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        setResult(RESULT_OK, intent);
        finish();
        overridePendingTransition(0, activityNeedToAnimate ? R.anim.slide_out_right : 0);
    }
}