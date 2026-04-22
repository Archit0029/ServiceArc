package com.example.servicearc;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProviderHomeFragment extends Fragment implements OnMapReadyCallback {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private MaterialSwitch switchOnline;
    private TextView txtBookings, txtEarnings, txtRequests, txtWelcome, txtOnlineStatus, txtRating, txtLocation;
    private RecyclerView requestRecycler;
    private ProviderRequestAdapter adapter;
    private List<ServiceRequest> requestList = new ArrayList<>();
    private GoogleMap mMap;
    private String providerId;
    private double currentCustomerLat = 0;
    private double currentCustomerLng = 0;
    private String activeRequestId = null;
    private Button btnNavigate, btnArrived, btnComplete, btnHelp;
    private SwipeRefreshLayout swipeRefresh;
    private ImageView profileBtn;

    // Assignment UI Elements
    private View cardAssignment;
    private TextView txtAssignmentTitle, txtClientName, txtClientPhone, txtClientAddress;

    public ProviderHomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_provider_home, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        providerId = auth.getUid();

        switchOnline = view.findViewById(R.id.switchOnline);
        txtOnlineStatus = view.findViewById(R.id.txtOnlineStatus);
        txtBookings = view.findViewById(R.id.txtBookings);
        txtEarnings = view.findViewById(R.id.txtEarnings);
        txtRequests = view.findViewById(R.id.txtRequests);
        txtWelcome = view.findViewById(R.id.txtWelcome);
        txtRating = view.findViewById(R.id.txtRating);
        txtLocation = view.findViewById(R.id.txtLocation);
        btnNavigate = view.findViewById(R.id.btnNavigate);
        btnArrived = view.findViewById(R.id.btnArrived);
        btnComplete = view.findViewById(R.id.btnComplete);
        btnHelp = view.findViewById(R.id.btnHelp);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        profileBtn = view.findViewById(R.id.profileBtn);

        // Initialize Assignment UI
        cardAssignment = view.findViewById(R.id.cardAssignment);
        txtAssignmentTitle = view.findViewById(R.id.txtAssignmentTitle);
        txtClientName = view.findViewById(R.id.txtClientName);
        txtClientPhone = view.findViewById(R.id.txtClientPhone);
        txtClientAddress = view.findViewById(R.id.txtClientAddress);

        requestRecycler = view.findViewById(R.id.requestRecycler);
        requestRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProviderRequestAdapter(requestList);
        requestRecycler.setAdapter(adapter);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapPreview);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        profileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ProviderProfileActivity.class);
            startActivity(intent);
        });

        setupOnlineSwitch();
        setupSwipeRefresh();
        loadRequests();
        loadProviderInfo();
        setupButtons();
        setupCustomerService(view);
        loadCurrentOnlineStatus();

        return view;
    }

    private void loadCurrentOnlineStatus() {
        db.collection("users").document(providerId).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.getBoolean("online") != null) {
                boolean isOnline = doc.getBoolean("online");
                switchOnline.setChecked(isOnline);
                updateOnlineUI(isOnline);
            }
        });
    }

    private void loadProviderInfo() {
        db.collection("users").document(providerId).addSnapshotListener((document, error) -> {
            if (document != null && document.exists()) {
                String name = document.getString("name");
                if (name != null && !name.isEmpty()) {
                    txtWelcome.setText("Hello, " + name + " 👋");
                }

                double rating = 0;
                if (document.contains("rating")) {
                    rating = document.getDouble("rating");
                }
                
                String ratingStr = String.format(Locale.getDefault(), "%.1f", rating);
                txtRating.setText(ratingStr);
                txtLocation.setText("Status: Active • ⭐ " + ratingStr);
            }
        });
    }

    private void setupCustomerService(View view) {
        CardView bannerCard = (CardView) btnHelp.getParent().getParent();
        Animation pulse = AnimationUtils.loadAnimation(getContext(), R.anim.pulse);
        bannerCard.startAnimation(pulse);

        btnHelp.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Connecting to Provider Support...", Toast.LENGTH_SHORT).show();
            if (getActivity() instanceof ProviderDashboardActivity) {
                // Navigate to Chat tab
                ((ProviderDashboardActivity) getActivity()).navigateToTab(R.id.nav_provider_chat);
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            loadRequests();
            loadProviderInfo();
            loadCurrentOnlineStatus();
            swipeRefresh.setRefreshing(false);
        });
    }

    private void setupOnlineSwitch() {
        switchOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateOnlineUI(isChecked);
            db.collection("users").document(providerId).update("online", isChecked)
                    .addOnSuccessListener(unused -> Toast.makeText(getContext(), isChecked ? "You are Online" : "You are Offline", Toast.LENGTH_SHORT).show());
        });
    }

    private void updateOnlineUI(boolean isOnline) {
        if (isOnline) {
            txtOnlineStatus.setText("ONLINE");
            txtOnlineStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            txtOnlineStatus.setText("OFFLINE");
            txtOnlineStatus.setTextColor(Color.parseColor("#AAAAAA"));
        }
    }

    private void loadRequests() {
        db.collection("service_requests").whereEqualTo("providerId", providerId)
                .addSnapshotListener((query, error) -> {
                    if (query == null) return;
                    
                    requestList.clear();
                    boolean hasActive = false;
                    int todayBookings = 0;
                    int earnings = 0;
                    long startOfDay = getStartOfDay();

                    for (DocumentSnapshot doc : query) {
                        ServiceRequest req = doc.toObject(ServiceRequest.class);
                        if (req == null) continue;
                        req.requestId = doc.getId();
                        
                        long ts = req.timestamp;
                        if (ts == 0) {
                            Long fbTs = doc.getLong("timestamp");
                            if (fbTs != null) ts = fbTs;
                        }

                        if (ts >= startOfDay) {
                            if (!"pending".equals(req.status) && !"rejected".equals(req.status)) {
                                todayBookings++;
                                if ("completed".equals(req.status) || "reviewed".equals(req.status)) {
                                    earnings += req.price;
                                }
                            }
                        }

                        if ("pending".equals(req.status)) {
                            requestList.add(req);
                        }
                        
                        // Added "waiting" status to active assignment list
                        if ("accepted".equals(req.status) || "arrived".equals(req.status) || "waiting".equals(req.status)) {
                            hasActive = true;
                            activeRequestId = doc.getId();
                            currentCustomerLat = req.customerLat;
                            currentCustomerLng = req.customerLng;
                            updateAssignmentUI(req);
                        }
                    }
                    
                    txtBookings.setText(String.valueOf(todayBookings));
                    txtEarnings.setText("₹" + earnings);
                    
                    if (!hasActive) {
                        hideAssignmentUI();
                    }

                    txtRequests.setText(String.valueOf(requestList.size()));
                    adapter.notifyDataSetChanged();
                });
    }

    private void updateAssignmentUI(ServiceRequest req) {
        if (txtAssignmentTitle == null || cardAssignment == null) return;
        
        txtAssignmentTitle.setVisibility(View.VISIBLE);
        cardAssignment.setVisibility(View.VISIBLE);
        
        txtClientName.setText("Client: " + req.customerName);
        txtClientPhone.setText("Phone: " + req.customerPhone);
        txtClientAddress.setText("Address: " + req.customerAddress);

        if ("waiting".equals(req.status)) {
            txtAssignmentTitle.setText("Assignment Status: Waiting (" + req.waitTime + " min)");
            btnArrived.setVisibility(View.VISIBLE);
            btnArrived.setText("Accept & Start");
        } else if ("arrived".equals(req.status)) {
            txtAssignmentTitle.setText("Assignment Status: Arrived");
            btnArrived.setVisibility(View.GONE);
        } else {
            txtAssignmentTitle.setText("Assignment Status: Accepted");
            btnArrived.setVisibility(View.VISIBLE);
            btnArrived.setText("I Have Arrived");
        }
        
        updateMap();
    }

    private void hideAssignmentUI() {
        if (txtAssignmentTitle != null) {
            txtAssignmentTitle.setVisibility(View.GONE);
            txtAssignmentTitle.setText("Current Assignment");
        }
        if (cardAssignment != null) cardAssignment.setVisibility(View.GONE);
        activeRequestId = null;
    }

    private long getStartOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTimeInMillis();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        updateMap();
    }

    private void updateMap() {
        if (mMap == null || currentCustomerLat == 0) return;
        LatLng customer = new LatLng(currentCustomerLat, currentCustomerLng);
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(customer).title("Customer Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(customer, 15));
    }

    private void setupButtons() {
        btnNavigate.setOnClickListener(v -> {
            if (currentCustomerLat != 0) {
                try {
                    Uri uri = Uri.parse("google.navigation:q=" + currentCustomerLat + "," + currentCustomerLng);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setPackage("com.google.android.apps.maps");
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Maps not found", Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnArrived.setOnClickListener(v -> {
            if (activeRequestId != null) {
                db.collection("service_requests").document(activeRequestId).get().addOnSuccessListener(doc -> {
                    final String customerId = doc.getString("customerId");
                    final String providerName = doc.getString("providerName");
                    final String currentStatus = doc.getString("status");
                    
                    final String newStatus;
                    final String title;
                    final String message;
                    
                    if ("waiting".equals(currentStatus)) {
                        newStatus = "accepted";
                        title = "Request Accepted";
                        message = providerName + " is now coming to your location.";
                    } else {
                        newStatus = "arrived";
                        title = "Provider Arrived";
                        message = providerName + " has arrived at your location.";
                    }

                    db.collection("service_requests").document(activeRequestId).update("status", newStatus)
                            .addOnSuccessListener(aVoid -> sendNotification(customerId, title, message));
                });
            }
        });
        btnComplete.setOnClickListener(v -> {
            if (activeRequestId != null) {
                db.collection("service_requests").document(activeRequestId).get().addOnSuccessListener(doc -> {
                    final String customerId = doc.getString("customerId");
                    final String providerName = doc.getString("providerName");
                    db.collection("service_requests").document(activeRequestId).update("status", "completed")
                            .addOnSuccessListener(unused -> {
                                sendNotification(customerId, "Service Completed", providerName + " has completed the work.");
                                hideAssignmentUI();
                            });
                });
            }
        });
    }

    private void sendNotification(String userId, String title, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        db.collection("notifications").add(notification);
    }
}
