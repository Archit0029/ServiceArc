package com.example.servicearc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    EditText editName, editPhone, editAddress;

    Button btnEditProfile, btnSaveProfile;

    TextView txtActiveBookings, txtCompletedBookings, menuLogout;

    FirebaseFirestore db;
    FirebaseAuth auth;

    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        BottomNavigationView bottomMenu = findViewById(R.id.bottomNav);
        bottomMenu.setSelectedItemId(R.id.nav_chat);

        bottomMenu.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, CustomerDashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_explore) {
                startActivity(new Intent(this, ExploreActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_booking) {
                startActivity(new Intent(this, BookingActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_chat) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });

        editName = findViewById(R.id.editName);
        editPhone = findViewById(R.id.editPhone);
        editAddress = findViewById(R.id.editAddress);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        txtActiveBookings = findViewById(R.id.txtActiveBookings);
        txtCompletedBookings = findViewById(R.id.txtCompletedBookings);

        menuLogout = findViewById(R.id.menuLogout);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        uid = auth.getUid();

        disableFields();

        loadProfile();

        loadBookingStats();


        // EDIT BUTTON

        btnEditProfile.setOnClickListener(v -> {

            enableFields();

            btnEditProfile.setVisibility(View.GONE);
            btnSaveProfile.setVisibility(View.VISIBLE);

        });


        // SAVE BUTTON

        btnSaveProfile.setOnClickListener(v -> saveProfile());


        // LOGOUT

        menuLogout.setOnClickListener(v -> {

            auth.signOut();

            finish();

        });

    }


    // ------------------------------------------------
    // LOAD PROFILE
    // ------------------------------------------------

    private void loadProfile(){

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if(documentSnapshot.exists()){

                        editName.setText(documentSnapshot.getString("name"));
                        editPhone.setText(documentSnapshot.getString("phone"));
                        editAddress.setText(documentSnapshot.getString("address"));

                    }

                });
    }


    // ------------------------------------------------
    // SAVE PROFILE
    // ------------------------------------------------

    private void saveProfile(){

        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String address = editAddress.getText().toString().trim();

        if(name.isEmpty() || phone.isEmpty() || address.isEmpty()){

            Toast.makeText(this,
                    "Please fill all fields",
                    Toast.LENGTH_LONG).show();

            return;
        }

        Map<String,Object> update = new HashMap<>();

        update.put("name",name);
        update.put("phone",phone);
        update.put("address",address);

        db.collection("users")
                .document(uid)
                .update(update)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this,
                            "Profile Updated",
                            Toast.LENGTH_SHORT).show();

                    disableFields();

                    btnSaveProfile.setVisibility(View.GONE);
                    btnEditProfile.setVisibility(View.VISIBLE);

                });
    }


    // ------------------------------------------------
    // ENABLE / DISABLE INPUT
    // ------------------------------------------------

    private void enableFields(){

        editName.setEnabled(true);
        editPhone.setEnabled(true);
        editAddress.setEnabled(true);

    }

    private void disableFields(){

        editName.setEnabled(false);
        editPhone.setEnabled(false);
        editAddress.setEnabled(false);

    }



    // ------------------------------------------------
    // LOAD BOOKING STATS
    // ------------------------------------------------

    private void loadBookingStats(){

        db.collection("service_requests")
                .whereEqualTo("customerId",uid)
                .get()
                .addOnSuccessListener(query -> {

                    int active = 0;
                    int completed = 0;

                    for(DocumentSnapshot doc : query){

                        String status = doc.getString("status");

                        if(status == null) continue;

                        if(status.equals("pending") ||
                                status.equals("accepted") ||
                                status.equals("in_progress")){

                            active++;

                        } else if(status.equals("completed") ||
                                status.equals("expired")){

                            completed++;

                        }
                    }

                    txtActiveBookings.setText(String.valueOf(active));
                    txtCompletedBookings.setText(String.valueOf(completed));

                });
    }

}