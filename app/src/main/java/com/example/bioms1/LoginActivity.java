package com.example.bioms1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;

import java.io.IOException;
import java.util.UUID;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Check if user is already logged in
        if (DataStorageManager.isUserLoggedIn(this)) {
            navigateToMainActivity();
            return;
        }

        // Initialize views
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);

        // Set click listeners
        btnLogin.setOnClickListener(v -> {
            try {
                performLogin();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        btnRegister.setOnClickListener(v -> showRegisterDialog());
    }

    private void performLogin() throws IOException {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username required");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password required");
            etPassword.requestFocus();
            return;
        }

        // Attempt login
        UserModel user = DataStorageManager.loginUser(this, username, password);

        if (user != null) {
            Toast.makeText(this, "Welcome " + user.getUsername(), Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
        } else {
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_LONG).show();
            etPassword.setText(""); // Clear password field
        }
    }

    private void showRegisterDialog() {
        // Create dialog view
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_register, null);

        EditText etNewUsername = dialogView.findViewById(R.id.et_new_username);
        EditText etNewPassword = dialogView.findViewById(R.id.et_new_password);
        EditText etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);
        EditText etEmail = dialogView.findViewById(R.id.et_email);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Account")
                .setView(dialogView)
                .setPositiveButton("Register", (dialog, which) -> {
                    String username = etNewUsername.getText().toString().trim();
                    String password = etNewPassword.getText().toString().trim();
                    String confirmPassword = etConfirmPassword.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();

                    // Validate registration input
                    if (TextUtils.isEmpty(username)) {
                        Toast.makeText(this, "Username required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (TextUtils.isEmpty(password)) {
                        Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!password.equals(confirmPassword)) {
                        Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(this, "Email required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Create new user
                    try {
                        UserModel newUser = new UserModel(
                                UUID.randomUUID().toString(),
                                username,
                                password,
                                email
                        );
                        DataStorageManager.saveUser(this, newUser);
                        Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}