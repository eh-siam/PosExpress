package com.max.posexpress.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.max.posexpress.R;
import com.max.posexpress.model.Product;
import com.max.posexpress.util.CountryConfig;
import com.max.posexpress.viewmodel.PosViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentFragment extends Fragment {

    private PosViewModel viewModel;
    private RelativeLayout loadingOverlay;
    private MaterialButton btnPayNow;
    private View paymentContent;
    private View layoutEmptyState;
    
    private RadioGroup rgOrderType;
    
    private MaterialCardView cardEmv, cardWallet, cardCash;
    private RadioButton rbEmv, rbWallet, rbCash;
    private View indicatorEmv, indicatorWallet, indicatorCash;
    private String selectedMethodName = "EMV Card Payment";
    
    private TextInputEditText etBkashNumberInDialog;
    
    private final ActivityResultLauncher<Intent> qrScannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    String scannedNumber = result.getData().getStringExtra("scanned_number");
                    if (scannedNumber != null && etBkashNumberInDialog != null) {
                        etBkashNumberInDialog.setText(scannedNumber);
                        Toast.makeText(requireContext(), "QR Scanned Successfully", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Intent intent = new Intent(requireContext(), QrScannerActivity.class);
                    qrScannerLauncher.launch(intent);
                } else {
                    Toast.makeText(requireContext(), "Camera permission is required to scan QR", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> onlinePaymentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    String jsonResult = result.getData().getStringExtra("payment_result");
                    if (jsonResult != null) {
                        finalizeWithGatewayResult(jsonResult);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(PosViewModel.class);

        paymentContent = view.findViewById(R.id.paymentContent);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        MaterialButton btnBackToShop = view.findViewById(R.id.btnBackToShop);

        com.google.android.material.appbar.MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        }

        rgOrderType = view.findViewById(R.id.rgOrderType);

        viewModel.getTotalAmount().observe(getViewLifecycleOwner(), total -> {
            if (total <= 0) {
                showEmptyState();
                btnBackToShop.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
            } else {
                TextView tvPayableAmount = view.findViewById(R.id.tvPayableAmount);
                tvPayableAmount.setText(String.format(Locale.getDefault(), "%s%.2f", viewModel.getCurrencySymbol(), total));
            }
        });

        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        btnPayNow = view.findViewById(R.id.btnPayNow);
        
        cardEmv = view.findViewById(R.id.cardEmv);
        cardWallet = view.findViewById(R.id.cardWallet);
        cardCash = view.findViewById(R.id.cardCash);
        
        rbEmv = view.findViewById(R.id.rbEmv);
        rbWallet = view.findViewById(R.id.rbWallet);
        rbCash = view.findViewById(R.id.rbCash);

        indicatorEmv = view.findViewById(R.id.indicatorEmv);
        indicatorWallet = view.findViewById(R.id.indicatorWallet);
        indicatorCash = view.findViewById(R.id.indicatorCash);

        // Multi-country payment visibility
        CountryConfig country = viewModel.getSelectedCountry();
        if (country != null && !country.isSupportsBkash()) {
            cardWallet.setVisibility(View.GONE);
        }

        cardEmv.setOnClickListener(v -> selectMethod(R.id.cardEmv));
        cardWallet.setOnClickListener(v -> selectMethod(R.id.cardWallet));
        cardCash.setOnClickListener(v -> selectMethod(R.id.cardCash));

        btnPayNow.setOnClickListener(v -> showPaymentConfirmation());

        // Handle Back Press during payment
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (loadingOverlay.getVisibility() == View.VISIBLE) {
                    Toast.makeText(requireContext(), "Processing payment, please wait...", Toast.LENGTH_SHORT).show();
                } else {
                    setEnabled(false);
                    requireActivity().onBackPressed();
                }
            }
        });
    }

    private void showPaymentConfirmation() {
        double amount = viewModel.getTotalAmount().getValue() != null ? viewModel.getTotalAmount().getValue() : 0.0;
        String formattedAmount = String.format(Locale.getDefault(), "%s%.2f", viewModel.getCurrencySymbol(), amount);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Payment")
                .setMessage(getString(R.string.msg_confirm_payment) + "\n\nTotal: " + formattedAmount)
                .setPositiveButton("Pay Now", (dialog, which) -> {
                    checkConnectionAndProceed();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkConnectionAndProceed() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (!isConnected && !"Cash Payment".equals(selectedMethodName)) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Offline Warning")
                    .setMessage(R.string.msg_offline_warning)
                    .setPositiveButton("Proceed Anyway", (dialog, which) -> startPaymentFlow())
                    .setNegativeButton("Wait for Internet", null)
                    .show();
        } else {
            startPaymentFlow();
        }
    }

    private void startPaymentFlow() {
        if ("e-Wallet / QR Code".equals(selectedMethodName)) {
            showBkashDialog();
        } else if ("EMV Card Payment".equals(selectedMethodName)) {
            launchOnlineGateway();
        } else {
            processStandardPayment(selectedMethodName, "");
        }
    }

    private void launchOnlineGateway() {
        Intent intent = new Intent(requireContext(), OnlinePaymentActivity.class);
        intent.putExtra("amount", viewModel.getTotalAmount().getValue());
        intent.putExtra("currency_symbol", viewModel.getCurrencySymbol());
        onlinePaymentLauncher.launch(intent);
    }

    private void finalizeWithGatewayResult(String gatewayJson) {
        try {
            JSONObject gatewayObj = new JSONObject(gatewayJson);
            processGatewayPayment("EMV Card Payment", gatewayObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showBkashDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_bkash_payment, null);
        etBkashNumberInDialog = dialogView.findViewById(R.id.etBkashNumber);
        com.google.android.material.textfield.TextInputLayout til = dialogView.findViewById(R.id.tilBkashNumber);
        MaterialButton btnScanQr = dialogView.findViewById(R.id.btnScanQr);

        btnScanQr.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(requireContext(), QrScannerActivity.class);
                qrScannerLauncher.launch(intent);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // Clear error when user starts typing
        etBkashNumberInDialog.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (til != null) til.setError(null);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Confirm", null) // Set null to override later
                .setNegativeButton("Cancel", (d, which) -> etBkashNumberInDialog = null)
                .create();

        dialog.show();

        // Override positive button to prevent closing on validation failure
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String bkashNumber = etBkashNumberInDialog.getText().toString().trim();
            if (bkashNumber.isEmpty()) {
                if (til != null) {
                    til.setError("Please enter or scan bKash number");
                    til.setErrorEnabled(true);
                }
                return;
            }
            
            if (bkashNumber.length() < 11) {
                if (til != null) {
                    til.setError("Invalid number (11 digits required)");
                    til.setErrorEnabled(true);
                }
                return;
            }

            processStandardPayment("bKash Payment", bkashNumber);
            etBkashNumberInDialog = null;
            dialog.dismiss();
        });
    }

    private void showEmptyState() {
        paymentContent.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }

    private void selectMethod(int cardId) {
        resetCard(cardEmv, rbEmv, indicatorEmv);
        resetCard(cardWallet, rbWallet, indicatorWallet);
        resetCard(cardCash, rbCash, indicatorCash);

        if (cardId == R.id.cardEmv) {
            highlightCard(cardEmv, rbEmv, indicatorEmv);
            selectedMethodName = rbEmv.getText().toString();
        } else if (cardId == R.id.cardWallet) {
            highlightCard(cardWallet, rbWallet, indicatorWallet);
            selectedMethodName = rbWallet.getText().toString();
        } else if (cardId == R.id.cardCash) {
            highlightCard(cardCash, rbCash, indicatorCash);
            selectedMethodName = rbCash.getText().toString();
        }
    }

    private void resetCard(MaterialCardView card, RadioButton rb, View indicator) {
        card.setStrokeWidth(convertDpToPx(1));
        card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.surfaceVariant));
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));
        rb.setChecked(false);
        indicator.setVisibility(View.GONE);
    }

    private void highlightCard(MaterialCardView card, RadioButton rb, View indicator) {
        card.setStrokeWidth(convertDpToPx(1));
        card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.successEmerald));
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.successContainerSubtle));
        rb.setChecked(true);
        indicator.setVisibility(View.VISIBLE);
    }
    
    private int convertDpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void processStandardPayment(String method, String bkashNumber) {
        executePayment(method, bkashNumber, null);
    }

    private void processGatewayPayment(String method, JSONObject gatewayResult) {
        executePayment(method, "", gatewayResult);
    }

    private void executePayment(String method, String infoStr, JSONObject gatewayResult) {
        loadingOverlay.setVisibility(View.VISIBLE);
        btnPayNow.setEnabled(false);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                double subtotal = viewModel.getTotalAmount() != null && viewModel.getTotalAmount().getValue() != null ? viewModel.getTotalAmount().getValue() : 0.0;
                double discountPercent = viewModel.getDiscountPercent();
                double taxPercent = viewModel.getTaxPercent();
                
                double discountAmount = subtotal * (discountPercent / 100.0);
                double taxAmount = (subtotal - discountAmount) * (taxPercent / 100.0);
                double totalAmount = subtotal - discountAmount + taxAmount;

                int selectedId = rgOrderType.getCheckedRadioButtonId();
                RadioButton rbOrderType = getView().findViewById(selectedId);
                String orderType = rbOrderType.getText().toString();

                JSONObject response = new JSONObject();
                response.put("status", "SUCCESS");
                String txnId = "TXN_" + System.currentTimeMillis();
                response.put("transaction_id", txnId);
                response.put("amount", totalAmount);
                response.put("subtotal", subtotal);
                response.put("discount_amount", discountAmount);
                response.put("tax_amount", taxAmount);
                response.put("discount_percent", discountPercent);
                response.put("tax_percent", taxPercent);
                response.put("method", method);
                response.put("order_type", orderType);

                if (gatewayResult != null) {
                    response.put("card_type", gatewayResult.optString("card_type"));
                    response.put("masked_card", gatewayResult.optString("masked_card"));
                    response.put("auth_code", gatewayResult.optString("auth_code"));
                    response.put("gateway", gatewayResult.optString("gateway"));
                } else if (!infoStr.isEmpty()) {
                    response.put("bkash_number", infoStr);
                }
                
                JSONArray itemsArray = new JSONArray();
                Map<Integer, Integer> cart = viewModel.getCartQuantities().getValue();
                List<Product> products = viewModel.getProductList().getValue();
                
                if (cart != null && products != null) {
                    for (Product p : products) {
                        Integer qty = cart.get(p.getId());
                        if (qty != null && qty > 0) {
                            String itemString;
                            double itemTotal = p.getPrice() * qty;
                            String sym = viewModel.getCurrencySymbol();
                            if (qty > 1) {
                                itemString = String.format(Locale.getDefault(), "%s x%d | %s%.2f", p.getName(), qty, sym, itemTotal);
                            } else {
                                itemString = String.format(Locale.getDefault(), "%s | %s%.2f", p.getName(), sym, p.getPrice());
                            }
                            itemsArray.put(itemString);
                        }
                    }
                }
                response.put("items", itemsArray);

                viewModel.setTransactionJson(response.toString());
                com.google.android.gms.tasks.Task<Void> task = viewModel.placeOrder(method, txnId, orderType, "", infoStr,
                        subtotal, discountAmount, taxAmount, discountPercent, taxPercent, totalAmount);
                
                if (task != null) {
                    task.addOnCompleteListener(t -> {
                        loadingOverlay.setVisibility(View.GONE);
                        btnPayNow.setEnabled(true);
                        if (t.isSuccessful()) {
                            Navigation.findNavController(requireView()).navigate(R.id.action_paymentFragment_to_receiptFragment);
                        } else {
                            Toast.makeText(requireContext(), R.string.msg_payment_failed, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    loadingOverlay.setVisibility(View.GONE);
                    btnPayNow.setEnabled(true);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                loadingOverlay.setVisibility(View.GONE);
                btnPayNow.setEnabled(true);
            }
        }, 2000);
    }
}
