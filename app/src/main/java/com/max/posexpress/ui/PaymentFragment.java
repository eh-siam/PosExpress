package com.max.posexpress.ui;

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
        layoutEmptyState = view.findViewById(R.id.layoutMainEmptyState);
        MaterialButton btnBackToShop = view.findViewById(R.id.btnBackToShop);

        com.google.android.material.appbar.MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        }

        rgOrderType = view.findViewById(R.id.rgOrderType);

        viewModel.getTotalAmount().observe(getViewLifecycleOwner(), total -> {
            if (total <= 0) {
                showEmptyState();
                if (btnBackToShop != null) {
                    btnBackToShop.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
                }
            } else {
                TextView tvPayableAmount = view.findViewById(R.id.tvPayableAmount);
                if (tvPayableAmount != null) {
                    tvPayableAmount.setText(String.format(Locale.getDefault(), "%s%.2f", viewModel.getCurrencySymbol(), total));
                }
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
            if (cardWallet != null) cardWallet.setVisibility(View.GONE);
        }

        if (cardEmv != null) cardEmv.setOnClickListener(v -> selectMethod(R.id.cardEmv));
        if (cardWallet != null) cardWallet.setOnClickListener(v -> selectMethod(R.id.cardWallet));
        if (cardCash != null) cardCash.setOnClickListener(v -> selectMethod(R.id.cardCash));

        if (btnPayNow != null) btnPayNow.setOnClickListener(v -> showPaymentConfirmation());

        // Handle Back Press during payment
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (loadingOverlay != null && loadingOverlay.getVisibility() == View.VISIBLE) {
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
                    startPaymentFlow();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startPaymentFlow() {
        executePayment(selectedMethodName);
    }

    private void showEmptyState() {
        if (paymentContent != null) paymentContent.setVisibility(View.GONE);
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
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
        if (card == null) return;
        card.setStrokeWidth(convertDpToPx(1));
        card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.surfaceVariant));
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));
        if (rb != null) rb.setChecked(false);
        if (indicator != null) indicator.setVisibility(View.GONE);
    }

    private void highlightCard(MaterialCardView card, RadioButton rb, View indicator) {
        if (card == null) return;
        card.setStrokeWidth(convertDpToPx(1));
        card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.successEmerald));
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.successContainerSubtle));
        if (rb != null) rb.setChecked(true);
        if (indicator != null) indicator.setVisibility(View.VISIBLE);
    }
    
    private int convertDpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void executePayment(String method) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
        if (btnPayNow != null) btnPayNow.setEnabled(false);

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
                String orderType = rbOrderType != null ? rbOrderType.getText().toString() : "Dine-in";

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
                com.google.android.gms.tasks.Task<Void> task = viewModel.placeOrder(method, txnId, orderType, "", "",
                        subtotal, discountAmount, taxAmount, discountPercent, taxPercent, totalAmount);
                
                if (task != null) {
                    task.addOnCompleteListener(t -> {
                        if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                        if (btnPayNow != null) btnPayNow.setEnabled(true);
                        if (t.isSuccessful()) {
                            Navigation.findNavController(requireView()).navigate(R.id.action_paymentFragment_to_receiptFragment);
                        } else {
                            Toast.makeText(requireContext(), R.string.msg_payment_failed, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    if (btnPayNow != null) btnPayNow.setEnabled(true);
                }

            } catch (JSONException e) {
                e.printStackTrace();
                if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                if (btnPayNow != null) btnPayNow.setEnabled(true);
            }
        }, 1500);
    }
}
