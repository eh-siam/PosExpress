package com.example.posexpress.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.posexpress.R;
import com.example.posexpress.repository.PosRepository;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        view.findViewById(R.id.btnSwitchCountry).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CountrySelectionActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.btnSupport).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@payswift.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Support Request - POS Express");
            try {
                startActivity(Intent.createChooser(intent, "Send Email"));
            } catch (Exception e) {
                Toast.makeText(getContext(), "No email client found", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btnHelp).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Documentation coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Add Logout Logic
        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                PosRepository.resetInstance();
                startActivity(new Intent(requireContext(), LoginActivity.class));
                requireActivity().finishAffinity();
            });
        }
    }
}
