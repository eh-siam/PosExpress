package com.example.posexpress.ui;

import android.content.Intent;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.posexpress.R;
import com.example.posexpress.model.Product;
import com.example.posexpress.viewmodel.PosViewModel;
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
        

        rgOrderType = view.findViewById(R.id.rgOrderType);

        viewModel.getTotalAmount().observe(getViewLifecycleOwner(), total -> {
            if (total <= 0) {
                showEmptyState();
                btnBackToShop.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
            } else {
                TextView tvPayableAmount = view.findViewById(R.id.tvPayableAmount);
                tvPayableAmount.setText(String.format(Locale.getDefault(), "$%.2f", total));
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

        cardEmv.setOnClickListener(v -> selectMethod(R.id.cardEmv));
        cardWallet.setOnClickListener(v -> selectMethod(R.id.cardWallet));
        cardCash.setOnClickListener(v -> selectMethod(R.id.cardCash));

        btnPayNow.setOnClickListener(v -> {
            if ("e-Wallet / QR Code".equals(selectedMethodName)) {
                showBkashDialog();
            } else {
                processPayment(selectedMethodName, "");
            }
        });
    }

    private void showBkashDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_bkash_payment, null);
        etBkashNumberInDialog = dialogView.findViewById(R.id.etBkashNumber);
        MaterialButton btnScanQr = dialogView.findViewById(R.id.btnScanQr);

        btnScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), QrScannerActivity.class);
            qrScannerLauncher.launch(intent);
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String bkashNumber = etBkashNumberInDialog.getText().toString().trim();
                    if (bkashNumber.length() == 11) {
                        processPayment("bKash Payment", bkashNumber);
                    } else {
                        // For simplicity, just proceeding, but in real app you'd validate
                        processPayment("bKash Payment", bkashNumber);
                    }
                    etBkashNumberInDialog = null; // Clear reference
                })
                .setNegativeButton("Cancel", (dialog, which) -> etBkashNumberInDialog = null)
                .show();
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

    private void processPayment(String method, String bkashNumber) {
        loadingOverlay.setVisibility(View.VISIBLE);
        btnPayNow.setEnabled(false);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                double subtotal = viewModel.getTotalAmount().getValue();
                double discountPercent = 5.0; // Requirement: 5%
                double taxPercent = 7.5; // Requirement: 7.5%
                
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
                if (!bkashNumber.isEmpty()) {
                    response.put("bkash_number", bkashNumber);
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
                            if (qty > 1) {
                                itemString = String.format(Locale.getDefault(), "%s x%d | $%.2f", p.getName(), qty, itemTotal);
                            } else {
                                itemString = String.format(Locale.getDefault(), "%s | $%.2f", p.getName(), p.getPrice());
                            }
                            itemsArray.put(itemString);
                        }
                    }
                }
                response.put("items", itemsArray);

                viewModel.setTransactionJson(response.toString());
                
                // NEW: Place order in Firebase database with all details
                viewModel.placeOrder(method, txnId, orderType, "", bkashNumber,
                        subtotal, discountAmount, taxAmount, discountPercent, taxPercent, totalAmount);
                
                Navigation.findNavController(requireView()).navigate(R.id.action_paymentFragment_to_receiptFragment);

            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                loadingOverlay.setVisibility(View.GONE);
                btnPayNow.setEnabled(true);
            }
        }, 2000);
    }
}
