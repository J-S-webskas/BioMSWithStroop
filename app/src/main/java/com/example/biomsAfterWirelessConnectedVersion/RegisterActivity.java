package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etUsername;
    private EditText etPassword;
    private Button btnCreateAccount;
    private Button btnGoBack;
    private EditText etEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_account);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.white));

            // Make status bar icons dark (for better visibility on white background)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = window.getDecorView();
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        // Initialize views
        etName = findViewById(R.id.et_name);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnCreateAccount = findViewById(R.id.btn_login);
        btnGoBack = findViewById(R.id.btn_go_back);
        etEmail = findViewById(R.id.et_email);

        // Create account button click
        btnCreateAccount.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            // Validate input
            if (TextUtils.isEmpty(name)) {
                etName.setError("Name required");
                etName.requestFocus();
                return;
            }

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

            // Check password length
            if (password.length() < 6) {
                etPassword.setError("Password must be at least 6 characters");
                etPassword.requestFocus();
                return;
            }

            // Create user
            User newUser = new User(name, username, password, email);
            if (DataStorageManager.userExists(this, username)) {
                Toast.makeText(this, "Username already exists. Please choose another.", Toast.LENGTH_SHORT).show();
                etUsername.setError("Username taken");
                etUsername.requestFocus();
                return;
            }

            // MUST SET CURRENT USER SO PROFILE CAN UPDATE INFORMATION CORRECTLY
            DataStorageManager.setCurrentUser(this, username);
            DataStorageManager.saveRegistrationDate(this, newUser.getUsername(), newUser.getCurrentDateTime());
            DataStorageManager.saveEmail(this, newUser.getUsername(), newUser.getEmail());
            DataStorageManager.savePassword(this, newUser.getUsername(), newUser.getPassword());
            Intent intent = new Intent(RegisterActivity.this, ChooseYourSex.class);
            startActivity(intent);
        });

        // Go back button click
        btnGoBack.setOnClickListener(v -> {
            finish();
        });
    }
}
