package com.example.servicearc;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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

public class ExploreFragment extends Fragment {

    private RecyclerView recycler;
    private ProviderAdapter adapter;
    private List<Provider> providerList = new ArrayList<>();
    private List<Provider> allProviders = new ArrayList<>();
    private FirebaseFirestore db;
    private EditText editSearch;
    private ChipGroup filterChipGroup;
    private FusedLocationProviderClient fusedLocationClient;
    private double userLat = 0, userLng = 0;

    public ExploreFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_explore, container, false);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        recycler = view.findViewById(R.id.exploreRecycler);
        editSearch = view.findViewById(R.id.editSearch);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProviderAdapter(providerList, this::sendServiceRequest);
        recycler.setAdapter(adapter);

        setupSearch();
        setupFilters();
        loadProviders();
        checkLocationPermission();

        return view;
    }

    public void filterByCategory(String category) {
        if (editSearch != null) {
            editSearch.setText(category);
            filterLocalProviders(category);
        }
    }

    private void setupSearch() {
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLocalProviders(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterLocalProviders(String query) {
        String lowerQuery = query.toLowerCase();
        providerList.clear();
        if (lowerQuery.isEmpty()) {
            providerList.addAll(allProviders);
        } else {
            for (Provider p : allProviders) {
                if ((p.serviceType != null && p.serviceType.toLowerCase().contains(lowerQuery)) ||
                    (p.name != null && p.name.toLowerCase().contains(lowerQuery))) {
                    providerList.add(p);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupFilters() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipAll) loadProviders();
            else if (checkedId == R.id.chipTopRated) loadTopRatedProviders();
            else if (checkedId == R.id.chipNearMe) loadNearbyProviders();
        });
    }

    private void loadProviders() {
        db.collection("users").whereEqualTo("role", "provider").get().addOnSuccessListener(query -> {
            allProviders.clear();
            providerList.clear();
            for (DocumentSnapshot doc : query) {
                Provider p = doc.toObject(Provider.class);
                if (p != null) { allProviders.add(p); providerList.add(p); }
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void loadTopRatedProviders() {
        db.collection("users").whereEqualTo("role", "provider").whereGreaterThanOrEqualTo("rating", 4.5)
                .orderBy("rating", Query.Direction.DESCENDING).get().addOnSuccessListener(query -> {
            providerList.clear();
            for (DocumentSnapshot doc : query) {
                Provider p = doc.toObject(Provider.class);
                if (p != null) providerList.add(p);
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void loadNearbyProviders() {
        if (userLat == 0) { loadProviders(); return; }
        db.collection("users").whereEqualTo("role", "provider").get().addOnSuccessListener(query -> {
            providerList.clear();
            for (DocumentSnapshot doc : query) {
                Provider p = doc.toObject(Provider.class);
                if (p != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(userLat, userLng, p.latitude, p.longitude, results);
                    p.distance = results[0] / 1000;
                    if (p.distance < 10) providerList.add(p);
                }
            }
            Collections.sort(providerList, (p1, p2) -> Float.compare(p1.distance, p2.distance));
            adapter.notifyDataSetChanged();
        });
    }

    private void sendServiceRequest(Provider provider) {
        String uid = FirebaseAuth.getInstance().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(document -> {
            Map<String, Object> request = new HashMap<>();
            request.put("customerId", uid);
            request.put("providerId", provider.uid);
            request.put("status", "pending");
            request.put("timestamp", System.currentTimeMillis());
            request.put("expireAt", System.currentTimeMillis() + 300000);
            db.collection("service_requests").add(request).addOnSuccessListener(doc -> {
                Toast.makeText(getContext(), "Request Sent", Toast.LENGTH_SHORT).show();
                provider.requestExpireTime = System.currentTimeMillis() + 300000;
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) { userLat = location.getLatitude(); userLng = location.getLongitude(); }
            });
        }
    }
}
