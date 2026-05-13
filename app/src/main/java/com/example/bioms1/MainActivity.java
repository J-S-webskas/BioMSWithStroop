package com.example.bioms1;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.bioms1.R;
import com.example.bioms1.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!DataStorageManager.isUserLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setStatusBarColor(com.google.android.material.R.color.design_default_color_secondary);

        // Then set your content view
        // If using DataBinding:
         ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        // Or without DataBinding:
        // setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set navigation listener
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment fragment = null;

            int itemId = item.getItemId();

            // Use if-else instead of switch for resource IDs
            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_trend) {
                fragment = new TrendFragment();
            } else if (itemId == R.id.nav_record) {
                fragment = new RecordFragment();
            } else if (itemId == R.id.nav_report) {
                fragment = new ReportFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });

        // Set Main as selected by default
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
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
}