package com.example.bioms1;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.bioms1.databinding.FragmentCameraBinding;


import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
Real-time display, without data collection and storage.
Camera frame -> MediaPipe processing -> Coordinate data -> Draw to screen -> Discard
 */

public class CameraFragment extends Fragment implements PoseLandmarkerHelper.LandmarkerListener {

    private static final String TAG = "Pose Landmarker";

    private FragmentCameraBinding _fragmentCameraBinding;

    private FragmentCameraBinding getFragmentCameraBinding() {
        return _fragmentCameraBinding;
    }

    private PoseLandmarkerHelper poseLandmarkerHelper;
    private MainViewModel viewModel;
    private Preview preview;
    private ImageAnalysis imageAnalyzer;  // Only analyze the current frame, do not save.
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private int cameraFacing = CameraSelector.LENS_FACING_BACK;

    private ExecutorService backgroundExecutor;

    private void showCountdownOverlay() {
        // Find a suitable container - either the viewFinder's parent or the fragment's root
        ViewGroup container;

        // Try to get the viewFinder's parent (which should be the camera preview container)
        ViewParent parent = getFragmentCameraBinding().viewFinder.getParent();
        if (parent instanceof ViewGroup) {
            container = (ViewGroup) parent;
        } else {
            // Fallback to the fragment's root view
            container = (ViewGroup) getFragmentCameraBinding().getRoot();
        }

        // Create overlay view
        View overlayView = new View(requireContext());
        overlayView.setBackgroundColor(Color.parseColor("#80000000")); // Semi-transparent black

        // Add overlay to container
        container.addView(overlayView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Create countdown text
        TextView countdownText = new TextView(requireContext());
        countdownText.setTextColor(Color.WHITE);
        countdownText.setTextSize(72);
        countdownText.setGravity(Gravity.CENTER);
        container.addView(countdownText, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Start countdown animation
        new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                countdownText.setText(String.valueOf(secondsLeft));

                // Add scale animation
                countdownText.animate()
                        .scaleX(1.5f)
                        .scaleY(1.5f)
                        .setDuration(500)
                        .withEndAction(() -> {
                            countdownText.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(500)
                                    .start();
                        })
                        .start();
            }

            @Override
            public void onFinish() {
                // Remove overlay and countdown text
                container.removeView(overlayView);
                container.removeView(countdownText);

                // Start camera
                setUpCamera();
            }
        }.start();
    }

    private void switchCamera() {
        if (cameraProvider == null) {
            return;
        }

        // Toggle camera facing
        if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
            cameraFacing = CameraSelector.LENS_FACING_FRONT;
        } else {
            cameraFacing = CameraSelector.LENS_FACING_FRONT;
        }

        // Rebind use cases with new camera selector
        bindCameraUseCases();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Toast.makeText(requireContext(), "Permission not granted", Toast.LENGTH_SHORT).show();
        }

        // Start the PoseLandmarkerHelper again when users come back
        // to the foreground.
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (poseLandmarkerHelper != null) {
                    if (poseLandmarkerHelper.isClose()) {
                        poseLandmarkerHelper.setupPoseLandmarker();
                    }
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (poseLandmarkerHelper != null) {
            viewModel.setMinPoseDetectionConfidence(poseLandmarkerHelper.minPoseDetectionConfidence);
            viewModel.setMinPoseTrackingConfidence(poseLandmarkerHelper.minPoseTrackingConfidence);
            viewModel.setMinPosePresenceConfidence(poseLandmarkerHelper.minPosePresenceConfidence);
            viewModel.setDelegate(poseLandmarkerHelper.currentDelegate);

            // Close the PoseLandmarkerHelper and release resources
            backgroundExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    poseLandmarkerHelper.clearPoseLandmarker();
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        _fragmentCameraBinding = null;
        super.onDestroyView();

        // Shut down our background executor
        backgroundExecutor.shutdown();
        try {
            backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Error shutting down executor", e);
        }
    }


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false);
        return _fragmentCameraBinding.getRoot();
    }

    // THIS IS WHERE THE FLOW STARTS: Calls setUpCamera()
    @SuppressLint("MissingPermission")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize view model (class)
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor();

        showCountdownOverlay();

        ImageButton btnSwitchCamera = view.findViewById(R.id.btn_switch_camera);
        btnSwitchCamera.setOnClickListener(v -> switchCamera());

//        Button btnCancel = view.findViewById(R.id.btn_cancel);
//        btnCancel.setOnClickListener(v -> {
//            stopCamera();
//
//            requireActivity().getSupportFragmentManager().popBackStack();  // pop back to record fragment so the topmost fragment within fragment_container is record fragment
//
//            /// Pops back to record page
//            // post delays the execution of the code below to make sure that popBackStack completes showing recordFragment/navigating bac
//            v.post(() -> {
//                // Code flow is now showing RecordFragment
//                Fragment currentFragment = requireActivity()
//                        .getSupportFragmentManager()
//                        .findFragmentById(R.id.fragment_container);
//                // calling the method of recordfragment
//                if (currentFragment instanceof RecordFragment) {
//                    ((RecordFragment) currentFragment).stopCurrentTestAndDiscardData();
//                }
//            });
//        });

        // Wait for the views to be properly laid out
        getFragmentCameraBinding().viewFinder.post(new Runnable() {
            @Override
            public void run() {
                // Set up the camera and its use cases
                setUpCamera();
            }
        });

        // Create the PoseLandmarkerHelper that will handle the inference. backgroundExecutor → new PoseLandmarkerHelper()
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                poseLandmarkerHelper = new PoseLandmarkerHelper(
                        viewModel.getCurrentMinPoseDetectionConfidence(),    // 1st: float
                        viewModel.getCurrentMinPoseTrackingConfidence(),     // 2nd: float
                        viewModel.getCurrentMinPosePresenceConfidence(),     // 3rd: float
                        viewModel.getCurrentModel(),                         // 4th: int currentModel
                        viewModel.getCurrentDelegate(),                      // 5th: int currentDelegate
                        RunningMode.LIVE_STREAM,                             // 6th: RunningMode
                        requireContext(),                                    // 7th: Context
                        CameraFragment.this                                  // 8th: LandmarkerListener
                );
            }
        });
    }

    // Update the values displayed in the bottom sheet. Reset Poselandmarker
    // helper.
    private void updateControlsUi() {
        if (poseLandmarkerHelper != null) {
            // Needs to be cleared instead of reinitialized because the GPU
            // delegate needs to be initialized on the thread using it when applicable
            backgroundExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    poseLandmarkerHelper.clearPoseLandmarker();
                    poseLandmarkerHelper.setupPoseLandmarker();
                }
            });
            getFragmentCameraBinding().overlay.clear();
        }
    }

    public void stopCamera() {
        Log.d(TAG, "Stopping camera...");

        // 1. Unbind camera use cases
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }

        // 2. Clear pose landmarker
        if (poseLandmarkerHelper != null) {
            poseLandmarkerHelper.clearPoseLandmarker();
            poseLandmarkerHelper = null;
        }

        // 3. Clear overlay
        if (_fragmentCameraBinding != null && _fragmentCameraBinding.overlay != null) {
            _fragmentCameraBinding.overlay.clear();
            _fragmentCameraBinding.overlay.invalidate();
        }

        // 4. Shutdown executor (if you want to completely stop)
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
        }

        Log.d(TAG, "Camera stopped");
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private void setUpCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // CameraProvider
                    cameraProvider = cameraProviderFuture.get();

                    // Build and bind the camera use cases
                    bindCameraUseCases();
                } catch (Exception e) {
                    Log.e(TAG, "Camera initialization failed", e);
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /*
    Preview (4:3 + rotation) <-> ImageAnalyzer (4:3 + rotation)
    What user sees                 What ML processes
    Same coordinate system       Same coordinate system
    overlay.setResults() can correctly map landmarks
    */
    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private void bindCameraUseCases() {
        // CameraProvider
        ProcessCameraProvider cameraProvider = this.cameraProvider;
        if (cameraProvider == null) {
            throw new IllegalStateException("Camera initialization failed.");
        }

        // Front/back camera view
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraFacing)
                .build();

        // Preview. Only using the 4:3 ratio because matches ML model input for mediapipe (rotation-aware)
        preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(getFragmentCameraBinding().viewFinder.getDisplay().getRotation())  //  Syncs preview rotation with device display rotation so image doesn't appear sideways when device rotates
                .build();

        // ImageAnalysis. Using RGBA 8888 to match how our models work. latest frame
        imageAnalyzer = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(getFragmentCameraBinding().viewFinder.getDisplay().getRotation())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        // The analyzer can then be assigned to the instance. Attach surface provider
        imageAnalyzer.setAnalyzer(backgroundExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                detectPose(image);
            }
        });

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll();

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            // Camera starts streaming to both preview and analyzer simultaneously
            // Automatically pauses/resumes with fragment
            camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
            );

            // Attach the viewfinder's surface provider to preview use case
            // SurfaceProvider = Interface that provides a drawing surface
            preview.setSurfaceProvider(getFragmentCameraBinding().viewFinder.getSurfaceProvider());
        } catch (Exception exc) {
            Log.e(TAG, "Use case binding failed", exc);
        }
    }

    // Processes raw image
    private void detectPose(ImageProxy imageProxy) {
        if (poseLandmarkerHelper != null) {
            // MediaPipe processing on background thread
            poseLandmarkerHelper.detectLiveStream(
                    imageProxy,
                    cameraFacing == CameraSelector.LENS_FACING_FRONT
            );
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (imageAnalyzer != null) {
            imageAnalyzer.setTargetRotation(getFragmentCameraBinding().viewFinder.getDisplay().getRotation());
        }
    }

    // Update UI after pose have been detected. Extracts original
    // Returns normalized [0,1] coordinates
    // image height/width to scale and place the landmarks properly through
    // OverlayView
    @Override
    public void onResults(PoseLandmarkerHelper.ResultBundle resultBundle) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (_fragmentCameraBinding == null) {
                        return;  // Fragment view is destroyed
                    }

                    // Check if this is a clear signal (empty results)
                    if (resultBundle.results == null || resultBundle.results.isEmpty()) {
                        // Clear the overlay - no skeleton to display
                        Log.d(TAG, "Clearing overlay - no pose detected");
                        getFragmentCameraBinding().overlay.clear();
                        getFragmentCameraBinding().overlay.invalidate();
                        return;  // Don't try to draw anything
                    }
                    try {
                    PoseLandmarkerResult result = resultBundle.results.get(0);
                    if (_fragmentCameraBinding != null && result != null && result.landmarks() != null && !result.landmarks().isEmpty()) {

                        // Pass necessary information to OverlayView for drawing on the canvas
                        getFragmentCameraBinding().overlay.setResults(
                                resultBundle.results.get(0), // Coordinates of the current frame
                                resultBundle.inputImageHeight,
                                resultBundle.inputImageWidth,
                                RunningMode.LIVE_STREAM
                        );

                        // Force a redraw
                        // Android doesn't automatically redraw when data changes / when the user moves
                        // Without invalidate(), first pose appears, then nothing updates,
                        // screen stays frozen with old pose data, new detections happen but aren't shown
                        // Without it, the landmarks wouldn't appear or update on screen
                        getFragmentCameraBinding().overlay.invalidate();
                    } else {
                        // Invalid result - clear overlay
                        Log.w(TAG, "Invalid pose result received");
                        getFragmentCameraBinding().overlay.clear();
                        getFragmentCameraBinding().overlay.invalidate();
                    }
                    } catch (Exception e) {
                        Log.e(TAG, "Error displaying pose: " + e.getMessage());
                        getFragmentCameraBinding().overlay.clear();
                        getFragmentCameraBinding().overlay.invalidate();
                    }
                }
            });
        }
    }

    @Override
    public void onError(String error, int errorCode) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onError(String error) {

    }
}