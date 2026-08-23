package com.example.biomsAfterWirelessConnectedVersion;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;
import static android.content.Intent.getIntent;

import static java.util.ResourceBundle.getBundle;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

public class RecordFragment extends Fragment {
    private static final int SPEED_TEST_REQUEST = 100;
    private Handler autoStopHandler;
    private static final int REQUEST_CODE_CANCEL_TEST = 200;
    private Runnable autoStopRunnable;
    private static final int BASELINE_DURATION_MS = 18000;
    private static final int DUAL_TASK_DURATION_MS = 18000;
    private static final int SENSORY_DURATION_MS = 18000;
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
    private static  List<Point2D> COMHistories = new ArrayList<>();
    private static Map<String, ArrayDeque<Float>> BalanceHistories = new HashMap<>();

    public static void addCOMPerFrame(double comXFinal, double comZFinal) {
        if (COMHistories == null) {
            COMHistories = new ArrayList<>();
        }
        Point2D point = new Point2D(comXFinal, comZFinal);
        COMHistories.add(point);
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
        if (assessmentType.equals("ARJAndBalance")) {
            startARJAndBalanceTest(context);
        } else if (assessmentType.equals("DualTask")) {
            //todo: set context
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
        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(requireContext());
        if (!allAssessment.isEmpty()) {
            AssessmentData newest = allAssessment.get(0);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            String todayStr = sdf.format(new Date());
            boolean arjDoneToday = false;
            boolean wsDoneToday = false;
            boolean dualTaskDoneToday = false;
            boolean sensoryDoneToday = false;

            for (AssessmentData assessment : allAssessment) {
                String assessmentDate = sdf.format(assessment.getTimestamp());
                // Only check today's assessments
                if (assessmentDate.equals(todayStr)) {
                    if (assessment.getStatus() != null) {
                        switch (assessment.getStatus()) {
                            case ARJ_BALANCE_COMPLETED:
                                arjDoneToday = true;
                                break;
                            case WS_ENDURANCE_COMPLETED:
                                wsDoneToday = true;
                                break;
                            case DUAL_TASK_COMPLETED:
                                dualTaskDoneToday = true;
                                break;
                            case SENSORY_ATAXIA_COMPLETED:
                                sensoryDoneToday = true;
                                break;
                        }
                    }
                }
            }
            if (wsDoneToday) {
                Toast.makeText(requireContext(), "Speed & endurance was already completed for today. Record again tomorrow!", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        /// CALL Speed
        Bundle bundle = getArguments();
        String distance = bundle.getString("meterType");
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
        // ✅ This is where you get data FROM HowWillYouTime
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

            // Save to assessment
            try {
                saveWSAndEnduranceResults(speed, distance);  // ← Pass the data!
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

    private void startARJAndBalanceTest(Context context) {
        if (currentTestType != TestType.NONE) { // If the user clicks another test while this test is running, discard this test data
            stopCurrentTestAndDiscardData();
            return;
        }
        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(context);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        String todayStr = sdf.format(new Date());
        boolean arjDoneToday = false;
        boolean wsDoneToday = false;
        boolean dualTaskDoneToday = false;
        boolean sensoryDoneToday = false;

        for (AssessmentData assessment : allAssessment) {
            String assessmentDate = sdf.format(assessment.getTimestamp());
            // Only check today's assessments
            if (assessmentDate.equals(todayStr)) {
                // Check INDIVIDUAL test dates instead of combined status
                if (assessment.getARJAndBalanceTestDate() != null) {
                    arjDoneToday = true;
                }
                if (assessment.getWSAndEnduranceTestDate() != null) {
                    wsDoneToday = true;
                }
                if (assessment.getDualTaskTestDate() != null) {
                    dualTaskDoneToday = true;
                }
                if (assessment.getSensoryAtaxiaTestDate() != null) {
                    sensoryDoneToday = true;
                }
            }
        }
        if (arjDoneToday) {
            Toast.makeText(context, "ARJ & balance was already completed for today. Record again tomorrow!", Toast.LENGTH_LONG).show();
            return;
        }
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
        Toast.makeText(context, "ARJ and balance test starts after countdown - Walk normally for 15 seconds", Toast.LENGTH_LONG).show();

        var auto_test_handler = new Handler(Looper.getMainLooper());
        var auto_test_runner = new Runnable() {
            @Override
            public void run() {
                setNavigationVisible(true);
                if (currentTestType == TestType.BASELINE) {
//                    if (viewModel.isCooldownActive()) {
//                        startCooldown();
//                    }
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
                        saveARJAndBalanceResults();
                        if (isAdded() && getActivity() != null) {
                            FragmentManager fm = getActivity().getSupportFragmentManager();
                            Fragment fragment = fm.findFragmentById(R.id.fragment_container);
                            if (fragment instanceof CameraFragment) {
                                CameraFragment cameraFragment = (CameraFragment) fragment;
                                cameraFragment.stopCamera();
                            }
                        }
                        Toast.makeText(context, "ARJ and balance test completed!",
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
        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(context);
        if (allAssessment.isEmpty()) {
            Toast.makeText(context, "Record ARJ & balance first!", Toast.LENGTH_SHORT).show();
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        String todayStr = sdf.format(new Date());
        boolean arjDoneToday = false;
        boolean wsDoneToday = false;
        boolean dualTaskDoneToday = false;
        boolean sensoryDoneToday = false;

        for (AssessmentData assessment : allAssessment) {
            String assessmentDate = sdf.format(assessment.getTimestamp());
            // Only check today's assessments
            if (assessmentDate.equals(todayStr)) {
                // Check INDIVIDUAL test dates instead of combined status
                if (assessment.getARJAndBalanceTestDate() != null) {
                    arjDoneToday = true;
                }
                if (assessment.getWSAndEnduranceTestDate() != null) {
                    wsDoneToday = true;
                }
                if (assessment.getDualTaskTestDate() != null) {
                    dualTaskDoneToday = true;
                }
                if (assessment.getSensoryAtaxiaTestDate() != null) {
                    sensoryDoneToday = true;
                }
            }
        }

        if (!arjDoneToday) {
            Toast.makeText(context, "Record ARJ & balance first!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sensoryDoneToday) {
            Toast.makeText(context, "Sensory ataxia test was already completed for today. Record again tomorrow!", Toast.LENGTH_SHORT).show();
            return;
        }
        setNavigationVisible(false);
        blockTouchEvents(true, context);  // Before adding CameraFragment

        // Create sensory ataxia test
        currentTestType = TestType.SENSORY_ATAXIA;
        Toast.makeText(context, "Sensory ataxia test starts after countdown - Close your eyes and walk in view for 15 seconds", Toast.LENGTH_LONG).show();

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
        Log.d(TAG, "startDualTaskTest - ENTERED method");
        if (currentTestType != TestType.NONE) { // If the user clicks another test while this test is running, discard this test data
            stopCurrentTestAndDiscardData();
            return;
        }
        Log.d(TAG, "Passed the evaluation currentTestType != TestType.NONE");
        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(context);
        if (allAssessment.isEmpty()) {
            Toast.makeText(context, "Record ARJ & balance first!", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Passed the evaluation allAssessment.isEmpty(), if true return;");

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        String todayStr = sdf.format(new Date());
        boolean arjDoneToday = false;
        boolean wsDoneToday = false;
        boolean dualTaskDoneToday = false;
        boolean sensoryDoneToday = false;

        for (AssessmentData assessment : allAssessment) {
            String assessmentDate = sdf.format(assessment.getTimestamp());
            // Only check today's assessments
            if (assessmentDate.equals(todayStr)) {
                // Check INDIVIDUAL test dates instead of combined status
                if (assessment.getARJAndBalanceTestDate() != null) {
                    arjDoneToday = true;
                }
                if (assessment.getWSAndEnduranceTestDate() != null) {
                    wsDoneToday = true;
                }
                if (assessment.getDualTaskTestDate() != null) {
                    dualTaskDoneToday = true;
                }
                if (assessment.getSensoryAtaxiaTestDate() != null) {
                    sensoryDoneToday = true;
                }
            }
        }
        if (!arjDoneToday) {
            Toast.makeText(context, "Record ARJ & balance first!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dualTaskDoneToday) {
            Toast.makeText(context, "Dual task test was already completed for today. Record again tomorrow!", Toast.LENGTH_LONG).show();
            return;
        }
        setNavigationVisible(false);
        blockTouchEvents(true, context);

        // Create dual task test
        currentTestType = TestType.DUAL_TASK;
        Toast.makeText(context, "Dual task test starts after countdown - Walk while counting backwards from 15", Toast.LENGTH_LONG).show();

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

    private void saveSensoryAtaxiaResults() throws IOException {

        AssessmentData todayAssessment = DataStorageManager.getOrCreateTodayAssessment(requireContext(), username);
        currentAssessment = todayAssessment;

        float balance = BalanceCalculator.calculateOffBalance(
                BalanceHistories.get("left_hip"),
                BalanceHistories.get("left_knee"));
        Map<String, Float> ARJScores = new HashMap<>();

        for (String joint : Arrays.asList("left_knee", "right_knee")) {
            float arj = AverageRectifiedJerk.calculateAverageRectifiedJerk(
                    angleHistories.get(joint), frameRate);
            ARJScores.put(joint, arj);
        }
        // Dual Task Cost (DTC): (Sensory Task Performance - Baseline Performance) / Baseline Performance × 100
        float baselineBalance = currentAssessment.getBaseline().getBalanceScore();
        Log.d("DTC_DEBUG", "baselineBalance: " + baselineBalance);
        if (baselineBalance > 0) {
            float balanceChange = ((balance - baselineBalance) / baselineBalance) * 100f;
            Log.d("DTC_DEBUG", "Calculated balanceChange: " + balanceChange);
            currentAssessment.getComparisons().setSensoryAtaxiaBalanceChange(balanceChange);
        } else {
            Log.d("DTC_DEBUG", "baselineBalance <= 0, skipping DTC calculation");
        }

        Float baselineARJLeftObj = currentAssessment.getBaseline().getARJ().get("left_knee");
        Float baselineARJRightObj = currentAssessment.getBaseline().getARJ().get("right_knee");
        float baselineARJLeft = baselineARJLeftObj != null ? baselineARJLeftObj : 0f;
        float baselineARJRight = baselineARJRightObj != null ? baselineARJRightObj : 0f;
        if (baselineARJLeft > 0 && baselineARJRight > 0) {
            float ARJLeftChange = ((ARJScores.get("left_knee") - baselineARJLeft) / baselineARJLeft) * 100f;
            float ARJRightChange = ((ARJScores.get("right_knee") - baselineARJRight) / baselineARJRight) * 100f;
            currentAssessment.getComparisons().setSensoryAtaxiaARJLeftKneeChange(ARJLeftChange);
            currentAssessment.getComparisons().setSensoryAtaxiaARJRightKneeChange(ARJRightChange);
        }

        currentAssessment.getSensoryAtaxia().setBalanceScore(balance);
        currentAssessment.getSensoryAtaxia().setARJ(ARJScores);
        currentAssessment.setStatus(AssessmentData.AssessmentStatus.SENSORY_ATAXIA_COMPLETED);
        currentAssessment.setSensoryAtaxiaTestDate(new Date());

        DataStorageManager.saveAssessment(requireContext(), currentAssessment);

        angleHistories.clear();
        BalanceHistories.clear();

        Intent intent = new Intent(getActivity(), CancellingTest.class);
        intent.putExtra("finished", true);
        startActivity(intent);

        Toast.makeText(requireContext(),
                "Sensory Ataxia test saved. Next test available tomorrow.",
                Toast.LENGTH_LONG).show();
    }

    private void saveDualTaskResults() throws IOException {

        AssessmentData todayAssessment = DataStorageManager.getOrCreateTodayAssessment(requireContext(), username);
        currentAssessment = todayAssessment;

        float balance = BalanceCalculator.calculateOffBalance(
                BalanceHistories.get("left_hip"),
                BalanceHistories.get("left_knee"));
        Map<String, Float> ARJScores = new HashMap<>();

        for (String joint : Arrays.asList("left_knee", "right_knee")) {
            float arj = AverageRectifiedJerk.calculateAverageRectifiedJerk(
                    angleHistories.get(joint), frameRate);
            ARJScores.put(joint, arj);
        }

        // Dual Task Cost (DC)
        Float baselineARJLeftObj = currentAssessment.getBaseline().getARJ().get("left_knee");
        Float baselineARJRightObj = currentAssessment.getBaseline().getARJ().get("right_knee");
        float baselineARJLeft = baselineARJLeftObj != null ? baselineARJLeftObj : 0f;
        float baselineARJRight = baselineARJRightObj != null ? baselineARJRightObj : 0f;
        if (baselineARJLeft > 0 && baselineARJRight > 0) {
            float ARJLeftChange = ((ARJScores.get("left_knee") - baselineARJLeft) / baselineARJLeft) * 100f;
            float ARJRightChange = ((ARJScores.get("right_knee") - baselineARJRight) / baselineARJRight) * 100f;
            currentAssessment.getComparisons().setDualTaskARJLeftKneeChange(ARJLeftChange);
            currentAssessment.getComparisons().setDualTaskARJRightKneeChange(ARJRightChange);
            Log.d("DTC_DEBUG", "  ✅ ARJ Left DTC = ((" + ARJScores.get("left_knee") + " - " + baselineARJLeft + ") / " + baselineARJLeft + ") * 100 = " + ARJLeftChange + "%");
        }

        float baselineBalance = currentAssessment.getBaseline().getBalanceScore();
        if (baselineBalance > 0) {
            float balanceChange = ((balance - baselineBalance) / baselineBalance) * 100f;
            currentAssessment.getComparisons().setDualTaskBalanceChange(balanceChange);
        }
        currentAssessment.getDualTask().setBalanceScore(balance);
        currentAssessment.getDualTask().setARJ(ARJScores);

        currentAssessment.setStatus(AssessmentData.AssessmentStatus.DUAL_TASK_COMPLETED);
        currentAssessment.setDualTaskTestDate(new Date());

        DataStorageManager.saveAssessment(requireContext(), currentAssessment);

        angleHistories.clear();
        BalanceHistories.clear();

        Intent intent = new Intent(getActivity(), CancellingTest.class);
        intent.putExtra("finished", true);
        startActivity(intent);

        Toast.makeText(requireContext(),
                "Dual task test saved. Next test available tomorrow.",
                Toast.LENGTH_LONG).show();
    }

    private void saveARJAndBalanceResults() throws IOException {
        AssessmentData todayAssessment = DataStorageManager.getOrCreateTodayAssessment(requireContext(), username);
        currentAssessment = todayAssessment;

        BalanceCalculator bc = new BalanceCalculator();
        float swayArea = bc.getSwayArea(COMHistories, requireContext());
        Map<String, Float> ARJScores = new HashMap<>();
        for (String joint : Arrays.asList("left_knee", "right_knee")) {
            float arj = AverageRectifiedJerk.calculateAverageRectifiedJerk(
                    angleHistories.get(joint), frameRate);

            ARJScores.put(joint, arj);
        }
        Toast.makeText(requireContext(),
                COMHistories.size() + " data collected for COM",
                Toast.LENGTH_LONG).show();
        currentAssessment.getBaseline().setBalanceScore(swayArea);
        currentAssessment.getBaseline().setARJ(ARJScores);
        currentAssessment.setStatus(AssessmentData.AssessmentStatus.ARJ_BALANCE_COMPLETED);
        currentAssessment.setARJAndBalanceTestDate(new Date());

        recalculateComparisons(currentAssessment);

        DataStorageManager.saveAssessment(requireContext(), currentAssessment);

        angleHistories.clear();
        BalanceHistories.clear();

        Toast.makeText(requireContext(),
                "ARJ and balance test saved. Next test available tomorrow.",
                Toast.LENGTH_LONG).show();
        Intent intent = new Intent(getActivity(), CancellingTest.class);
        intent.putExtra("finished", true);
        startActivity(intent);
    }

    private void saveWSAndEnduranceResults(float speed, float endurance) throws IOException {
        AssessmentData todayAssessment = DataStorageManager.getOrCreateTodayAssessment(requireContext(), username);
        currentAssessment = todayAssessment;
        currentAssessment.getBaseline().setWalkingSpeed(speed);
        currentAssessment.getBaseline().setEnduranceScore(endurance);
        currentAssessment.setStatus(AssessmentData.AssessmentStatus.WS_ENDURANCE_COMPLETED);
        currentAssessment.setWSAndEnduranceTestDate(new Date());

        DataStorageManager.saveAssessment(requireContext(), currentAssessment);

        angleHistories.clear();
        BalanceHistories.clear();

        Toast.makeText(requireContext(),
                "Speed and endurance test saved. Next test available tomorrow.",
                Toast.LENGTH_LONG).show();
        if (getActivity() != null && isAdded()) {
            Intent intent = new Intent(getActivity(), CancellingTest.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("finished", true);
            intent.putExtra("Saving", true);
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
        Float newBaselineARJLeft = assessment.getBaseline().getARJ().get("left_knee");
        Float newBaselineARJRight = assessment.getBaseline().getARJ().get("right_knee");

        // RECALCULATE DUAL TASK DTC
        DualTaskAssessment dt = assessment.getDualTask();
        if (dt != null) {
            if (newBaselineBalance > 0 && dt.getBalanceScore() > 0) {
                float dtBalanceChange = ((dt.getBalanceScore() - newBaselineBalance) / newBaselineBalance) * 100f;
                assessment.getComparisons().setDualTaskBalanceChange(dtBalanceChange);
            }

            if (newBaselineARJLeft != null && newBaselineARJLeft > 0 && dt.getARJ().get("left_knee") != null) {
                float dtARJLeft = dt.getARJ().get("left_knee");
                float dtLeftChange = ((dtARJLeft - newBaselineARJLeft) / newBaselineARJLeft) * 100f;
                assessment.getComparisons().setDualTaskARJLeftKneeChange(dtLeftChange);
            }

            if (newBaselineARJRight != null && newBaselineARJRight > 0 && dt.getARJ().get("right_knee") != null) {
                float dtARJRight = dt.getARJ().get("right_knee");
                float dtRightChange = ((dtARJRight - newBaselineARJRight) / newBaselineARJRight) * 100f;
                assessment.getComparisons().setDualTaskARJRightKneeChange(dtRightChange);
            }
        }

        // RECALCULATE SENSORY ATAXIA DTC
        SensoryAtaxiaAssessment sa = assessment.getSensoryAtaxia();
        if (sa != null) {
            if (newBaselineBalance > 0 && sa.getBalanceScore() > 0) {
                float saBalanceChange = ((sa.getBalanceScore() - newBaselineBalance) / newBaselineBalance) * 100f;
                assessment.getComparisons().setSensoryAtaxiaBalanceChange(saBalanceChange);
            }

            if (newBaselineARJLeft != null && newBaselineARJLeft > 0 && sa.getARJ().get("left_knee") != null) {
                float saARJLeft = sa.getARJ().get("left_knee");
                float saLeftChange = ((saARJLeft - newBaselineARJLeft) / newBaselineARJLeft) * 100f;
                assessment.getComparisons().setSensoryAtaxiaARJLeftKneeChange(saLeftChange);
            }

            if (newBaselineARJRight != null && newBaselineARJRight > 0 && sa.getARJ().get("right_knee") != null) {
                float saARJRight = sa.getARJ().get("right_knee");
                float saRightChange = ((saARJRight - newBaselineARJRight) / newBaselineARJRight) * 100f;
                assessment.getComparisons().setSensoryAtaxiaARJRightKneeChange(saRightChange);
            }
        }
    }
}