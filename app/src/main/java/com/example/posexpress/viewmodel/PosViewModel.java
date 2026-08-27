package com.example.posexpress.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.posexpress.model.Category;
import com.example.posexpress.model.Order;
import com.example.posexpress.model.Product;
import com.example.posexpress.repository.PosRepository;
import com.example.posexpress.util.AppPreferences;
import com.example.posexpress.util.CountryConfig;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PosViewModel extends AndroidViewModel {

    private final PosRepository repository;
    
    private final MutableLiveData<List<Product>> productList = new MutableLiveData<>(new ArrayList<>());
    private final MediatorLiveData<List<Product>> filteredProductList = new MediatorLiveData<>();
    private final MutableLiveData<List<Category>> categoryList = new MutableLiveData<>();
    private final MutableLiveData<Map<Integer, Integer>> cartQuantities = new MutableLiveData<>();
    private final MutableLiveData<Double> totalAmount = new MutableLiveData<>(0.0);
    private final MutableLiveData<String> transactionJson = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>("All");

    // Dashboard Data
    private final MutableLiveData<List<Order>> allOrders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Double> todaySales = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> todayTxnCount = new MutableLiveData<>(0);
    private final MutableLiveData<Map<String, Double>> methodStats = new MutableLiveData<>(new HashMap<>());
    private boolean isObservingOrders = false;

    // Order Pagination State
    private final MutableLiveData<List<Order>> paginatedOrders = new MutableLiveData<>(new ArrayList<>());
    private final int ORDER_PAGE_SIZE = 15;
    private String lastLoadedOrderId = null;
    private boolean isLastOrderPage = false;
    private final MutableLiveData<Boolean> isLoadingMoreOrders = new MutableLiveData<>(false);

    // Pagination State
    private final int PAGE_SIZE = 10;
    private String lastLoadedId = null;
    private boolean isLastPage = false;
    private final MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>(false);

    public PosViewModel(@NonNull Application application) {
        super(application);
        this.repository = PosRepository.getInstance(application);
        filteredProductList.addSource(productList, products -> applyFilter());
        filteredProductList.addSource(selectedCategory, category -> applyFilter());
    }

    public void startObservingData() {
        // If data is already loaded, don't restart observation
        List<Product> currentProducts = productList.getValue();
        if (currentProducts != null && !currentProducts.isEmpty()) {
            return;
        }

        isLoading.setValue(true);
        lastLoadedId = null;
        isLastPage = false;
        // productList is already initialized
        
        // Observe Categories
        repository.observeCategories(new PosRepository.CategoryCallback() {
            @Override
            public void onCategoriesChanged(List<Category> categories) {
                categoryList.setValue(new ArrayList<>(categories));
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue("Categories: " + error);
            }
        });

        // Load First Page of Products
        loadNextPage();
    }

    public void loadNextPage() {
        if (isLastPage || Boolean.TRUE.equals(isLoadingMore.getValue())) {
            return;
        }

        // Only block if we are already loading and it's NOT the first page
        if (lastLoadedId != null && Boolean.TRUE.equals(isLoading.getValue())) {
            return;
        }

        if (lastLoadedId != null) {
            isLoadingMore.setValue(true);
        }

        repository.fetchProductsPage(PAGE_SIZE, lastLoadedId, new PosRepository.DataCallback() {
            @Override
            public void onDataChanged(List<Product> page) {
                List<Product> currentList = productList.getValue();
                if (currentList == null) currentList = new ArrayList<>();
                
                if (page.isEmpty()) {
                    isLastPage = true;
                } else {
                    currentList.addAll(page);
                    productList.setValue(currentList);
                    lastLoadedId = String.valueOf(page.get(page.size() - 1).getId());
                    if (page.size() < PAGE_SIZE) {
                        isLastPage = true;
                    }
                }
                
                calculateTotal();
                isLoading.setValue(false);
                isLoadingMore.setValue(false);
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                isLoading.setValue(false);
                isLoadingMore.setValue(false);
            }
        });
    }

    public LiveData<List<Product>> getProductList() { return filteredProductList; }
    public LiveData<List<Category>> getCategoryList() { return categoryList; }
    public LiveData<Map<Integer, Integer>> getCartQuantities() { return cartQuantities; }
    public LiveData<Double> getTotalAmount() { return totalAmount; }
    public LiveData<String> getTransactionJson() { return transactionJson; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsLoadingMore() { return isLoadingMore; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSelectedCategory() { return selectedCategory; }

    public String getCurrencySymbol() {
        CountryConfig country = new AppPreferences(getApplication()).getSelectedCountry();
        return country != null ? country.getCurrencySymbol() : "$";
    }

    public CountryConfig getSelectedCountry() {
        return new AppPreferences(getApplication()).getSelectedCountry();
    }

    // Dashboard Getters
    public LiveData<Double> getTodaySales() { return todaySales; }
    public LiveData<Integer> getTodayTxnCount() { return todayTxnCount; }
    public LiveData<Map<String, Double>> getMethodStats() { return methodStats; }
    public LiveData<List<Order>> getAllOrders() { return allOrders; }
    public LiveData<List<Order>> getPaginatedOrders() { return paginatedOrders; }
    public LiveData<Boolean> getIsLoadingMoreOrders() { return isLoadingMoreOrders; }

    public void loadNextOrderPage() {
        if (isLastOrderPage || Boolean.TRUE.equals(isLoadingMoreOrders.getValue())) {
            return;
        }

        isLoadingMoreOrders.setValue(true);

        repository.fetchOrdersPage(ORDER_PAGE_SIZE, lastLoadedOrderId, new PosRepository.OrderCallback() {
            @Override
            public void onOrdersChanged(List<Order> page) {
                List<Order> currentList = paginatedOrders.getValue();
                if (currentList == null) currentList = new ArrayList<>();

                if (page.isEmpty()) {
                    isLastOrderPage = true;
                } else {
                    // Firebase limitToLast returns ascending, but for UI we want descending
                    // Sort the page itself descending
                    page.sort((o1, o2) -> {
                        long t1 = (o1.getTimestamp() instanceof Long) ? (Long) o1.getTimestamp() : 0;
                        long t2 = (o2.getTimestamp() instanceof Long) ? (Long) o2.getTimestamp() : 0;
                        return Long.compare(t2, t1);
                    });

                    currentList.addAll(page);
                    paginatedOrders.setValue(new ArrayList<>(currentList));
                    
                    // The last item in limitToLast ascending is the 'newest' in that chunk
                    // But we are querying with endBefore(lastOrderId).
                    // So we need the 'oldest' key in the current batch to be the anchor for the next endBefore.
                    // In ascending, that's index 0.
                    lastLoadedOrderId = page.get(page.size() - 1).getOrderId();
                    // Wait, if it's limitToLast, index 0 is the smallest (oldest) key in that batch.
                    // Let's find the absolute oldest key in this batch.
                    String oldestKey = page.get(0).getOrderId();
                    for(Order o : page) {
                        if (o.getOrderId().compareTo(oldestKey) < 0) {
                            oldestKey = o.getOrderId();
                        }
                    }
                    lastLoadedOrderId = oldestKey;

                    if (page.size() < ORDER_PAGE_SIZE) {
                        isLastOrderPage = true;
                    }
                }
                isLoadingMoreOrders.setValue(false);
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                isLoadingMoreOrders.setValue(false);
            }
        });
    }

    public void resetOrderPagination() {
        lastLoadedOrderId = null;
        isLastOrderPage = false;
        paginatedOrders.setValue(new ArrayList<>());
        loadNextOrderPage();
    }

    public void startObservingOrders() {
        if (isObservingOrders) return;
        
        isObservingOrders = true;
        repository.observeOrders(new PosRepository.OrderCallback() {
            @Override
            public void onOrdersChanged(List<Order> orders) {
                allOrders.setValue(orders);
                calculateDashboardStats(orders);
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue("Orders: " + error);
            }
        });
    }

    private void calculateDashboardStats(List<Order> orders) {
        double totalSales = 0.0;
        int txnCount = 0;
        Map<String, Double> stats = new HashMap<>();
        stats.put("EMV Card Payment", 0.0);
        stats.put("e-Wallet / QR Code", 0.0);
        stats.put("Cash Payment", 0.0);
        stats.put("bKash Payment", 0.0);

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long todayStart = today.getTimeInMillis();

        for (Order order : orders) {
            long timestamp = 0;
            if (order.getTimestamp() instanceof Long) {
                timestamp = (Long) order.getTimestamp();
            }

            if (timestamp >= todayStart) {
                totalSales += order.getTotalAmount();
                txnCount++;

                String method = order.getPaymentMethod();
                if (method != null) {
                    Double current = stats.get(method);
                    if (current == null) current = 0.0;
                    stats.put(method, current + order.getTotalAmount());
                }
            }
        }

        todaySales.setValue(totalSales);
        todayTxnCount.setValue(txnCount);
        methodStats.setValue(stats);
    }

    public void updateQuantity(Product product, int quantity) {
        repository.updateCartQuantity(product.getId(), quantity);
        cartQuantities.setValue(repository.getCartQuantities());
        calculateTotal();
    }

    public void addProduct(String name, String priceStr, String category) {
        checkAndAddCategory(category);
        try {
            double price = Double.parseDouble(priceStr);
            int nextId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            repository.addProduct(new Product(nextId, name, price, category, ""));
        } catch (NumberFormatException ignored) {}
    }

    public void editProduct(Product product, String name, String priceStr, String category) {
        checkAndAddCategory(category);
        try {
            double price = Double.parseDouble(priceStr);
            product.setName(name);
            product.setPrice(price);
            product.setCategory(category);
            repository.updateProduct(product);
        } catch (NumberFormatException ignored) {}
    }

    private void checkAndAddCategory(String categoryName) {
        List<Category> currentCategories = categoryList.getValue();
        boolean exists = false;
        if (currentCategories != null) {
            for (Category c : currentCategories) {
                if (c.getName().equalsIgnoreCase(categoryName)) {
                    exists = true;
                    break;
                }
            }
        }
        if (!exists && !categoryName.equalsIgnoreCase("General")) {
            repository.addCategory(new Category(null, categoryName));
        }
    }

    public void deleteProduct(Product product) {
        repository.deleteProduct(product);
    }

    public boolean isProductNameDuplicate(String name) {
        List<Product> allProducts = productList.getValue();
        if (allProducts == null) return false;
        for (Product p : allProducts) {
            if (p.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finalizes the order by saving to Firebase 'orders' node.
     */
    public void placeOrder(String method, String transactionId, String orderType, String customerName, String customerPhone,
                           double subtotal, double discountAmount, double taxAmount, double discountPercent, double taxPercent, double total) {
        List<String> itemStrings = new ArrayList<>();
        List<Product> products = productList.getValue();
        Map<Integer, Integer> cart = repository.getCartQuantities();

        if (products != null && cart != null) {
            for (Product p : products) {
                Integer qty = cart.get(p.getId());
                if (qty != null && qty > 0) {
                    double itemSubtotal = p.getPrice() * qty;
                    itemStrings.add(String.format(Locale.getDefault(), "%s x%d = $%.2f", p.getName(), qty, itemSubtotal));
                }
            }
            // Save to Firebase
            repository.placeOrder(total, method, itemStrings, transactionId, orderType, customerName, customerPhone,
                    subtotal, discountAmount, taxAmount, discountPercent, taxPercent);
        }
    }

    private void calculateTotal() {
        double total = 0.0;
        List<Product> products = productList.getValue();
        Map<Integer, Integer> cart = repository.getCartQuantities();
        if (products != null && cart != null) {
            for (Product p : products) {
                Integer qty = cart.get(p.getId());
                if (qty != null && qty > 0) {
                    total += (p.getPrice() * qty);
                }
            }
        }
        totalAmount.setValue(total);
    }

    public void setTransactionJson(String json) {
        repository.setTransactionJson(json);
        transactionJson.setValue(json);
    }

    public void setSelectedCategory(String category) {
        selectedCategory.setValue(category);
        applyFilter();
    }

    private void applyFilter() {
        List<Product> allProducts = productList.getValue();
        if (allProducts == null) {
            filteredProductList.setValue(new ArrayList<>());
            return;
        }

        String filter = selectedCategory.getValue();
        
        if (filter == null || filter.equals("All")) {
            filteredProductList.setValue(new ArrayList<>(allProducts));
        } else {
            List<Product> filtered = new ArrayList<>();
            for (Product p : allProducts) {
                if (p.getCategory() != null && p.getCategory().equalsIgnoreCase(filter)) {
                    filtered.add(p);
                }
            }
            filteredProductList.setValue(filtered);
        }
    }

    public void resetCart() {
        repository.resetCart();
        cartQuantities.setValue(repository.getCartQuantities());
        totalAmount.setValue(0.0);
    }
}
