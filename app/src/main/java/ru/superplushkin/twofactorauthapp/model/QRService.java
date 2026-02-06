package ru.superplushkin.twofactorauthapp.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class QRService implements Parcelable {
    private final String serviceName;
    private final String secretKey;
    private final String account;
    private final String issuer;
    private final String algorithm;
    private final short digits;

    public QRService(String serviceName, String secretKey, String account, String issuer, String algorithm, short digits){
        this.serviceName = serviceName;
        this.secretKey = secretKey;
        this.account = account;
        this.issuer = issuer;
        this.algorithm = algorithm;
        this.digits = digits;
    }
    protected QRService(Parcel in) {
        serviceName = in.readString();
        secretKey = in.readString();
        account = in.readString();
        issuer = in.readString();
        algorithm = in.readString();
        digits = (short) in.readInt();
    }

    public String getServiceName() {
        return serviceName;
    }
    public String getSecretKey() {
        return secretKey;
    }
    public String getAccount() {
        return account;
    }
    public String getIssuer() {
        return issuer;
    }
    public String getAlgorithm() {
        return algorithm;
    }
    public short getDigits() {
        return digits;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<QRService> CREATOR = new Parcelable.Creator<>() {
        @Override
        public QRService createFromParcel(Parcel in) {
            return new QRService(in);
        }

        @Override
        public QRService[] newArray(int size) {
            return new QRService[size];
        }
    };

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(serviceName);
        dest.writeString(secretKey);
        dest.writeString(account);
        dest.writeString(issuer);
        dest.writeString(algorithm);
        dest.writeInt(digits);
    }
}
