package com.example.servicearc;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.content.pm.PackageManager;
import com.google.android.gms.location.*;
import java.util.Locale;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import java.util.Calendar;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class CustomerDetailsActivity extends AppCompatActivity {

    private EditText etName, etDob, etCity, etState, etPincode, etArea, etLandmark, etMobile;
    private Button btnSubmit;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_details);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLocation();
        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        // Views
        etName = findViewById(R.id.etName);
        etDob = findViewById(R.id.etDob);
        etDob.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePicker = new DatePickerDialog(
                    CustomerDetailsActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {

                        String dob = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        etDob.setText(dob);

                    },
                    year, month, day
            );

            datePicker.show();
        });
        etCity = findViewById(R.id.etCity);
        etState = findViewById(R.id.etState);
        etPincode = findViewById(R.id.etPincode);
        etArea = findViewById(R.id.etArea);
        etLandmark = findViewById(R.id.etLandmark);
        etMobile = findViewById(R.id.etMobile);
        btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> saveCustomer());
    }
    private void getLocation() {

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                != getPackageManager().PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    100
            );
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {

                    if (location != null) {

                        fillAddress(location);

                    } else {
                        requestNewLocation();
                    }

                });
    }
    private void requestNewLocation() {

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setNumUpdates(1);

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                != getPackageManager().PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {

                        Location location = locationResult.getLastLocation();

                        if (location != null) {
                            fillAddress(location);
                        }
                    }
                },
                getMainLooper()
        );
    }
    private void fillAddress(Location location) {

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {

            List<Address> addresses =
                    geocoder.getFromLocation(
                            location.getLatitude(),
                            location.getLongitude(),
                            1
                    );

            if (!addresses.isEmpty()) {

                Address address = addresses.get(0);

                etCity.setText(address.getLocality());
                etState.setText(address.getAdminArea());
                etPincode.setText(address.getPostalCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            getLocation();
        }
    }
    private void saveCustomer() {

        String name = etName.getText().toString().trim();
        String dob = etDob.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String state = etState.getText().toString().trim();
        String pincode = etPincode.getText().toString().trim();
        String area = etArea.getText().toString().trim();
        String landmark = etLandmark.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name required");
            return;
        }

        if (dob.isEmpty()) {
            etDob.setError("DOB required");
            return;
        }

        if (city.isEmpty()) {
            etCity.setError("City required");
            return;
        }

        if (state.isEmpty()) {
            etState.setError("State required");
            return;
        }

        if (!pincode.matches("\\d{6}")) {
            etPincode.setError("Enter valid 6 digit pincode");
            return;
        }

        if (!mobile.matches("\\d{10}")) {
            etMobile.setError("Enter valid 10 digit mobile number");
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) return;

        Map<String, Object> map = new HashMap<>();
        map.put("role", "customer");
        map.put("name", name);
        map.put("dob", dob);
        map.put("city", city);
        map.put("state", state);
        map.put("pincode", pincode);
        map.put("area", area);
        map.put("landmark", landmark);
        map.put("mobile", mobile);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(map, SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    Intent intent = new Intent(
                            CustomerDetailsActivity.this,
                            CustomerDashboardActivity.class
                    );

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(intent);
                });
    }
}