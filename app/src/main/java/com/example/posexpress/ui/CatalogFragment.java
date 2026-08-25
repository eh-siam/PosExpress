package com.example.posexpress.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posexpress.R;
import com.example.posexpress.model.Product;
import com.example.posexpress.viewmodel.PosViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Locale;

public class CatalogFragment extends Fragment implements ProductAdapter.OnProductActionListener {

    private PosViewModel viewModel;
    private ProductAdapter adapter;
    private RecyclerView recyclerView;
    private View layoutEmptyState;
    private TextView tvTotalAmount;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_catalog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(PosViewModel.class);

        tvTotalAmount = view.findViewById(R.id.tvTotalAmount);
        recyclerView = view.findViewById(R.id.recyclerView);
        layoutEmptyState = view.findViewById(R.id.layoutMainEmptyState);
        progressBar = view.findViewById(R.id.progressBar); // Need to ensure this exists in fragment_catalog
        MaterialButton btnProceedPay = view.findViewById(R.id.btnProceedPay);
        FloatingActionButton fabAddProduct = view.findViewById(R.id.fabAddProduct);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        viewModel.getProductList().observe(getViewLifecycleOwner(), products -> {
            if (adapter == null) {
                adapter = new ProductAdapter(products, this);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.setProductList(products);
            }
            checkEmptyState(products.isEmpty());
        });

        viewModel.getTotalAmount().observe(getViewLifecycleOwner(), total -> {
            tvTotalAmount.setText(String.format(Locale.getDefault(), "$%.2f", total));
        });

        viewModel.getCartQuantities().observe(getViewLifecycleOwner(), quantities -> {
            if (adapter != null) {
                adapter.setQuantities(quantities);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });

        fabAddProduct.setOnClickListener(v -> showProductDialog(null));
        btnProceedPay.setOnClickListener(v -> {
            Double total = viewModel.getTotalAmount().getValue();
            if (total != null && total > 0) {
                Navigation.findNavController(requireView()).navigate(R.id.action_catalogFragment_to_paymentFragment);
            } else {
                Toast.makeText(getContext(), "Your cart is empty!", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.startObservingData();
    }

    @Override
    public void onQuantityChanged(Product product, int newQuantity) {
        viewModel.updateQuantity(product, newQuantity);
    }

    @Override
    public void onEditProduct(Product product) {
        showProductDialog(product);
    }

    @Override
    public void onDeleteProduct(Product product) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + product.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> viewModel.deleteProduct(product))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkEmptyState(boolean isEmpty) {
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void showProductDialog(Product productToEdit) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_product, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etProductName);
        TextInputEditText etPrice = dialogView.findViewById(R.id.etProductPrice);

        if (productToEdit != null) {
            etName.setText(productToEdit.getName());
            etPrice.setText(String.valueOf(productToEdit.getPrice()));
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(productToEdit == null ? "Add New Product" : "Edit Product")
                .setIcon(productToEdit == null ? android.R.drawable.ic_input_add : android.R.drawable.ic_menu_edit)
                .setView(dialogView)
                .setPositiveButton(productToEdit == null ? "Add" : "Update", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String priceStr = etPrice.getText().toString().trim();
                    if (productToEdit == null) {
                        viewModel.addProduct(name, priceStr);
                    } else {
                        viewModel.editProduct(productToEdit, name, priceStr);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
