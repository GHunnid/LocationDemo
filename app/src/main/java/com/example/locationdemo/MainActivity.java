package com.example.locationdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.locationdemo.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap myMap;
    private FusedLocationProviderClient locationClient;

    private EditText etSearch;
    private Button btnSearch;
    private Button btnLocate;
    private Button btnDirections;

    private LatLng currentUserLatLng = null;
    private LatLng searchedTargetLatLng = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Link Java variables to XML elements
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnLocate = findViewById(R.id.btnGetLocation);
        btnDirections = findViewById(R.id.btnGetDirections);

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        // Load the map fragment programmatically
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Standard Java Click Listeners
        btnLocate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionsAndGetLocation();
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String locationName = etSearch.getText().toString().trim();
                if (!locationName.isEmpty()) {
                    searchPlace(locationName);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a place name", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnDirections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGoogleMapsForDirections();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        myMap = googleMap;
        // Start camera at a default location (New York)
        LatLng defaultPlace = new LatLng(40.7128, -74.0060);
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPlace, 10f));
    }

    private void searchPlace(String locationName) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);

            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                searchedTargetLatLng = new LatLng(address.getLatitude(), address.getLongitude());

                myMap.clear();
                myMap.addMarker(new MarkerOptions().position(searchedTargetLatLng).title(locationName));
                myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(searchedTargetLatLng, 14f));
            } else {
                Toast.makeText(this, "Location not found!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGoogleMapsForDirections() {
        if (searchedTargetLatLng == null) {
            Toast.makeText(this, "Search for a destination first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Standard Google Maps intent URI
        String uriString = "google.navigation:q=" + searchedTargetLatLng.latitude + "," + searchedTargetLatLng.longitude;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));

        // REMOVED THE PACKAGE CHECK TO BYPASS ANDROID SYSTEM RESTRICTIONS
        try {
            startActivity(intent);
        } catch (Exception e) {
            // Fallback: If the app really isn't there, open it in the web browser
            String webUriString = "https://google.com" + searchedTargetLatLng.latitude + "," + searchedTargetLatLng.longitude;
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUriString));
            startActivity(webIntent);
        }
    }


    private void checkPermissionsAndGetLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        } else {
            fetchLastLocation();
        }
    }

    private void fetchLastLocation() {
        try {
            locationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        currentUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        myMap.clear();
                        myMap.addMarker(new MarkerOptions().position(currentUserLatLng).title("I am here!"));
                        myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUserLatLng, 15f));
                    } else {
                        Toast.makeText(MainActivity.this, "Could not get location. Turn on GPS.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLastLocation();
        } else {
            Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
        }
    }
}
