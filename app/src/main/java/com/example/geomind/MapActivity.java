
package com.example.geomind;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map;
    private MyLocationNewOverlay locationOverlay;
    private final int MAX_GEOFENCES = 5;

    private final ArrayList<GeofenceData> geofences = new ArrayList<>();
    private boolean isSelecting = false;

    private static class GeofenceData {
        GeoPoint location;
        int radius;
        String triggerType;
        String message;
        Marker marker;
        Polygon circle;

        GeofenceData(GeoPoint location, int radius, String triggerType, String message, Marker marker, Polygon circle) {
            this.location = location;
            this.radius = radius;
            this.triggerType = triggerType;
            this.message = message;
            this.marker = marker;
            this.circle = circle;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        setContentView(R.layout.activity_map);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });

        setupMap();

        FloatingActionButton btnAddGeofence = findViewById(R.id.btnAddGeofence);
        btnAddGeofence.setOnClickListener(v -> {
            if (geofences.size() >= MAX_GEOFENCES) {
                Toast.makeText(this, "You can only add 5 geofences", Toast.LENGTH_SHORT).show();
                return;
            }
            showLocationSelectDialog();
        });
    }

    private void setupMap() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(this, "Please enable GPS", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                return;
            }

            locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            locationOverlay.enableMyLocation();
            map.getOverlays().add(locationOverlay);

            locationOverlay.runOnFirstFix(() -> {
                GeoPoint current = locationOverlay.getMyLocation();
                if (current != null) {
                    runOnUiThread(() -> {
                        map.getController().setZoom(18.0);
                        map.getController().setCenter(current);
                        loadAndDrawGeofences();
                    });
                }
                monitorGeofences();
            });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void loadAndDrawGeofences() {
        geofences.clear();
        map.getOverlays().clear();
        map.getOverlays().add(locationOverlay);

        List<GeofenceModel> savedList = loadGeofences(this);
        if (savedList.isEmpty()) {
            Toast.makeText(this, "Currently no geofence created. Tap '+' to set a location reminder.", Toast.LENGTH_LONG).show();
        }

        for (GeofenceModel data : savedList) {
            addGeofence(new GeoPoint(data.latitude, data.longitude), (int) data.radius, "Entry", data.message);
        }
        map.invalidate();
    }

    private void monitorGeofences() {
        new Thread(() -> {
            List<String> triggeredMessages = new ArrayList<>();
            while (true) {
                GeoPoint current = locationOverlay.getMyLocation();
                if (current != null) {
                    for (GeofenceData g : geofences) {
                        double distance = current.distanceToAsDouble(g.location);
                        if (distance <= g.radius && !triggeredMessages.contains(g.message)) {
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Entered Geofence: " + g.message, Toast.LENGTH_LONG).show();
                                sendNotification(g.message);
                            });
                            triggeredMessages.add(g.message);
                        }
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_REQUEST_CODE);
                return;
            }
        }
    }

    private void showLocationSelectDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_select_location, null);
        new AlertDialog.Builder(this)
                .setTitle("Select Geofence Location")
                .setView(view)
                .setPositiveButton("Next", (d, w) -> {
                    Toast.makeText(this, "Tap on map to select location", Toast.LENGTH_SHORT).show();
                    isSelecting = true;
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (isSelecting && event.getAction() == MotionEvent.ACTION_UP) {
            GeoPoint point = (GeoPoint) map.getProjection().fromPixels((int) event.getX(), (int) event.getY());
            isSelecting = false;
            showGeofenceConfigDialog(point);
        }
        return super.dispatchTouchEvent(event);
    }

    private void showGeofenceConfigDialog(GeoPoint point) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_geofence_config, null);
        EditText etRadius = view.findViewById(R.id.etRadius);
        Spinner spinner = view.findViewById(R.id.spinnerTrigger);
        EditText etMessage = view.findViewById(R.id.etMessage);

        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"Entry", "Exit", "Both"}));

        new AlertDialog.Builder(this)
                .setTitle("Configure Geofence")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    int radius = etRadius.getText().toString().isEmpty() ? 100 : Integer.parseInt(etRadius.getText().toString());
                    String type = spinner.getSelectedItem().toString();
                    String msg = etMessage.getText().toString();

                    addGeofence(point, radius, type, msg);
                    saveGeofences();
                    loadAndDrawGeofences();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addGeofence(GeoPoint point, int radius, String type, String msg) {
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);

        Polygon circle = new Polygon(map);
        circle.setPoints(Polygon.pointsAsCircle(point, radius));
        circle.setFillColor(0x4400FF00);
        circle.setStrokeColor(0xFF00AA00);
        circle.setStrokeWidth(2);
        map.getOverlays().add(circle);

        GeofenceData g = new GeofenceData(point, radius, type, msg, marker, circle);
        geofences.add(g);
        map.invalidate();

        marker.setOnMarkerClickListener((m, v) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Remove Geofence?")
                    .setPositiveButton("Yes", (d, w) -> {
                        map.getOverlays().remove(g.marker);
                        map.getOverlays().remove(g.circle);
                        geofences.remove(g);
                        saveGeofences();
                        loadAndDrawGeofences();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });
    }

    private void saveGeofences() {
        List<GeofenceModel> list = new ArrayList<>();
        for (GeofenceData g : geofences) {
            list.add(new GeofenceModel(g.location.getLatitude(), g.location.getLongitude(), g.radius, g.message));
        }
        SharedPreferences prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("geofences", new Gson().toJson(list)).apply();
    }

    public List<GeofenceModel> loadGeofences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString("geofences", null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<GeofenceModel>>() {}.getType();
        return new Gson().fromJson(json, type);
    }

    private void sendNotification(String message) {
        if (!checkNotificationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 200);
            return;
        }

        String CHANNEL_ID = "GEOFENCE_ALERT";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Geofence Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Location Reminder")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }
}
