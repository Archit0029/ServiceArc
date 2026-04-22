package com.example.servicearc;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    private Button btnCustomer, btnProvider;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        // Back button
        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    getOnBackPressedDispatcher().onBackPressed()
            );
        }

        btnCustomer = findViewById(R.id.btnCustomer);
        btnProvider = findViewById(R.id.btnProvider);

        // Continue as Customer
        if (btnCustomer != null) {
            btnCustomer.setOnClickListener(v -> {
                Intent intent = new Intent(
                        RoleSelectionActivity.this,
                        CustomerDetailsActivity.class
                );
                startActivity(intent);
                finish(); // remove this screen from stack
            });
        }

        // Continue as Provider
        if (btnProvider != null) {
            btnProvider.setOnClickListener(v -> {
                Intent intent = new Intent(
                        RoleSelectionActivity.this,
                        ProviderDetailsActivity.class
                );
                startActivity(intent);
                finish(); // remove this screen from stack
            });
        }
    }
}