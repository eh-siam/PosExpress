package com.max.posexpress.model;

import androidx.annotation.Keep;
import java.util.List;

/**
 * Order Model to store transaction details in Firebase.
 */
@Keep
public class Order {
    private String orderId;
    private String transactionId;
    private double totalAmount;
    private String paymentMethod;
    private Object timestamp; // Using Object for ServerValue.TIMESTAMP
    private List<String> items; // List of item descriptions (Name xQty = $Total)

    // New Fields for Detailed Billing
    private String orderType; // Dine-in, Takeaway, Delivery
    private String customerName;
    private String customerPhone;
    private double subtotal;
    private double discountAmount;
    private double taxAmount;
    private double discountPercent;
    private double taxPercent;

    public Order() {}

    public Order(String orderId, String transactionId, double totalAmount, String paymentMethod, Object timestamp, List<String> items) {
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.timestamp = timestamp;
        this.items = items;
    }

    // Extended Constructor
    public Order(String orderId, String transactionId, double totalAmount, String paymentMethod, Object timestamp, List<String> items,
                 String orderType, String customerName, String customerPhone, double subtotal,
                 double discountAmount, double taxAmount, double discountPercent, double taxPercent) {
        this(orderId, transactionId, totalAmount, paymentMethod, timestamp, items);
        this.orderType = orderType;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.discountPercent = discountPercent;
        this.taxPercent = taxPercent;
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Object totalAmount) {
        if (totalAmount instanceof Number) {
            this.totalAmount = ((Number) totalAmount).doubleValue();
        } else if (totalAmount instanceof String) {
            try {
                this.totalAmount = Double.parseDouble((String) totalAmount);
            } catch (NumberFormatException e) {
                this.totalAmount = 0.0;
            }
        }
    }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Object getTimestamp() { return timestamp; }
    public void setTimestamp(Object timestamp) { this.timestamp = timestamp; }

    public List<String> getItems() { return items; }
    public void setItems(List<String> items) { this.items = items; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }
    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }
    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }
    public double getTaxPercent() { return taxPercent; }
    public void setTaxPercent(double taxPercent) { this.taxPercent = taxPercent; }
}
