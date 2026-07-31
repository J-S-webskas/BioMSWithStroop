package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

public class ResetPassword extends AppCompatActivity {
    private static final String TAG = "ResetPasswordActivity";
    private static final String PREF_NAME = "reset_prefs";
    private static final String KEY_RESET_TOKEN = "reset_token_";
    private static final String KEY_RESET_TIMESTAMP = "reset_timestamp_";
    private static final long TOKEN_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24 hours
    private EditText et_username, et_password, et_password_confirm;
    String username;
    private Button save;
    private String emailFromLink;
    private String tokenFromLink;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reset_password);

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

        et_password = findViewById(R.id.et_password);
        et_password_confirm = findViewById(R.id.et_password_confirm);
        et_username = findViewById(R.id.et_username);
        save = findViewById(R.id.save);

        // Get data from deep link
        Uri data = getIntent().getData();
        if (data != null) {
            tokenFromLink = data.getQueryParameter("token");
            emailFromLink = data.getQueryParameter("user"); // "user" parameter is actually email

            // Verify token
            if (!TextUtils.isEmpty(tokenFromLink) && !TextUtils.isEmpty(emailFromLink)) {
                if (!verifyResetToken(emailFromLink, tokenFromLink)) {
                    Toast.makeText(this, "Invalid or expired reset link", Toast.LENGTH_LONG).show();
                    save.setEnabled(false);
                }
            }
        }

        save.setOnClickListener(v -> handleResetPassword());

    }

    // todo: check link validity, and resended link validity
    private boolean verifyResetToken(String email, String token) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String storedToken = prefs.getString("reset_token_" + email, null);
        long timestamp = prefs.getLong("reset_timestamp_" + email, 0);

        if (storedToken == null || !storedToken.equals(token)) {
            return false;
        }

        if (System.currentTimeMillis() - timestamp > TOKEN_EXPIRY_MS) {
            prefs.edit()
                    .remove("reset_token_" + email)
                    .remove("reset_timestamp_" + email)
                    .apply();
            return false;
        }

        return true;
    }

    private void clearResetToken(String email) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit()
                .remove("reset_token_" + email)
                .remove("reset_timestamp_" + email)
                .apply();
    }

    private void handleResetPassword() {
        String password = et_password.getText().toString().trim();
        String passwordConfirm = et_password_confirm.getText().toString().trim();

        username = et_username.getText().toString().trim();
        if (!DataStorageManager.userExists(this, username)) {
            Toast.makeText(this, "Invalid username", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if email exists
        if (!DataStorageManager.emailExists(this, emailFromLink)) {
            Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            et_password.setError("Password required");
            et_password.requestFocus();
            return;
        }

        if (password.length() < 6) {
            et_password.setError("Minimum 6 characters");
            et_password.requestFocus();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check token again if coming from link
        if (!TextUtils.isEmpty(tokenFromLink) && !TextUtils.isEmpty(emailFromLink)) {
            if (!verifyResetToken(emailFromLink, tokenFromLink)) {
                Toast.makeText(this, "Link expired. Request a new one.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Save password using email as identifier
        if (DataStorageManager.savePassword(this, username, password)) {
            clearResetToken(emailFromLink);
            Toast.makeText(this, "Password reset successful!", Toast.LENGTH_LONG).show();

            Intent intent = new Intent(ResetPassword.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Failed to reset password", Toast.LENGTH_SHORT).show();
        }
    }
}
