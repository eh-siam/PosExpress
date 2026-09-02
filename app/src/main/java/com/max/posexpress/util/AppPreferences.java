package com.max.posexpress.util;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String PREF_NAME = "PosExpressPrefs";
    private static final String KEY_COUNTRY_CODE = "selected_country_code";
    private static final String KEY_TAX_PERCENT = "tax_percent";
    private static final String KEY_DISCOUNT_PERCENT = "discount_percent";

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setCountryCode(String code) {
        prefs.edit().putString(KEY_COUNTRY_CODE, code).apply();
    }

    public String getCountryCode() {
        return prefs.getString(KEY_COUNTRY_CODE, null);
    }

    public void setTaxPercent(float tax) {
        prefs.edit().putFloat(KEY_TAX_PERCENT, tax).apply();
    }

    public float getTaxPercent() {
        return prefs.getFloat(KEY_TAX_PERCENT, 7.5f); // Default 7.5%
    }

    public void setDiscountPercent(float discount) {
        prefs.edit().putFloat(KEY_DISCOUNT_PERCENT, discount).apply();
    }

    public float getDiscountPercent() {
        return prefs.getFloat(KEY_DISCOUNT_PERCENT, 0.0f); // Default 0%
    }

    public CountryConfig getSelectedCountry() {
        String code = getCountryCode();
        if (code == null) return null;
        return CountryConfig.getByCode(code);
    }
}
