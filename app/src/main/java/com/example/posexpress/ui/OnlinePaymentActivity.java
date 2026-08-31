package com.example.posexpress.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.posexpress.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class OnlinePaymentActivity extends AppCompatActivity {

    private double amount;
    private String currencySymbol;
    private TextInputEditText etCardNumber, etExpiry, etCvv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_payment);

        amount = getIntent().getDoubleExtra("amount", 0.0);
        currencySymbol = getIntent().getStringExtra("currency_symbol");
        if (currencySymbol == null) currencySymbol = "$";

        setupUI();
    }

    private void setupUI() {
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        TextView tvAmount = findViewById(R.id.tvPaymentAmount);
        TextView tvInvoice = findViewById(R.id.tvInvoiceId);
        
        tvAmount.setText(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, amount));
        tvInvoice.setText("Invoice: INV-" + System.currentTimeMillis() / 1000);

        etCardNumber = findViewById(R.id.etCardNumber);
        etExpiry = findViewById(R.id.etCardExpiry);
        etCvv = findViewById(R.id.etCardCvv);

        // Card Number Formatting (4-4-4-4)
        etCardNumber.addTextChangedListener(new TextWatcher() {
            private boolean isDeleting = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                isDeleting = count > after;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isDeleting) return;
                int len = s.length();
                if (len > 0 && len % 5 == 0 && s.charAt(len - 1) != ' ') {
                    s.insert(len - 1, " ");
                }
            }
        });

        // Expiry Formatting (MM/YY)
        etExpiry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 2 && !s.toString().contains("/")) {
                    s.append("/");
                }
            }
        });

        findViewById(R.id.btnSecurePay).setOnClickListener(v -> validateAndPay());
    }

    private void validateAndPay() {
        String card = etCardNumber.getText().toString().replace(" ", "");
        String expiry = etExpiry.getText().toString();
        String cvv = etCvv.getText().toString();

        if (card.length() < 16) {
            Toast.makeText(this, "Enter valid card number", Toast.LENGTH_SHORT).show();
            return;
        }
        if (expiry.length() < 5) {
            Toast.makeText(this, "Enter valid expiry date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cvv.length() < 3) {
            Toast.makeText(this, "Enter valid CVV", Toast.LENGTH_SHORT).show();
            return;
        }

        showOtpDialog();
    }

    private void showOtpDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_otp_verification, null);
        TextInputEditText etOtp = dialogView.findViewById(R.id.etOtp);

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String otp = etOtp.getText().toString();
                    if (otp.length() == 6) {
                        finalizeTransaction();
                    } else {
                        Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                        showOtpDialog(); // Show again if invalid
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void finalizeTransaction() {
        try {
            JSONObject result = new JSONObject();
            result.put("status", "SUCCESS");
            result.put("method", "Online Card Payment");
            result.put("card_type", getCardType(etCardNumber.getText().toString()));
            result.put("masked_card", "**** **** **** " + etCardNumber.getText().toString().substring(etCardNumber.length() - 4));
            result.put("auth_code", "AUTH_" + (int)(Math.random() * 900000 + 100000));
            result.put("gateway", "PaySwift Secure");

            Intent data = new Intent();
            data.putExtra("payment_result", result.toString());
            setResult(Activity.RESULT_OK, data);
            finish();
        } catch (JSONException e) {
            e.printStackTrace();
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    private String getCardType(String number) {
        if (number.startsWith("4")) return "Visa";
        if (number.startsWith("5")) return "Mastercard";
        return "Credit Card";
    }
}
