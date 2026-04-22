package com.example.servicearc;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExploreActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private ProviderAdapter adapter;
    private List<Provider> providerList = new ArrayList<>();
    private List<Provider> allProviders = new ArrayList<>(); // Master list for local filtering
    private FirebaseFirestore db;

    private EditText editSearch;
    private ChipGroup filterChipGroup;

    private FusedLocationProviderClient fusedLocationClient;
    private double userLat = 0, userLng = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupBottomNavigation();
        setupSearch();
        setupFilters();

        loadProviders();
        checkLocationPermission();
    }

    private void initViews() {
        recycler = findViewById(R.id.exploreRecycler);
        editSearch = findViewById(R.id.editSearch);
        filterChipGroup = findViewById(R.id.filterChipGroup);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProviderAdapter(providerList, provider -> {
            sendServiceRequest(provider);
        });
        recycler.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomMenu = findViewById(R.id.bottomNav);
        bottomMenu.setSelectedItemId(R.id.nav_explore);
        bottomMenu.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, CustomerDashboardActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_explore) {
                return true;
            } else if (itemId == R.id.nav_booking) {
                startActivity(new Intent(this, BookingActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_chat) {
                startActivity(new Intent(this, ChatActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void setupSearch() {
        // Real-time search as user types
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLocalProviders(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterLocalProviders(editSearch.getText().toString().trim());
                return true;
            }
            return false;
        });
    }

    private void setupFilters() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chipAll) {
                loadProviders();
            } else if (checkedId == R.id.chipTopRated) {
                loadTopRatedProviders();
            } else if (checkedId == R.id.chipNearMe) {
                loadNearbyProviders();
            }
        });
    }

    private void loadProviders() {
        db.collection("users")
                .whereEqualTo("role", "provider")
                .get()
                .addOnSuccessListener(query -> {
                    allProviders.clear();
                    providerList.clear();
                    for (DocumentSnapshot doc : query) {
                        Provider p = doc.toObject(Provider.class);
                        if (p != null) {
                            allProviders.add(p);
                            providerList.add(p);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    // Check for passed filter category
                    String category = getIntent().getStringExtra("filter_category");
                    if (category != null) {
                        editSearch.setText(category);
                        filterLocalProviders(category);
                    }
                });
    }

    private void filterLocalProviders(String query) {
        String lowerQuery = query.toLowerCase();
        providerList.clear();
        
        if (lowerQuery.isEmpty()) {
            providerList.addAll(allProviders);
        } else {
            for (Provider p : allProviders) {
                boolean matchesService = p.serviceType != null && p.serviceType.toLowerCase().contains(lowerQuery);
                boolean matchesName = p.name != null && p.name.toLowerCase().contains(lowerQuery);
                
                if (matchesService || matchesName) {
                    providerList.add(p);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadTopRatedProviders() {
        db.collection("users")
                .whereEqualTo("role", "provider")
                .whereGreaterThanOrEqualTo("rating", 4.5)
                .orderBy("rating", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    providerList.clear();
                    for (DocumentSnapshot doc : query) {
                        Provider p = doc.toObject(Provider.class);
                        if (p != null) providerList.add(p);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void loadNearbyProviders() {
        if (userLat == 0 || userLng == 0) {
            loadProviders(); // Fallback
            return;
        }
        db.collection("users")
                .whereEqualTo("role", "provider")
                .get()
                .addOnSuccessListener(query -> {
                    providerList.clear();
                    for (DocumentSnapshot doc : query) {
                        Provider p = doc.toObject(Provider.class);
                        if (p != null) {
                            float[] results = new float[1];
                            Location.distanceBetween(userLat, userLng, p.latitude, p.longitude, results);
                            p.distance = results[0] / 1000;
                            if (p.distance < 10) { // Within 10km
                                providerList.add(p);
                            }
                        }
                    }
                    Collections.sort(providerList, (p1, p2) -> Float.compare(p1.distance, p2.distance));
                    adapter.notifyDataSetChanged();
                });
    }

    private void sendServiceRequest(Provider provider) {
        if (userLat == 0 || userLng == 0) {
            Toast.makeText(this, "Location not ready", Toast.LENGTH_LONG).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(document -> {
            if (!document.exists()) return;

            Map<String, Object> request = new HashMap<>();
            request.put("customerId", uid);
            request.put("providerId", provider.uid);
            request.put("customerName", document.getString("name"));
            request.put("customerPhone", document.getString("phone"));
            request.put("customerAddress", document.getString("address"));
            request.put("customerLat", userLat);
            request.put("customerLng", userLng);
            request.put("status", "pending");
            request.put("timestamp", System.currentTimeMillis());
            request.put("expireAt", System.currentTimeMillis() + 300000);

            db.collection("service_requests").add(request).addOnSuccessListener(doc -> {
                Toast.makeText(this, "Request Sent", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    userLat = location.getLatitude();
                    userLng = location.getLongitude();
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission();
        }
    }
}
