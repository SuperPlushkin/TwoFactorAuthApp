package ru.superplushkin.twofactorauthapp.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import ru.superplushkin.twofactorauthapp.subclasses.LocaleHelper;
import ru.superplushkin.twofactorauthapp.subclasses.ThemeHelper;

public abstract class MyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        LocaleHelper.updateLocale(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.updateLocale(base));
    }
//
//    private void setLocale() {
//        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
//        String language = prefs.getString("language", "ru");
//        Locale locale = language.equals("en") ? Locale.ENGLISH : new Locale("ru");
//
//        Locale.setDefault(locale);
//        Resources resources = getResources();
//        Configuration config = resources.getConfiguration();
//        config.setLocale(locale);
//        resources.updateConfiguration(config, resources.getDisplayMetrics());
//    }
}