package com.max.posexpress.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.max.posexpress.R;
import com.max.posexpress.model.Order;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<HistoryListItem> displayItems = new ArrayList<>();
    private final SimpleDateFormat headerFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public OrderHistoryAdapter(List<Order> orderList) {
        setOrderList(orderList);
    }

    public void setOrderList(List<Order> newList) {
        List<HistoryListItem> items = new ArrayList<>();
        if (newList != null) {
            String lastDate = "";
            for (Order order : newList) {
                String currentDate = "N/A";
                if (order.getTimestamp() instanceof Long) {
                    currentDate = headerFormat.format(new Date((Long) order.getTimestamp()));
                }
                
                if (!currentDate.equals(lastDate)) {
                    items.add(new HistoryListItem(TYPE_HEADER, null, currentDate));
                    lastDate = currentDate;
                }
                items.add(new HistoryListItem(TYPE_ITEM, order, null));
            }
        }
        this.displayItems = items;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_history, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HistoryListItem item = displayItems.get(position);
        
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvDate.setText(item.date);
        } else if (holder instanceof ItemViewHolder) {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            Order order = item.order;

            com.max.posexpress.util.CountryConfig country = new com.max.posexpress.util.AppPreferences(itemHolder.itemView.getContext()).getSelectedCountry();
            String sym = country != null ? country.getCurrencySymbol() : "$";

            itemHolder.tvOrderId.setText(order.getTransactionId());
            itemHolder.tvOrderAmount.setText(String.format(Locale.getDefault(), "%s%.2f", sym, order.getTotalAmount()));

            if (order.getTimestamp() instanceof Long) {
                itemHolder.tvOrderTime.setText(timeFormat.format(new Date((Long) order.getTimestamp())));
            } else {
                itemHolder.tvOrderTime.setText("");
            }
            
            String method = order.getPaymentMethod();
            itemHolder.tvOrderMethod.setText(method);

            // Apply dynamic colors and icons
            int colorRes = R.color.textSecondary;
            int containerColorRes = R.color.surfaceVariant;
            if (method != null) {
                if (method.equals("Cash Payment")) {
                    colorRes = R.color.paymentCash;
                    containerColorRes = R.color.successContainer;
                } else if (method.equals("EMV Card Payment") || method.equals("Online Card Payment")) {
                    colorRes = R.color.paymentCard;
                    containerColorRes = R.color.surfaceVariant; // Can refine these
                } else if (method.contains("bKash") || method.contains("Wallet")) {
                    colorRes = R.color.paymentWallet;
                    containerColorRes = R.color.surfaceVariant;
                }
            }
            itemHolder.tvOrderMethod.setTextColor(ContextCompat.getColor(itemHolder.itemView.getContext(), colorRes));
            itemHolder.tvOrderMethod.getBackground().setTint(ContextCompat.getColor(itemHolder.itemView.getContext(), containerColorRes));
            itemHolder.ivPaymentIcon.setImageTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(itemHolder.itemView.getContext(), colorRes)));
            itemHolder.cvIconContainer.setCardBackgroundColor(ContextCompat.getColor(itemHolder.itemView.getContext(), containerColorRes));
            
            StringBuilder itemsStr = new StringBuilder();
            if (order.getItems() != null) {
                for (int i = 0; i < order.getItems().size(); i++) {
                    itemsStr.append(order.getItems().get(i));
                    if (i < order.getItems().size() - 1) {
                        itemsStr.append(", ");
                    }
                }
            }
            itemHolder.tvOrderItems.setText(itemsStr.toString());
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    private static class HistoryListItem {
        int type;
        Order order;
        String date;

        HistoryListItem(int type, Order order, String date) {
            this.type = type;
            this.order = order;
            this.date = date;
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDateHeader);
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderAmount, tvOrderMethod, tvOrderItems, tvOrderTime;
        ImageView ivPaymentIcon;
        com.google.android.material.card.MaterialCardView cvIconContainer;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderAmount = itemView.findViewById(R.id.tvOrderAmount);
            tvOrderMethod = itemView.findViewById(R.id.tvOrderMethod);
            tvOrderItems = itemView.findViewById(R.id.tvOrderItems);
            tvOrderTime = itemView.findViewById(R.id.tvOrderTime);
            ivPaymentIcon = itemView.findViewById(R.id.ivPaymentIcon);
            cvIconContainer = itemView.findViewById(R.id.cvIconContainer);
        }
    }
}
