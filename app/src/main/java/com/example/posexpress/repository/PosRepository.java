package com.example.posexpress.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import com.example.posexpress.model.Category;
import com.example.posexpress.model.Order;
import com.example.posexpress.model.Product;
import com.example.posexpress.util.AppPreferences;
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
    private final DatabaseReference categoriesRef;
    private final List<Product> productList = new ArrayList<>();
    private final List<Category> categoryList = new ArrayList<>();
    private final Map<Integer, Integer> cartQuantities = new HashMap<>();
    private String transactionJson;

    public interface DataCallback {
        void onDataChanged(List<Product> products);
        void onError(String error);
    }

    public interface CategoryCallback {
        void onCategoriesChanged(List<Category> categories);
        void onError(String error);
    }

    public interface OrderCallback {
        void onOrdersChanged(List<Order> orders);
        void onError(String error);
    }

    private PosRepository(Context context) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        AppPreferences prefs = new AppPreferences(context);
        String countryCode = prefs.getCountryCode();
        if (countryCode == null) countryCode = "BD"; // Default

        productsRef = db.getReference("products").child(countryCode);
        ordersRef = db.getReference("orders").child(countryCode);
        categoriesRef = db.getReference("categories").child(countryCode);
    }

    public static synchronized PosRepository getInstance(Context context) {
        if (instance == null) {
            instance = new PosRepository(context.getApplicationContext());
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

    /**
     * Fetches products in pages for pagination.
     */
    public void fetchProductsPage(int pageSize, String lastId, DataCallback callback) {
        com.google.firebase.database.Query query;
        if (lastId == null) {
            query = productsRef.orderByKey().limitToFirst(pageSize);
        } else {
            query = productsRef.orderByKey().startAfter(lastId).limitToFirst(pageSize);
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Product> page = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Product product = child.getValue(Product.class);
                    if (product != null) {
                        page.add(product);
                    }
                }
                callback.onDataChanged(page);
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

    /**
     * Fetches all categories from Firebase node 'categories'.
     */
    public void observeCategories(CategoryCallback callback) {
        categoriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Category category = child.getValue(Category.class);
                    if (category != null) {
                        categoryList.add(category);
                    }
                }
                callback.onCategoriesChanged(categoryList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void observeOrders(OrderCallback callback) {
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Order> orders = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Order order = child.getValue(Order.class);
                    if (order != null) {
                        orders.add(order);
                    }
                }
                callback.onOrdersChanged(orders);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void fetchOrdersPage(int pageSize, String lastOrderId, OrderCallback callback) {
        com.google.firebase.database.Query query;
        if (lastOrderId == null) {
            query = ordersRef.orderByKey().limitToLast(pageSize);
        } else {
            query = ordersRef.orderByKey().endBefore(lastOrderId).limitToLast(pageSize);
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Order> page = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Order order = child.getValue(Order.class);
                    if (order != null) {
                        page.add(order);
                    }
                }
                // Firebase limitToLast returns in ascending order, we want newest first eventually
                // but keeping repository logic simple.
                callback.onOrdersChanged(page);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void addCategory(Category category) {
        String id = categoriesRef.push().getKey();
        if (id != null) {
            category.setId(id);
            categoriesRef.child(id).setValue(category);
        }
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
    public void placeOrder(double totalAmount, String paymentMethod, List<String> cartItems, String transactionId,
                           String orderType, String customerName, String customerPhone, double subtotal,
                           double discountAmount, double taxAmount, double discountPercent, double taxPercent) {
        String orderId = ordersRef.push().getKey();
        if (orderId != null) {
            Order order = new Order(orderId, transactionId, totalAmount, paymentMethod, ServerValue.TIMESTAMP, cartItems,
                    orderType, customerName, customerPhone, subtotal, discountAmount, taxAmount, discountPercent, taxPercent);
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
