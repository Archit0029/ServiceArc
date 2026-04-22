package com.example.servicearc;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

public class ProviderMapActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private GoogleMap mMap;

    private double customerLat;
    private double customerLng;

    private double providerLat;
    private double providerLng;

    private FusedLocationProviderClient fusedLocationClient;

    Button btnNavigate;

    private static final int LOCATION_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_map);

        btnNavigate = findViewById(R.id.btnNavigate);

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        customerLat =
                getIntent().getDoubleExtra("customerLat",0);

        customerLng =
                getIntent().getDoubleExtra("customerLng",0);

        if(customerLat == 0 || customerLng == 0){

            Toast.makeText(this,
                    "Customer location not available",
                    Toast.LENGTH_LONG).show();

            finish();
            return;
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment)
                        getSupportFragmentManager()
                                .findFragmentById(R.id.map);

        if(mapFragment != null){
            mapFragment.getMapAsync(this);
        }

        btnNavigate.setOnClickListener(v -> openGoogleMaps());
    }

    // --------------------------------------------------
    // MAP READY
    // --------------------------------------------------

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        if(ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);

            return;
        }

        mMap.setMyLocationEnabled(true);

        getProviderLocation();
    }

    // --------------------------------------------------
    // GET PROVIDER LOCATION
    // --------------------------------------------------

    private void getProviderLocation(){

        if(ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {

                    if(location != null){

                        providerLat = location.getLatitude();
                        providerLng = location.getLongitude();

                        showLocations();
                    }
                    else{

                        Toast.makeText(this,
                                "Provider location not detected",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // --------------------------------------------------
    // SHOW MARKERS + ROUTE
    // --------------------------------------------------

    private void showLocations(){

        LatLng providerLocation =
                new LatLng(providerLat,providerLng);

        LatLng customerLocation =
                new LatLng(customerLat,customerLng);

        mMap.addMarker(new MarkerOptions()
                .position(providerLocation)
                .title("Your Location")
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_BLUE)));

        mMap.addMarker(new MarkerOptions()
                .position(customerLocation)
                .title("Customer Location"));

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(providerLocation,13));

        // Draw route line
        mMap.addPolyline(new PolylineOptions()
                .add(providerLocation,customerLocation)
                .width(8)
                .color(android.graphics.Color.BLUE));
    }

    // --------------------------------------------------
    // OPEN GOOGLE MAP NAVIGATION
    // --------------------------------------------------

    private void openGoogleMaps(){

        Uri uri =
                Uri.parse("google.navigation:q="
                        +customerLat+","+customerLng);

        Intent mapIntent =
                new Intent(Intent.ACTION_VIEW,uri);

        mapIntent.setPackage("com.google.android.apps.maps");

        if(mapIntent.resolveActivity(getPackageManager()) != null){

            startActivity(mapIntent);
        }
        else{

            Toast.makeText(this,
                    "Google Maps not installed",
                    Toast.LENGTH_LONG).show();
        }
    }

    // --------------------------------------------------
    // PERMISSION RESULT
    // --------------------------------------------------

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults){

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);

        if(requestCode == LOCATION_PERMISSION_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED){

            getProviderLocation();
        }
    }
}