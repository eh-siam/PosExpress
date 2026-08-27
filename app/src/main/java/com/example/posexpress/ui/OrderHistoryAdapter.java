package com.example.posexpress.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.posexpress.R;
import com.example.posexpress.model.Order;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {

    private List<Order> orderList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public OrderHistoryAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    public void setOrderList(List<Order> newList) {
        this.orderList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orderList.get(position);
        
        // This is a bit tricky since we don't have ViewModel here. 
        // We can get it from context or pass symbol in constructor.
        // For now, let's get the selected country from preferences directly.
        com.example.posexpress.util.CountryConfig country = new com.example.posexpress.util.AppPreferences(holder.itemView.getContext()).getSelectedCountry();
        String sym = country != null ? country.getCurrencySymbol() : "$";

        holder.tvOrderId.setText(order.getTransactionId());
        holder.tvOrderAmount.setText(String.format(Locale.getDefault(), "%s%.2f", sym, order.getTotalAmount()));
        holder.tvOrderMethod.setText(order.getPaymentMethod());
        
        if (order.getTimestamp() instanceof Long) {
            holder.tvOrderTime.setText(dateFormat.format(new Date((Long) order.getTimestamp())));
        } else {
            holder.tvOrderTime.setText("N/A");
        }

        StringBuilder itemsStr = new StringBuilder();
        if (order.getItems() != null) {
            for (int i = 0; i < order.getItems().size(); i++) {
                itemsStr.append(order.getItems().get(i));
                if (i < order.getItems().size() - 1) {
                    itemsStr.append(", ");
                }
            }
        }
        holder.tvOrderItems.setText(itemsStr.toString());
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderTime, tvOrderAmount, tvOrderMethod, tvOrderItems;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderTime = itemView.findViewById(R.id.tvOrderTime);
            tvOrderAmount = itemView.findViewById(R.id.tvOrderAmount);
            tvOrderMethod = itemView.findViewById(R.id.tvOrderMethod);
            tvOrderItems = itemView.findViewById(R.id.tvOrderItems);
        }
    }
}
