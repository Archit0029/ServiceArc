package com.example.servicearc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ProviderProfileActivity extends AppCompatActivity {

    private EditText editName, editPhone, editAddress;
    private Button btnEditProfile, btnSaveProfile;
    private TextView menuLogout;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String providerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_profile);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        providerId = auth.getUid();

        editName = findViewById(R.id.editName);
        editPhone = findViewById(R.id.editPhone);
        editAddress = findViewById(R.id.editAddress);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        menuLogout = findViewById(R.id.menuLogout);

        if (editName == null || editPhone == null || editAddress == null || btnEditProfile == null || btnSaveProfile == null || menuLogout == null) {
            Toast.makeText(this, "UI components missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        disableFields();
        loadProfile();
        setupBottomNavigation();

        btnEditProfile.setOnClickListener(v -> {
            enableFields();
            btnEditProfile.setVisibility(View.GONE);
            btnSaveProfile.setVisibility(View.VISIBLE);
        });

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        menuLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadProfile() {
        if (providerId == null) return;
        db.collection("users")
                .document(providerId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        editName.setText(document.getString("name"));
                        editPhone.setText(document.getString("phone"));
                        editAddress.setText(document.getString("address"));
                        
                        TextView profileName = findViewById(R.id.txtProfileName);
                        if (profileName != null) profileName.setText(document.getString("name"));
                        
                        TextView profileService = findViewById(R.id.txtProfileService);
                        if (profileService != null) profileService.setText(document.getString("serviceType"));
                    }
                });
    }

    private void saveProfile() {
        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String address = editAddress.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("phone", phone);
        data.put("address", address);

        db.collection("users")
                .document(providerId)
                .update(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                    disableFields();
                    btnSaveProfile.setVisibility(View.GONE);
                    btnEditProfile.setVisibility(View.VISIBLE);
                    loadProfile(); // Refresh UI
                });
    }

    private void enableFields() {
        editName.setEnabled(true);
        editPhone.setEnabled(true);
        editAddress.setEnabled(true);
    }

    private void disableFields() {
        editName.setEnabled(false);
        editPhone.setEnabled(false);
        editAddress.setEnabled(false);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;
        
        bottomNav.setSelectedItemId(R.id.nav_provider_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_provider_home) {
                startActivity(new Intent(this, ProviderDashboardActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;
            } else if (id == R.id.nav_provider_bookings) {
                startActivity(new Intent(this, ProviderBookingsActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;
            } else if (id == R.id.nav_provider_chat) {
                startActivity(new Intent(this, ProviderChatActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;
            } else if (id == R.id.nav_provider_profile) {
                return true;
            }
            return false;
        });
    }
}