package com.example.geomind;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;

import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class Afterlogin extends Activity {

    private ListView listView;
    private GeofenceAdapter adapter;
    private List<GeofenceModel> geofenceList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_afterlogin);

        String username = getIntent().getStringExtra("LoginUsername");


        Button btnSetLocation = findViewById(R.id.btnSetLocation);
        btnSetLocation.setOnClickListener(view -> {
            Intent intent = new Intent(Afterlogin.this, MapActivity.class);
            intent.putExtra("LoginUsername", username);
            startActivity(intent);
        });

        listView = findViewById(R.id.geofenceListView);
        loadAndDisplayGeofences();
    }

    private void loadAndDisplayGeofences() {
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String json = prefs.getString("geofences", null);
        if (json != null) {
            Type type = new TypeToken<List<GeofenceModel>>() {}.getType();
            geofenceList = new Gson().fromJson(json, type);
        } else {
            geofenceList = new ArrayList<>();
        }

        adapter = new GeofenceAdapter(this, geofenceList);
        listView.setAdapter(adapter);
    }

    private void saveGeofences() {
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = new Gson().toJson(geofenceList);
        editor.putString("geofences", json);
        editor.apply();
    }

    private class GeofenceAdapter extends ArrayAdapter<GeofenceModel> {

        public GeofenceAdapter(Context context, List<GeofenceModel> geofences) {
            super(context, 0, geofences);
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_geofence, parent, false);
            }

            GeofenceModel g = getItem(position);

            TextView infoText = convertView.findViewById(R.id.geofenceInfo);
            Button btnDelete = convertView.findViewById(R.id.btnDelete);

            String info = "Lat: " + g.latitude +
                    "\nLng: " + g.longitude +
                    "\nRadius: " + g.radius +
                    "\nMsg: " + g.message;
            infoText.setText(info);

            btnDelete.setOnClickListener(v -> {
                remove(g);
                saveGeofences();
                notifyDataSetChanged();
                Toast.makeText(getContext(), "Geofence deleted", Toast.LENGTH_SHORT).show();
            });

            return convertView;
        }
    }
}
