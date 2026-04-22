package com.example.servicearc;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private EditText editName, editPhone, editAddress;
    private TextView txtProfileName, txtDisplayPhone, txtDisplayAddress;
    private TextView txtActiveBookings, txtCompletedBookings;
    private FrameLayout editProfileOverlay;
    private Button btnEditProfile, btnSaveProfile, btnCancelEdit;
    private TextView menuSupport, menuPrivacy, menuTerms, menuLogout;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String uid;

    public ProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Display views
        txtProfileName = view.findViewById(R.id.txtProfileName);
        txtDisplayPhone = view.findViewById(R.id.txtDisplayPhone);
        txtDisplayAddress = view.findViewById(R.id.txtDisplayAddress);
        txtActiveBookings = view.findViewById(R.id.txtActiveBookings);
        txtCompletedBookings = view.findViewById(R.id.txtCompletedBookings);

        // Edit views
        editProfileOverlay = view.findViewById(R.id.editProfileOverlay);
        editName = view.findViewById(R.id.editName);
        editPhone = view.findViewById(R.id.editPhone);
        editAddress = view.findViewById(R.id.editAddress);
        
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        btnCancelEdit = view.findViewById(R.id.btnCancelEdit);

        // Menu items
        menuSupport = view.findViewById(R.id.menuSupport);
        menuPrivacy = view.findViewById(R.id.menuPrivacy);
        menuTerms = view.findViewById(R.id.menuTerms);
        menuLogout = view.findViewById(R.id.menuLogout);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        uid = auth.getUid();

        loadProfile();
        loadBookingStats();

        // EDIT PROFILE LOGIC
        btnEditProfile.setOnClickListener(v -> {
            editProfileOverlay.setVisibility(View.VISIBLE);
        });

        btnCancelEdit.setOnClickListener(v -> {
            editProfileOverlay.setVisibility(View.GONE);
        });

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        // MENU OPTIONS
        menuSupport.setOnClickListener(v -> {
            if (getActivity() instanceof CustomerDashboardActivity) {
                ((CustomerDashboardActivity) getActivity()).openExploreWithFilter("Support");
            }
        });

        menuPrivacy.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Privacy Policy coming soon", Toast.LENGTH_SHORT).show();
        });

        menuTerms.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Terms & Conditions coming soon", Toast.LENGTH_SHORT).show();
        });

        menuLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    private void loadProfile() {
        if (uid == null) return;
        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String phone = documentSnapshot.getString("phone");
                String address = documentSnapshot.getString("address");

                // Update display
                txtProfileName.setText(name != null ? name : "Customer");
                txtDisplayPhone.setText(phone != null ? phone : "Not Provided");
                txtDisplayAddress.setText(address != null ? address : "Not Provided");

                // Pre-fill edit fields
                editName.setText(name);
                editPhone.setText(phone);
                editAddress.setText(address);
            }
        });
    }

    private void saveProfile() {
        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String address = editAddress.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("name", name);
        update.put("phone", phone);
        update.put("address", address);

        db.collection("users").document(uid).update(update).addOnSuccessListener(unused -> {
            Toast.makeText(getContext(), "Profile Updated", Toast.LENGTH_SHORT).show();
            
            // Update display UI
            txtProfileName.setText(name);
            txtDisplayPhone.setText(phone);
            txtDisplayAddress.setText(address);
            
            // Hide overlay
            editProfileOverlay.setVisibility(View.GONE);
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadBookingStats() {
        if (uid == null) return;
        db.collection("service_requests").whereEqualTo("customerId", uid).get().addOnSuccessListener(query -> {
            int active = 0, completed = 0;
            for (DocumentSnapshot doc : query) {
                String status = doc.getString("status");
                if (status == null) continue;
                if (status.equals("pending") || status.equals("accepted") || status.equals("in_progress") || status.equals("arrived")) active++;
                else if (status.equals("completed")) completed++;
            }
            txtActiveBookings.setText(String.valueOf(active));
            txtCompletedBookings.setText(String.valueOf(completed));
        });
    }
}
