package ru.superplushkin.twofactorauthapp.subclasses;

import ru.superplushkin.twofactorauthapp.model.SimpleService;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServiceDiffCallback extends DiffUtil.Callback {
    private final List<SimpleService> oldList;
    private final List<SimpleService> newList;

    public ServiceDiffCallback(List<SimpleService> oldList, List<SimpleService> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        SimpleService oldItem = oldList.get(oldItemPosition);
        SimpleService newItem = newList.get(newItemPosition);

        return Objects.equals(oldItem.getServiceName(), newItem.getServiceName()) &&
                Objects.equals(oldItem.getAccount(), newItem.getAccount()) &&
                oldItem.isFavorite() == newItem.isFavorite();
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        SimpleService oldItem = oldList.get(oldItemPosition);
        SimpleService newItem = newList.get(newItemPosition);

        List<Object> payload = new ArrayList<>();

        if (!Objects.equals(oldItem.getServiceName(), newItem.getServiceName()))
            payload.add("name_changed");

        if (!Objects.equals(oldItem.getAccount(), newItem.getAccount()))
            payload.add("account_changed");

        if (oldItem.isFavorite() != newItem.isFavorite())
            payload.add("favorite_changed");

        return payload.isEmpty() ? null : payload;
    }
}
