package com.group1.scansaver.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.group1.scansaver.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapview);

        // Load OSMDroid configuration
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("prefs", MODE_PRIVATE));

        // Initialize the MapView
        mapView = findViewById(R.id.mapView);
        mapView.setMultiTouchControls(true);

        // Check for location permissions
        if (checkPermissions()) {
            // Permissions granted, proceed to load the map
            loadMap();
        } else {
            // Request permissions
            requestPermissions();
        }
    }

    private void loadMap() {
        // Set initial map zoom and location (example: New York City)
        GeoPoint startPoint = new GeoPoint(40.7128, -74.0060);
        mapView.getController().setZoom(12.0);
        mapView.getController().setCenter(startPoint);
        addMarker(startPoint, "New York City");
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
                // Permission granted, load the map
                loadMap();
            } else {
                // Permission denied, handle appropriately (e.g., show a message)
                finish();  // Close the activity if permission is denied
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
