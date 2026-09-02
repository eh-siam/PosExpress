package com.max.posexpress.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.max.posexpress.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etBusinessName, etEmail, etPassword, etConfirmPassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();

        etBusinessName = findViewById(R.id.etBusinessName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnSignup).setOnClickListener(v -> signupUser());
        findViewById(R.id.tvGoToLogin).setOnClickListener(v -> finish());

        startEntryAnimations();
    }

    private void startEntryAnimations() {
        View cvLogo = findViewById(R.id.cvLogo);
        View tvTitle = findViewById(R.id.tvTitle);
        View tvSubtitle = findViewById(R.id.tvSubtitle);
        View cardSignupForm = findViewById(R.id.cardSignupForm);
        View tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // Logo Animation: Scale and Fade
        cvLogo.setScaleX(0.7f);
        cvLogo.setScaleY(0.7f);
        cvLogo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(600).setInterpolator(new android.view.animation.OvershootInterpolator()).start();

        // Title Text: Slide from Left and Fade
        tvTitle.setTranslationX(-100f);
        tvTitle.animate().translationX(0f).alpha(1f).setDuration(500).setStartDelay(300).start();

        // Subtitle Text: Slide from Left and Fade (with delay)
        tvSubtitle.setTranslationX(-100f);
        tvSubtitle.animate().translationX(0f).alpha(1f).setDuration(500).setStartDelay(400).start();

        // Signup Card: Slide from Bottom and Fade
        cardSignupForm.setTranslationY(200f);
        cardSignupForm.animate().translationY(0f).alpha(1f).setDuration(700).setStartDelay(500).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();

        // Bottom Link: Simple Fade In
        tvGoToLogin.animate().alpha(1f).setDuration(500).setStartDelay(1000).start();
    }

    private void signupUser() {
        String business = etBusinessName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (business.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, R.string.msg_fill_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, R.string.msg_pass_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Optionally save business name to database/profile here
                        startActivity(new Intent(SignupActivity.this, CountrySelectionActivity.class));
                        finishAffinity();
                    } else {
                        Toast.makeText(SignupActivity.this, R.string.msg_auth_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
