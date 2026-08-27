package com.example.posexpress.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posexpress.R;
import com.example.posexpress.model.Order;
import com.example.posexpress.viewmodel.PosViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderHistoryFragment extends Fragment {

    private PosViewModel viewModel;
    private OrderHistoryAdapter adapter;
    private RecyclerView recyclerView;
    private View layoutEmptyHistory;
    private View pbHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(PosViewModel.class);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        recyclerView = view.findViewById(R.id.rvOrderHistory);
        layoutEmptyHistory = view.findViewById(R.id.layoutEmptyHistory);
        pbHistory = view.findViewById(R.id.pbHistory);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        viewModel.getPaginatedOrders().observe(getViewLifecycleOwner(), orders -> {
            updateUI(orders, Boolean.TRUE.equals(viewModel.getIsLoadingMoreOrders().getValue()));
        });

        viewModel.getIsLoadingMoreOrders().observe(getViewLifecycleOwner(), loading -> {
            List<Order> orders = viewModel.getPaginatedOrders().getValue();
            updateUI(orders, loading);
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                        viewModel.loadNextOrderPage();
                    }
                }
            }
        });
        
        viewModel.resetOrderPagination();
    }

    private void updateUI(List<Order> orders, boolean isLoading) {
        if (isLoading && (orders == null || orders.isEmpty())) {
            // Initial load
            pbHistory.setVisibility(View.VISIBLE);
            layoutEmptyHistory.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        } else if (orders == null || orders.isEmpty()) {
            // No data after loading
            pbHistory.setVisibility(View.GONE);
            layoutEmptyHistory.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            // Data available
            pbHistory.setVisibility(View.GONE);
            layoutEmptyHistory.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            if (adapter == null) {
                adapter = new OrderHistoryAdapter(new ArrayList<>(orders));
                recyclerView.setAdapter(adapter);
            } else {
                adapter.setOrderList(new ArrayList<>(orders));
            }

            if (isLoading) {
                Toast.makeText(getContext(), "Loading more history...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView = null;
        layoutEmptyHistory = null;
        pbHistory = null;
    }
}
