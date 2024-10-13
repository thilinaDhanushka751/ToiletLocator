package com.example.idontknow;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.idontknow.DashboardActivity;
import com.example.idontknow.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap myMap;
    private DatabaseReference mDatabase;
    private EditText addressEditText;
    private EditText nameEditText;
    private RatingBar ratingBar;

    // Define the boundaries for Sri Lanka
    private static final LatLngBounds SRI_LANKA_BOUNDS = new LatLngBounds(
            new LatLng(5.916667, 79.652222), // SW bounds
            new LatLng(9.837611, 81.881455)  // NE bounds
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge display
        EdgeToEdge.enable(this);

        // Set the layout for the activity
        setContentView(R.layout.activity_main);

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize views
        addressEditText = findViewById(R.id.text_address);
        nameEditText = findViewById(R.id.text_name);
        ratingBar = findViewById(R.id.rating_bar);

        // Set up bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.home);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.dashboard) {
                    startActivity(new Intent(getApplicationContext(), DashboardActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                }
                if (id == R.id.home) {
                    return true;
                }
                if (id == R.id.about) {
                    startActivity(new Intent(getApplicationContext(), AboutActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            }
        });

        // Apply system window insets
        // Apply system window insets excluding bottom navigation view
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.main).setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            findViewById(R.id.bottom_navigation).setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });



        // Set onClickListener for "ADD TOILET" button
        Button addToiletButton = findViewById(R.id.add_toilet);
        addToiletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToilet();
            }
        });
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.green));

    }

    // Callback method when the map is ready to be used

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        // Get location permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permissions if not granted
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // Enable my location button
        myMap.setMyLocationEnabled(true);

        // Set the map's camera to the specified bounds
        myMap.setLatLngBoundsForCameraTarget(SRI_LANKA_BOUNDS);

        // Add markers to the map
        myMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Clear previous markers
                myMap.clear();

                // Add a marker at the clicked location
                myMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));

                // Get latitude and longitude
                double latitude = latLng.latitude;
                double longitude = latLng.longitude;

                // call method handle address
                updateAddressText(latLng);
            }
        });

        // Zoom into user's location
        zoomToUserLocation();
    }

    // Method to zoom into user's location
    private void zoomToUserLocation() {
        // Check if location permission is granted
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Get the last known location from the Fused Location Provider
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                // Get the user's latitude and longitude
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();

                                // Create a LatLng object for the user's location
                                LatLng userLocation = new LatLng(latitude, longitude);

                                // Move the camera to the user's location with a suitable zoom level
                                myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f));
                            }
                        }
                    });
        }
    }

    // Method to update the Address text using reverse geocoding
    private void updateAddressText(LatLng latLng) {
        // Perform reverse geocoding to get the address
        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                String addressLine = address.getAddressLine(0);
                addressEditText.setText(addressLine);
            } else {
                addressEditText.setText("Address not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
            addressEditText.setText("Error retrieving address");
        }
    }

    // Method to add toilet information to Firebase
    private void addToilet() {
        // Get location information
        String address = addressEditText.getText().toString().trim();
        String name = nameEditText.getText().toString().trim();
        float rating = ratingBar.getRating();

        // Check if address and name are not empty
        if (address.isEmpty() || name.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter address and name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the selected location from the map
        LatLng selectedLocation = myMap.getCameraPosition().target;
        double latitude = selectedLocation.latitude;
        double longitude = selectedLocation.longitude;

        // Update database with marker info
        updateDatabase(latitude, longitude, address, name, rating);
    }

    // Method to update the database with marker information
    private void updateDatabase(double latitude, double longitude, String address, String name, float rating) {
        // Get reference to the "toilets" node in the database
        DatabaseReference toiletsRef = mDatabase.child("toilets").push();

        // Update latitude, longitude, address, name, and rating values
        toiletsRef.child("latitude").setValue(latitude);
        toiletsRef.child("longitude").setValue(longitude);
        toiletsRef.child("address").setValue(address);
        toiletsRef.child("name").setValue(name);
        toiletsRef.child("rating").setValue(rating);

        // Show a message to indicate that the update is complete
        Toast.makeText(MainActivity.this, "Toilet added successfully", Toast.LENGTH_SHORT).show();
    }
}
