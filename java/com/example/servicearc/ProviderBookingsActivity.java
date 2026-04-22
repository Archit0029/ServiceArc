package com.example.servicearc;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ProviderBookingsActivity extends AppCompatActivity {

    private RecyclerView bookingRecycler;
    private BookingAdapter adapter;
    private List<ServiceRequest> bookingList = new ArrayList<>();
    private FirebaseFirestore db;
    private TextView tabActive;
    private TextView tabHistory;
    private boolean showActive = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        setupBottomNavigation();

        bookingRecycler = findViewById(R.id.bookingRecycler);
        tabActive = findViewById(R.id.tabActive);
        tabHistory = findViewById(R.id.tabHistory);

        bookingRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingAdapter(bookingList);
        bookingRecycler.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadBookings();

        tabActive.setOnClickListener(v -> {
            showActive = true;
            tabActive.setTextColor(0xFFFFFFFF);
            tabHistory.setTextColor(0xFFBBBBBB);
            loadBookings();
        });

        tabHistory.setOnClickListener(v -> {
            showActive = false;
            tabHistory.setTextColor(0xFFFFFFFF);
            tabActive.setTextColor(0xFFBBBBBB);
            loadBookings();
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.provider_bottom_menu);
        bottomNav.setSelectedItemId(R.id.nav_provider_bookings);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_provider_home) {
                startActivity(new Intent(this, ProviderDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_provider_bookings) {
                return true;
            } else if (id == R.id.nav_provider_chat) {
                startActivity(new Intent(this, ProviderChatActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_provider_profile) {
                startActivity(new Intent(this, ProviderProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadBookings() {
        String uid = FirebaseAuth.getInstance().getUid();
        db.collection("service_requests")
                .whereEqualTo("providerId", uid)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    bookingList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ServiceRequest req = doc.toObject(ServiceRequest.class);
                        if (req == null) continue;

                        if (showActive) {
                            if (req.status.equals("accepted") || req.status.equals("in_progress")) {
                                bookingList.add(req);
                            }
                        } else {
                            if (req.status.equals("completed") || req.status.equals("cancelled")) {
                                bookingList.add(req);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
