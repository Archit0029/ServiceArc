package com.example.servicearc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ProviderBookingsFragment extends Fragment {

    private RecyclerView bookingRecycler;
    private BookingAdapter adapter;
    private List<ServiceRequest> bookingList = new ArrayList<>();
    private FirebaseFirestore db;
    private TextView tabActive;
    private TextView tabHistory;
    private boolean showActive = true;

    public ProviderBookingsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_booking, container, false);

        // Hide bottom nav if it exists in this layout (since it's now in the Activity/Dashboard)
        View nav = view.findViewById(R.id.bottomNav);
        if (nav != null) nav.setVisibility(View.GONE);

        bookingRecycler = view.findViewById(R.id.bookingRecycler);
        tabActive = view.findViewById(R.id.tabActive);
        tabHistory = view.findViewById(R.id.tabHistory);

        bookingRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
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

        return view;
    }

    private void loadBookings() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        
        db.collection("service_requests")
                .whereEqualTo("providerId", uid)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    bookingList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ServiceRequest req = doc.toObject(ServiceRequest.class);
                        if (req == null) continue;
                        
                        // Fix: Manually set the requestId from the document ID
                        req.requestId = doc.getId();

                        if (showActive) {
                            // Showing all current/active requests including pending ones
                            if ("pending".equals(req.status) || "accepted".equals(req.status) || 
                                "in_progress".equals(req.status) || "arrived".equals(req.status)) {
                                bookingList.add(req);
                            }
                        } else {
                            if ("completed".equals(req.status) || "cancelled".equals(req.status) || "rejected".equals(req.status)) {
                                bookingList.add(req);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
