package ru.superplushkin.twofactorauthapp.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import ru.superplushkin.twofactorauthapp.R;
import ru.superplushkin.twofactorauthapp.model.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ServiceDetailsBottomSheet extends BottomSheetDialogFragment {

    private Service service;
    private OnServiceBottomSheetClickListener clickListener;

    private boolean isKeyVisible = false;

    public static ServiceDetailsBottomSheet newInstance(Service service) {
        var fragment = new ServiceDetailsBottomSheet();
        var args = new Bundle();
        args.putParcelable("service", service);
        fragment.setArguments(args);

        return fragment;
    }
    public void setClickListener(OnServiceBottomSheetClickListener listener) {
        this.clickListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            service = getArguments().getParcelable("service");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_service_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvServiceName = view.findViewById(R.id.tvServiceName);
        TextView tvAccount = view.findViewById(R.id.tvAccount);
        TextView tvIssuer = view.findViewById(R.id.tvIssuer);
        TextView tvCreatedAt = view.findViewById(R.id.tvCreatedAt);
        TextView tvUsageCount = view.findViewById(R.id.tvUsageCount);
        TextView tvSecretKey = view.findViewById(R.id.tvSecretKey);
        TextView tvAlgorithm = view.findViewById(R.id.tvAlgorithm);
        TextView tvDigits = view.findViewById(R.id.tvDigits);
        MaterialButton btnToggleKey = view.findViewById(R.id.btnToggleKey);
        MaterialButton btnQR = view.findViewById(R.id.btnQR);
        MaterialButton btnEdit = view.findViewById(R.id.btnEdit);
        MaterialButton btnDelete = view.findViewById(R.id.btnDelete);

        if (service != null) {
            String serviceName = service.getServiceName();
            tvServiceName.setText(serviceName);
            tvServiceName.setOnClickListener(v -> copyCodeToClipboard(serviceName));

            String account = service.getAccount();
            tvAccount.setText(account != null && !account.isEmpty() ? account : getString(R.string.not_specified));
            tvAccount.setOnClickListener(v -> copyCodeToClipboard(account));
            
            String issuer = service.getIssuer();
            tvIssuer.setText(issuer != null && !issuer.isEmpty() ? issuer : getString(R.string.not_specified));
            tvIssuer.setOnClickListener(v -> copyCodeToClipboard(issuer));

            String createdAt = service.getCreatedAt();
            if (createdAt != null && !createdAt.isEmpty()) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                    Date date = inputFormat.parse(createdAt);
                    if (date != null)
                        tvCreatedAt.setText(outputFormat.format(date));
                } catch (Exception e) {
                    tvCreatedAt.setText(createdAt);
                }
            }
            tvIssuer.setOnClickListener(v -> copyCodeToClipboard(createdAt));

            tvUsageCount.setText(String.format(Locale.getDefault(), getString(R.string.service_used_times), service.getUsageCount()));

            String secretKey = service.getSecretKey();
            if (secretKey != null && !secretKey.isEmpty())
                tvSecretKey.setText("*".repeat(secretKey.length()));
            tvSecretKey.setOnClickListener(v -> {
                if(isKeyVisible)
                    copyCodeToClipboard(secretKey);
            });

            String algorithm = service.getAlgorithm();
            tvAlgorithm.setText(algorithm != null && !algorithm.isEmpty() ? algorithm : "SHA1");
            tvAlgorithm.setOnClickListener(v -> copyCodeToClipboard(algorithm));

            short digits = service.getDigits();
            tvDigits.setText(digits > 0 ? String.valueOf(digits) : "6");
            tvDigits.setOnClickListener(v -> copyCodeToClipboard(String.valueOf(digits)));
        }

        btnToggleKey.setOnClickListener(v -> {
            String secretKey = service.getSecretKey();
            isKeyVisible = !isKeyVisible;

            if (isKeyVisible) {
                tvSecretKey.setText(secretKey);
                btnToggleKey.setText(getString(R.string.service_hide_button));
            } else {
                tvSecretKey.setText("*".repeat(secretKey.length()));
                btnToggleKey.setText(getString(R.string.service_show_button));
            }
        });

        btnQR.setOnClickListener(v -> {
            if (clickListener != null && service != null)
                clickListener.onQRClick(service);
            dismiss();
        });

        btnEdit.setOnClickListener(v -> {
            if (clickListener != null && service != null)
                clickListener.onEditClick(service);
            dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            if (clickListener != null && service != null)
                clickListener.onDeleteClick(service);
            dismiss();
        });
    }

    private void copyCodeToClipboard(String text) {
        try {
            ClipData clip = ClipData.newPlainText("2FA Data", text);
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(clip);
        } catch (Exception e) {
            Toast.makeText(getContext(), "✗ Copy failed", Toast.LENGTH_SHORT).show();
        }
    }

    public interface OnServiceBottomSheetClickListener {
        void onQRClick(Service service);
        void onEditClick(Service service);
        void onDeleteClick(Service service);
    }
}