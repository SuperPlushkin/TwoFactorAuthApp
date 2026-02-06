package ru.superplushkin.twofactorauthapp.activity;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import ru.superplushkin.twofactorauthapp.R;
import ru.superplushkin.twofactorauthapp.subclasses.LocaleHelper;
import ru.superplushkin.twofactorauthapp.subclasses.ThemeHelper;

public class LeftSlideDialogFragment extends DialogFragment {

    private MaterialButtonToggleGroup languageToggleGroup;
    private MaterialAutoCompleteTextView themeSpinner;
    private ArrayAdapter<String> themeAdapter;

    private String[] themeCodesArray;

    private OnSettingsAppliedListener listener;

    private String previousLanguage;
    private String previousTheme;

    public interface OnSettingsAppliedListener {
        void onSettingsApplied(boolean needRestart);
    }

    public void setOnSettingsAppliedListener(OnSettingsAppliedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.LeftSlideDialogStyle);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null)
            getDialog().getWindow().setGravity(Gravity.START);

        return inflater.inflate(R.layout.menu_settings_drawer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        var settingsContainer = view.findViewById(R.id.settings_container);
        ViewGroup.LayoutParams params = settingsContainer.getLayoutParams();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.75);
        settingsContainer.setLayoutParams(params);

        themeCodesArray = getResources().getStringArray(R.array.themes_codes_array);

        previousLanguage = LocaleHelper.getCurrentLanguage(requireContext());
        previousTheme = ThemeHelper.getSavedTheme(requireContext());

        languageToggleGroup = view.findViewById(R.id.languagesContainer);
        themeSpinner = view.findViewById(R.id.themeSpinner);

        setupLanguageButtons();
        setupThemeToggleGroup();

        setupListeners();
        view.findViewById(R.id.background_overlay).setOnClickListener(v -> dismiss());
    }

    private void selectSpinnerText(String theme) {
        int position = -1;
        for (int i = 0; i < themeCodesArray.length; i++) {
            if (themeCodesArray[i].equals(theme)){
                position = i;
                break;
            }
        }

        if (themeAdapter != null && position > -1 && themeAdapter.getCount() > position)
            themeSpinner.setText(themeAdapter.getItem(position), false);
    }
    private void selectLanguageButton(String languageCode) {
        for (int i = 0; i < languageToggleGroup.getChildCount(); i++) {
            View child = languageToggleGroup.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                if (languageCode.equals(button.getTag())) {
                    languageToggleGroup.check(button.getId());
                    break;
                }
            }
        }
    }

    private void setupLanguageButtons() {
        String[] languages = getResources().getStringArray(R.array.languages_name_array);
        String[] languageCodes = getResources().getStringArray(R.array.language_codes_array);

        LayoutInflater inflater = getLayoutInflater();
        languageToggleGroup.removeAllViews();

        for (int i = 0; i < languages.length; i++) {
            var button = (MaterialButton) inflater.inflate(R.layout.language_button, languageToggleGroup, false);
            button.setId(View.generateViewId());
            button.setText(languages[i]);
            button.setTag(languageCodes[i]);

            var params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 8, 0);
            button.setLayoutParams(params);

            languageToggleGroup.addView(button);
        }

        selectLanguageButton(previousLanguage);
    }
    private void setupThemeToggleGroup() {
        String[] themesArray = getResources().getStringArray(R.array.themes_name_array);
        themeAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_dropdown, themesArray);
        themeSpinner.setAdapter(themeAdapter);
        themeSpinner.setDropDownVerticalOffset(4);
        themeSpinner.setDropDownBackgroundResource(R.drawable.bg_dropdown);

        selectSpinnerText(previousTheme);
    }

    private void setupListeners() {
        languageToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                MaterialButton checkedButton = group.findViewById(checkedId);
                String newLanguage = (String)checkedButton.getTag();

                if (!previousLanguage.equals(newLanguage))
                    showRestartConfirmationDialogLocale(newLanguage);
            }
        });

        themeSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String newTheme = themeCodesArray[position];

            if (!previousTheme.equals(newTheme))
                showRestartConfirmationDialogTheme(newTheme);
        });
    }

    private void showRestartConfirmationDialogLocale(String newValue) {
        FragmentActivity activity = getActivity();
        if (activity == null)
            return;

        new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle(R.string.settings_restart_required)
                .setMessage(R.string.settings_restart_message)
                .setPositiveButton(R.string.ok_button, (dialog, which) -> {
                    LocaleHelper.setCurrentLanguage(requireContext(), newValue);
                    previousLanguage = newValue;

                    if (listener != null)
                        listener.onSettingsApplied(true);
                })
                .setNegativeButton(R.string.later_button, (dialog, which) -> selectLanguageButton(previousLanguage))
                .setOnCancelListener(dialog -> selectLanguageButton(previousLanguage))
                .show();
    }
    private void showRestartConfirmationDialogTheme(String newValue) {
        FragmentActivity activity = getActivity();
        if (activity == null)
            return;

        selectSpinnerText(newValue);

        new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle(R.string.settings_restart_required)
                .setMessage(R.string.settings_restart_message)
                .setPositiveButton(R.string.ok_button, (dialog, which) -> {
                    ThemeHelper.saveTheme(requireContext(), newValue);
                    previousTheme = newValue;

                    if (listener != null)
                        listener.onSettingsApplied(true);
                })
                .setNegativeButton(R.string.later_button, (dialog, which) -> selectSpinnerText(previousTheme))
                .setOnCancelListener(dialog -> selectSpinnerText(previousTheme))
                .show();
    }

    @Override
    public void onDestroyView() {
//        if (themeSpinner != null && themeSpinner.getAdapter() != null)
//            themeSpinner.setAdapter(null);

//        if (languageToggleGroup != null) {
//            languageToggleGroup.clearOnButtonCheckedListeners();
//            languageToggleGroup.removeAllViews();
//        }
//
//        languageToggleGroup = null;
//        themeSpinner = null;

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
//        listener = null;

        super.onDestroy();
    }
}