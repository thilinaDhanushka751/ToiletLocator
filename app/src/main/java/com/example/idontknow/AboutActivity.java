package com.example.idontknow;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AboutActivity extends AppCompatActivity {
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location currentLocation;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<ToiletInfo> toiletList = new ArrayList<>();
    private DatabaseReference mDatabase;
    private final int FINE_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        listView = findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Get user location
        getLastLocation();

        // Set up bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.about);
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
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            }
        });

        // Set up ListView item click listener to open Google Maps
        listView.setOnItemClickListener((parent, view, position, id) -> {
            ToiletInfo selectedToilet = toiletList.get(position);
            openGoogleMaps(selectedToilet.latitude, selectedToilet.longitude);
        });
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                    fetchToiletsData();
                } else {
                    Toast.makeText(AboutActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchToiletsData() {
        mDatabase.child("toilets").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                toiletList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Double latitude = snapshot.child("latitude").getValue(Double.class);
                    Double longitude = snapshot.child("longitude").getValue(Double.class);
                    String name = snapshot.child("name").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);
                    Double rating = snapshot.child("rating").getValue(Double.class);

                    if (latitude != null && longitude != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), latitude, longitude, results);
                        float distance = results[0];
                        toiletList.add(new ToiletInfo(name, address, rating, distance, latitude, longitude));
                    }
                }

                // Sort toilets by distance
                Collections.sort(toiletList, new Comparator<ToiletInfo>() {
                    @Override
                    public int compare(ToiletInfo o1, ToiletInfo o2) {
                        return Float.compare(o1.distance, o2.distance);
                    }
                });

                // Limit the list to 8 items
                List<ToiletInfo> limitedList = toiletList.subList(0, Math.min(8, toiletList.size()));

                // Update the ListView
                List<String> displayList = new ArrayList<>();
                for (ToiletInfo toilet : limitedList) {
                    // Convert distance to kilometers and format to 1 decimal place
                    float distanceInKm = toilet.distance / 1000;
                    String formattedDistance = String.format("%.1f km", distanceInKm);
                    displayList.add(toilet.name + "\n" + toilet.address + "\nRating: " + toilet.rating + "\nDistance: " + formattedDistance);
                }
                adapter.clear();
                adapter.addAll(displayList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AboutActivity.this, "Failed to retrieve toilets data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openGoogleMaps(double latitude, double longitude) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latitude + "," + longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Google Maps app is not installed.", Toast.LENGTH_SHORT).show();
        }
    }

    static class ToiletInfo {
        String name;
        String address;
        Double rating;
        float distance;
        double latitude;
        double longitude;

        ToiletInfo(String name, String address, Double rating, float distance, double latitude, double longitude) {
            this.name = name;
            this.address = address;
            this.rating = rating;
            this.distance = distance;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
