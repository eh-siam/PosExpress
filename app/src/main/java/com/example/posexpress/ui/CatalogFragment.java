package com.example.posexpress.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.example.posexpress.model.Category;
import com.example.posexpress.model.Product;
import com.example.posexpress.viewmodel.PosViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CatalogFragment extends Fragment implements ProductAdapter.OnProductActionListener {

    private PosViewModel viewModel;
    private ProductAdapter adapter;
    private RecyclerView recyclerView;
    private View layoutEmptyState;
    private TextView tvTotalAmount, tvEmptyTitle, tvEmptySubtitle;
    private android.widget.ImageView ivEmptyIcon;
    private MaterialButton btnEmptyAction;
    private ProgressBar progressBar;
    private ChipGroup chipGroupCategories;
    private FloatingActionButton fabAddProduct;

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
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle);
        ivEmptyIcon = view.findViewById(R.id.ivEmptyIcon);
        btnEmptyAction = view.findViewById(R.id.btnEmptyAction);

        progressBar = view.findViewById(R.id.progressBar);
        MaterialButton btnProceedPay = view.findViewById(R.id.btnProceedPay);
        fabAddProduct = view.findViewById(R.id.fabAddProduct);
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories);
        View btnDashboard = view.findViewById(R.id.btnDashboard);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Ensure adapter is attached if it already exists (from backstack)
        if (adapter != null) {
            recyclerView.setAdapter(adapter);
        }

        viewModel.getProductList().observe(getViewLifecycleOwner(), products -> {
            if (adapter == null) {
                adapter = new ProductAdapter(products, this);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.setProductList(products);
            }
            updateUIState();
        });

        viewModel.getCategoryList().observe(getViewLifecycleOwner(), categories -> {
            // Update Chips
            chipGroupCategories.removeAllViews();
            
            // Add "All" chip
            Chip allChip = new Chip(requireContext());
            allChip.setText("All");
            allChip.setCheckable(true);
            allChip.setChecked(true);
            allChip.setTag("All");
            chipGroupCategories.addView(allChip);

            for (Category category : categories) {
                Chip chip = new Chip(requireContext());
                chip.setText(category.getName());
                chip.setCheckable(true);
                chip.setTag(category.getName());
                chipGroupCategories.addView(chip);
            }
            
            chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (!checkedIds.isEmpty()) {
                    Chip selectedChip = group.findViewById(checkedIds.get(0));
                    if (selectedChip != null) {
                        viewModel.setSelectedCategory(selectedChip.getTag().toString());
                    }
                }
            });
        });

        viewModel.getTotalAmount().observe(getViewLifecycleOwner(), total -> {
            tvTotalAmount.setText(String.format(Locale.getDefault(), "%s%.2f", viewModel.getCurrencySymbol(), total));
        });

        viewModel.getCartQuantities().observe(getViewLifecycleOwner(), quantities -> {
            if (adapter != null) {
                adapter.setQuantities(quantities);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            updateUIState();
        });

        viewModel.getIsLoadingMore().observe(getViewLifecycleOwner(), loading -> {
            if (loading) {
                Toast.makeText(getContext(), "Loading more products...", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && dy > 0) { // check for scroll down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                        viewModel.loadNextPage();
                    }
                }
            }
        });

        fabAddProduct.setOnClickListener(v -> showProductDialog(null));
        btnDashboard.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_catalogFragment_to_dashboardFragment));
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clear references to views to avoid memory leaks
        recyclerView = null;
        layoutEmptyState = null;
        tvTotalAmount = null;
        progressBar = null;
    }

    private void updateUIState() {
        List<Product> products = viewModel.getProductList().getValue();
        Boolean loading = viewModel.getIsLoading().getValue();

        if (Boolean.TRUE.equals(loading)) {
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            if (products != null && !products.isEmpty()) {
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.GONE);
            }
        } else {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            boolean isEmpty = (products == null || products.isEmpty());
            
            if (isEmpty) {
                recyclerView.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.VISIBLE);
                
                String currentCategory = viewModel.getSelectedCategory().getValue();
                if (currentCategory == null || currentCategory.equals("All")) {
                    // Whole catalog is empty
                    ivEmptyIcon.setImageResource(android.R.drawable.ic_menu_agenda);
                    tvEmptyTitle.setText("Catalog is Empty");
                    tvEmptySubtitle.setText("You haven't added any products yet.");
                    btnEmptyAction.setText("Add Product");
                    btnEmptyAction.setOnClickListener(v -> showProductDialog(null));
                    
                    // Hide FAB when no products exist at all
                    if (fabAddProduct != null) fabAddProduct.setVisibility(View.GONE);
                } else {
                    // Filtered category is empty
                    ivEmptyIcon.setImageResource(android.R.drawable.ic_menu_search);
                    tvEmptyTitle.setText("No products in " + currentCategory);
                    tvEmptySubtitle.setText("Try checking another category or add one.");
                    btnEmptyAction.setText("View All");
                    btnEmptyAction.setOnClickListener(v -> {
                        viewModel.setSelectedCategory("All");
                        // Manually check/reset chip if possible
                        if (chipGroupCategories != null) {
                            chipGroupCategories.check(R.id.chipAll);
                        }
                    });
                    
                    // Show FAB if products exist but are filtered out
                    if (fabAddProduct != null) fabAddProduct.setVisibility(View.VISIBLE);
                }
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                layoutEmptyState.setVisibility(View.GONE);
                if (fabAddProduct != null) fabAddProduct.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showProductDialog(Product productToEdit) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_product, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etProductName);
        TextInputEditText etPrice = dialogView.findViewById(R.id.etProductPrice);
        AutoCompleteTextView etCategory = dialogView.findViewById(R.id.etProductCategory);

        // Setup Category Dropdown
        List<Category> categories = viewModel.getCategoryList().getValue();
        List<String> categoryNames = new ArrayList<>();
        if (categories != null) {
            for (Category c : categories) categoryNames.add(c.getName());
        }
        
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames);
        etCategory.setAdapter(catAdapter);

        if (productToEdit != null) {
            etName.setText(productToEdit.getName());
            etPrice.setText(String.valueOf(productToEdit.getPrice()));
            etCategory.setText(productToEdit.getCategory(), false);
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(productToEdit == null ? "Add New Product" : "Edit Product")
                .setIcon(productToEdit == null ? android.R.drawable.ic_input_add : android.R.drawable.ic_menu_edit)
                .setView(dialogView)
                .setPositiveButton(productToEdit == null ? "Add" : "Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String category = etCategory.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Please enter product name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (priceStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter price", Toast.LENGTH_SHORT).show();
                return;
            }

            // Duplicate Name Check
            if (productToEdit == null) {
                // Adding new product
                if (viewModel.isProductNameDuplicate(name)) {
                    Toast.makeText(getContext(), "Product '" + name + "' already exists!", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                // Editing existing product - check if name changed and new name is duplicate
                if (!productToEdit.getName().equalsIgnoreCase(name) && viewModel.isProductNameDuplicate(name)) {
                    Toast.makeText(getContext(), "Product '" + name + "' already exists!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (category.isEmpty()) category = "General";

            if (productToEdit == null) {
                viewModel.addProduct(name, priceStr, category);
            } else {
                viewModel.editProduct(productToEdit, name, priceStr, category);
            }
            dialog.dismiss();
        });
    }
}
