package com.max.posexpress.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.max.posexpress.HostActivity;
import com.max.posexpress.R;
import com.max.posexpress.util.AppPreferences;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnLogin).setOnClickListener(v -> loginUser());
        findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> signInWithGoogle());
        findViewById(R.id.tvGoToSignup).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });

        startEntryAnimations();
    }

    private void startEntryAnimations() {
        View cvLogo = findViewById(R.id.cvLogo);
        View tvWelcome = findViewById(R.id.tvWelcome);
        View tvSubtitle = findViewById(R.id.tvSubtitle);
        View cardLoginForm = findViewById(R.id.cardLoginForm);
        View tvGoToSignup = findViewById(R.id.tvGoToSignup);

        // Logo Animation: Scale and Fade
        cvLogo.setScaleX(0.7f);
        cvLogo.setScaleY(0.7f);
        cvLogo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(600).setInterpolator(new android.view.animation.OvershootInterpolator()).start();

        // Welcome Text: Slide from Left and Fade
        tvWelcome.setTranslationX(-100f);
        tvWelcome.animate().translationX(0f).alpha(1f).setDuration(500).setStartDelay(300).start();

        // Subtitle Text: Slide from Left and Fade (with delay)
        tvSubtitle.setTranslationX(-100f);
        tvSubtitle.animate().translationX(0f).alpha(1f).setDuration(500).setStartDelay(400).start();

        // Login Card: Slide from Bottom and Fade
        cardLoginForm.setTranslationY(200f);
        cardLoginForm.animate().translationY(0f).alpha(1f).setDuration(700).setStartDelay(500).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();

        // Bottom Link: Simple Fade In
        tvGoToSignup.animate().alpha(1f).setDuration(500).setStartDelay(1000).start();
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        progressBar.setVisibility(View.VISIBLE);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        String name = "";
                        if (mAuth.getCurrentUser() != null) {
                            name = mAuth.getCurrentUser().getDisplayName();
                        }
                        showWelcomeDialog(name);
                    } else {
                        Toast.makeText(LoginActivity.this, R.string.msg_auth_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showWelcomeDialog(String name) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.title_welcome_user, name))
                .setMessage(getString(R.string.msg_login_confirm, name))
                .setCancelable(false)
                .setPositiveButton(R.string.btn_continue_pos, (dialog, which) -> navigateNext())
                .show();
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.msg_fill_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        navigateNext();
                    } else {
                        Toast.makeText(LoginActivity.this, R.string.msg_auth_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateNext() {
        AppPreferences prefs = new AppPreferences(this);
        Intent intent;
        if (prefs.getCountryCode() != null) {
            intent = new Intent(LoginActivity.this, HostActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, CountrySelectionActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
