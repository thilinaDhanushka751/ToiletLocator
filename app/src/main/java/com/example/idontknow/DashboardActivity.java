package com.example.idontknow;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.idontknow.R;

import java.util.HashMap;

public class DashboardActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener {
    private final int FINE_PERMISSION_CODE = 1;
    private GoogleMap myMapD;
    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    private DatabaseReference mDatabase;
    private HashMap<Marker, ToiletInfo> markerInfoMap = new HashMap<>();

    // Define the boundaries for Sri Lanka
    private static final LatLngBounds SRI_LANKA_BOUNDS = new LatLngBounds(
            new LatLng(5.916667, 79.652222), // SW bounds
            new LatLng(9.837611, 81.881455)  // NE bounds
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Get user location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Set up bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.dashboard);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.dashboard) {
                    return true;
                }
                if (id == R.id.home) {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    overridePendingTransition(0, 0);
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

        // Apply system window insets to the main layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMapD = googleMap;

        // Move the camera to Sri Lanka bounds
        myMapD.moveCamera(CameraUpdateFactory.newLatLngBounds(SRI_LANKA_BOUNDS, 0));

        // Set the map's camera to the specified bounds
        myMapD.setLatLngBoundsForCameraTarget(SRI_LANKA_BOUNDS);
        // Add markers to map
        addMarkersToMap();

        // Set up marker click listener
        myMapD.setOnMarkerClickListener(this);
        myMapD.setOnMarkerDragListener(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }
        googleMap.setMyLocationEnabled(true);

        // Set up custom info window adapter
        CustomInfoWindowAdapter customInfoWindowAdapter = new CustomInfoWindowAdapter();
        myMapD.setInfoWindowAdapter(customInfoWindowAdapter);

    }

    private void zoomInToMyLocation() {
        // Check if currentLocation is available
        if (currentLocation != null) {
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

            // Define zoom level
            float zoomLevel = 15.0f; // You can adjust this value as needed

            // Move camera to user's location with zoom
            myMapD.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, zoomLevel));
        } else {
            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
        }
    }

    // Adding markers for the toilets retrieved from Firebase
    private void addMarkersToMap() {
        // Retrieve the toilets data from Firebase and add markers to the map
        mDatabase.child("toilets").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Iterate through each toilet in the database
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Get the latitude and longitude of the toilet
                    Double latitude = snapshot.child("latitude").getValue(Double.class);
                    Double longitude = snapshot.child("longitude").getValue(Double.class);
                    String name = snapshot.child("name").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);
                    Double rating = snapshot.child("rating").getValue(Double.class);

                    // Check if latitude and longitude are not null
                    if (latitude != null && longitude != null) {
                        // Add marker to the map
                        LatLng toiletLocation = new LatLng(latitude, longitude);
                        Marker marker = myMapD.addMarker(new MarkerOptions().position(toiletLocation).title(name));
                        // Store additional info in markerInfoMap
                        markerInfoMap.put(marker, new ToiletInfo(name, address, rating));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle database error
                Toast.makeText(DashboardActivity.this, "Failed to retrieve toilets data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "Location permission is denied. Please allow the permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;

                    // If map is ready, add the marker and zoom to the user's location
                    if (myMapD != null) {
                        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        myMapD.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15.0f));
                    } else {
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                        mapFragment.getMapAsync(DashboardActivity.this);
                    }
                } else {
                    Toast.makeText(DashboardActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        // Handle marker click
        marker.showInfoWindow();
        return false;
    }


    @Override
    public void onMarkerDrag(@NonNull Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        // Show the info window with additional information when marker drag ends
        marker.showInfoWindow();
    }

    @Override
    public void onMarkerDragStart(@NonNull Marker marker) {

    }

    // Custom info window adapter
    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private final View infoWindowView;

        CustomInfoWindowAdapter() {
            infoWindowView = getLayoutInflater().inflate(R.layout.custom_info_window, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            // Use default InfoWindow frame
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            TextView tvTitle = infoWindowView.findViewById(R.id.tv_title);
            TextView tvAddress = infoWindowView.findViewById(R.id.tv_address);
            TextView tvRating = infoWindowView.findViewById(R.id.tv_rating);

            ToiletInfo toiletInfo = markerInfoMap.get(marker);
            if (toiletInfo != null) {
                tvTitle.setText(toiletInfo.name != null ? toiletInfo.name : "N/A");
                tvAddress.setText(toiletInfo.address != null ? toiletInfo.address : "N/A");
                tvRating.setText(toiletInfo.rating != null ? String.valueOf(toiletInfo.rating) : "N/A");
            }

            // Set the click listener for the info window view
            infoWindowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Open Google Maps with directions from current location to the clicked marker
                    openGoogleMaps(marker);
                }
            });

            return infoWindowView;
        }
    }


    // Open Google Maps with directions from current location to the clicked marker
    private void openGoogleMaps(Marker marker) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + marker.getPosition().latitude + "," + marker.getPosition().longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Google Maps app is not installed.", Toast.LENGTH_SHORT).show();
        }
    }

    // Class to store additional toilet info
    class ToiletInfo {
        String name;
        String address;
        Double rating;

        ToiletInfo(String name, String address, Double rating) {
            this.name = name;
            this.address = address;
            this.rating = rating;
        }
    }
}
