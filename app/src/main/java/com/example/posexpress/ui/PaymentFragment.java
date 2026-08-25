package com.example.posexpress.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        MaterialButton btnBackToShop = view.findViewById(R.id.btnBackToShop);

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

        btnPayNow.setOnClickListener(v -> processPayment(selectedMethodName));
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

    private void processPayment(String method) {
        loadingOverlay.setVisibility(View.VISIBLE);
        btnPayNow.setEnabled(false);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                double total = viewModel.getTotalAmount().getValue();
                JSONObject response = new JSONObject();
                response.put("status", "SUCCESS");
                response.put("transaction_id", "TXN_" + System.currentTimeMillis());
                response.put("amount", total);
                response.put("method", method);
                
                JSONArray itemsArray = new JSONArray();
                Map<Integer, Integer> cart = viewModel.getCartQuantities().getValue();
                List<Product> products = viewModel.getProductList().getValue();
                
                if (cart != null && products != null) {
                    for (Product p : products) {
                        Integer qty = cart.get(p.getId());
                        if (qty != null && qty > 0) {
                            String itemString;
                            double subtotal = p.getPrice() * qty;
                            if (qty > 1) {
                                itemString = String.format(Locale.getDefault(), "%s x%d | $%.2f", p.getName(), qty, subtotal);
                            } else {
                                itemString = String.format(Locale.getDefault(), "%s | $%.2f", p.getName(), p.getPrice());
                            }
                            itemsArray.put(itemString);
                        }
                    }
                }
                response.put("items", itemsArray);

                viewModel.setTransactionJson(response.toString());
                
                // NEW: Place order in Firebase database
                viewModel.placeOrder(method);
                
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
