package com.example.biomsAfterWirelessConnectedVersion;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class UserManager {
    private static final String PREF_NAME = "user_preferences";

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Login user - Uses DataStorageManager for secure verification
     */
    public static User loginUser(Context context, String username, String password) {
        // Check if user exists
        if (!DataStorageManager.userExists(context, username)) {
            Log.w(TAG, "User not found: " + username);
            return null;
        }

        // Verify password using secure hashing
        boolean isValid = DataStorageManager.verifyPassword(context, username, password);

        if (isValid) {
            // Load user data
            String email = DataStorageManager.getEmail(context, username);
            String registrationDate = DataStorageManager.getRegistrationDate(context, username);

            // Create user object (password is not stored in plain text)
            User user = new User(username, "", email, registrationDate);

            // Save current user session
            DataStorageManager.setCurrentUser(context, username);

            Log.d(TAG, "Login successful for: " + username);
            return user;
        } else {
            Log.w(TAG, "Invalid password for: " + username);
            return null;
        }
    }

    /**
     * Logout user - Clear session only
     */
    public static void logout(Context context) {
        DataStorageManager.clearCurrentUser(context);
        Log.d(TAG, "User logged out");
    }

    /**
     * Check if user is logged in
     */
    public static boolean isLoggedIn(Context context) {
        String username = DataStorageManager.getCurrentUser(context);
        return username != null && !username.isEmpty() && !username.equals("default_user");
    }

}