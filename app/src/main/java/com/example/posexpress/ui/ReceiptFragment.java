package com.example.posexpress.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import androidx.print.PrintHelper;

import com.example.posexpress.R;
import com.example.posexpress.viewmodel.PosViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReceiptFragment extends Fragment {

    private PosViewModel viewModel;
    private TextView tvTxnId, tvMethod, tvTimestamp, tvCustomerInfo, tvOrderType;
    private TextView tvSubtotal, tvDiscountLabel, tvDiscountAmount, tvTaxLabel, tvTaxAmount, tvGrandTotal;
    private android.widget.ImageView ivQrCode;
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
        tvCustomerInfo = view.findViewById(R.id.tvCustomerInfo);
        tvOrderType = view.findViewById(R.id.tvOrderType);
        
        tvSubtotal = view.findViewById(R.id.tvSubtotal);
        tvDiscountLabel = view.findViewById(R.id.tvDiscountLabel);
        tvDiscountAmount = view.findViewById(R.id.tvDiscountAmount);
        tvTaxLabel = view.findViewById(R.id.tvTaxLabel);
        tvTaxAmount = view.findViewById(R.id.tvTaxAmount);
        tvGrandTotal = view.findViewById(R.id.tvGrandTotal);
        
        ivQrCode = view.findViewById(R.id.ivQrCode);
        
        layoutItemsList = view.findViewById(R.id.layoutItemsList);
        MaterialButton btnNewTransaction = view.findViewById(R.id.btnNewTransaction);
        MaterialButton btnPrintReceipt = view.findViewById(R.id.btnPrintReceipt);
        View receiptCard = view.findViewById(R.id.receiptCard);

        viewModel.getTransactionJson().observe(getViewLifecycleOwner(), this::showReceipt);

        btnPrintReceipt.setOnClickListener(v -> {
            if (receiptCard != null) {
                doPrint(receiptCard);
            }
        });

        btnNewTransaction.setOnClickListener(v -> {
            viewModel.resetCart();
            Navigation.findNavController(view).navigate(R.id.action_receiptFragment_to_catalogFragment);
        });
    }

    private void doPrint(View view) {
        PrintHelper printHelper = new PrintHelper(requireContext());
        printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        
        Bitmap bitmap = createBitmapFromView(view);
        if (bitmap != null) {
            printHelper.printBitmap("POSExpress_Receipt_" + System.currentTimeMillis(), bitmap);
        } else {
            Toast.makeText(getContext(), "Failed to generate receipt image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap createBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            bgDrawable.draw(canvas);
        } else {
            canvas.drawColor(Color.WHITE);
        }
        view.draw(canvas);
        return bitmap;
    }

    private void showReceipt(String jsonString) {
        if (jsonString == null) {
            tvTxnId.setText("N/A");
            return;
        }

        try {
            JSONObject response = new JSONObject(jsonString);
            String txnId = response.getString("transaction_id");
            String method = response.getString("method");
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

            // Billing Data
            double amount = response.optDouble("amount", 0.0);
            double subtotal = response.optDouble("subtotal", amount);
            double discountAmount = response.optDouble("discount_amount", 0.0);
            double taxAmount = response.optDouble("tax_amount", 0.0);
            double discountPercent = response.optDouble("discount_percent", 0.0);
            double taxPercent = response.optDouble("tax_percent", 0.0);

            // Customer Data
            String name = response.optString("customer_name", "Walk-in Customer");
            String phone = response.optString("customer_phone", "");
            String orderType = response.optString("order_type", "Dine-in");

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
            
            String customerDisplay = name + (phone.isEmpty() ? "" : " (" + phone + ")");
            tvCustomerInfo.setText(customerDisplay);
            tvOrderType.setText(orderType);

            tvSubtotal.setText(String.format(Locale.getDefault(), "$%.2f", subtotal));
            tvDiscountLabel.setText(String.format(Locale.getDefault(), "Discount (%.1f%%)", discountPercent));
            tvDiscountAmount.setText(String.format(Locale.getDefault(), "-$%.2f", discountAmount));
            tvTaxLabel.setText(String.format(Locale.getDefault(), "VAT (%.1f%%)", taxPercent));
            tvTaxAmount.setText(String.format(Locale.getDefault(), "+$%.2f", taxAmount));
            tvGrandTotal.setText(String.format(Locale.getDefault(), "$%.2f", amount));

            generateQRCode(txnId);

        } catch (JSONException e) {
            Toast.makeText(getContext(), "Receipt Display Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateQRCode(String data) {
        MultiFormatWriter writer = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            ivQrCode.setImageBitmap(bmp);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}
