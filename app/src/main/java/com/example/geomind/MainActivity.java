package com.example.geomind;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    LinearLayout loginLayout, signupLayout;
    EditText loginUsername, loginPassword;
    EditText signupUsername, signupEmail, signupPassword, confirmPassword;
    Button loginButton, registerButton;
    TextView goToSignup, goToLogin;

    static HashMap<String, String> users = new HashMap<>();
    SharedPreferences prefs;
    Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginLayout = findViewById(R.id.loginLayout);
        signupLayout = findViewById(R.id.signupLayout);
        loginUsername = findViewById(R.id.loginUsername);
        loginPassword = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);
        goToSignup = findViewById(R.id.goToSignup);
        signupUsername = findViewById(R.id.signupUsername);
        signupEmail = findViewById(R.id.signupEmail);
        signupPassword = findViewById(R.id.signupPassword);
        confirmPassword = findViewById(R.id.confirmPassword);
        registerButton = findViewById(R.id.registerButton);
        goToLogin = findViewById(R.id.goToLogin);

        prefs = getSharedPreferences("GeoMindPrefs", MODE_PRIVATE);

        // Load saved users from SharedPreferences
        loadUsers();

        // Navigate to Signup
        goToSignup.setOnClickListener(view -> {
            loginLayout.setVisibility(View.GONE);
            signupLayout.setVisibility(View.VISIBLE);
        });

        // Navigate to Login
        goToLogin.setOnClickListener(view -> {
            signupLayout.setVisibility(View.GONE);
            loginLayout.setVisibility(View.VISIBLE);
        });

        // Login logic
        loginButton.setOnClickListener(view -> {
            String username = loginUsername.getText().toString().trim();
            String password = loginPassword.getText().toString();

            if (!users.containsKey(username)) {
                Toast.makeText(this, "User not found. Please register.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (users.get(username).equals(password)) {
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, Afterlogin.class);
                intent.putExtra("loginUsername", username);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Incorrect password.", Toast.LENGTH_SHORT).show();
            }
        });

        // Register logic
        registerButton.setOnClickListener(view -> {
            String username = signupUsername.getText().toString().trim();
            String email = signupEmail.getText().toString().trim();
            String password = signupPassword.getText().toString();
            String confirm = confirmPassword.getText().toString();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (users.containsKey(username)) {
                Toast.makeText(this, "Username already exists!", Toast.LENGTH_SHORT).show();
                return;
            }

            users.put(username, password);
            saveUsers(); // Save updated users HashMap
            Toast.makeText(this, "Registered Successfully! You can login now.", Toast.LENGTH_SHORT).show();
            signupLayout.setVisibility(View.GONE);
            loginLayout.setVisibility(View.VISIBLE);
        });
    }

    private void loadUsers() {
        String json = prefs.getString("users", null);
        if (json != null) {
            Type type = new TypeToken<HashMap<String, String>>() {}.getType();
            users = gson.fromJson(json, type);
        }
    }

    private void saveUsers() {
        SharedPreferences.Editor editor = prefs.edit();
        String json = gson.toJson(users);
        editor.putString("users", json);
        editor.apply();
    }
}
