package com.example.biomsAfterWirelessConnectedVersion;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SizeF;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import com.example.biomsAfterWirelessConnectedVersion.databinding.FragmentCameraBinding;


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

public class CameraFragment extends Fragment implements PoseLandmarkerHelper.LandmarkerListener, SensorEventListener {
    private View rootView;
    private static final String TAG = "Pose Landmarker";
    private static final int REQUEST_CODE_CANCEL_TEST = 200;
    private FragmentCameraBinding _fragmentCameraBinding;
    private RecordFragment recordFragment;
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
    private boolean isCountdownActive = false;
    private TextView countdownTextView;
    private View countdownOverlay;
    private String assessmentType = "";
    private Handler countdownHandler = new Handler(Looper.getMainLooper());
    private boolean isPreviewMode = true;
    private String SOURCE_ACTIVITY = "";
    Button btnCancel;

    private StroopVoiceManager stroopVoiceManager;

    // Phone-motion (gyro) audit: max rotation of the device itself since
    // the current test started -- read by the export's SystemLog so a
    // trial can be checked for camera motion after the fact.
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private boolean gyroArmed = false;
    private float[] gyroRefMatrix = null;
    private float maxPhoneRotationDeg = 0f;
    private LinearLayout stroopVoiceContainer;
    private TextView tvStroopWord;
    private TextView tvVoiceStatus;

    private void setupStroopVoiceOverlay() {
        if (getFragmentCameraBinding() != null && getFragmentCameraBinding().stroopOverlay != null) {
            // Get the root view of the included layout using DataBinding
            stroopVoiceContainer = (LinearLayout) getFragmentCameraBinding().stroopOverlay.getRoot();

            // Get child views directly from the included binding
            tvStroopWord = getFragmentCameraBinding().stroopOverlay.tvStroopWord;
            tvVoiceStatus = getFragmentCameraBinding().stroopOverlay.tvVoiceStatus;

            stroopVoiceContainer.setVisibility(View.GONE);
        }
    }

    public void onDualTaskStarted() {
        if (stroopVoiceContainer != null) {
            stroopVoiceContainer.setVisibility(View.VISIBLE);
        }

        // Without the mic permission the recognizer fails silently in a
        // restart loop and the test "hears" nothing at all. It is requested
        // on a different screen (MarkDistanceAndCamera), so gate on it here,
        // where it actually matters.
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO}, 2001);
            if (tvVoiceStatus != null) {
                tvVoiceStatus.setText("Microphone permission needed — grant it, then restart the test");
            }
            return;
        }

        stroopVoiceManager = new StroopVoiceManager(requireContext(), new StroopVoiceManager.StroopVoiceListener() {
            @Override
            public void onNewTrial(String wordText, int colorInt) {
                if (tvStroopWord != null) {
                    tvStroopWord.setText(wordText);
                    tvStroopWord.setTextColor(colorInt);
                }
            }

            @Override
            public void onSpokenResult(String spokenWord, boolean isCorrect) {
                if (tvVoiceStatus != null) {
                    tvVoiceStatus.setText("Heard: " + spokenWord + (isCorrect ? " ✓" : " ✗"));
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("StroopVoice", errorMessage);
                // Surface it: a log-only error made a missing/unavailable
                // recognition service look like "the test hears nothing."
                if (tvVoiceStatus != null) {
                    tvVoiceStatus.setText(errorMessage);
                }
                if (getContext() != null) {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });

        stroopVoiceManager.startTest();
    }

    public StroopVoiceManager.VoiceStroopResult stopVoiceStroop() {
        if (stroopVoiceContainer != null) {
            stroopVoiceContainer.setVisibility(View.GONE);
        }
        if (stroopVoiceManager != null) {
            return stroopVoiceManager.stopTest();
        }
        return null;
    }

    private void setViewsEnabled(boolean enabled) {
        if (rootView != null) {
            // Disable all clickable views in the fragment
            rootView.setEnabled(enabled);
            rootView.setClickable(enabled);
        }
    }

    private void showCountdown() {
        if (!isAdded() || getActivity() == null || _fragmentCameraBinding == null) {
            Log.e(TAG, "Cannot show countdown - fragment not ready");
            return;
        }
        isCountdownActive = true;

        // Fresh anchor calibration for this countdown.
        if (poseLandmarkerHelper != null) {
            poseLandmarkerHelper.resetCalibration();
        }

        // Find container
        ViewGroup container = (ViewGroup) getFragmentCameraBinding().getRoot();

        // Create overlay
        countdownOverlay = new View(requireContext());
        countdownOverlay.setBackgroundColor(Color.parseColor("#80000000"));
        container.addView(countdownOverlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Create countdown text
        countdownTextView = new TextView(requireContext());
        countdownTextView.setTextColor(Color.WHITE);
        countdownTextView.setTextSize(120);
        countdownTextView.setGravity(Gravity.CENTER);
        container.addView(countdownTextView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Start countdown from 3
        startCountdownSequence(3);
    }

    private void startCountdownSequence(int count) {
        if (!isAdded() || _fragmentCameraBinding == null) {
            Log.e(TAG, "Fragment not ready, stopping countdown");
            isCountdownActive = false;
            return;
        }
        if (countdownTextView == null) {
            Log.e(TAG, "Countdown TextView is null");
            isCountdownActive = false;
            return;
        }
        if (count > 0) {
            countdownTextView.setText(String.valueOf(count));
            // Animation
            countdownTextView.setScaleX(1.5f);
            countdownTextView.setScaleY(1.5f);
            countdownTextView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .start();

            // Schedule next count
            countdownHandler.postDelayed(() -> startCountdownSequence(count - 1), 1000);
        } else {
            countdownTextView.animate()
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                    .setDuration(500)
                    .withEndAction(() -> {
                        // Remove countdown UI
                        if (_fragmentCameraBinding != null && countdownOverlay != null && countdownTextView != null) {
                            try {
                                ViewGroup container = (ViewGroup) getFragmentCameraBinding().getRoot();
                                container.removeView(countdownOverlay);
                                container.removeView(countdownTextView);
                            } catch (Exception e) {
                                Log.e(TAG, "Error removing countdown views: " + e.getMessage());
                            }
                        }
                        countdownOverlay = null;
                        countdownTextView = null;

                        // Start the actual test
                        isCountdownActive = false;
                        // Freeze the COM anchor from the countdown
                        // calibration and start the phone-motion audit.
                        if (poseLandmarkerHelper != null) {
                            poseLandmarkerHelper.freezeAnchorForTest();
                        }
                        startPhoneMotionTracking();
                        // Tell RecordFragment that test has started
                        RecordFragment.setTestStarted(true);
                        PoseLandmarkerHelper.resetDroppedFrameCount();
                        lockFocusForTest();
                        if ("DualTask".equals(assessmentType)) {
                            onDualTaskStarted();
                        }
                    })
                    .start();
        }
    }
    private void switchCamera() {
        if (cameraProvider == null) {
            return;
        }

        // Toggle camera facing
        if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
            cameraFacing = CameraSelector.LENS_FACING_FRONT;
        } else {
            cameraFacing = CameraSelector.LENS_FACING_BACK;
        }

        // Rebind use cases with new camera selector
        bindCameraUseCases();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        // Check if we came back from background after CancellingTest

        if (getActivity() != null) {
            // Check if CancellingTest was shown
            boolean wasProcessing = getActivity().getIntent().getBooleanExtra("wasProcessing", false);

            // If we were in CancellingTest and now back, navigate to main screen
            if (wasProcessing) {
                // Go to AssessmentFragment or MainActivity
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                getActivity().finish();
            }
        }
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Toast.makeText(requireContext(), "Permission not granted", Toast.LENGTH_SHORT).show();
        }
        setViewsEnabled(true);

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

        // Register the rotation-vector sensor for the phone-motion audit.
        if (sensorManager == null && getContext() != null) {
            sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        }
        if (sensorManager != null) {
            rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            if (rotationVectorSensor != null) {
                sensorManager.registerListener(this, rotationVectorSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        gyroArmed = false;
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

    /** Begin (or restart) phone-motion tracking for a test that is starting. */
    public void startPhoneMotionTracking() {
        gyroArmed = true;
        gyroRefMatrix = null;
        maxPhoneRotationDeg = 0f;
    }

    /** Max device rotation (degrees) since the current test started. */
    public float getMaxPhoneRotationDeg() {
        return maxPhoneRotationDeg;
    }

    /** Anchor diagnostics from the pose helper (frozen Z, source, calibration size). */
    public PoseLandmarkerHelper.AnchorDiagnostics getAnchorDiagnostics() {
        return poseLandmarkerHelper != null
                ? poseLandmarkerHelper.getAnchorDiagnostics()
                : new PoseLandmarkerHelper.AnchorDiagnostics();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!gyroArmed || event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) {
            return;
        }
        float[] current = new float[9];
        SensorManager.getRotationMatrixFromVector(current, event.values);
        if (gyroRefMatrix == null) {
            gyroRefMatrix = current.clone();
            return;
        }
        float[] delta = new float[3];
        SensorManager.getAngleChange(delta, current, gyroRefMatrix);
        double magRad = Math.sqrt(delta[0] * delta[0] + delta[1] * delta[1] + delta[2] * delta[2]);
        float deg = (float) Math.toDegrees(magRad);
        if (deg > maxPhoneRotationDeg) {
            maxPhoneRotationDeg = deg;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for the audit.
    }

    /** The horizontal FOV (degrees) actually used for this phone's COM
     *  scale conversion — calculated from the camera characteristics, or
     *  the 60° default when the device does not report them. */
    public double getCurrentFovDegrees() {
        return poseLandmarkerHelper != null
                ? poseLandmarkerHelper.getFovDegrees()
                : PoseLandmarkerHelper.DEFAULT_FOV_H_DEGREES;
    }

    @Override
    public void onDestroyView() {
        // Preview must never inherit a test's focus lock: whatever path
        // leaves the test screen, the lock ends here.
        releaseFocusLock();
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
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false);
        return _fragmentCameraBinding.getRoot();
    }

    // THIS IS WHERE THE FLOW STARTS: Calls setUpCamera()
    @SuppressLint("MissingPermission")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnCancel = view.findViewById(R.id.btn_cancel);

        Bundle bundle = getArguments();
        if (bundle != null) {
            assessmentType = bundle.getString("assessmentType");
            boolean someoneRecord = bundle.getBoolean("someoneRecord");
            isPreviewMode = bundle.getBoolean("preview");
            SOURCE_ACTIVITY = bundle.getString("SOURCE_ACTIVITY");
            if ("DualTask".equals(assessmentType)) {
                cameraFacing = CameraSelector.LENS_FACING_FRONT;
            } else if (someoneRecord) {
                cameraFacing = CameraSelector.LENS_FACING_BACK;
            } else {
                cameraFacing = CameraSelector.LENS_FACING_FRONT;
            }
        }

        // Initialize view model (class)
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor();

        TextView tvInstruction = view.findViewById(R.id.tv_preview_instruction);
        if (isPreviewMode) {
            tvInstruction.setVisibility(VISIBLE);
        } else {
            tvInstruction.setVisibility(GONE);
        }

        ImageButton back = view.findViewById(R.id.back);
        back.setOnClickListener(v -> {
            if (SOURCE_ACTIVITY != null && SOURCE_ACTIVITY.equals("AssessmentFragment")) {
                previewCalledFromAssessmentFragmentExiting();
                return; // Don't proceed with RecordFragment logic
            }
            requireActivity().finish();// This closes the Activity and returns to whatever was before that Activity.
        });

        setupStroopVoiceOverlay();

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        // If in preview mode, allow back
                        if (isPreviewMode) {
                            if (SOURCE_ACTIVITY != null && SOURCE_ACTIVITY.equals("AssessmentFragment")) {
                                previewCalledFromAssessmentFragmentExiting();
                            } else {
                                requireActivity().finish();
                            }
                        }
                    }
                }
        );

        final String finalAssessmentType = assessmentType; // lambda variable seems to change the original value, so need a copy....
        if (!isPreviewMode) {
            // TEST MODE (direct entry) -> WILL NEVER HAPPEN I THINK
            back.setVisibility(GONE);
            btnCancel.setText("Cancel Test");
            btnCancel.setBackgroundColor(Color.BLACK);
            btnCancel.setTextColor(Color.WHITE);
        } else {
            // PREVIEW MODE
            if (SOURCE_ACTIVITY != null && SOURCE_ACTIVITY.equals("AssessmentFragment")) {
                btnCancel.setVisibility(GONE);
            } else {
                back.setVisibility(VISIBLE);
                btnCancel.setText("Start Test");
                btnCancel.setBackgroundColor(Color.WHITE);
                btnCancel.setTextColor(Color.GRAY);
                viewModel.setAssessmentType(finalAssessmentType);
            }

            btnCancel.setEnabled(false);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                btnCancel.setEnabled(true);
                btnCancel.setAlpha(1.0f);
            }, 3000);

            // todo: Must wait for 1 second before hitting btn Start test, or else the preview countdown is frozen on screen..unfixable yikes
            btnCancel.setOnClickListener(v -> {
                if (poseLandmarkerHelper != null) {
                    poseLandmarkerHelper.freezeAnchorForTest();
                }
                startPhoneMotionTracking();
                RecordFragment.setTestStarted(true);
                // This is in test view

                RecordFragment recordFragment = new RecordFragment();
                FragmentManager fm = getParentFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();

                // Add the new fragment but hide it immediately
                transaction.add(R.id.fragment_container, recordFragment);
                transaction.hide(recordFragment);  // Hidden but running
                transaction.addToBackStack(null);
                transaction.commit();
                recordFragment.setViewModel(viewModel);
                recordFragment.setContext(requireContext());
                recordFragment.setActivity(requireActivity());
                recordFragment.startTest(assessmentType, requireContext());
                Log.d("CameraFragment", "Background recording started!");

                // Switch to TEST MODE
                isPreviewMode = false;

                // Update UI
                back.setVisibility(GONE);
                btnCancel.setText("Cancel Test");
                btnCancel.setBackgroundColor(Color.BLACK);
                btnCancel.setTextColor(Color.WHITE);
                tvInstruction.setVisibility(GONE);

                // Cancel any existing countdown
                cancelCountdown();

                // Start a fresh countdown
                showCountdown();
                btnCancel.setEnabled(false);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    btnCancel.setEnabled(true);
                    btnCancel.setAlpha(1.0f);
                }, 3000);

                btnCancel.setOnClickListener(v2 -> {
                    RecordFragment.setTestStarted(false);
                     Fragment fragment = fm.findFragmentById(R.id.fragment_container);
                     if (fragment instanceof CameraFragment) {
                         CameraFragment cameraFragment = (CameraFragment) fragment;
                         cameraFragment.stopCamera();
                     }
                     viewModel.setShouldDiscard(true);
                     viewModel.setCooldownActive(true);
                     if (recordFragment != null) {
                         recordFragment.cancelTest();  // Direct method call!
                     }
                 });
            });
        }

        // Wait for the views to be properly laid out
        getFragmentCameraBinding().viewFinder.post(new Runnable() {
            @Override
            public void run() {
                // Set up the camera and its use cases
                setUpCamera();
                // Use a small delay to ensure camera is ready
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    showCountdown();
                }, 500);
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
                        CameraFragment.this,                                  // 8th: LandmarkerListener
                        viewModel
                );
            }
        });
    }

    private void cancelTest() {
        FragmentManager fm = getParentFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        RecordFragment.setTestStarted(false);
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment instanceof CameraFragment) {
            CameraFragment cameraFragment = (CameraFragment) fragment;
            cameraFragment.stopCamera();
        }
        viewModel.setShouldDiscard(true);
        viewModel.setCooldownActive(true);
        if (recordFragment != null) {
            recordFragment.cancelTest();
        }
    }

    public void previewCalledFromAssessmentFragmentExiting() {
        // Coming from AssessmentFragment - just close everything
        stopCamera();
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack(); // Returns to AssessmentFragment
        } else {
            // If no back stack, just finish
            requireActivity().finish();
        }
    }

    private void cancelCountdown() {
        countdownHandler.removeCallbacksAndMessages(null);

        // Remove the countdown UI if it exists
        if (countdownOverlay != null && countdownTextView != null) {
            try {
                ViewGroup container = (ViewGroup) getFragmentCameraBinding().getRoot();
                container.removeView(countdownOverlay);
                container.removeView(countdownTextView);
            } catch (Exception e) {
                Log.e(TAG, "Error removing countdown views: " + e.getMessage());
            }
            countdownOverlay = null;
            countdownTextView = null;
        }

        // Remove any pending callbacks
        countdownHandler.removeCallbacksAndMessages(null);
        isCountdownActive = false;
    }

    private void setupCancelButton(Button btnCancel) {
        btnCancel.setOnClickListener(v -> {
            stopCamera();

            // Start 15-second cooldown immediately
            if (getActivity() != null) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                Fragment cancelFragment = fm.findFragmentById(R.id.fragment_container);
                viewModel.setCooldownActive(true);
                Toast.makeText(requireContext(), "System is cleaning up. Please wait for a notice to continue testing",
                        Toast.LENGTH_SHORT).show();
            }

            viewModel.setShouldDiscard(true);
            viewModel.setCooldownActive(true);
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

    /**
     * Locks focus, exposure and white balance at the center of the frame for
     * the duration of a test — the iPhone-style AE/AF lock. Without it,
     * continuous autofocus keeps re-hunting as the patient sways or steps
     * closer, which pulses the image in and out and shifts the tracked
     * landmarks. The metering action never auto-cancels, so the lock holds
     * until releaseFocusLock() runs when the test ends.
     */
    public void lockFocusForTest() {
        if (camera == null || getFragmentCameraBinding() == null) return;
        try {
            PreviewView viewFinder = getFragmentCameraBinding().viewFinder;
            if (viewFinder.getWidth() <= 0 || viewFinder.getHeight() <= 0) return;
            SurfaceOrientedMeteringPointFactory factory =
                    new SurfaceOrientedMeteringPointFactory(viewFinder.getWidth(), viewFinder.getHeight());
            MeteringPoint center = factory.createPoint(
                    viewFinder.getWidth() / 2f, viewFinder.getHeight() / 2f);
            FocusMeteringAction action = new FocusMeteringAction.Builder(center,
                    FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB)
                    .disableAutoCancel()
                    .build();
            ListenableFuture<FocusMeteringResult> result =
                    camera.getCameraControl().startFocusAndMetering(action);
            result.addListener(() -> {
                try {
                    FocusMeteringResult r = result.get();
                    Log.d(TAG, "Focus locked for test, focus successful: "
                            + (r != null && r.isFocusSuccessful()));
                } catch (Exception e) {
                    Log.e(TAG, "Focus lock failed", e);
                }
            }, ContextCompat.getMainExecutor(requireContext()));
        } catch (Exception e) {
            Log.e(TAG, "Could not lock focus", e);
        }
    }

    /** Releases the test focus lock, returning to continuous autofocus. */
    public void releaseFocusLock() {
        if (camera == null) return;
        try {
            camera.getCameraControl().cancelFocusAndMetering();
            Log.d(TAG, "Focus lock released");
        } catch (Exception e) {
            Log.e(TAG, "Could not release focus lock", e);
        }
    }

    public void stopCamera() {
        releaseFocusLock();
        stopVoiceStroop();
        gyroArmed = false;
        // Stop accepting new frames
        if (imageAnalyzer != null) {
            imageAnalyzer.clearAnalyzer();
            imageAnalyzer = null;
        }

        // Clear pose landmarker
        if (poseLandmarkerHelper != null) {
            poseLandmarkerHelper.releaseAnchor();
            poseLandmarkerHelper.clearPoseLandmarker();
            poseLandmarkerHelper = null;
        }

        // Clear overlay
        if (_fragmentCameraBinding != null && _fragmentCameraBinding.overlay != null) {
            _fragmentCameraBinding.overlay.clear();
            _fragmentCameraBinding.overlay.invalidate();
        }

        // Unbind camera use cases
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }

        // Shutdown executor (if you want to completely stop)
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
        }
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

            updateDynamicFov();
        } catch (Exception exc) {
            Log.e(TAG, "Use case binding failed", exc);
        }
    }

    private void updateDynamicFov() {
        try {
            Context ctx = getContext();
            if (ctx != null && poseLandmarkerHelper != null) {
                CameraManager cameraManager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
                if (cameraManager != null) {
                    int targetFacing = (cameraFacing == CameraSelector.LENS_FACING_FRONT) ?
                            CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK;
                    for (String id : cameraManager.getCameraIdList()) {
                        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                        if (facing != null && facing == targetFacing) {
                            float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                            SizeF sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                            if (focalLengths != null && focalLengths.length > 0 && sensorSize != null && sensorSize.getWidth() > 0) {
                                double fovH = 2.0 * Math.toDegrees(Math.atan((sensorSize.getWidth() / 2.0) / focalLengths[0]));
                                poseLandmarkerHelper.setFovDegrees(fovH);
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception exc) {
            Log.e(TAG, "Since dynamic pov failed, defaulting to 60 degrees", exc);
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

                    // Check if this is a clear signal (if there is empty results: no person captured)
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CANCEL_TEST) {
            // Re-enable views
            setViewsEnabled(true);

            if (resultCode == getActivity().RESULT_OK && data != null) {
                boolean cancelled = data.getBooleanExtra("cancelled", false);
                boolean saving = data.getBooleanExtra("Saving", false);
                boolean finished = data.getBooleanExtra("finished", false);
                if (cancelled || finished || saving) {
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra("navigate_to_assessment", true);
                    startActivity(intent);
                }
            }
        }
    }
    @Override
    public void onError(String error) {

    }
}