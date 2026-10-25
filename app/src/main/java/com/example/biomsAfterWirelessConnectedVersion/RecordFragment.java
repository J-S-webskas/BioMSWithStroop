package com.example.biomsAfterWirelessConnectedVersion;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecordFragment extends Fragment {
    private static final int SPEED_TEST_REQUEST = 100;
    private Handler autoStopHandler;
    private static final int REQUEST_CODE_CANCEL_TEST = 200;
    private Runnable autoStopRunnable;
    private static final int BASELINE_DURATION_MS = 33000;
    private static final int DUAL_TASK_DURATION_MS = 33000;
    private static final int SENSORY_DURATION_MS = 33000;
    private static boolean hasValidPoseDuringTest = false;
    private MainViewModel viewModel;
    private float meterType = 0;
    private String username = "123";
    private static float frameRate = 0;
    private Activity activity;
    private Context context;
    private AssessmentData currentAssessment = new AssessmentData(username);
    private Calendar baselineTestDate;
    private static Map<String, ArrayDeque<Float>> angleHistories = new HashMap<>();
    private static  List<COMSample> COMHistories = new ArrayList<>();
    private static Map<String, ArrayDeque<Float>> BalanceHistories = new HashMap<>();

    public static void addCOMPerFrame(double comXFinal, double comZFinal, long timestampMs) {
        if (COMHistories == null) {
            COMHistories = new ArrayList<>();
        }
        COMHistories.add(new COMSample(comXFinal, comZFinal, timestampMs));
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setActivity(FragmentActivity fragmentActivity) {
        this.activity = fragmentActivity;
    }
    private enum TestType {NONE, BASELINE, DUAL_TASK, SENSORY_ATAXIA}
    private TestType currentTestType = TestType.NONE;
    private static boolean isTestStarted = false;
    private TextView cancellingTest;
    private TextView dataNotSaved;
    private String currentSessionId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        String assessmentType = viewModel.getAssessmentType();
        /// If the test is not cancelled (finished), change the test cancelled messages to "Saving Test"
        /// And the dataNotSaved message to "If this is a mistake, you can delete the test"
//        setupTestControls(view);

    }

    public void setViewModel(MainViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // RETURN NULL - This fragment has NO UI
        // It runs purely in the background as a controller
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        return null;
    }

    private void setupTestControls(View view) {
        cancellingTest = view.findViewById(R.id.cancellingTest);
        dataNotSaved = view.findViewById(R.id.dataNotSaved);
    }

    public static void setHasValidPose(boolean hasPose) {
        if (isTestStarted) {
            hasValidPoseDuringTest = hasPose;
        }
    }

    public static boolean getHasValidPose() {
        return hasValidPoseDuringTest;
    }

    public static void resetPoseFlag() {
        hasValidPoseDuringTest = false;
        isTestStarted = false;
    }

    public static void setTestStarted(boolean started) {
        isTestStarted = started;
    }

    private void setNavigationVisible(boolean visible) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisible(visible);
        }
    }

    public static boolean isTestStarted() {
        return isTestStarted;
    }

    private void startPreviewMode() {
        // Check if camera permission is granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("Camera", "Camera permission not granted");
            Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.setPreviewMode(true);
        /// CALL CAMERA
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, new CameraFragment())
                .addToBackStack("camera")
                .commitAllowingStateLoss();
    }

    public void startTest(String assessmentType, Context context) {
        if (assessmentType.equals("SwayAreaAndBalance")) {
            startSwayAreaAndBalanceTest(context);
        } else if (assessmentType.equals("DualTask")) {
            startDualTaskTest(context);
        } else if (assessmentType.equals("SensoryAtaxia")) {
            startSensoryAtaxiaTest(context);
        } else if (assessmentType.equals("SpeedAndEndurance")) {
            startWSAndEnduranceTest();
        }
    }

    public void startCooldown() {
        viewModel.setCooldownActive(true);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            viewModel.setCooldownActive(false);
            viewModel.setShouldDiscard(false);  // Reset flag after cooldown
            if (isAdded() && getActivity() != null) {
                Toast.makeText(requireContext(), "Ready to start new test", Toast.LENGTH_SHORT).show();
            }
        }, 5000);
    }

    public static void HipBalanceaddAnglePerFrame(String name, float hip_angle) {
        ArrayDeque<Float> HipAngles = BalanceHistories.get(name);
        if (HipAngles == null) {
            HipAngles = new ArrayDeque<>();
            BalanceHistories.put(name, HipAngles);
        }
        HipAngles.add(hip_angle);
    }
    public static void KneeBalanceaddAnglePerFrame(String name, float knee_angle) {
        ArrayDeque<Float> KneeAngles = BalanceHistories.get(name);
        if (KneeAngles == null) {
            KneeAngles = new ArrayDeque<>();
            BalanceHistories.put(name, KneeAngles);
        }
        KneeAngles.add(knee_angle);
    }

    // Add angle per frame from PoseLandmarkerHelper to Map<String, ArrayDeque<Float>> angleHistories. Called from PoseLandmarkerHlper
    public static void addAnglePerFrame(String name, float angle) {
        // Gets reference to the ArrayDeque inside of angleHistories
        ArrayDeque<Float> angles = angleHistories.get(name);
        if (angles == null) {
            // The angleHistories's arrayDeque is created if there isn't anything
            angles = new ArrayDeque<>();
            // Store the reference IN the map (key = name, value = deque)
            angleHistories.put(name, angles);
        }
        angles.add(angle);  // updates the deque that angleHistory.get(name) references
    }

    public void stopCurrentTestAndDiscardData() {
//        if (!isAdded() || getActivity() == null) {
//            return;
//        }

        if (currentTestType == TestType.NONE) {
            Toast.makeText(context, "No test running", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), CancellingTest.class);
            intent.putExtra("simplyNavigatingToMainPage", true);
            startActivity(intent);
            return;
        }
        // Cancel the auto-stop timer
        if (autoStopHandler != null && autoStopRunnable != null) {
            autoStopHandler.removeCallbacks(autoStopRunnable);
        }
        String testName = getTestName(currentTestType);
        // Clear test state
        TestType cancelledTest = currentTestType;
        currentTestType = TestType.NONE;
        if (angleHistories != null) {
            angleHistories.clear();
        }
        Toast.makeText(context, testName + " test cancelled - results not saved", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(activity, CancellingTest.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("finished", true);
        intent.putExtra("Saving", false);
        intent.putExtra("noPoseDetected", true);
        intent.putExtra("cancelledMidway", true);
        startActivity(intent);
        setNavigationVisible(true);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private static String getTestName(TestType testType) {
        switch (testType) {

            case BASELINE:
                return "Baseline";

            case DUAL_TASK:
                return "Dual task";

            case SENSORY_ATAXIA:
                return "Sensory ataxia";

            default:
                return "Test";
        }
    }

    private void startWSAndEnduranceTest() {
        if (currentTestType != TestType.NONE) { // If the user clicks another test while this test is running, discard this test data
            stopCurrentTestAndDiscardData();
            return;
        }
        /// CALL Speed
        Bundle bundle = getArguments();
        String distance = bundle != null ? bundle.getString("meterType") : "0";
        Intent intent = new Intent(requireContext(), WSAndEndurance.class);
        intent.putExtra("meterType", distance);
        startActivityForResult(intent, SPEED_TEST_REQUEST);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        // This is where you get data FROM HowWillYouTime
        Bundle bundle = getArguments();
        if (bundle != null) {
            meterType = bundle.getFloat("meterType", 0);
            // This data comes from HowWillYouTime
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        currentTestType = TestType.BASELINE;
        baselineTestDate = Calendar.getInstance();
        currentTestType = TestType.NONE;
        if (requestCode == SPEED_TEST_REQUEST && resultCode == RESULT_OK) {
            // Get the data from Speed activity
            long timeInMillis = data.getLongExtra("testDuration", 0);
            float distance = meterType;
            float timeInSeconds = timeInMillis / 1000.0f; // Convert to seconds
            float speed = distance/timeInSeconds;

            StepMetrics.Result stepResult = null;
            long[] stepTimestamps = data.getLongArrayExtra("stepTimestampsNanos");
            float[] stepMagnitudes = data.getFloatArrayExtra("stepMagnitudes");
            if (stepTimestamps != null && stepMagnitudes != null
                    && stepTimestamps.length == stepMagnitudes.length) {
                ArrayList<Long> timestamps = new ArrayList<>();
                ArrayList<Float> magnitudes = new ArrayList<>();
                for (long t : stepTimestamps) timestamps.add(t);
                for (float m : stepMagnitudes) magnitudes.add(m);
                stepResult = StepMetrics.compute(timestamps, magnitudes, timeInSeconds, distance);
            }

            // Save to assessment
            try {
                saveWSAndEnduranceResults(speed, distance, stepResult);  // pass the data!
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void doesNotHaveValidPose() {
        // No pose detected - don't save, but still navigate back
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(),
                    "❌ No pose detected. Please ensure you're visible to the camera.",
                    Toast.LENGTH_LONG).show();
        }
        currentTestType = TestType.NONE;
        resetPoseFlag();
        Intent intent = new Intent(activity, CancellingTest.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("finished", true);
        intent.putExtra("Saving", false);
        startActivity(intent);
        setNavigationVisible(true);
    }

    private static void clearAllAccumulators() {
        if (angleHistories != null) angleHistories.clear();
        if (COMHistories != null) COMHistories.clear();
        if (BalanceHistories != null) BalanceHistories.clear();
    }

    private void startSwayAreaAndBalanceTest(Context context) {
        if (currentTestType != TestType.NONE) { // If the user clicks another test while this test is running, discard this test data
            stopCurrentTestAndDiscardData();
            return;
        }
        clearAllAccumulators();

        // Check if camera permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("Camera", "Camera permission not granted");
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        blockTouchEvents(true, context);  // Before adding CameraFragment
        setNavigationVisible(false);

        // Start baseline recording
        currentTestType = TestType.BASELINE;
        baselineTestDate = Calendar.getInstance();
        Toast.makeText(context, "Sway area and balance test starts after countdown", Toast.LENGTH_LONG).show();

        var auto_test_handler = new Handler(Looper.getMainLooper());
        var auto_test_runner = new Runnable() {
            @Override
            public void run() {
                setNavigationVisible(true);
                if (currentTestType == TestType.BASELINE) {
                    setNavigationVisible(true);
                    // Check if test was canceled before saving
                    if (viewModel.shouldDiscard()) {
                        startCooldown();
                        stopCurrentTestAndDiscardData();
                        viewModel.setShouldDiscard(false);  // Reset flag
                        return;
                    }
                    if (!RecordFragment.getHasValidPose())  {
//                      does set should discard equal true, because get valid pose should run
                        viewModel.setCooldownActive(true);
                        startCooldown();
                        doesNotHaveValidPose();
                        RecordFragment.setTestStarted(false);
                        return;
                    }
                    try {
                        saveSwayAreaAndBalanceResults();
                        if (isAdded() && getActivity() != null) {
                            FragmentManager fm = getActivity().getSupportFragmentManager();
                            Fragment fragment = fm.findFragmentById(R.id.fragment_container);
                            if (fragment instanceof CameraFragment) {
                                CameraFragment cameraFragment = (CameraFragment) fragment;
                                cameraFragment.stopCamera();
                            }
                        }
                        Toast.makeText(context, "Sway area and balance test completed!",
                                Toast.LENGTH_SHORT).show();
                        blockTouchEvents(false, context);  // In onResume or after camera stops
                        Intent intent = new Intent(activity, CancellingTest.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("finished", false);
                        intent.putExtra("Saving", true);
                        startActivity(intent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    currentTestType = TestType.NONE;
                }
            }
        };
        auto_test_handler.postDelayed(auto_test_runner, BASELINE_DURATION_MS); // 15 seconds = 15000ms
    }

    private void startSensoryAtaxiaTest(Context context) {
        // Check if camera permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("Camera", "Camera permission not granted");
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentTestType != TestType.NONE) { // If the user clicks another test while this test is running, discard this test data
            stopCurrentTestAndDiscardData();
            return;
        }
        clearAllAccumulators();
        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(context);

        boolean hasSwabEver = false;
        for (AssessmentData assessment : allAssessment) {
            if (assessment.getSwayAreaAndBalanceTestDate() != null ||
                    (assessment.getBaseline() != null && assessment.getBaseline().getBalanceScore() > 0)) {
                hasSwabEver = true;
                break;
            }
        }

        if (!hasSwabEver) {
            Toast.makeText(context, "Record sway area and balance test first!", Toast.LENGTH_SHORT).show();
            return;
        }

        setNavigationVisible(false);
        blockTouchEvents(true, context);  // Before adding CameraFragment

        // Create sensory ataxia test
        currentTestType = TestType.SENSORY_ATAXIA;
        Toast.makeText(context, "Sensory ataxia test starts after countdown", Toast.LENGTH_LONG).show();

        autoStopHandler = new Handler(Looper.getMainLooper());
        autoStopRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentTestType == TestType.SENSORY_ATAXIA) {
                    try {
                        setNavigationVisible(true);
                        // Check if test was canceled before saving
                        if (viewModel.shouldDiscard()) {
                            startCooldown();
                            stopCurrentTestAndDiscardData();
                            viewModel.setShouldDiscard(false);  // Reset flag
                            return;
                        }
                        if (!RecordFragment.getHasValidPose())  {
//                      does set should discard equal true, because get valid pose should run
                            viewModel.setCooldownActive(true);
                            startCooldown();
                            doesNotHaveValidPose();
                            RecordFragment.setTestStarted(false);
                            return;
                        }
                        saveSensoryAtaxiaResults();
                        if (isAdded() && getActivity() != null) {
                            FragmentManager fm = getActivity().getSupportFragmentManager();
                            Fragment fragment = fm.findFragmentById(R.id.fragment_container);
                            if (fragment instanceof CameraFragment) {
                                CameraFragment cameraFragment = (CameraFragment) fragment;
                                cameraFragment.stopCamera();
                            }
                        }
                        Toast.makeText(context, "Sensory ataxia test completed!",
                                Toast.LENGTH_SHORT).show();
                        blockTouchEvents(false, context);  // In onResume or after camera stops
                        Intent intent = new Intent(activity, CancellingTest.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("finished", false);
                        intent.putExtra("Saving", true);
                        startActivity(intent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    currentTestType = TestType.NONE;
                }
            }
        };
        autoStopHandler.postDelayed(autoStopRunnable, SENSORY_DURATION_MS); // 15 seconds = 15000ms
    }

    private void startDualTaskTest(Context context) {

        // Check if camera permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("Camera", "Camera permission not granted");
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        clearAllAccumulators();
        Log.d(TAG, "startDualTaskTest - ENTERED method");
        if (currentTestType != TestType.NONE) { // If the user clicks another test while this test is running, discard this test data
            stopCurrentTestAndDiscardData();
            return;
        }
        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(context);

        boolean hasSwabEver = false;
        for (AssessmentData assessment : allAssessment) {
            if (assessment.getSwayAreaAndBalanceTestDate() != null ||
                    (assessment.getBaseline() != null && assessment.getBaseline().getBalanceScore() > 0)) {
                hasSwabEver = true;
                break;
            }
        }
        if (!hasSwabEver) {
            Toast.makeText(context, "Record sway area and balance first!", Toast.LENGTH_SHORT).show();
            return;
        }

        setNavigationVisible(false);
        blockTouchEvents(true, context);

        // Create dual task test
        currentTestType = TestType.DUAL_TASK;
        Toast.makeText(context, "Dual task test starts after countdown", Toast.LENGTH_LONG).show();

        autoStopHandler = new Handler(Looper.getMainLooper());
        autoStopRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentTestType == TestType.DUAL_TASK) {
                    try {
                        setNavigationVisible(true);
                        if (viewModel.shouldDiscard()) {
                            startCooldown();
                            stopCurrentTestAndDiscardData();
                            viewModel.setShouldDiscard(false);  // Reset flag
                            return;
                        }
                        if (!RecordFragment.getHasValidPose())  {
//                      does set should discard equal true, because get valid pose should run
                            viewModel.setCooldownActive(true);
                            startCooldown();
                            doesNotHaveValidPose();
                            RecordFragment.setTestStarted(false);
                            return;
                        }
                        saveDualTaskResults();
                        if (isAdded() && getActivity() != null) {
                            FragmentManager fm = getActivity().getSupportFragmentManager();
                            Fragment fragment = fm.findFragmentById(R.id.fragment_container);
                            if (fragment instanceof CameraFragment) {
                                CameraFragment cameraFragment = (CameraFragment) fragment;
                                cameraFragment.stopCamera();
                            }
                        }
                        Toast.makeText(requireContext(), "Dual task test completed!",
                                Toast.LENGTH_SHORT).show();
                        blockTouchEvents(false, context);  // In onResume or after camera stops
                        Intent intent = new Intent(activity, CancellingTest.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("finished", false);
                        intent.putExtra("Saving", true);
                        startActivity(intent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    currentTestType = TestType.NONE;
                }
            }
        };
        autoStopHandler.postDelayed(autoStopRunnable, DUAL_TASK_DURATION_MS);
    }

    public static void setFrameRate(float fps) {
        frameRate = fps;
    }


    private float getLatestBaselineBalance() {
        List<AssessmentData> all = DataStorageManager.loadAllAssessments(requireContext());
        for (AssessmentData a : all) {
            if (a.getBaseline() != null && a.getBaseline().getBalanceScore() > 0) {
                return a.getBaseline().getBalanceScore();
            }
        }
        return 0f;
    }

    /** Mean velocity of the most recent stored Baseline trial (the same
     *  trial getLatestBaselineBalance reads), for DTC comparisons. The
     *  per-trial saves used to read this from the fresh assessment's own
     *  empty Baseline slot, so velocity/path DTCs were never stored. */
    private float getLatestBaselineVelocity() {
        List<AssessmentData> all = DataStorageManager.loadAllAssessments(requireContext());
        for (AssessmentData a : all) {
            if (a.getBaseline() != null && a.getBaseline().getBalanceScore() > 0) {
                return a.getBaseline().getSwayMeanVelocity();
            }
        }
        return 0f;
    }

    /** Path length of the most recent stored Baseline trial. */
    private float getLatestBaselinePathLength() {
        List<AssessmentData> all = DataStorageManager.loadAllAssessments(requireContext());
        for (AssessmentData a : all) {
            if (a.getBaseline() != null && a.getBaseline().getBalanceScore() > 0) {
                return a.getBaseline().getSwayPathLength();
            }
        }
        return 0f;
    }

    /**
     * Builds the CSV export package for the trial being saved (see
     * TrialCsvExporter). Must run while the trial's in-memory series still
     * exist — the history maps are cleared right after saving. Export
     * problems must never break saving, so everything is guarded.
     */
    /** Load the plausibility-gate bounds for this protocol (trial setup prefs). */
    private void applyPlausibilityGateConfig() {
        try {
            SharedPreferences p = requireContext().getSharedPreferences(
                    PoseLandmarkerHelper.TRIAL_SETUP_PREFS, Context.MODE_PRIVATE);
            BalanceCalculator.GATE_AP_RANGE_MM = p.getFloat("gate_ap_range_mm",
                    BalanceCalculator.DEFAULT_GATE_AP_RANGE_MM);
            BalanceCalculator.GATE_ML_RANGE_MM = p.getFloat("gate_ml_range_mm",
                    BalanceCalculator.DEFAULT_GATE_ML_RANGE_MM);
        } catch (Exception ignored) {
            // Defaults stay in effect.
        }
    }

    /** The CameraFragment that ran the test, wherever it currently sits
     *  in the fragment manager -- by save time it is not necessarily the
     *  fragment in the container (this RecordFragment may be). */
    private CameraFragment findCameraFragment() {
        try {
            if (getActivity() == null) return null;
            for (Fragment f : getActivity().getSupportFragmentManager().getFragments()) {
                if (f instanceof CameraFragment) {
                    return (CameraFragment) f;
                }
            }
        } catch (Exception ignored) {
            // Fall through: audit fields keep their defaults.
        }
        return null;
    }

    private void generateCsvExport(String conditionLabel, SwayMetrics metrics) {
        try {
            Map<String, List<Float>> jointSeries = new HashMap<>();
            for (Map.Entry<String, ArrayDeque<Float>> e : angleHistories.entrySet()) {
                jointSeries.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
            for (Map.Entry<String, ArrayDeque<Float>> e : BalanceHistories.entrySet()) {
                jointSeries.putIfAbsent(e.getKey(), new ArrayList<>(e.getValue()));
            }
            // The camera distance the user recorded this trial with
            // (entered + confirmed before the test); exported in the
            // SystemLog and stored on the assessment.
            try {
                SharedPreferences setupPrefs = requireContext().getSharedPreferences(
                        PoseLandmarkerHelper.TRIAL_SETUP_PREFS, Context.MODE_PRIVATE);
                metrics.cameraDistanceM = setupPrefs.getFloat(
                        PoseLandmarkerHelper.KEY_MANUAL_DISTANCE_M, 0f);
            } catch (Exception ignored) {
                // Distance stays 0 = none entered.
            }
            double fov = PoseLandmarkerHelper.DEFAULT_FOV_H_DEGREES;
            if (metrics != null) {
                // Anchor + phone-motion audit for the SystemLog. The
                // snapshot the pose helper took when THIS test ended is
                // the trustworthy source: by save time the camera
                // fragment may already be reset for the next test,
                // replaced in the container, or destroyed -- reading it
                // then produced the default tuple (0 / "none" / 0) and
                // the fallback FOV in past exports.
                PoseLandmarkerHelper.AnchorDiagnostics diag =
                        PoseLandmarkerHelper.getLastTrialDiagnostics();
                CameraFragment cam = findCameraFragment();
                if (diag == null && cam != null) {
                    diag = cam.getAnchorDiagnostics();
                }
                if (diag != null) {
                    metrics.anchorZ = diag.anchorZ;
                    metrics.anchorSource = diag.source;
                    metrics.calibFramesUsed = diag.calibFramesUsed;
                    metrics.calibLowConfidence = diag.lowConfidence;
                    metrics.anchorClampedFrames = diag.clampedFrames;
                    if (diag.fovDegrees > 0) {
                        fov = diag.fovDegrees;
                    }
                }
                if (cam != null) {
                    if (diag == null || diag.fovDegrees <= 0) {
                        fov = cam.getCurrentFovDegrees();
                    }
                    metrics.phoneRotationDeg = cam.getMaxPhoneRotationDeg();
                }
            }
            // Record the dropped-frame count and the FOV actually used
            // on the metrics object itself, so the save methods can
            // persist them with the assessment (an export generated
            // later from stored data then shows the same SystemLog as
            // this at-save export).
            metrics.droppedFrames = PoseLandmarkerHelper.getDroppedFrameCount();
            metrics.fovUsedDegrees = fov;
            TrialCsvExporter.exportTrial(requireContext(), conditionLabel, metrics,
                    jointSeries, metrics.droppedFrames, metrics.fovUsedDegrees);
            // Also store the joint angles as this trial's sidecar file,
            // so an export generated later from stored data can include
            // them (they are not part of the assessment itself -- see
            // TrialCsvExporter's sidecar note).
            if (currentAssessment != null) {
                TrialCsvExporter.saveJointAnglesSidecar(requireContext(),
                        currentAssessment.getSessionId(), conditionLabel,
                        jointSeries, metrics);
            }
        } catch (Exception e) {
            Log.e("RecordFragment", "CSV export failed", e);
        }
    }

    private void saveSensoryAtaxiaResults() throws IOException {
        AssessmentData trialAssessment = new AssessmentData(username);
        currentAssessment = trialAssessment;

        BalanceCalculator bc = new BalanceCalculator();
        applyPlausibilityGateConfig();
        SwayMetrics metrics = bc.computeSwayMetrics(COMHistories);
        float swayArea = metrics.swayAreaMm2;
        Map<String, Float> ARJScores = new HashMap<>();

        float baselineBalance = getLatestBaselineBalance();
        if (baselineBalance > 0) {
            float balanceChange = ((swayArea - baselineBalance) / baselineBalance) * 100f;
            currentAssessment.getComparisons().setSensoryAtaxiaBalanceChange(balanceChange);
        }

        float baselineVel = getLatestBaselineVelocity();
        if (baselineVel > 0) {
            float vChange = ((metrics.meanVelocityMmS - baselineVel) / baselineVel) * 100f;
            currentAssessment.getComparisons().setSensoryAtaxiaVelocityChange(vChange);
        }
        float baselinePath = getLatestBaselinePathLength();
        if (baselinePath > 0) {
            float pChange = ((metrics.pathLengthMm - baselinePath) / baselinePath) * 100f;
            currentAssessment.getComparisons().setSensoryAtaxiaPathLengthChange(pChange);
        }

        currentAssessment.getSensoryAtaxia().setBalanceScore(swayArea);
        currentAssessment.getSensoryAtaxia().setSwayPathLength(metrics.pathLengthMm);
        currentAssessment.getSensoryAtaxia().setSwayMeanVelocity(metrics.meanVelocityMmS);
        currentAssessment.getSensoryAtaxia().setSwayRmsAP(metrics.rmsAPMm);
        currentAssessment.getSensoryAtaxia().setSwayRmsML(metrics.rmsMLMm);
        currentAssessment.getSensoryAtaxia().setComTrajX(metrics.trajX);
        currentAssessment.getSensoryAtaxia().setComTrajZ(metrics.trajZ);
        currentAssessment.getSensoryAtaxia().setComFrameRate(metrics.frameRate);
        currentAssessment.getSensoryAtaxia().setSwayMetricsValid(metrics.valid);
        currentAssessment.getSensoryAtaxia().setSwayRangeFlag(metrics.rangeFlag);
        currentAssessment.getSensoryAtaxia().setARJ(ARJScores);
        currentAssessment.setStatus(AssessmentData.AssessmentStatus.SENSORY_ATAXIA_COMPLETED);
        currentAssessment.setSensoryAtaxiaTestDate(new Date());

        generateCsvExport("SensoryAtaxia", metrics);
        currentAssessment.getSensoryAtaxia().setCameraDistanceM(metrics.cameraDistanceM);
        // Persist the trial diagnostics with the assessment, so an
        // export generated later from stored data reproduces the full
        // at-save SystemLog. generateCsvExport has just enriched
        // `metrics` with the end-of-test anchor snapshot, the
        // dropped-frame count and the FOV actually used.
        currentAssessment.getSensoryAtaxia().setDiagnosticsRecorded(true);
        currentAssessment.getSensoryAtaxia().setDroppedFrames(metrics.droppedFrames);
        currentAssessment.getSensoryAtaxia().setInterpolatedFrames(metrics.interpolatedFrames);
        currentAssessment.getSensoryAtaxia().setDespikedFrames(metrics.despikedFrames);
        currentAssessment.getSensoryAtaxia().setFovUsedDegrees(metrics.fovUsedDegrees);
        currentAssessment.getSensoryAtaxia().setAnchorZ(metrics.anchorZ);
        currentAssessment.getSensoryAtaxia().setAnchorSource(metrics.anchorSource);
        currentAssessment.getSensoryAtaxia().setCalibFramesUsed(metrics.calibFramesUsed);
        currentAssessment.getSensoryAtaxia().setCalibLowConfidence(metrics.calibLowConfidence);
        currentAssessment.getSensoryAtaxia().setAnchorDisagreement(metrics.anchorDisagreement);
        currentAssessment.getSensoryAtaxia().setAnchorClampedFrames(metrics.anchorClampedFrames);
        currentAssessment.getSensoryAtaxia().setPhoneRotationDeg(metrics.phoneRotationDeg);
        currentAssessment.getSensoryAtaxia().setApRangeMm(metrics.apRangeMm);
        currentAssessment.getSensoryAtaxia().setMlRangeMm(metrics.mlRangeMm);
        DataStorageManager.saveAssessment(requireContext(), currentAssessment);

        angleHistories.clear();
        BalanceHistories.clear();

        Intent intent = new Intent(getActivity(), CancellingTest.class);
        intent.putExtra("finished", true);
        intent.putExtra("testSaved", true);
        startActivity(intent);
    }

    private void saveDualTaskResults() throws IOException {
        // One assessment file per trial, exactly like the Baseline and
        // Sensory Ataxia saves. This method used to load the shared
        // "today" assessment and write into its single DualTask slot,
        // so every dual-task recording of the day overwrote the same
        // slot of the same file  reports, which collect one trial per
        // file, could only ever show a single dual-task trial per day.
        AssessmentData trialAssessment = new AssessmentData(username);
        currentAssessment = trialAssessment;

        BalanceCalculator bc = new BalanceCalculator();
        applyPlausibilityGateConfig();
        SwayMetrics metrics = bc.computeSwayMetrics(COMHistories);
        float swayArea = metrics.swayAreaMm2;
        Map<String, Float> ARJScores = new HashMap<>();

        float baselineBalance = getLatestBaselineBalance();
        if (baselineBalance > 0) {
            float balanceChange = ((swayArea - baselineBalance) / baselineBalance) * 100f;
            currentAssessment.getComparisons().setDualTaskBalanceChange(balanceChange);
        }
        float baselineVel = getLatestBaselineVelocity();
        if (baselineVel > 0) {
            float vChange = ((metrics.meanVelocityMmS - baselineVel) / baselineVel) * 100f;
            currentAssessment.getComparisons().setDualTaskVelocityChange(vChange);
        }
        float baselinePath = getLatestBaselinePathLength();
        if (baselinePath > 0) {
            float pChange = ((metrics.pathLengthMm - baselinePath) / baselinePath) * 100f;
            currentAssessment.getComparisons().setDualTaskPathLengthChange(pChange);
        }
        currentAssessment.getDualTask().setBalanceScore(swayArea);
        currentAssessment.getDualTask().setSwayPathLength(metrics.pathLengthMm);
        currentAssessment.getDualTask().setSwayMeanVelocity(metrics.meanVelocityMmS);
        currentAssessment.getDualTask().setSwayRmsAP(metrics.rmsAPMm);
        currentAssessment.getDualTask().setSwayRmsML(metrics.rmsMLMm);
        currentAssessment.getDualTask().setComTrajX(metrics.trajX);
        currentAssessment.getDualTask().setComTrajZ(metrics.trajZ);
        currentAssessment.getDualTask().setComFrameRate(metrics.frameRate);
        currentAssessment.getDualTask().setSwayMetricsValid(metrics.valid);
        currentAssessment.getDualTask().setSwayRangeFlag(metrics.rangeFlag);
        currentAssessment.getDualTask().setARJ(ARJScores);

        // Persist the Stroop voice results recorded during this trial.
        // stopVoiceStroop()'s result used to be discarded entirely, so no
        // Stroop data ever reached storage or reports. The camera is still
        // running at this point (it is stopped right after this save), so
        // stop the voice test here and store what it recorded.
        try {
            StroopVoiceManager.VoiceStroopResult stroop = null;
            if (getActivity() != null) {
                for (Fragment f : getActivity().getSupportFragmentManager().getFragments()) {
                    if (f instanceof CameraFragment) {
                        StroopVoiceManager.VoiceStroopResult r =
                                ((CameraFragment) f).stopVoiceStroop();
                        if (r != null && (stroop == null || r.totalTrials > stroop.totalTrials)) {
                            stroop = r;
                        }
                    }
                }
            }
            if (stroop != null && stroop.totalTrials > 0) {
                DualTaskAssessment dt = currentAssessment.getDualTask();
                dt.setStroopTotalTrials(stroop.totalTrials);
                dt.setStroopCorrectCount(stroop.correctCount);
                dt.setStroopAccuracyPercent(stroop.accuracyPercent);
                dt.setStroopAvgReactionMs(stroop.avgReactionTimeMs);
                List<StroopAnswerRecord> records = new ArrayList<>();
                for (StroopVoiceManager.StroopAnswer a : stroop.answers) {
                    records.add(new StroopAnswerRecord(a.wordText, a.inkColorName,
                            a.spokenText, a.correct, a.reactionTimeMs));
                }
                dt.setStroopAnswers(records);
            }
        } catch (Exception e) {
            Log.e("RecordFragment", "Could not capture Stroop results", e);
        }

        currentAssessment.setStatus(AssessmentData.AssessmentStatus.DUAL_TASK_COMPLETED);
        currentAssessment.setDualTaskTestDate(new Date());

        generateCsvExport("DualTask", metrics);
        currentAssessment.getDualTask().setCameraDistanceM(metrics.cameraDistanceM);
        currentAssessment.getDualTask().setDiagnosticsRecorded(true);
        currentAssessment.getDualTask().setDroppedFrames(metrics.droppedFrames);
        currentAssessment.getDualTask().setInterpolatedFrames(metrics.interpolatedFrames);
        currentAssessment.getDualTask().setDespikedFrames(metrics.despikedFrames);
        currentAssessment.getDualTask().setFovUsedDegrees(metrics.fovUsedDegrees);
        currentAssessment.getDualTask().setAnchorZ(metrics.anchorZ);
        currentAssessment.getDualTask().setAnchorSource(metrics.anchorSource);
        currentAssessment.getDualTask().setCalibFramesUsed(metrics.calibFramesUsed);
        currentAssessment.getDualTask().setCalibLowConfidence(metrics.calibLowConfidence);
        currentAssessment.getDualTask().setAnchorDisagreement(metrics.anchorDisagreement);
        currentAssessment.getDualTask().setAnchorClampedFrames(metrics.anchorClampedFrames);
        currentAssessment.getDualTask().setPhoneRotationDeg(metrics.phoneRotationDeg);
        currentAssessment.getDualTask().setApRangeMm(metrics.apRangeMm);
        currentAssessment.getDualTask().setMlRangeMm(metrics.mlRangeMm);
        DataStorageManager.saveAssessment(requireContext(), currentAssessment);
        DataStorageManager.saveAssessment(requireContext(), currentAssessment);

        angleHistories.clear();
        BalanceHistories.clear();

        Intent intent = new Intent(getActivity(), CancellingTest.class);
        intent.putExtra("finished", true);
        intent.putExtra("testSaved", true);
        startActivity(intent);
    }

    private void saveSwayAreaAndBalanceResults() throws IOException {
        AssessmentData trialAssessment = new AssessmentData(username);
        currentAssessment = trialAssessment;

        BalanceCalculator bc = new BalanceCalculator();
        applyPlausibilityGateConfig();
        SwayMetrics metrics = bc.computeSwayMetrics(COMHistories);
        float swayArea = metrics.swayAreaMm2;
        currentAssessment.getBaseline().setBalanceScore(swayArea);
        currentAssessment.getBaseline().setSwayPathLength(metrics.pathLengthMm);
        currentAssessment.getBaseline().setSwayMeanVelocity(metrics.meanVelocityMmS);
        currentAssessment.getBaseline().setSwayRmsAP(metrics.rmsAPMm);
        currentAssessment.getBaseline().setSwayRmsML(metrics.rmsMLMm);
        currentAssessment.getBaseline().setComTrajX(metrics.trajX);
        currentAssessment.getBaseline().setComTrajZ(metrics.trajZ);
        currentAssessment.getBaseline().setComFrameRate(metrics.frameRate);
        currentAssessment.getBaseline().setSwayMetricsValid(metrics.valid);
        currentAssessment.getBaseline().setSwayRangeFlag(metrics.rangeFlag);
        currentAssessment.setStatus(AssessmentData.AssessmentStatus.SWAY_AREA_BALANCE_COMPLETED);
        currentAssessment.setSwayAreaAndBalanceTestDate(new Date());

        recalculateComparisons(currentAssessment);
        generateCsvExport("Baseline", metrics);
        currentAssessment.getBaseline().setCameraDistanceM(metrics.cameraDistanceM);
        currentAssessment.getBaseline().setDiagnosticsRecorded(true);
        currentAssessment.getBaseline().setDroppedFrames(metrics.droppedFrames);
        currentAssessment.getBaseline().setInterpolatedFrames(metrics.interpolatedFrames);
        currentAssessment.getBaseline().setDespikedFrames(metrics.despikedFrames);
        currentAssessment.getBaseline().setFovUsedDegrees(metrics.fovUsedDegrees);
        currentAssessment.getBaseline().setAnchorZ(metrics.anchorZ);
        currentAssessment.getBaseline().setAnchorSource(metrics.anchorSource);
        currentAssessment.getBaseline().setCalibFramesUsed(metrics.calibFramesUsed);
        currentAssessment.getBaseline().setCalibLowConfidence(metrics.calibLowConfidence);
        currentAssessment.getBaseline().setAnchorDisagreement(metrics.anchorDisagreement);
        currentAssessment.getBaseline().setAnchorClampedFrames(metrics.anchorClampedFrames);
        currentAssessment.getBaseline().setPhoneRotationDeg(metrics.phoneRotationDeg);
        currentAssessment.getBaseline().setApRangeMm(metrics.apRangeMm);
        currentAssessment.getBaseline().setMlRangeMm(metrics.mlRangeMm);
        DataStorageManager.saveAssessment(requireContext(), currentAssessment);

        angleHistories.clear();
        BalanceHistories.clear();
        Intent intent = new Intent(getActivity(), CancellingTest.class);
        intent.putExtra("finished", true);
        intent.putExtra("testSaved", true);
        startActivity(intent);
    }

    private void saveWSAndEnduranceResults(float speed, float endurance, StepMetrics.Result steps) throws IOException {
        AssessmentData trialAssessment = new AssessmentData(username);
        currentAssessment = trialAssessment;
        currentAssessment.getBaseline().setWalkingSpeed(speed);
        currentAssessment.getBaseline().setEnduranceScore(endurance);
        if (steps != null && steps.hasData) {
            currentAssessment.getBaseline().setStepCount(steps.stepCount);
            currentAssessment.getBaseline().setCadence(steps.cadence);
            currentAssessment.getBaseline().setMeanStepLength(steps.meanStepLengthM);
            currentAssessment.getBaseline().setStepTimeMeanS(steps.stepTimeMeanS);
            currentAssessment.getBaseline().setStepTimeSDS(steps.stepTimeSDS);
            currentAssessment.getBaseline().setStepTimeCVPct(steps.stepTimeCVPct);
            currentAssessment.getBaseline().setStepTimeN(steps.stepTimeN);
            currentAssessment.getBaseline().setStepMethod("imu");
            currentAssessment.getBaseline().setWalkPaused(steps.walkPaused);
        }
        currentAssessment.setStatus(AssessmentData.AssessmentStatus.WS_ENDURANCE_COMPLETED);
        currentAssessment.setWSAndEnduranceTestDate(new Date());

        DataStorageManager.saveAssessment(requireContext(), currentAssessment);

        angleHistories.clear();
        BalanceHistories.clear();
        if (getActivity() != null && isAdded()) {
            Intent intent = new Intent(getActivity(), CancellingTest.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("finished", true);
            intent.putExtra("Saving", true);
            intent.putExtra("testSaved", true);
            startActivity(intent);
        }
    }

    private View touchBlocker;

    private void blockTouchEvents(boolean block, Context context) {
        if (block) {
            if (touchBlocker == null) {
                touchBlocker = new View(context);
                touchBlocker.setBackgroundColor(Color.TRANSPARENT);
                touchBlocker.setClickable(true);
                touchBlocker.setFocusable(true);
                ViewGroup container = (ViewGroup) getView();
                if (container != null) {
                    container.addView(touchBlocker, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                }
            }
        } else {
            if (touchBlocker != null && getView() != null) {
                ((ViewGroup) getView()).removeView(touchBlocker);
                touchBlocker = null;
            }
        }
    }

    public void cancelTest() {
        if (autoStopHandler != null && autoStopRunnable != null) {
            autoStopHandler.removeCallbacks(autoStopRunnable);
        }
        if (currentTestType == TestType.NONE) {
            Toast.makeText(context, "No test running", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), CancellingTest.class);
            intent.putExtra("Saving", true);
            intent.putExtra("finished", true);
            startActivity(intent);
            return;
        }
        if (viewModel.shouldDiscard()) {
            stopCurrentTestAndDiscardData();
            return;
        }
        if (!getHasValidPose()) {
            doesNotHaveValidPose();
        }
    }

    private void recalculateComparisons(AssessmentData assessment) {
        if (assessment == null || assessment.getBaseline() == null) return;

        float newBaselineBalance = assessment.getBaseline().getBalanceScore();
        float newBaselineVel = assessment.getBaseline().getSwayMeanVelocity();
        float newBaselinePath = assessment.getBaseline().getSwayPathLength();
//        Float newBaselineARJLeft = assessment.getBaseline().getARJ().get("left_knee");
//        Float newBaselineARJRight = assessment.getBaseline().getARJ().get("right_knee");

        // RECALCULATE DUAL TASK DTC
        DualTaskAssessment dt = assessment.getDualTask();
        if (dt != null) {
            if (newBaselineBalance > 0 && dt.getBalanceScore() > 0) {
                float dtBalanceChange = ((dt.getBalanceScore() - newBaselineBalance) / newBaselineBalance) * 100f;
                assessment.getComparisons().setDualTaskBalanceChange(dtBalanceChange);
            }

            if (newBaselineVel > 0 && dt.getSwayMeanVelocity() > 0) {
                float dtVelChange = ((dt.getSwayMeanVelocity() - newBaselineVel) / newBaselineVel) * 100f;
                assessment.getComparisons().setDualTaskVelocityChange(dtVelChange);
            }
            if (newBaselinePath > 0 && dt.getSwayPathLength() > 0) {
                float dtPathChange = ((dt.getSwayPathLength() - newBaselinePath) / newBaselinePath) * 100f;
                assessment.getComparisons().setDualTaskPathLengthChange(dtPathChange);
            }

//            if (newBaselineARJLeft != null && newBaselineARJLeft > 0 && dt.getARJ().get("left_knee") != null) {
//                float dtARJLeft = dt.getARJ().get("left_knee");
//                float dtLeftChange = ((dtARJLeft - newBaselineARJLeft) / newBaselineARJLeft) * 100f;
//                assessment.getComparisons().setDualTaskARJLeftKneeChange(dtLeftChange);
//            }
//
//            if (newBaselineARJRight != null && newBaselineARJRight > 0 && dt.getARJ().get("right_knee") != null) {
//                float dtARJRight = dt.getARJ().get("right_knee");
//                float dtRightChange = ((dtARJRight - newBaselineARJRight) / newBaselineARJRight) * 100f;
//                assessment.getComparisons().setDualTaskARJRightKneeChange(dtRightChange);
//            }
        }

        // RECALCULATE SENSORY ATAXIA DTC
        SensoryAtaxiaAssessment sa = assessment.getSensoryAtaxia();
        if (sa != null) {
            if (newBaselineBalance > 0 && sa.getBalanceScore() > 0) {
                float saBalanceChange = ((sa.getBalanceScore() - newBaselineBalance) / newBaselineBalance) * 100f;
                assessment.getComparisons().setSensoryAtaxiaBalanceChange(saBalanceChange);
            }

            if (newBaselineVel > 0 && sa.getSwayMeanVelocity() > 0) {
                float saVelChange = ((sa.getSwayMeanVelocity() - newBaselineVel) / newBaselineVel) * 100f;
                assessment.getComparisons().setSensoryAtaxiaVelocityChange(saVelChange);
            }
            if (newBaselinePath > 0 && sa.getSwayPathLength() > 0) {
                float saPathChange = ((sa.getSwayPathLength() - newBaselinePath) / newBaselinePath) * 100f;
                assessment.getComparisons().setSensoryAtaxiaPathLengthChange(saPathChange);
            }

//            if (newBaselineARJLeft != null && newBaselineARJLeft > 0 && sa.getARJ().get("left_knee") != null) {
//                float saARJLeft = sa.getARJ().get("left_knee");
//                float saLeftChange = ((saARJLeft - newBaselineARJLeft) / newBaselineARJLeft) * 100f;
//                assessment.getComparisons().setSensoryAtaxiaARJLeftKneeChange(saLeftChange);
//            }
//
//            if (newBaselineARJRight != null && newBaselineARJRight > 0 && sa.getARJ().get("right_knee") != null) {
//                float saARJRight = sa.getARJ().get("right_knee");
//                float saRightChange = ((saARJRight - newBaselineARJRight) / newBaselineARJRight) * 100f;
//                assessment.getComparisons().setSensoryAtaxiaARJRightKneeChange(saRightChange);
//            }
        }
    }
}