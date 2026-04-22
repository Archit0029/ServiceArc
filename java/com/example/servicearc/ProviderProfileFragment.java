package com.example.servicearc;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProviderProfileFragment extends Fragment {

    private TextView txtProfileName, txtProfileService, txtDisplayPhone, txtDisplayAddress;
    private TextView txtProfileRating, txtProfileJobs, txtProfileEarnings;
    private Button btnEditProfile;
    private TextView menuSupport, menuPrivacy, menuTerms, menuLogout;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String providerId;

    public ProviderProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_provider_profile, container, false);

        // Hide bottom nav if it exists in this layout (not needed in ViewPager fragment)
        View nav = view.findViewById(R.id.bottomNav);
        if (nav != null) nav.setVisibility(View.GONE);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        providerId = auth.getUid();

        // Initialize Views with corrected IDs from activity_provider_profile.xml
        txtProfileName = view.findViewById(R.id.editName);
        txtProfileService = view.findViewById(R.id.txtProfileService);
        txtDisplayPhone = view.findViewById(R.id.editPhone);
        txtDisplayAddress = view.findViewById(R.id.editAddress);
        
        txtProfileRating = view.findViewById(R.id.txtProfileRating);
        txtProfileJobs = view.findViewById(R.id.txtProfileJobs);
        txtProfileEarnings = view.findViewById(R.id.txtProfileEarnings);
        
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        
        menuSupport = view.findViewById(R.id.menuSupport);
        menuPrivacy = view.findViewById(R.id.menuPrivacy);
        menuTerms = view.findViewById(R.id.menuTerms);
        menuLogout = view.findViewById(R.id.menuLogout);

        loadProfile();
        loadStats();

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showEditDialog());
        }

        if (menuSupport != null) {
            menuSupport.setOnClickListener(v -> {
                if (getActivity() instanceof ProviderDashboardActivity) {
                    ((ProviderDashboardActivity) getActivity()).navigateToTab(R.id.nav_provider_chat);
                }
            });
        }

        if (menuPrivacy != null) menuPrivacy.setOnClickListener(v -> Toast.makeText(getContext(), "Privacy Policy coming soon", Toast.LENGTH_SHORT).show());
        if (menuTerms != null) menuTerms.setOnClickListener(v -> Toast.makeText(getContext(), "Terms & Conditions coming soon", Toast.LENGTH_SHORT).show());

        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                auth.signOut();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        return view;
    }

    private void loadProfile() {
        if (providerId == null) return;
        db.collection("users").document(providerId).get().addOnSuccessListener(document -> {
            if (document.exists() && isAdded()) {
                String name = document.getString("name");
                String phone = document.getString("phone");
                String address = document.getString("address");
                String service = document.getString("serviceType");

                if (txtProfileName != null) txtProfileName.setText(name != null ? name : "N/A");
                if (txtProfileService != null) txtProfileService.setText(service != null ? service : "Provider");
                if (txtDisplayPhone != null) txtDisplayPhone.setText(phone != null ? phone : "N/A");
                if (txtDisplayAddress != null) txtDisplayAddress.setText(address != null ? address : "Address not set");
            }
        });
    }

    private void loadStats() {
        if (providerId == null) return;
        db.collection("service_requests").whereEqualTo("providerId", providerId).whereEqualTo("status", "completed").get()
                .addOnSuccessListener(query -> {
                    if (!isAdded()) return;
                    int jobs = query.size();
                    int earnings = 0;
                    for (DocumentSnapshot doc : query) {
                        Long price = doc.getLong("price");
                        if (price != null) earnings += price;
                    }
                    if (txtProfileJobs != null) txtProfileJobs.setText(String.valueOf(jobs));
                    if (txtProfileEarnings != null) txtProfileEarnings.setText("₹" + (earnings >= 1000 ? (earnings/1000.0) + "k" : earnings));
                });
    }

    private void showEditDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText editName = dialogView.findViewById(R.id.dialogEditName);
        EditText editPhone = dialogView.findViewById(R.id.dialogEditPhone);
        EditText editAddress = dialogView.findViewById(R.id.dialogEditAddress);
        Button btnSave = dialogView.findViewById(R.id.btnSaveDialog);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Pre-fill
        if (txtProfileName != null) editName.setText(txtProfileName.getText());
        if (txtDisplayPhone != null) editPhone.setText(txtDisplayPhone.getText());
        if (txtDisplayAddress != null) editAddress.setText(txtDisplayAddress.getText());

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme).setView(dialogView).create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String address = editAddress.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                Toast.makeText(getContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("phone", phone);
            data.put("address", address);

            db.collection("users").document(providerId).update(data).addOnSuccessListener(unused -> {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Profile Updated", Toast.LENGTH_SHORT).show();
                    loadProfile();
                }
                dialog.dismiss();
            });
        });

        dialog.show();
    }
}
