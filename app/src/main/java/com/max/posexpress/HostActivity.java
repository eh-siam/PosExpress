package com.max.posexpress;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.max.posexpress.viewmodel.PosViewModel;

public class HostActivity extends AppCompatActivity {
    private PosViewModel viewModel;
    private TextView tvOfflineBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);

        tvOfflineBanner = findViewById(R.id.tvOfflineBanner);
        viewModel = new ViewModelProvider(this).get(PosViewModel.class);

        viewModel.getIsConnected().observe(this, isConnected -> {
            if (isConnected) {
                tvOfflineBanner.setVisibility(View.GONE);
            } else {
                tvOfflineBanner.setVisibility(View.VISIBLE);
            }
        });
    }
}
