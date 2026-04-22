package com.example.servicearc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PhoneEntryActivity extends AppCompatActivity {

    private EditText etPhone;
    private Button btnContinue;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_entry);

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        etPhone = findViewById(R.id.etPhone);
        btnContinue = findViewById(R.id.btnContinue);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnContinue.setOnClickListener(v -> savePhoneNumber());
    }

    private void savePhoneNumber() {

        String phone = etPhone.getText().toString().trim();

        if (phone.isEmpty()) {
            etPhone.setError("Mobile number required");
            return;
        }

        if (phone.length() != 10) {
            etPhone.setError("Enter valid 10 digit number");
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnContinue.setEnabled(false);

        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> userData = new HashMap<>();
        userData.put("phone", phone);
        userData.put("role", "");
        userData.put("name", "");
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("users")
                .document(uid)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    progressBar.setVisibility(View.GONE);

                    Intent intent = new Intent(
                            PhoneEntryActivity.this,
                            RoleSelectionActivity.class
                    );

                    startActivity(intent);
                })
                .addOnFailureListener(e -> {

                    progressBar.setVisibility(View.GONE);
                    btnContinue.setEnabled(true);

                    Log.e("PHONE_ENTRY", "Firestore Error", e);

                    Toast.makeText(
                            PhoneEntryActivity.this,
                            "Database error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}