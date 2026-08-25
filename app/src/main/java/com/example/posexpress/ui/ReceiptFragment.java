package com.example.posexpress.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.posexpress.R;
import com.example.posexpress.viewmodel.PosViewModel;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReceiptFragment extends Fragment {

    private PosViewModel viewModel;
    private TextView tvTxnId, tvMethod, tvTimestamp, tvAmountPaid;
    private android.widget.LinearLayout layoutItemsList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_receipt, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(PosViewModel.class);

        tvTxnId = view.findViewById(R.id.tvTxnId);
        tvMethod = view.findViewById(R.id.tvMethod);
        tvTimestamp = view.findViewById(R.id.tvTimestamp);
        tvAmountPaid = view.findViewById(R.id.tvAmountPaid);
        layoutItemsList = view.findViewById(R.id.layoutItemsList);
        MaterialButton btnNewTransaction = view.findViewById(R.id.btnNewTransaction);

        viewModel.getTransactionJson().observe(getViewLifecycleOwner(), this::showReceipt);

        btnNewTransaction.setOnClickListener(v -> {
            viewModel.resetCart();
            Navigation.findNavController(view).navigate(R.id.action_receiptFragment_to_catalogFragment);
        });
    }

    private void showReceipt(String jsonString) {
        if (jsonString == null) {
            tvTxnId.setText("N/A");
            return;
        }

        try {
            JSONObject response = new JSONObject(jsonString);
            String txnId = response.getString("transaction_id");
            double amount = response.getDouble("amount");
            String method = response.getString("method");
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

            JSONArray itemsArray = response.optJSONArray("items");
            if (itemsArray != null && itemsArray.length() > 0) {
                layoutItemsList.removeAllViews();
                for (int i = 0; i < itemsArray.length(); i++) {
                    String itemStr = itemsArray.getString(i);
                    String[] parts = itemStr.split("\\|");
                    
                    if (parts.length == 2) {
                        View itemView = getLayoutInflater().inflate(R.layout.receipt_item_row, layoutItemsList, false);
                        TextView tvDesc = itemView.findViewById(R.id.tvItemDescription);
                        TextView tvPrice = itemView.findViewById(R.id.tvItemPrice);
                        
                        tvDesc.setText(parts[0].trim());
                        tvPrice.setText(parts[1].trim());
                        
                        layoutItemsList.addView(itemView);
                    }
                }
            }

            tvTxnId.setText(txnId);
            tvMethod.setText(method);
            tvTimestamp.setText(date);
            tvAmountPaid.setText(String.format(Locale.getDefault(), "$%.2f", amount));

        } catch (JSONException e) {
            Toast.makeText(getContext(), "Receipt Display Error", Toast.LENGTH_SHORT).show();
        }
    }
}
