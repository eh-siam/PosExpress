package com.example.posexpress.repository;

import androidx.annotation.NonNull;
import com.example.posexpress.model.Order;
import com.example.posexpress.model.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository handling all Firebase Realtime Database operations.
 */
public class PosRepository {
    private static PosRepository instance;
    private final DatabaseReference productsRef;
    private final DatabaseReference ordersRef;
    private final List<Product> productList = new ArrayList<>();
    private final Map<Integer, Integer> cartQuantities = new HashMap<>();
    private String transactionJson;

    public interface DataCallback {
        void onDataChanged(List<Product> products);
        void onError(String error);
    }

    private PosRepository() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        productsRef = db.getReference("products");
        ordersRef = db.getReference("orders");
    }

    public static synchronized PosRepository getInstance() {
        if (instance == null) {
            instance = new PosRepository();
        }
        return instance;
    }

    /**
     * Fetches all products from Firebase node 'products' using real-time listener.
     */
    public void observeProducts(DataCallback callback) {
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Product product = child.getValue(Product.class);
                    if (product != null) {
                        productList.add(product);
                    }
                }
                callback.onDataChanged(productList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public List<Product> getProducts() {
        return productList;
    }

    public void addProduct(Product product) {
        productsRef.child(String.valueOf(product.getId())).setValue(product);
    }

    public void updateProduct(Product product) {
        productsRef.child(String.valueOf(product.getId())).setValue(product);
    }

    public void deleteProduct(Product product) {
        productsRef.child(String.valueOf(product.getId())).removeValue();
        cartQuantities.remove(product.getId());
    }

    /**
     * Saves order details under the Firebase 'orders' node with a unique ID and timestamp.
     */
    public void placeOrder(double totalAmount, String paymentMethod, List<String> cartItems) {
        String orderId = ordersRef.push().getKey();
        if (orderId != null) {
            Order order = new Order(orderId, totalAmount, paymentMethod, ServerValue.TIMESTAMP, cartItems);
            ordersRef.child(orderId).setValue(order);
        }
    }

    public Map<Integer, Integer> getCartQuantities() {
        return cartQuantities;
    }

    public void updateCartQuantity(int productId, int quantity) {
        if (quantity <= 0) {
            cartQuantities.remove(productId);
        } else {
            cartQuantities.put(productId, quantity);
        }
    }

    public void resetCart() {
        cartQuantities.clear();
        transactionJson = null;
    }

    public String getTransactionJson() {
        return transactionJson;
    }

    public void setTransactionJson(String json) {
        this.transactionJson = json;
    }
}
