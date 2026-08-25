package com.example.posexpress.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.posexpress.model.Category;
import com.example.posexpress.model.Product;
import com.example.posexpress.repository.PosRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PosViewModel extends ViewModel {

    private final PosRepository repository = PosRepository.getInstance();
    
    private final MutableLiveData<List<Product>> productList = new MutableLiveData<>();
    private final MutableLiveData<List<Product>> filteredProductList = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categoryList = new MutableLiveData<>();
    private final MutableLiveData<Map<Integer, Integer>> cartQuantities = new MutableLiveData<>();
    private final MutableLiveData<Double> totalAmount = new MutableLiveData<>(0.0);
    private final MutableLiveData<String> transactionJson = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>("All");

    // Pagination State
    private final int PAGE_SIZE = 10;
    private String lastLoadedId = null;
    private boolean isLastPage = false;
    private final MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>(false);

    public void startObservingData() {
        isLoading.setValue(true);
        lastLoadedId = null;
        isLastPage = false;
        productList.setValue(new ArrayList<>()); // Reset list
        
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
                
                applyFilter();
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
        String filter = selectedCategory.getValue();
        
        if (allProducts == null) return;
        
        if (filter == null || filter.equals("All")) {
            filteredProductList.setValue(allProducts);
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
