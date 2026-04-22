package com.example.servicearc;

import android.content.Intent;
import android.location.Location;
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

public class BookingActivity extends AppCompatActivity {

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

        BottomNavigationView bottomMenu = findViewById(R.id.bottomNav);
        bottomMenu.setSelectedItemId(R.id.nav_booking);

        bottomMenu.setOnItemSelectedListener(item -> {

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, CustomerDashboardActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_explore) {
                startActivity(new Intent(this, ExploreActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_booking) {
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

        bookingRecycler = findViewById(R.id.bookingRecycler);
        tabActive = findViewById(R.id.tabActive);
        tabHistory = findViewById(R.id.tabHistory);

        bookingRecycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BookingAdapter(bookingList);
        bookingRecycler.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // Load bookings initially
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

    // ------------------------------------------
    // LOAD BOOKINGS
    // ------------------------------------------

    private void loadBookings() {

        String uid = FirebaseAuth.getInstance().getUid();

        db.collection("service_requests")
                .whereEqualTo("customerId", uid)
                .addSnapshotListener((value, error) -> {

                    if (value == null) return;

                    bookingList.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {

                        ServiceRequest req =
                                doc.toObject(ServiceRequest.class);

                        if (req == null) continue;

                        // Calculate distance if location available
                        if (req.providerLat != 0 && req.providerLng != 0) {

                            float[] result = new float[1];

                            Location.distanceBetween(
                                    req.customerLat,
                                    req.customerLng,
                                    req.providerLat,
                                    req.providerLng,
                                    result
                            );

                            req.distance = result[0] / 1000;
                        }

                        // FILTER ACTIVE BOOKINGS
                        if (showActive) {

                            if (req.status.equals("pending") ||
                                    req.status.equals("accepted") ||
                                    req.status.equals("in_progress")) {

                                bookingList.add(req);
                            }

                        }
                        // FILTER HISTORY BOOKINGS
                        else {

                            if (req.status.equals("completed") ||
                                    req.status.equals("expired") ||
                                    req.status.equals("cancelled")) {

                                bookingList.add(req);
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }
}
