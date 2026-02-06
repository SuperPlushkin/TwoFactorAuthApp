package ru.superplushkin.twofactorauthapp.activity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ru.superplushkin.twofactorauthapp.R;
import ru.superplushkin.twofactorauthapp.model.Service;

public class QRCodeBottomSheet extends BottomSheetDialogFragment {

    private Service service;
    private Bitmap qrBitmap;

    public static QRCodeBottomSheet newInstance(Service service) {
        var fragment = new QRCodeBottomSheet();
        var args = new Bundle();
        args.putParcelable("service", service);
        fragment.setArguments(args);
        return fragment;
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
        return inflater.inflate(R.layout.fragment_qr_code, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView qrImageView = view.findViewById(R.id.qrImageView);
        TextView tvServiceName = view.findViewById(R.id.tvServiceName);
        MaterialButton btnSaveToGallery = view.findViewById(R.id.btnSaveToGallery);

        if (service != null) {
            tvServiceName.setText(service.getServiceName());
            generateQRCode();

            if (qrBitmap != null)
                qrImageView.setImageBitmap(qrBitmap);
        }

        btnSaveToGallery.setOnClickListener(v -> {
            if (qrBitmap == null)
                return;

            try {
                saveQRCode(Environment.DIRECTORY_PICTURES);
                Toast.makeText(getContext(), R.string.saved_to_gallery, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(getContext(), R.string.failed_to_save, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateQRCode() {
        if (service == null)
            return;

        String keyName = service.getServiceName() != null ? service.getServiceName() : "";
        String account = service.getAccount() != null ? service.getAccount() : "";
        String secretKey = service.getSecretKey();
        String issuer = service.getIssuer() != null ? service.getIssuer() : service.getServiceName();
        String algorithm = service.getAlgorithm() != null ? service.getAlgorithm() : "SHA1";
        short digits = service.getDigits() > 0 ? service.getDigits() : 6;

        String label;
        if (!account.isEmpty() && !keyName.isEmpty()) {
            label = keyName + ":" + account;
        } else if (!issuer.isEmpty()) {
            label = keyName;
        } else if (!account.isEmpty()) {
            label = account;
        } else {
            label = "Unknown";
        }

        StringBuilder qrBuilder = new StringBuilder();
        qrBuilder.append("otpauth://totp/")
                .append(Uri.encode(label))
                .append("?secret=").append(secretKey)
                .append("&issuer=").append(Uri.encode(issuer))
                .append("&algorithm=").append(algorithm)
                .append("&digits=").append(digits);

        try {
            BitMatrix bitMatrix = new QRCodeWriter().encode(qrBuilder.toString(), BarcodeFormat.QR_CODE, 512, 512);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            qrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    qrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);

        } catch (WriterException e) {
            Toast.makeText(getContext(), R.string.failed_to_generate_qr_code, Toast.LENGTH_SHORT).show();
        }
    }
    private void saveQRCode(String path) throws IOException {
        if (qrBitmap == null)
            return;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "QR_" + service.getServiceName() + "_" + timeStamp + ".png";

        ContentResolver resolver = requireContext().getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, path);

        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            OutputStream os = resolver.openOutputStream(uri);
            if (os != null) {
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.close();
            }
        }
    }
}