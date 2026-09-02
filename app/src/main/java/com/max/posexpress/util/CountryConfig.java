package com.max.posexpress.util;

import java.util.ArrayList;
import java.util.List;

public class CountryConfig {
    private final String code;
    private final String name;
    private final String currencySymbol;
    private final String currencyCode;
    private final boolean supportsBkash;

    public CountryConfig(String code, String name, String currencySymbol, String currencyCode, boolean supportsBkash) {
        this.code = code;
        this.name = name;
        this.currencySymbol = currencySymbol;
        this.currencyCode = currencyCode;
        this.supportsBkash = supportsBkash;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getCurrencySymbol() { return currencySymbol; }
    public String getCurrencyCode() { return currencyCode; }
    public boolean isSupportsBkash() { return supportsBkash; }

    public static List<CountryConfig> getAvailableCountries() {
        List<CountryConfig> countries = new ArrayList<>();
        countries.add(new CountryConfig("BD", "Bangladesh", "৳", "BDT", true));
        countries.add(new CountryConfig("US", "United States", "$", "USD", false));
        countries.add(new CountryConfig("UK", "United Kingdom", "£", "GBP", false));
        countries.add(new CountryConfig("EU", "Germany (Euro)", "€", "EUR", false));
        countries.add(new CountryConfig("IN", "India", "₹", "INR", false));
        countries.add(new CountryConfig("AE", "United Arab Emirates", "AED ", "AED", false));
        return countries;
    }

    public static CountryConfig getByCode(String code) {
        for (CountryConfig country : getAvailableCountries()) {
            if (country.getCode().equalsIgnoreCase(code)) {
                return country;
            }
        }
        return getAvailableCountries().get(0); // Default to BD
    }
}
