package com.example.biomsAfterWirelessConnectedVersion;

import static android.content.ContentValues.TAG;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CANCEL_TEST = 200;

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scheduleAutoReportCheck();

        setStatusBarColor(com.google.android.material.R.color.design_default_color_background);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (DataStorageManager.getFirsttime(this)) {
            bottomNavigationView.setVisibility(View.GONE);
            Intent intent1 = new Intent(this, WelcomeToBioMS.class);
            startActivity(intent1);
            finish();
            return;
        }
        bottomNavigationView.setVisibility(View.VISIBLE);

        Intent intent = getIntent();
        Boolean navigate_to_home = intent.getBooleanExtra("avigate_to_home", false);
        Boolean cancelled = intent.getBooleanExtra("cancelled", false);
        Boolean finished = intent.getBooleanExtra("finished", false);
        Boolean saving = intent.getBooleanExtra("Saving", false);
        if (cancelled || finished || saving) {
            clearBackStack();
            AssessmentFragment fragment = new AssessmentFragment();
            this.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            selectBottomNavItem(R.id.nav_record);
            return;
        }
        if (navigate_to_home) {
            clearBackStack();
            HomeFragment fragment = new HomeFragment();
            this.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            selectBottomNavItem(R.id.nav_home);
            return;
        }

        if (intent.getBooleanExtra("navigate_to_assessment", false)) {
            // Load AssessmentFragment
            clearBackStack();
            AssessmentFragment fragment = new AssessmentFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
//                    .addToBackStack("Assessment fragment")
                    .commit();

            // Clear the flag so it doesn't trigger again
            intent.removeExtra("navigate_to_assessment");
            // Must reset flag!!!! IMPORTANT
            getIntent().removeExtra("navigate_to_assessment");
            selectBottomNavItem(R.id.nav_record);
            return;
        }

        // Set navigation listener
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment fragment = null;

            int itemId = item.getItemId();
            Log.d(TAG, "========== BOTTOM NAV CLICKED ==========");
            Log.d(TAG, "Item ID clicked: " + itemId);
            Log.d(TAG, "Item title: " + item.getTitle());

            // Use if-else instead of switch for resource IDs
            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
                Log.d(TAG, "Home selected");
            } else if (itemId == R.id.nav_trend) {
                Log.d(TAG, "Trend selected");
                fragment = new TrendFragment();
            } else if (itemId == R.id.nav_record) {
                Log.d(TAG, "Record selected");
                fragment = new AssessmentFragment();
            } else if (itemId == R.id.nav_report) {
                Log.d(TAG, "Report selected");
                fragment = new ReportFragment();
            }

            if (fragment != null) {
                clearBackStack();
                loadFragment(fragment);
                return true;
            }
            return false;
        });

        // Set Main as selected by default
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    public void setBottomNavigationVisible(boolean visible) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void loadFragment(Fragment fragment) {
        try {
            Log.d(TAG, "Loading fragment: " + fragment.getClass().getSimpleName());
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
            Log.d(TAG, "Fragment loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error loading fragment: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
    private void setStatusBarColor(int colorResId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, colorResId));

            // Adjust status bar icon colors (dark vs light)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                View decorView = window.getDecorView();
                int color = ContextCompat.getColor(this, colorResId);

                // Check if the color is light (icons should be dark)
                if (isColorLight(color)) {
                    decorView.setSystemUiVisibility(
                            decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    );
                } else {
                    decorView.setSystemUiVisibility(
                            decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    );
                }
            }
        }
    }

    private boolean isColorLight(int color) {
        double darkness = 0.299 * android.graphics.Color.red(color) +
                0.587 * android.graphics.Color.green(color) +
                0.114 * android.graphics.Color.blue(color);
        return darkness > 160;
    }

    public void selectBottomNavItem(int itemId) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(itemId);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);  // Store the new intent

        // Check for navigation flags
        if (intent.getBooleanExtra("navigate_to_assessment", false)) {
            clearBackStack();
            // Load AssessmentFragment
            AssessmentFragment fragment = new AssessmentFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();

            // Clear the flag so it doesn't trigger again
            intent.removeExtra("navigate_to_assessment");
            selectBottomNavItem(R.id.nav_record);
        }
    }
    private void clearBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        int count = fm.getBackStackEntryCount();
        for (int i = 0; i < count; i++) {
            fm.popBackStack();
        }
    }

    private void scheduleAutoReportCheck() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Create intent for the broadcast receiver
        Intent intent = new Intent(this, AutoReportReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set the alarm to trigger at 11:50 PM every day
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);  // 11 PM
        calendar.set(Calendar.MINUTE, 50);       // 50 minutes
        calendar.set(Calendar.SECOND, 0);

        // If the time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    // Fallback to inexact alarm if exact alarms are not allowed by the system/user
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException scheduling exact alarm: " + e.getMessage(), e);
            // Inexact alarm fallback in case SecurityException is thrown
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

}