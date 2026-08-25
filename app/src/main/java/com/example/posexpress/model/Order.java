package com.example.posexpress.model;

import androidx.annotation.Keep;
import java.util.List;
import java.util.Map;

/**
 * Order Model to store transaction details in Firebase.
 */
@Keep
public class Order {
    private String orderId;
    private double totalAmount;
    private String paymentMethod;
    private Object timestamp; // Using Object for ServerValue.TIMESTAMP
    private List<String> items; // List of item descriptions (Name xQty = $Total)

    public Order() {}

    public Order(String orderId, double totalAmount, String paymentMethod, Object timestamp, List<String> items) {
        this.orderId = orderId;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.timestamp = timestamp;
        this.items = items;
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Object getTimestamp() { return timestamp; }
    public void setTimestamp(Object timestamp) { this.timestamp = timestamp; }

    public List<String> getItems() { return items; }
    public void setItems(List<String> items) { this.items = items; }
}
