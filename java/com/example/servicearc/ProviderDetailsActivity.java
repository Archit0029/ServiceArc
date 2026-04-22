package com.example.servicearc;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.*;

public class ProviderDetailsActivity extends AppCompatActivity {

    EditText etName, etMobile, etCity, etState, etPincode, etAddress, etExperience;
    Spinner spinnerService;
    Button btnSubmit;

    FusedLocationProviderClient fusedLocationClient;

    double latitude;
    double longitude;

    String[] services = {
            "Carpenters","Painters","AC Services","Appliance Repair",
            "Housekeeping","Pest Control","Water Tank Cleaner",
            "Laundry Services","Packers & Movers","Gardeners",
            "Computer/IT Technicians","Home Security","Driver","Plumber"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_details);

        etName = findViewById(R.id.etName);
        etMobile = findViewById(R.id.etMobile);
        etCity = findViewById(R.id.etCity);
        etState = findViewById(R.id.etState);
        etPincode = findViewById(R.id.etPincode);
        etAddress = findViewById(R.id.etAddress);
        etExperience = findViewById(R.id.etExperience);
        spinnerService = findViewById(R.id.spinnerService);
        btnSubmit = findViewById(R.id.btnSubmit);

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item,
                        services);

        spinnerService.setAdapter(adapter);

        // Detect location automatically
        getLocation();

        btnSubmit.setOnClickListener(v -> saveProvider());
    }

    // Get GPS location
    private void getLocation() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    101);

            return;
        }

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {

            if (location != null) {

                latitude = location.getLatitude();
                longitude = location.getLongitude();

                getAddressFromLocation(latitude, longitude);

            } else {

                Toast.makeText(
                        this,
                        "Unable to detect location",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Convert GPS → Address
    private void getAddressFromLocation(double lat, double lng) {

        Geocoder geocoder =
                new Geocoder(this, Locale.getDefault());

        try {

            List<Address> addresses =
                    geocoder.getFromLocation(lat, lng, 1);

            if (addresses != null && !addresses.isEmpty()) {

                Address address = addresses.get(0);

                etAddress.setText(address.getAddressLine(0));
                etCity.setText(address.getLocality());
                etState.setText(address.getAdminArea());
                etPincode.setText(address.getPostalCode());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Permission result
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);

        if (requestCode == 101 &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            getLocation();
        }
    }

    // Save provider details
    private void saveProvider() {

        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            Toast.makeText(this,
                    "User not logged in",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> map = new HashMap<>();

        map.put("role", "provider");
        map.put("name", etName.getText().toString());
        map.put("mobile", etMobile.getText().toString());
        map.put("serviceType",
                spinnerService.getSelectedItem().toString());

        map.put("experience", etExperience.getText().toString());

        map.put("city", etCity.getText().toString());
        map.put("state", etState.getText().toString());
        map.put("pincode", etPincode.getText().toString());
        map.put("address", etAddress.getText().toString());

        map.put("latitude", latitude);
        map.put("longitude", longitude);

        map.put("rating", 0);
        map.put("reviewCount", 0);
        map.put("isOnline", false);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(map)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Provider Registered Successfully",
                            Toast.LENGTH_SHORT).show();

                    // Go to Provider Dashboard
                    Intent intent = new Intent(
                            ProviderDetailsActivity.this,
                            ProviderDashboardActivity.class
                    );

                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}