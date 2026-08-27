package com.example.posexpress.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.posexpress.R;
import com.example.posexpress.viewmodel.PosViewModel;

import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private PosViewModel viewModel;
    private TextView tvTodaySales, tvTodayTxnCount;
    private TextView tvCashSales, tvWalletSales, tvCardSales;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(PosViewModel.class);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        tvTodaySales = view.findViewById(R.id.tvTodaySales);
        tvTodayTxnCount = view.findViewById(R.id.tvTodayTxnCount);
        tvCashSales = view.findViewById(R.id.tvCashSales);
        tvWalletSales = view.findViewById(R.id.tvWalletSales);
        tvCardSales = view.findViewById(R.id.tvCardSales);

        view.findViewById(R.id.btnViewHistory).setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_dashboardFragment_to_orderHistoryFragment));

        observeDashboardData();
        viewModel.startObservingOrders();
    }

    private void observeDashboardData() {
        viewModel.getTodaySales().observe(getViewLifecycleOwner(), sales -> 
            tvTodaySales.setText(String.format(Locale.getDefault(), "%s%.2f", viewModel.getCurrencySymbol(), sales)));

        viewModel.getTodayTxnCount().observe(getViewLifecycleOwner(), count -> 
            tvTodayTxnCount.setText(String.valueOf(count)));

        viewModel.getMethodStats().observe(getViewLifecycleOwner(), stats -> {
            updateMethodStats(stats);
        });
    }

    private void updateMethodStats(Map<String, Double> stats) {
        Double cash = stats.get("Cash Payment");
        Double card = stats.get("EMV Card Payment");
        
        // Combine bKash and e-Wallet for display
        Double wallet = stats.get("e-Wallet / QR Code");
        Double bkash = stats.get("bKash Payment");
        
        double totalWallet = (wallet != null ? wallet : 0.0) + (bkash != null ? bkash : 0.0);

        String sym = viewModel.getCurrencySymbol();
        tvCashSales.setText(String.format(Locale.getDefault(), "%s%.2f", sym, cash != null ? cash : 0.0));
        tvCardSales.setText(String.format(Locale.getDefault(), "%s%.2f", sym, card != null ? card : 0.0));
        tvWalletSales.setText(String.format(Locale.getDefault(), "%s%.2f", sym, totalWallet));
    }
}
