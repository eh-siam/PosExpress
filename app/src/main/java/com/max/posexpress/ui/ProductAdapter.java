package com.max.posexpress.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.max.posexpress.R;
import com.max.posexpress.model.Product;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private List<Product> productList;
    private final OnProductActionListener listener;
    private Map<String, Integer> productQuantities = new HashMap<>();

    public interface OnProductActionListener {
        void onQuantityChanged(Product product, int newQuantity);
        void onEditProduct(Product product);
        void onDeleteProduct(Product product);
    }

    public ProductAdapter(List<Product> productList, OnProductActionListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    public void setProductList(List<Product> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    public void setQuantities(Map<String, Integer> quantities) {
        this.productQuantities = quantities;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.tvName.setText(product.getName());
        
        com.max.posexpress.util.CountryConfig country = new com.max.posexpress.util.AppPreferences(holder.itemView.getContext()).getSelectedCountry();
        String sym = country != null ? country.getCurrencySymbol() : "$";
        holder.tvPrice.setText(String.format(Locale.getDefault(), "%s%.2f", sym, product.getPrice()));
        
        String category = product.getCategory();
        if (category == null || category.isEmpty()) {
            holder.tvCategory.setText("General");
        } else {
            holder.tvCategory.setText(category);
        }
        
        int quantity = productQuantities.getOrDefault(product.getId(), 0);
        
        if (quantity > 0) {
            holder.btnAdd.setVisibility(View.GONE);
            holder.layoutQtyControls.setVisibility(View.VISIBLE);
            holder.tvQty.setText(String.valueOf(quantity));
            
            holder.selectionIndicator.setVisibility(View.VISIBLE);
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.successContainerSubtle));
            holder.cardView.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.successEmerald));
            holder.cardView.setStrokeWidth(convertDpToPx(holder.itemView, 1));
            
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText("x" + quantity);
        } else {
            holder.btnAdd.setVisibility(View.VISIBLE);
            holder.layoutQtyControls.setVisibility(View.GONE);
            
            holder.selectionIndicator.setVisibility(View.GONE);
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            holder.cardView.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.surfaceVariant));
            holder.cardView.setStrokeWidth(convertDpToPx(holder.itemView, 1));
            
            holder.tvBadge.setVisibility(View.GONE);
        }

        holder.btnAdd.setOnClickListener(v -> listener.onQuantityChanged(product, 1));
        holder.btnPlus.setOnClickListener(v -> listener.onQuantityChanged(product, quantity + 1));
        holder.btnMinus.setOnClickListener(v -> listener.onQuantityChanged(product, quantity - 1));
        
        holder.btnMore.setOnClickListener(v -> {
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(v.getContext(), v);
            popup.getMenu().add(0, 1, 0, "Edit");
            popup.getMenu().add(0, 2, 1, "Delete");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    listener.onEditProduct(product);
                    return true;
                } else if (item.getItemId() == 2) {
                    listener.onDeleteProduct(product);
                    return true;
                }
                return false;
            });
            popup.show();
        });
        
        holder.btnMore.setVisibility(View.VISIBLE);
    }

    private int convertDpToPx(View view, int dp) {
        float density = view.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvBadge, tvQty, tvCategory;
        MaterialButton btnAdd, btnPlus, btnMinus;
        MaterialCardView cardView;
        LinearLayout layoutQtyControls;
        ImageButton btnMore;
        View selectionIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvCategory = itemView.findViewById(R.id.tvProductCategory);
            tvBadge = itemView.findViewById(R.id.tvQuantityBadge);
            tvQty = itemView.findViewById(R.id.tvQuantity);
            btnAdd = itemView.findViewById(R.id.btnAddCart);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            cardView = itemView.findViewById(R.id.productCard);
            layoutQtyControls = itemView.findViewById(R.id.layoutQtyControls);
            btnMore = itemView.findViewById(R.id.btnMore);
            selectionIndicator = itemView.findViewById(R.id.selectionIndicator);
        }
    }
}
