package com.example.servicearc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class ServiceNavigationActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    GoogleMap mMap;
    double lat, lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_navigation);

        lat = getIntent().getDoubleExtra("lat", 0);
        lng = getIntent().getDoubleExtra("lng", 0);

        TextView name = findViewById(R.id.customerName);
        TextView address = findViewById(R.id.customerAddress);
        TextView mobile = findViewById(R.id.customerMobile);

        name.setText(getIntent().getStringExtra("customerName"));
        address.setText(getIntent().getStringExtra("customerAddress"));
        mobile.setText(getIntent().getStringExtra("customerMobile"));

        Button navigate = findViewById(R.id.btnNavigate);
        navigate.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        });

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng customerLocation = new LatLng(lat, lng);
        mMap.addMarker(new MarkerOptions()
                .position(customerLocation)
                .title("Customer Location"));
        mMap.moveCamera(CameraUpdateFactory
                .newLatLngZoom(customerLocation, 15));
    }
}
