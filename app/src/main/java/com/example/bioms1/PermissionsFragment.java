package com.example.bioms1;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.Navigation;;

public class PermissionsFragment extends Fragment {

    private static final String[] PERMISSIONS_REQUIRED = new String[]{Manifest.permission.CAMERA};

    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(
                                getContext(),
                                "Permission request granted",
                                Toast.LENGTH_LONG
                        ).show();
                        navigateToCamera();
                    } else {
                        Toast.makeText(
                                getContext(),
                                "Permission request denied",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {
            navigateToCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void navigateToCamera() {
        getLifecycle().addObserver(new androidx.lifecycle.LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull androidx.lifecycle.LifecycleOwner source,
                                       @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_START) {
                    getLifecycle().removeObserver(this);
                }
            }
        });
    }

    /** Convenience method used to check if all permissions required by this app are granted */
    public static boolean hasPermissions(Context context) {
        for (String permission : PERMISSIONS_REQUIRED) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}