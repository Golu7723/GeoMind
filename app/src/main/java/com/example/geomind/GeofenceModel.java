package com.example.geomind;

public class GeofenceModel {
    public double latitude;
    public double longitude;
    public float radius;
    public String message;

    public GeofenceModel(double lat, double lng, float rad, String msg) {
        this.latitude = lat;
        this.longitude = lng;
        this.radius = rad;
        this.message = msg;
    }

    // Add these getters
    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getRadius() {
        return radius;
    }

    public String getMessage() {
        return message;
    }

    // Optional if you want to assign a name
    public String getName() {
        return "Geofence@" + latitude + "," + longitude;  // or whatever makes sense
    }
}
