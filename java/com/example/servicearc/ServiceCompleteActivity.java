package com.example.servicearc;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ServiceCompleteActivity extends AppCompatActivity {

    Button btnCompleteService;

    String providerId;

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_complete);

        db = FirebaseFirestore.getInstance();

        btnCompleteService = findViewById(R.id.btnCompleteService);

        // receive provider id from previous activity
        providerId = getIntent().getStringExtra("providerId");

        btnCompleteService.setOnClickListener(v -> showRatingDialog());
    }

    // Rating popup
    private void showRatingDialog() {

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.rating_dialog);
        dialog.setCancelable(true);

        RatingBar ratingBar = dialog.findViewById(R.id.ratingBar);
        EditText reviewText = dialog.findViewById(R.id.reviewText);
        Button submitReview = dialog.findViewById(R.id.submitReview);

        submitReview.setOnClickListener(v -> {

            float rating = ratingBar.getRating();
            String review = reviewText.getText().toString();

            if (rating == 0) {

                Toast.makeText(this,
                        "Please give rating",
                        Toast.LENGTH_SHORT).show();

                return;
            }

            String customerId = FirebaseAuth.getInstance().getUid();

            Map<String,Object> reviewData = new HashMap<>();

            reviewData.put("providerId", providerId);
            reviewData.put("customerId", customerId);
            reviewData.put("rating", rating);
            reviewData.put("reviewText", review);
            reviewData.put("timestamp", System.currentTimeMillis());

            db.collection("reviews")
                    .add(reviewData)
                    .addOnSuccessListener(documentReference -> {

                        updateProviderRating(rating);

                        Toast.makeText(this,
                                "Review Submitted",
                                Toast.LENGTH_SHORT).show();

                        dialog.dismiss();
                        finish();
                    });

        });

        dialog.show();
    }

    // Update provider average rating
    private void updateProviderRating(float newRating) {

        db.collection("users")
                .document(providerId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    double currentRating = 0;
                    long reviewCount = 0;

                    if(snapshot.contains("rating"))
                        currentRating = snapshot.getDouble("rating");

                    if(snapshot.contains("reviewCount"))
                        reviewCount = snapshot.getLong("reviewCount");

                    double totalRating =
                            (currentRating * reviewCount) + newRating;

                    reviewCount++;

                    double newAverage = totalRating / reviewCount;

                    Map<String,Object> update = new HashMap<>();

                    update.put("rating", newAverage);
                    update.put("reviewCount", reviewCount);

                    db.collection("users")
                            .document(providerId)
                            .update(update);
                });
    }
}