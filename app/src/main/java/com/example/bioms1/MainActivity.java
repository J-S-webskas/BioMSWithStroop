package com.example.bioms1;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.bioms1.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
}