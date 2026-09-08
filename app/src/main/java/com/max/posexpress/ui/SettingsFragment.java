package com.max.posexpress.ui;

import android.content.Intent;
import android.net.Uri;
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

import com.max.posexpress.R;
import com.max.posexpress.model.Category;
import com.max.posexpress.repository.PosRepository;
import com.max.posexpress.viewmodel.PosViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private PosViewModel viewModel;
    private android.widget.TextView tvTaxValue, tvDiscountValue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(PosViewModel.class);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        tvTaxValue = view.findViewById(R.id.tvTaxValue);
        tvDiscountValue = view.findViewById(R.id.tvDiscountValue);
        
        updateConfigUI();

        view.findViewById(R.id.btnConfigTax).setOnClickListener(v -> showConfigDialog("Tax", viewModel.getTaxPercent(), true));
        view.findViewById(R.id.btnConfigDiscount).setOnClickListener(v -> showConfigDialog("Discount", viewModel.getDiscountPercent(), false));

        view.findViewById(R.id.cardManageCategories).setOnClickListener(v -> showManageCategoriesDialog());

        view.findViewById(R.id.btnSupport).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@payswift.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Support Request - POS Express");
            try {
                startActivity(Intent.createChooser(intent, "Send Email"));
            } catch (Exception e) {
                Toast.makeText(getContext(), "No email client found", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btnHelp).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Documentation coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Add Logout Logic
        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut();
                
                // Sign out from Google to allow account switching
                GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
                        .addOnCompleteListener(task -> {
                            PosRepository.resetInstance();
                            startActivity(new Intent(requireContext(), LoginActivity.class));
                            requireActivity().finishAffinity();
                        });
            });
        }
    }

    private void updateConfigUI() {
        if (tvTaxValue != null) tvTaxValue.setText(String.format(Locale.US, "%.1f%%", viewModel.getTaxPercent()));
        if (tvDiscountValue != null) tvDiscountValue.setText(String.format(Locale.US, "%.1f%%", viewModel.getDiscountPercent()));
    }

    private void showManageCategoriesDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_manage_categories, null);
        RecyclerView rvCategories = dialogView.findViewById(R.id.rvManageCategories);
        MaterialButton btnAddCategory = dialogView.findViewById(R.id.btnAddCategory);

        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        CategoryManageAdapter adapter = new CategoryManageAdapter(category -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Category")
                    .setMessage("Are you sure you want to delete \"" + category.getName() + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        viewModel.deleteCategory(category);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        rvCategories.setAdapter(adapter);

        viewModel.getCategoryList().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                adapter.setCategories(categories);
            }
        });

        btnAddCategory.setOnClickListener(v -> {
            TextInputEditText etCategory = new TextInputEditText(requireContext());
            android.widget.FrameLayout container = new android.widget.FrameLayout(requireContext());
            int padding = convertDpToPx(24);
            container.setPadding(padding, convertDpToPx(8), padding, 0);
            container.addView(etCategory);
            etCategory.setHint("Category Name");

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Add Category")
                    .setView(container)
                    .setPositiveButton("Add", (dialog, which) -> {
                        String name = etCategory.getText().toString().trim();
                        if (!name.isEmpty()) {
                            viewModel.addCategory(name);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Manage Categories")
                .setView(dialogView)
                .setPositiveButton("Done", null)
                .show();
        
        viewModel.startObservingData();
    }

    private void showConfigDialog(String title, float currentValue, boolean isTax) {
        // Create a simple container for the input
        android.widget.FrameLayout container = new android.widget.FrameLayout(requireContext());
        com.google.android.material.textfield.TextInputLayout til = new com.google.android.material.textfield.TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox);
        
        int padding = convertDpToPx(24);
        container.setPadding(padding, convertDpToPx(8), padding, 0);
        
        TextInputEditText etValue = new TextInputEditText(til.getContext());
        etValue.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etValue.setText(String.valueOf(currentValue));
        etValue.setSelection(etValue.getText().length());
        
        til.setHint(title + " Percentage (%)");
        til.setSuffixText("%");
        til.addView(etValue);
        container.addView(til);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Configure " + title)
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String valStr = etValue.getText().toString().trim();
                    if (!valStr.isEmpty()) {
                        try {
                            float newVal = Float.parseFloat(valStr);
                            if (isTax) viewModel.updateTaxPercent(newVal);
                            else viewModel.updateDiscountPercent(newVal);
                            updateConfigUI();
                            Toast.makeText(getContext(), title + " updated successfully", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Invalid value", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int convertDpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static class CategoryManageAdapter extends RecyclerView.Adapter<CategoryManageAdapter.ViewHolder> {
        private List<Category> categories = new ArrayList<>();
        private final OnDeleteClickListener listener;

        public interface OnDeleteClickListener {
            void onDelete(Category category);
        }

        public CategoryManageAdapter(OnDeleteClickListener listener) {
            this.listener = listener;
        }

        public void setCategories(List<Category> newList) {
            this.categories = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_manage_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Category category = categories.get(position);
            holder.tvName.setText(category.getName());
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(category));
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView tvName;
            android.widget.ImageButton btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvCategoryName);
                btnDelete = itemView.findViewById(R.id.btnDeleteCategory);
            }
        }
    }
}
