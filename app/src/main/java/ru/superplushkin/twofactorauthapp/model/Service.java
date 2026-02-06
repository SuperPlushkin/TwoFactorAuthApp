package ru.superplushkin.twofactorauthapp.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

public class Service extends SimpleService implements Parcelable {
    private String secretKey;
    private String issuer;
    private String code;
    private String algorithm;
    private short digits;
    private int usageCount;

    public Service(long id, String serviceName, String secretKey, String account, String issuer, String algorithm, short digits,
                   String createdAt, int usageCount, boolean isFavorite, int sortOrder) {
        super(id, serviceName, account, createdAt, isFavorite, sortOrder);
        this.secretKey = secretKey;
        this.issuer = issuer;
        this.algorithm = algorithm;
        this.digits = digits;
        this.usageCount = usageCount;
    }
    protected Service(Parcel in) {
        super(in);
        issuer = in.readString();
        secretKey = in.readString();
        code = in.readString();
        algorithm = in.readString();
        digits = (short) in.readInt();
        usageCount = in.readInt();
    }

    public static final Creator<Service> CREATOR = new Creator<>() {
        @Override
        public Service createFromParcel(Parcel in) {
            return new Service(in);
        }

        @Override
        public Service[] newArray(int size) {
            return new Service[size];
        }
    };

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(issuer);
        dest.writeString(secretKey);
        dest.writeString(code);
        dest.writeString(algorithm);
        dest.writeInt(digits);
        dest.writeInt(usageCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getSecretKey() { return secretKey; }
    public String getIssuer() { return issuer; }
    public String getAlgorithm() { return algorithm; }
    public short getDigits() { return digits; }
    public int getUsageCount() { return usageCount; }

    public void incrementUsageCount() {
        this.usageCount++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Service service = (Service) o;
        return usageCount == service.usageCount &&
                Objects.equals(secretKey, service.secretKey) &&
                Objects.equals(issuer, service.issuer) &&
                Objects.equals(code, service.code) &&
                Objects.equals(algorithm, service.algorithm) &&
                digits == service.digits;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), secretKey, issuer, code, usageCount, algorithm, digits);
    }

    @NonNull
    @Override
    public String toString() {
        return "Service{" +
                "id=" + id +
                ", serviceName='" + serviceName + '\'' +
                ", account='" + account + '\'' +
                ", issuer='" + issuer + '\'' +
                ", usageCount=" + usageCount +
                ", isFavorite=" + isFavorite +
                ", createdAt='" + createdAt + '\'' +
                ", sortOrder=" + sortOrder +
                '}';
    }
}