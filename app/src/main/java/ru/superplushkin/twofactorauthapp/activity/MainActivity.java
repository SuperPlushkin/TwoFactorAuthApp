package ru.superplushkin.twofactorauthapp.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import ru.superplushkin.twofactorauthapp.db.DatabaseHelper;
import ru.superplushkin.twofactorauthapp.R;
import ru.superplushkin.twofactorauthapp.model.SimpleService;
import ru.superplushkin.twofactorauthapp.subclasses.LocaleHelper;
import ru.superplushkin.twofactorauthapp.subclasses.ServiceDiffCallback;
import ru.superplushkin.twofactorauthapp.adapter.SimpleServiceAdapter;
import ru.superplushkin.twofactorauthapp.model.Service;

public class MainActivity extends MyActivity {

    private DatabaseHelper dbHelper;

    private RecyclerView recyclerView;
    private MenuItem favoritesFirstItem;
    private TextView fabHint;
    private View emptyStateView;
    private FloatingActionButton fab;
    
    private List<SimpleService> serviceList;
    private SimpleServiceAdapter adapter;
    
    private Integer currentSortType = 0;
    private boolean favoritesFirst = true;
    private boolean isFabHintVisible = false;

    private ActivityResultLauncher<Intent> addServiceLauncher;
    private ActivityResultLauncher<Intent> serviceDetailLauncher;


    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initViews();

        adapter = new SimpleServiceAdapter(this, serviceList);
        adapter.setOnSortTypeChangeListener(new SimpleServiceAdapter.OnSortTypeChangeListener(){
            @Override
            public void onSortTypeChangedToCustom() {
                if (currentSortType == SortType.CUSTOM.toInt())
                    return;

                currentSortType = SortType.CUSTOM.toInt();
                saveSortPreferences();

                updateFavouritesFirstMenuItem();

                Toast.makeText(MainActivity.this, R.string.switched_to_your_sort_order, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemOrderChanged(List<SimpleService> newOrder) {
                saveCustomOrderToDatabase(newOrder);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        var callback = new SimpleServiceAdapter.ItemTouchHelperCallback(adapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        fab.setOnClickListener(view -> navigateToAddService());
        fab.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    updateFabHint();
                    return false;
            }
            return false;
        });

        setupResultLaunchers();
        getSortPreferences();
        loadServices();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        fab = findViewById(R.id.fab);
        fabHint = findViewById(R.id.fabHint);
        emptyStateView = findViewById(R.id.emptyStateView);

        dbHelper = new DatabaseHelper(this);
        serviceList = new ArrayList<>();
    }
    private void setupResultLaunchers() {
        addServiceLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK)
                    loadServices();
            }
        );

        serviceDetailLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK)
                    return;

                Intent data = result.getData();
                if (data != null){
                    long id = data.getLongExtra("service_id", -1);
                    if (data.getBooleanExtra("serviceNeedToDelete", false) && id != -1){
                        Service serviceToDelete = dbHelper.getService(id);
                        deleteService(serviceToDelete);
                    }
                    else loadServices();
                }
            }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        favoritesFirstItem = menu.findItem(R.id.action_favorites_first);

        updateFavouritesFirstMenuItem();

        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        var settingsDialog = new LeftSlideDialogFragment();
        settingsDialog.setOnSettingsAppliedListener((needRestart) -> {
            if (needRestart)
                new Handler(Looper.getMainLooper()).postDelayed(this::restartApplication, 200);
        });
        settingsDialog.show(getSupportFragmentManager(), "settings_dialog");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_sort) {
            showSortDialog();
            return true;
        } else if (id == R.id.action_favorites_first) {
            if (currentSortType != SortType.CUSTOM.toInt()) {
                favoritesFirst = !item.isChecked();
                item.setChecked(favoritesFirst);
                saveSortPreferences();
                loadServices();
            }
            return true;
        } else if (id == R.id.action_reset_order) {
            showResetCustomOrder();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSortDialog() {
        String[] sortOptions = {
            getString(R.string.by_default),
            getString(R.string.alphabetical_a_z),
            getString(R.string.alphabetical_z_a),
            getString(R.string.date_added_old_first),
            getString(R.string.date_added_new_first),
            getString(R.string.custom_order)
        };

        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.sort)
                .setSingleChoiceItems(sortOptions, currentSortType, (dialog, which) -> {
                    currentSortType = which;
                    saveSortPreferences();
                    updateFavouritesFirstMenuItem();
                    loadServices();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }
    private void showResetCustomOrder() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_order)
                .setMessage(R.string.reset_custom_sort_order)
                .setPositiveButton(R.string.reset_button, (dialog, which) -> {
                    new Thread(() -> {
                        List<Service> services = dbHelper.getAllServices();

                        services.sort((s1, s2) -> {
                            if (s1.isFavorite() != s2.isFavorite())
                                return s1.isFavorite() ? -1 : 1;

                            String name1 = s1.getDisplayName();
                            String name2 = s2.getDisplayName();
                            return name1.compareToIgnoreCase(name2);
                        });

                        for (int i = 0; i < services.size(); i++)
                            dbHelper.updateServiceOrder(services.get(i).getId(), i);

                        runOnUiThread(() -> {
                            currentSortType = SortType.DEFAULT.toInt();
                            saveSortPreferences();
                            loadServices();
                            Toast.makeText(this, R.string.sort_order_reseted, Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }

    private void updateFavouritesFirstMenuItem() {
        if (favoritesFirstItem != null){
            favoritesFirstItem.setVisible(currentSortType != SortType.CUSTOM.toInt());
            favoritesFirstItem.setChecked(favoritesFirst);
        }
    }
    private void updateFabHint() {
        isFabHintVisible = !isFabHintVisible;

        if (!isFabHintVisible) {
            fabHint.animate().scaleX(0.1f).scaleY(0.1f)
                    .setDuration(150)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        fabHint.setVisibility(View.INVISIBLE);
                        fabHint.setScaleX(1f);
                        fabHint.setScaleY(1f);
                    }).start();
        }
        else {
            fabHint.setVisibility(View.VISIBLE);
            fabHint.setAlpha(1f);
            fabHint.setScaleX(0.1f);
            fabHint.setScaleY(0.1f);
            fabHint.animate().scaleX(1f).scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(new OvershootInterpolator(0.8f))
                    .start();
        }
    }

    private void loadServices() {
        List<SimpleService> newServices = sortServices(dbHelper.getAllSimpleServices());
        List<SimpleService> oldServices = new ArrayList<>(serviceList);

        if (favoritesFirst && currentSortType != SortType.CUSTOM.toInt())
            newServices = sortFavoritesFirstServices(newServices);

        var diffCallback = new ServiceDiffCallback(oldServices, newServices);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        serviceList.clear();
        serviceList.addAll(newServices);

        diffResult.dispatchUpdatesTo(adapter);

        checkNoServices();
    }
    private void checkNoServices() {
        if (emptyStateView != null) {
            if (serviceList.isEmpty()) {
                emptyStateView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);

                Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
                emptyStateView.startAnimation(fadeIn);
            } else {
                emptyStateView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void getSortPreferences() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        currentSortType = prefs.getInt("current_sort_type", 0);
        favoritesFirst = prefs.getBoolean("favorites_first", true);
    }
    private void saveSortPreferences() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("current_sort_type", currentSortType);
        editor.putBoolean("favorites_first", favoritesFirst);
        editor.apply();
    }
    private void saveCustomOrderToDatabase(List<SimpleService> services) {
        new Thread(() -> {
            for (int i = 0; i < services.size(); i++) {
                SimpleService service = services.get(i);
                dbHelper.updateServiceOrder(service.getId(), i);
                service.setSortOrder(i);
            }
        }).start();
    }

    private List<SimpleService> sortServices(List<SimpleService> services) {
        if (currentSortType == SortType.NAME_ASC.toInt() || currentSortType == SortType.DEFAULT.toInt() ) {
            services.sort((s1, s2) -> {
                String name1 = s1.getDisplayName();
                String name2 = s2.getDisplayName();
                return name1.compareToIgnoreCase(name2);
            });
        }
        else if (currentSortType == SortType.NAME_DESC.toInt() ) {
            services.sort((s1, s2) -> {
                String name1 = s1.getDisplayName();
                String name2 = s2.getDisplayName();
                return name2.compareToIgnoreCase(name1);
            });
        }
        else if (currentSortType == SortType.DATE_ASC.toInt() ) {
            services.sort((s1, s2) -> {
                String date1 = s1.getCreatedAt();
                String date2 = s2.getCreatedAt();
                return date1.compareTo(date2);
            });
        }
        else if (currentSortType == SortType.DATE_DESC.toInt() ) {
            services.sort((s1, s2) -> {
                String date1 = s1.getCreatedAt();
                String date2 = s2.getCreatedAt();
                return date2.compareTo(date1);
            });
        }
        else if (currentSortType == SortType.CUSTOM.toInt() )
            services.sort(Comparator.comparingInt(SimpleService::getSortOrder));

        return services;
    }
    private List<SimpleService> sortFavoritesFirstServices(List<SimpleService> services) {
        return services.stream().sorted((s1, s2) -> Boolean.compare(!s1.isFavorite(), !s2.isFavorite())).collect(Collectors.toList());
    }

    public void navigateToServiceDetails(long serviceId) {
        Intent intent = new Intent(MainActivity.this, ServiceDetailActivity.class);
        intent.putExtra("SERVICE_ID", serviceId);
        serviceDetailLauncher.launch(intent);
    }
    private void navigateToAddService() {
        Intent intent = new Intent(MainActivity.this, ServiceAddActivity.class);
        addServiceLauncher.launch(intent);
    }

    public void toggleFavorite(long serviceId, int position) {
        boolean newState = !adapter.getServices().get(position).isFavorite();

        adapter.updateFavoriteStatus(position, newState);
        new Thread(() -> dbHelper.toggleFavorite(serviceId)).start();

        Snackbar.make(fab, newState ? R.string.added_to_favorites : R.string.removed_from_favorites, Snackbar.LENGTH_SHORT).show();
    }

    private enum SortType {
        DEFAULT,
        NAME_ASC,
        NAME_DESC,
        DATE_ASC,
        DATE_DESC,
        CUSTOM;

        private static final EnumMap<SortType, Integer> sortTypeReverse = new EnumMap<>(SortType.class) {{
            put(SortType.DEFAULT, 0);
            put(SortType.NAME_ASC, 1);
            put(SortType.NAME_DESC, 2);
            put(SortType.DATE_ASC, 3);
            put(SortType.DATE_DESC, 4);
            put(SortType.CUSTOM, 5);
        }};

        public int toInt(){
            Integer num = sortTypeReverse.get(this);
            if(num == null) num = Integer.MIN_VALUE;
            return num;
        }
    }

    public void deleteService(Service serviceToDelete){
        long id = serviceToDelete.getId();

        adapter.removeService(id);

        new Thread(() -> {
            dbHelper.deleteService(id);

            runOnUiThread(() -> {
                checkNoServices();
                showUndoSnackbar(serviceToDelete);
            });
        }).start();
    }
    private void showUndoSnackbar(Service deletedService) {
        Snackbar.make(fab, String.format(getString(R.string.service_deleted), deletedService.getServiceName()), Snackbar.LENGTH_LONG)
                .setAction(R.string.cancel_button, v -> {
                    long newId = dbHelper.restoreService(deletedService);
                    if (newId != -1) {

                        Service serviceRestored = dbHelper.getService(newId);
                        if (serviceRestored != null) {

                            int newPosition = serviceList.size();
                            adapter.restoreService(serviceRestored);

                            loadServices();

                            Toast.makeText(this, R.string.service_restored, Toast.LENGTH_SHORT).show();

                            recyclerView.smoothScrollToPosition(newPosition);
                        }
                    }
                })
                .setActionTextColor(ContextCompat.getColor(this, R.color.attention_color)).show();
    }

    private void restartApplication() {
        String savedLanguage = LocaleHelper.getCurrentLanguage(this);

        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(new Locale(savedLanguage));

        resources.updateConfiguration(config, resources.getDisplayMetrics());

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        android.os.Process.killProcess(android.os.Process.myPid());
    }
}