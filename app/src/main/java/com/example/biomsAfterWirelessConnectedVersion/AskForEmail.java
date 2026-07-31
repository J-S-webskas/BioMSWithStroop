package com.example.biomsAfterWirelessConnectedVersion;

import static android.content.ContentValues.TAG;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class AskForEmail extends AppCompatActivity {

    private EditText et_email;
    private Button send_code;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ask_for_email);

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
        send_code = findViewById(R.id.send_code);
        et_email = findViewById(R.id.et_email);

        send_code.setOnClickListener(v -> {
            String email = et_email.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                et_email.setError("Email is required");
                et_email.requestFocus();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                et_email.setError("Please enter a valid email address");
                et_email.requestFocus();
                return;
            }
            if (DataStorageManager.emailExists(this, email)) {
                sendResetEmail(email);
                startCooldown();
            } else {
                Toast.makeText(this, "Email does not exist", Toast.LENGTH_SHORT).show();
                et_email.setError("No email found");
                et_email.requestFocus();
            }
        });
    }
    private void startCooldown() {
        // Disable button and change appearance
        send_code.setEnabled(false);
        send_code.setText("Resend code");
        send_code.setAlpha(0.5f); // Dim the button

        // Start 60-second cooldown
        handler.postDelayed(new Runnable() {
            int seconds = 60;

            @Override
            public void run() {
                seconds--;
                if (seconds > 0) {
                    send_code.setText("Wait " + seconds + "s");
                    handler.postDelayed(this, 1000);
                } else {
                    // Re-enable button
                    send_code.setEnabled(true);
                    send_code.setText("Send Code");
                    send_code.setAlpha(1.0f);
                }
            }
        }, 1000);
    };

    /**
     * Generate a secure reset token
     */
    private String generateResetToken(String email) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String input = email + timestamp + new SecureRandom().nextLong();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.encodeToString(hash, Base64.URL_SAFE | Base64.NO_PADDING);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error generating token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Save reset token for verification
     */
    private void saveResetToken(String email, String token) {
        SharedPreferences prefs = getSharedPreferences("reset_prefs", MODE_PRIVATE);
        prefs.edit()
                .putString("reset_token_" + email, token)
                .putLong("reset_timestamp_" + email, System.currentTimeMillis())
                .apply();
    }

    /**
     * Send email using Intent
     */
    private void sendResetEmail(String email) {
        // Generate a unique reset token
        String resetToken = generateResetToken(email);

        // Save the token (with timestamp) for verification
        saveResetToken(email, resetToken);

        // Build deep link (preferred) and web fallback
        String deepLink = "biomsapp://reset-password?token=" + resetToken + "&user=" + email;

        // Send email with both links
        sendEmail(email, deepLink);
    }

    private void sendEmail(String email, String deepLink) {
        String subject = "Password Reset Request";

        // Build HTML body with clickable link
        String body = "Click the link below to reset your password:\n\n" +
                deepLink + "\n\n" +
                "If you didn't request this, please ignore this email.\n\n" +
                "Warm regards,\n" +
                "BioMS Team";

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/plain");  // Changed to HTML
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);  // Use Html.fromHtml

        try {
            startActivity(Intent.createChooser(emailIntent, "Send Email"));
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                showOpenGmailDialog();
            }, 1500);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No email app installed", Toast.LENGTH_SHORT).show();
            copyLinkToClipboard(deepLink);
        }
    }

    /**
     * Show a dialog asking if the user wants to open Gmail
     */
    private void showOpenGmailDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Open Gmail?")
                .setMessage("Would you like to open Gmail to check your reset email?")
                .setPositiveButton("Open Gmail", (dialog, which) -> openGmailApp())
                .setNegativeButton("Later", (dialog, which) -> {
                    Toast.makeText(this, "Check your email for the reset link", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    /**
     * Open Gmail app directly
     */
    private void openGmailApp() {
        try {
            // Try to open Gmail app
            Intent gmailIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
            if (gmailIntent != null) {
                startActivity(gmailIntent);
            } else {
                // If Gmail is not installed, open browser to Gmail
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com"));
                startActivity(browserIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening Gmail: " + e.getMessage());
            Toast.makeText(this, "Unable to open Gmail. Please check your email app.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Build formatted email body
     */
    private String buildEmailBody(String resetLink) {
        return "Hi!\n\n" +
                "We received a request to reset your password.\n\n" +
                "Click the link below to reset your password:\n" +
                resetLink + "\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you didn't request this, please ignore this email.\n\n" +
                "Warm regards,\n" +
                "BioMS Team";
    }

    /**
     * Copy reset link to clipboard as fallback
     */
    private void copyLinkToClipboard(String resetLink) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Password Reset Link", resetLink);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Reset link copied to clipboard", Toast.LENGTH_LONG).show();
    }
}
