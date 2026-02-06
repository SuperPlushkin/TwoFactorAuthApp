package ru.superplushkin.twofactorauthapp.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class SimpleService implements Parcelable {

    protected long id;
    protected String serviceName;
    protected String account;
    protected boolean isFavorite;
    protected String createdAt;
    protected int sortOrder;

    public SimpleService(long id, String serviceName, String account, String createdAt, boolean isFavorite, int sortOrder) {
        this.id = id;
        this.serviceName = serviceName;
        this.account = account;
        this.createdAt = createdAt;
        this.isFavorite = isFavorite;
        this.sortOrder = sortOrder;
    }
    protected SimpleService(Parcel in) {
        id = in.readLong();
        serviceName = in.readString();
        account = in.readString();
        isFavorite = in.readByte() != 0;
        createdAt = in.readString();
        sortOrder = in.readInt();
    }

    public static final Creator<SimpleService> CREATOR = new Creator<>() {
        @Override
        public SimpleService createFromParcel(Parcel in) {
            return new SimpleService(in);
        }

        @Override
        public SimpleService[] newArray(int size) {
            return new SimpleService[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(serviceName);
        dest.writeString(account);
        dest.writeByte((byte) (isFavorite ? 1 : 0));
        dest.writeString(createdAt);
        dest.writeInt(sortOrder);
    }

    public long getId() { return id; }
    public String getServiceName() { return serviceName; }
    public String getDisplayName() {
        return serviceName != null && !serviceName.isEmpty() ? serviceName : "Unknown Service";
    }
    public String getAccount() { return account; }
    public String getCreatedAt() { return createdAt; }
    public boolean isFavorite() { return isFavorite; }
    public int getSortOrder() { return sortOrder; }

    public void setId(long id) { this.id = id; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    @NonNull
    @Override
    public String toString() {
        return "SimpleService{" +
                "id=" + id +
                ", serviceName='" + serviceName + '\'' +
                ", account='" + account + '\'' +
                ", isFavorite=" + isFavorite +
                ", createdAt='" + createdAt + '\'' +
                ", sortOrder=" + sortOrder +
                '}';
    }
}