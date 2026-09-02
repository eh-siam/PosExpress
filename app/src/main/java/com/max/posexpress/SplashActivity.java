package com.max.posexpress;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.max.posexpress.ui.CountrySelectionActivity;
import com.max.posexpress.ui.LoginActivity;
import com.max.posexpress.util.AppPreferences;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        startAnimations();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            AppPreferences prefs = new AppPreferences(this);
            Intent intent;

            if (auth.getCurrentUser() == null) {
                // Not logged in
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            } else if (prefs.getCountryCode() != null) {
                // Logged in + Configured
                intent = new Intent(SplashActivity.this, HostActivity.class);
            } else {
                // Logged in + Needs Setup
                intent = new Intent(SplashActivity.this, CountrySelectionActivity.class);
            }
            startActivity(intent);
            finish();
        }, 3000); // Increased to 3 seconds for better UX with animations
    }

    private void startAnimations() {
        android.view.View logo = findViewById(R.id.cvLogoSplash);
        android.view.View name = findViewById(R.id.tvAppNameSplash);
        android.view.View tagline = findViewById(R.id.tvTaglineSplash);
        android.view.View footer = findViewById(R.id.tvFooterSplash);

        // Logo: Pop in with overshoot
        logo.setScaleX(0.5f);
        logo.setScaleY(0.5f);
        logo.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(800)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .start();

        // App Name: Slide up and fade
        name.setTranslationY(50f);
        name.animate()
                .translationY(0f)
                .alpha(1.0f)
                .setDuration(600)
                .setStartDelay(400)
                .start();

        // Tagline: Slide up and fade
        tagline.setTranslationY(30f);
        tagline.animate()
                .translationY(0f)
                .alpha(1.0f)
                .setDuration(600)
                .setStartDelay(600)
                .start();

        // Footer: Fade in
        footer.animate()
                .alpha(1.0f)
                .setDuration(1000)
                .setStartDelay(1000)
                .start();
    }
}
