package com.group1.scansaver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.group1.scansaver.api.UPCApiRequest;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView mapView;

    private FusedLocationProviderClient fusedLocationClient;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private UPCApiRequest apiReq;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapview);

        Button buttonBack = findViewById(R.id.backButton);
        buttonBack.setOnClickListener(v -> finish());

        // Load OSMDroid configuration
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("prefs", MODE_PRIVATE));

        Intent intent = getIntent();
        String storeName = intent.getStringExtra("store_name");
        if(storeName == null || storeName.isEmpty()){
            storeName = "";
        }else{

        }

        // Initialize the MapView
        mapView = findViewById(R.id.mapView);
        mapView.setMultiTouchControls(true);

        // Check for location permissions
        if (checkPermissions()) {

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            apiReq = new UPCApiRequest();

            String finalStoreName = storeName;
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.

                            if (location != null) {
                                loadMap(location.getLatitude(), location.getLongitude());

                                executorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        List<double[]> coordinates = apiReq.getStoreLocations(finalStoreName, location.getLatitude(), location.getLongitude());
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (!coordinates.isEmpty()) {
                                                    for (double[] cords : coordinates) {
                                                        GeoPoint storeLoc = new GeoPoint(cords[0], cords[1]);
                                                        addMarker(storeLoc, finalStoreName);
                                                    }
                                                } else {
                                                    Toast.makeText(MapActivity.this, "No store locations found", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }
                                });

                            }else{
                                loadMap(0,0);
                            }
                        }

                    });

        } else {
            requestPermissions();
        }
    }

    private void loadMap(double lat, double longi) {


        GeoPoint startPoint = new GeoPoint(lat, longi);
        mapView.getController().setZoom(14.0);
        mapView.getController().setCenter(startPoint);
        addMarker(startPoint, "Current Location");
    }

    private void addMarker(GeoPoint point, String title) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);
        mapView.getOverlays().add(marker);
        mapView.invalidate();  // Refresh the map
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION
        }, REQUEST_PERMISSIONS_REQUEST_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                finish();
                overridePendingTransition(0, 0);
                startActivity(getIntent());
                overridePendingTransition(0, 0);
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();  // Needed to manage the MapView lifecycle
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();  // Needed to manage the MapView lifecycle
    }
}
