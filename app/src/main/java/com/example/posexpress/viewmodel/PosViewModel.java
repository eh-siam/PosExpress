package com.example.posexpress.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.posexpress.model.Product;
import com.example.posexpress.repository.PosRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PosViewModel extends ViewModel {

    private final PosRepository repository = PosRepository.getInstance();
    
    private final MutableLiveData<List<Product>> productList = new MutableLiveData<>();
    private final MutableLiveData<Map<Integer, Integer>> cartQuantities = new MutableLiveData<>();
    private final MutableLiveData<Double> totalAmount = new MutableLiveData<>(0.0);
    private final MutableLiveData<String> transactionJson = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public void startObservingData() {
        isLoading.setValue(true);
        repository.observeProducts(new PosRepository.DataCallback() {
            @Override
            public void onDataChanged(List<Product> products) {
                productList.setValue(new ArrayList<>(products));
                cartQuantities.setValue(repository.getCartQuantities());
                calculateTotal();
                isLoading.setValue(false);
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                isLoading.setValue(false);
            }
        });
    }

    public LiveData<List<Product>> getProductList() { return productList; }
    public LiveData<Map<Integer, Integer>> getCartQuantities() { return cartQuantities; }
    public LiveData<Double> getTotalAmount() { return totalAmount; }
    public LiveData<String> getTransactionJson() { return transactionJson; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void updateQuantity(Product product, int quantity) {
        repository.updateCartQuantity(product.getId(), quantity);
        cartQuantities.setValue(repository.getCartQuantities());
        calculateTotal();
    }

    public void addProduct(String name, String priceStr) {
        try {
            double price = Double.parseDouble(priceStr);
            int nextId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            repository.addProduct(new Product(nextId, name, price, "Default", ""));
        } catch (NumberFormatException ignored) {}
    }

    public void editProduct(Product product, String name, String priceStr) {
        try {
            double price = Double.parseDouble(priceStr);
            product.setName(name);
            product.setPrice(price);
            repository.updateProduct(product);
        } catch (NumberFormatException ignored) {}
    }

    public void deleteProduct(Product product) {
        repository.deleteProduct(product);
    }

    /**
     * Finalizes the order by saving to Firebase 'orders' node.
     */
    public void placeOrder(String method) {
        List<String> itemStrings = new ArrayList<>();
        List<Product> products = productList.getValue();
        Map<Integer, Integer> cart = repository.getCartQuantities();
        Double total = totalAmount.getValue();

        if (products != null && cart != null && total != null) {
            for (Product p : products) {
                Integer qty = cart.get(p.getId());
                if (qty != null && qty > 0) {
                    double subtotal = p.getPrice() * qty;
                    itemStrings.add(String.format(Locale.getDefault(), "%s x%d = $%.2f", p.getName(), qty, subtotal));
                }
            }
            // Save to Firebase
            repository.placeOrder(total, method, itemStrings);
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

    public void resetCart() {
        repository.resetCart();
        cartQuantities.setValue(repository.getCartQuantities());
        totalAmount.setValue(0.0);
    }
}
