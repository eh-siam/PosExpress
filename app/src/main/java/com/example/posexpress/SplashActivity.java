package com.example.posexpress;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

import com.example.posexpress.ui.CountrySelectionActivity;
import com.example.posexpress.util.AppPreferences;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AppPreferences prefs = new AppPreferences(this);
            Intent intent;
            if (prefs.getCountryCode() != null) {
                intent = new Intent(SplashActivity.this, HostActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, CountrySelectionActivity.class);
            }
            startActivity(intent);
            finish();
        }, 2000);
    }
}
