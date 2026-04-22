package com.example.servicearc;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private ProviderAdapter adapter;
    private List<Provider> providerList = new ArrayList<>();
    private double userLat = 0, userLng = 0;
    private SwipeRefreshLayout swipeRefresh;
    private EditText editSearch;
    private ImageView imgNotification;

    // Active Status UI
    private CardView cardActiveStatus;
    private TextView txtStatusTitle, txtStatusDesc, txtRequestTimer;
    private Button btnCancelRequest;
    private CountDownTimer requestTimer;
    private ListenerRegistration statusListener;

    // Completion & Rating UI
    private CardView cardCompletionStatus;
    private RatingBar ratingBarCompletion;
    private EditText editReview;
    private Button btnSubmitReview;
    private TextView txtCompletionDesc;
    private String currentActiveRequestId;
    private String currentProviderId;

    private static final int LOCATION_PERMISSION_CODE = 101;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        recyclerView = view.findViewById(R.id.providerRecycler);
        editSearch = view.findViewById(R.id.editSearch);
        imgNotification = view.findViewById(R.id.imgNotification);

        // Status UI
        cardActiveStatus = view.findViewById(R.id.cardActiveStatus);
        txtStatusTitle = view.findViewById(R.id.txtStatusTitle);
        txtStatusDesc = view.findViewById(R.id.txtStatusDesc);
        txtRequestTimer = view.findViewById(R.id.txtRequestTimer);
        btnCancelRequest = view.findViewById(R.id.btnCancelRequest);

        // Completion UI
        cardCompletionStatus = view.findViewById(R.id.cardCompletionStatus);
        ratingBarCompletion = view.findViewById(R.id.ratingBarCompletion);
        editReview = view.findViewById(R.id.editReview);
        btnSubmitReview = view.findViewById(R.id.btnSubmitReview);
        txtCompletionDesc = view.findViewById(R.id.txtCompletionDesc);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProviderAdapter(providerList, this::sendServiceRequest);
        recyclerView.setAdapter(adapter);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        setupSwipeRefresh();
        setupSearch();
        setupCategoryClicks(view);
        setupNotificationClick();
        listenRequestStatus();
        setupCustomerService(view);

        btnSubmitReview.setOnClickListener(v -> submitReview());

        return view;
    }

    private void setupCustomerService(View view) {
        CardView banner = (CardView) view.findViewById(R.id.btnHelp).getParent().getParent();
        Animation pulse = AnimationUtils.loadAnimation(getContext(), R.anim.pulse);
        banner.startAnimation(pulse);

        view.findViewById(R.id.btnHelp).setOnClickListener(v -> {
            ((CustomerDashboardActivity)requireActivity()).openExploreWithFilter("Support");
            Toast.makeText(getContext(), "Connecting to Customer Service...", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupNotificationClick() {
        imgNotification.setOnClickListener(v -> showNotificationsDialog());
    }

    private void showNotificationsDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_notifications, null);
        RecyclerView notifRecycler = dialogView.findViewById(R.id.notificationRecycler);
        TextView txtNoNotif = dialogView.findViewById(R.id.txtNoNotifications);
        TextView btnClear = dialogView.findViewById(R.id.btnClearNotifications);

        List<Notification> notifications = new ArrayList<>();
        NotificationAdapter notifAdapter = new NotificationAdapter(notifications);
        notifRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        notifRecycler.setAdapter(notifAdapter);

        String uid = FirebaseAuth.getInstance().getUid();
        db.collection("notifications")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    notifications.clear();
                    for (DocumentSnapshot doc : value) {
                        Notification n = doc.toObject(Notification.class);
                        if (n != null) notifications.add(n);
                    }
                    notifAdapter.notifyDataSetChanged();
                    txtNoNotif.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                });

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        btnClear.setOnClickListener(v -> {
            db.collection("notifications").whereEqualTo("userId", uid).get().addOnSuccessListener(querySnapshot -> {
                WriteBatch batch = db.batch();
                for (DocumentSnapshot doc : querySnapshot) batch.delete(doc.getReference());
                batch.commit();
            });
        });
        dialog.show();
    }

    private void setupCategoryClicks(View view) {
        view.findViewById(R.id.catElectrician).setOnClickListener(v -> ((CustomerDashboardActivity)requireActivity()).openExploreWithFilter("Electrician"));
        view.findViewById(R.id.catPlumber).setOnClickListener(v -> ((CustomerDashboardActivity)requireActivity()).openExploreWithFilter("Plumber"));
        view.findViewById(R.id.catAC).setOnClickListener(v -> ((CustomerDashboardActivity)requireActivity()).openExploreWithFilter("AC Service"));
        view.findViewById(R.id.catCleaner).setOnClickListener(v -> ((CustomerDashboardActivity)requireActivity()).openExploreWithFilter("Cleaner"));
        view.findViewById(R.id.catPainter).setOnClickListener(v -> ((CustomerDashboardActivity)requireActivity()).openExploreWithFilter("Painter"));
    }

    private void setupSearch() {
        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchProviders(editSearch.getText().toString().trim());
                return true;
            }
            return false;
        });
    }

    private void searchProviders(String serviceType) {
        if (serviceType.isEmpty()) { loadProviders(); return; }
        db.collection("users").whereEqualTo("role", "provider").whereEqualTo("online", true).whereEqualTo("serviceType", serviceType).get()
                .addOnSuccessListener(this::updateProviderList);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            loadProviders();
            swipeRefresh.setRefreshing(false);
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
            return;
        }
        mMap.setMyLocationEnabled(true);
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create().setPriority(Priority.PRIORITY_HIGH_ACCURACY).setInterval(3000);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;
                Location location = result.getLastLocation();
                userLat = location.getLatitude();
                userLng = location.getLongitude();
                LatLng myLocation = new LatLng(userLat, userLng);
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(myLocation).title("You"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15));
                loadProviders();
            }
        };
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, requireActivity().getMainLooper());
        }
    }

    private void loadProviders() {
        db.collection("users").whereEqualTo("role", "provider").whereEqualTo("online", true).get().addOnSuccessListener(this::updateProviderList);
    }

    private void updateProviderList(QuerySnapshot query) {
        providerList.clear();
        mMap.clear();
        LatLng myLocation = new LatLng(userLat, userLng);
        mMap.addMarker(new MarkerOptions().position(myLocation).title("You"));
        for (DocumentSnapshot doc : query) {
            Provider p = doc.toObject(Provider.class);
            if (p != null) {
                p.uid = doc.getId();
                if (p.latitude != 0 && p.longitude != 0) {
                    float[] results = new float[1];
                    Location.distanceBetween(userLat, userLng, p.latitude, p.longitude, results);
                    p.distance = results[0] / 1000;
                    mMap.addMarker(new MarkerOptions().position(new LatLng(p.latitude, p.longitude)).title(p.name));
                }
                providerList.add(p);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void sendServiceRequest(Provider provider) {
        if (userLat == 0 || userLng == 0) { Toast.makeText(getContext(), "Location not ready", Toast.LENGTH_LONG).show(); return; }
        String uid = FirebaseAuth.getInstance().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(document -> {
            if (!document.exists()) return;
            Map<String, Object> request = new HashMap<>();
            request.put("customerId", uid);
            request.put("providerId", provider.uid);
            request.put("providerName", provider.name);
            request.put("customerName", document.getString("name"));
            request.put("customerPhone", document.getString("phone"));
            request.put("customerAddress", document.getString("address"));
            request.put("customerLat", userLat);
            request.put("customerLng", userLng);
            request.put("status", "pending");
            request.put("timestamp", System.currentTimeMillis());
            request.put("expireAt", System.currentTimeMillis() + 300000);
            db.collection("service_requests").add(request).addOnSuccessListener(doc -> {
                Toast.makeText(getContext(), "Request Sent", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void listenRequestStatus() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (statusListener != null) statusListener.remove();
        
        statusListener = db.collection("service_requests")
                .whereEqualTo("customerId", uid)
                .whereIn("status", Arrays.asList("pending", "accepted", "arrived", "in_progress", "waiting", "completed"))
                .addSnapshotListener((query, error) -> {
                    if (query == null || query.isEmpty()) {
                        cardActiveStatus.setVisibility(View.GONE);
                        cardCompletionStatus.setVisibility(View.GONE);
                        if (requestTimer != null) requestTimer.cancel();
                        return;
                    }

                    // Sort to get the most recent active or completed request
                    DocumentSnapshot doc = query.getDocuments().get(0);
                    String status = doc.getString("status");
                    String providerName = doc.getString("providerName");
                    String requestId = doc.getId();
                    currentActiveRequestId = requestId;
                    currentProviderId = doc.getString("providerId");

                    if ("completed".equals(status)) {
                        cardActiveStatus.setVisibility(View.GONE);
                        cardCompletionStatus.setVisibility(View.VISIBLE);
                        txtCompletionDesc.setText("Your request with " + providerName + " has been completed. Please rate your experience.");
                        if (requestTimer != null) requestTimer.cancel();
                        return;
                    }

                    // Hide completion card if an active request is present
                    cardCompletionStatus.setVisibility(View.GONE);
                    cardActiveStatus.setVisibility(View.VISIBLE);
                    btnCancelRequest.setOnClickListener(v -> cancelRequest(requestId));

                    if ("pending".equals(status)) {
                        txtStatusTitle.setText("Searching for Provider...");
                        txtStatusDesc.setText("Please wait while we connect you.");
                        txtRequestTimer.setVisibility(View.VISIBLE);
                        long expireAt = doc.getLong("expireAt");
                        startTimer(expireAt - System.currentTimeMillis());
                    } else if ("waiting".equals(status)) {
                        if (requestTimer != null) requestTimer.cancel();
                        txtRequestTimer.setVisibility(View.GONE);
                        
                        long waitTime = doc.getLong("waitTime") != null ? doc.getLong("waitTime") : 0;
                        txtStatusTitle.setText("Provider requested time");
                        txtStatusDesc.setText(providerName + " will be arrive in " + waitTime + " min.");
                    } else {
                        if (requestTimer != null) requestTimer.cancel();
                        txtRequestTimer.setVisibility(View.GONE);

                        if ("accepted".equals(status) || "in_progress".equals(status)) {
                            txtStatusTitle.setText("Request Accepted");
                            txtStatusDesc.setText(providerName + " is arrive soon.");
                        } else if ("arrived".equals(status)) {
                            txtStatusTitle.setText("Provider Arrived");
                            txtStatusDesc.setText("Your work will be done soon.");
                        }
                    }
                });
    }

    private void startTimer(long millis) {
        if (requestTimer != null) requestTimer.cancel();
        if (millis <= 0) {
            txtRequestTimer.setText("Request Expired");
            return;
        }
        requestTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long min = (millisUntilFinished / 1000) / 60;
                long sec = (millisUntilFinished / 1000) % 60;
                txtRequestTimer.setText(String.format(Locale.getDefault(), "Time remaining: %02d:%02d", min, sec));
            }

            @Override
            public void onFinish() {
                txtRequestTimer.setText("Request Expired");
            }
        }.start();
    }

    private void cancelRequest(String requestId) {
        db.collection("service_requests").document(requestId).update("status", "cancelled")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Request Cancelled", Toast.LENGTH_SHORT).show();
                    cardActiveStatus.setVisibility(View.GONE);
                });
    }

    private void submitReview() {
        float rating = ratingBarCompletion.getRating();
        String review = editReview.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(getContext(), "Please provide a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("providerId", currentProviderId);
        reviewData.put("customerId", uid);
        reviewData.put("rating", rating);
        reviewData.put("reviewText", review);
        reviewData.put("timestamp", System.currentTimeMillis());

        db.collection("reviews").add(reviewData).addOnSuccessListener(docRef -> {
            updateProviderRating(currentProviderId, rating);
            db.collection("service_requests").document(currentActiveRequestId).update("status", "reviewed")
                    .addOnSuccessListener(aVoid -> {
                        // Clear the review UI
                        ratingBarCompletion.setRating(0);
                        editReview.setText("");
                        cardCompletionStatus.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Thank you for your review!", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void updateProviderRating(String providerId, float newRating) {
        db.collection("users").document(providerId).get().addOnSuccessListener(snapshot -> {
            double currentRating = snapshot.contains("rating") ? snapshot.getDouble("rating") : 0;
            long reviewCount = snapshot.contains("reviewCount") ? snapshot.getLong("reviewCount") : 0;

            double totalRating = (currentRating * reviewCount) + newRating;
            reviewCount++;
            double newAverage = totalRating / reviewCount;

            Map<String, Object> update = new HashMap<>();
            update.put("rating", newAverage);
            update.put("reviewCount", reviewCount);

            db.collection("users").document(providerId).update(update);
        });
    }

    private void addNotification(String title, String message, String userId) {
        db.collection("notifications").add(new Notification(title, message, userId));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (statusListener != null) statusListener.remove();
        if (requestTimer != null) requestTimer.cancel();
    }
}
