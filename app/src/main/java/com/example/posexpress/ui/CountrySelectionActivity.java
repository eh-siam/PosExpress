package com.example.posexpress.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.posexpress.HostActivity;
import com.example.posexpress.R;
import com.example.posexpress.util.AppPreferences;
import com.example.posexpress.util.CountryConfig;

import java.util.ArrayList;
import java.util.List;

public class CountrySelectionActivity extends AppCompatActivity {

    private String selectedCountryCode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_country_selection);

        AutoCompleteTextView actvCountry = findViewById(R.id.actvCountry);
        List<CountryConfig> countries = CountryConfig.getAvailableCountries();
        List<String> countryNames = new ArrayList<>();
        for (CountryConfig c : countries) {
            countryNames.add(c.getName() + " (" + c.getCurrencyCode() + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, countryNames);
        actvCountry.setAdapter(adapter);

        actvCountry.setOnItemClickListener((parent, view, position, id) -> {
            selectedCountryCode = countries.get(position).getCode();
        });

        findViewById(R.id.btnContinue).setOnClickListener(v -> {
            if (selectedCountryCode == null) {
                Toast.makeText(this, "Please select a country", Toast.LENGTH_SHORT).show();
                return;
            }

            AppPreferences prefs = new AppPreferences(this);
            prefs.setCountryCode(selectedCountryCode);

            startActivity(new Intent(this, HostActivity.class));
            finish();
        });
    }
}
