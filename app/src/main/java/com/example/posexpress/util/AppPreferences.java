package com.example.posexpress.util;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String PREF_NAME = "PosExpressPrefs";
    private static final String KEY_COUNTRY_CODE = "selected_country_code";

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

    public CountryConfig getSelectedCountry() {
        String code = getCountryCode();
        if (code == null) return null;
        return CountryConfig.getByCode(code);
    }
}
