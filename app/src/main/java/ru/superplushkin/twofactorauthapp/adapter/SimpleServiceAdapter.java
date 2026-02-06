package ru.superplushkin.twofactorauthapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.core.content.ContextCompat;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import ru.superplushkin.twofactorauthapp.R;
import ru.superplushkin.twofactorauthapp.activity.MainActivity;
import ru.superplushkin.twofactorauthapp.model.SimpleService;

import java.util.Collections;
import java.util.List;

public class SimpleServiceAdapter extends RecyclerView.Adapter<SimpleServiceAdapter.ViewHolder> {
    private final List<SimpleService> services;
    private final MainActivity context;

    public interface OnSortTypeChangeListener {
        void onSortTypeChangedToCustom();
        void onItemOrderChanged(List<SimpleService> newOrder);
    }

    private OnSortTypeChangeListener sortTypeChangeListener;

    public void setOnSortTypeChangeListener(OnSortTypeChangeListener listener) {
        this.sortTypeChangeListener = listener;
    }

    public SimpleServiceAdapter(MainActivity context, List<SimpleService> services) {
        this.context = context;
        this.services = services;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service_simple, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        SimpleService service = services.get(position);
        holder.bind(service);
    }

    @Override
    public int getItemCount() {
        return services.size();
    }

    public void updateFavoriteStatus(int position, boolean isFavorite) {
        if (position >= 0 && position < services.size()) {
            SimpleService service = services.get(position);
            service.setFavorite(isFavorite);
            notifyItemChanged(position);
        }
    }

    public void removeService(long id) {
        int position = -1;
        for (int i = 0; i < services.size(); i++) {
            if (services.get(i).getId() == id) {
                position = i;
                break;
            }
        }

        if (position >= 0 && position < services.size()) {
            services.remove(position);
            notifyItemRemoved(position);

            if (position < services.size())
                notifyItemRangeChanged(position, services.size() - position);
        }
    }
    public void restoreService(SimpleService service) {
        if (service != null) {
            int insertPosition = services.size();
            services.add(service);
            notifyItemInserted(insertPosition);
        }
    }

    public List<SimpleService> getServices() {
        return services;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvServiceName, tvAccount;
        ImageView ivServiceIcon, ivFavorite;
        View favoriteContainer, serviceContainer;
        SimpleService currentService;

        public ViewHolder(View itemView) {
            super(itemView);
            tvServiceName = itemView.findViewById(R.id.tvServiceName);
            tvAccount = itemView.findViewById(R.id.tvAccount);
            ivServiceIcon = itemView.findViewById(R.id.ivServiceIcon);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            favoriteContainer = itemView.findViewById(R.id.favoriteContainer);
            serviceContainer = itemView.findViewById(R.id.serviceContainer);
        }

        public void bind(SimpleService service) {
            currentService = service;

            tvServiceName.setText(service.getServiceName());

            String accountOrIssuer = service.getAccount();
            if (accountOrIssuer == null || accountOrIssuer.isEmpty())
                accountOrIssuer = "";

            tvAccount.setText(accountOrIssuer);
            tvAccount.setVisibility(!accountOrIssuer.isEmpty() ? View.VISIBLE : View.GONE);

            updateFavoriteIcon(service.isFavorite());
            favoriteContainer.setOnClickListener(v -> {
                int position = getAbsoluteAdapterPosition();
                if (position != RecyclerView.NO_POSITION && currentService != null) {
                    context.toggleFavorite(currentService.getId(), position);
                    updateFavoriteIcon(!currentService.isFavorite());
                }
            });

            serviceContainer.setOnClickListener(v -> {
                if (currentService != null)
                    context.navigateToServiceDetails(currentService.getId());
            });
        }

        private void updateFavoriteIcon(boolean isFavorite) {
            if (isFavorite) {
                ivFavorite.setImageResource(R.drawable.ic_star);
                ivFavorite.setColorFilter(ContextCompat.getColor(context, R.color.favorite_star_background));
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
                ivFavorite.setColorFilter(ContextCompat.getColor(context, R.color.not_favorite_star_background));
            }
        }
    }

    public static class ItemTouchHelperCallback extends ItemTouchHelper.Callback {
        private final SimpleServiceAdapter adapter;
        private boolean isDragging = false;

        public ItemTouchHelperCallback(SimpleServiceAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {

            if (!isDragging && adapter.sortTypeChangeListener != null)
                adapter.sortTypeChangeListener.onSortTypeChangedToCustom();

            isDragging = true;

            int fromPosition = source.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            if (fromPosition >= 0 && toPosition >= 0 && fromPosition != toPosition) {
                adapter.onItemMove(fromPosition, toPosition);
                return true;
            }
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null)
                viewHolder.itemView.setAlpha(0.8f);
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);

            if (isDragging && adapter.sortTypeChangeListener != null)
                adapter.sortTypeChangeListener.onItemOrderChanged(adapter.services);

            isDragging = false;

            viewHolder.itemView.setAlpha(1.0f);
        }
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < 0 || fromPosition >= services.size() || toPosition < 0 || toPosition >= services.size())
            return;

        Collections.swap(services, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }
}