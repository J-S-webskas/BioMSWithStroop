package com.example.bioms1;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecordFragment extends Fragment {

    private static final int SPEED_TEST_REQUEST = 100;
    private Handler autoStopHandler;
    private Runnable autoStopRunnable;

    private static final int BASELINE_DURATION_MS = 20000;
    private static final int DUAL_TASK_DURATION_MS = 20000;
    private static final int SENSORY_DURATION_MS = 20000;

    private Date latestTimeStamp;
    private String todaySessionId = "";
    private String username = "123";
    private static float frameRate = 0;

    private AssessmentData currentAssessment = new AssessmentData(username);
    private Calendar baselineTestDate;

    private static Map<String, ArrayDeque<Float>> angleHistories = new HashMap<>();
    private static Map<String, ArrayDeque<Float>> BalanceHistories = new HashMap<>();

    private enum TestType { NONE, BASELINE, DUAL_TASK, SENSORY_ATAXIA }
    private TestType currentTestType = TestType.NONE;
    private TestResult baselineResult;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_record, container, false);
        setupTestControls(view);
        return view;
    }


    private void setupTestControls(View view) {

        CardView btnBaseline = view.findViewById(R.id.btn_baseline);
        CardView btnDualTask = view.findViewById(R.id.btn_dual_task);
        CardView btnSensory = view.findViewById(R.id.btn_sensory);
        CardView btnSpeed = view.findViewById(R.id.btn_walking_speed_and_endurance);
        Button howTo = view.findViewById(R.id.howTo);

        btnBaseline.setOnClickListener(v -> startARJAndBalanceTest());
        btnDualTask.setOnClickListener(v -> {Log.d("DualTaskDebug", "*** DUAL TASK BUTTON CLICKED ***");
            startDualTaskTest();});
        btnSensory.setOnClickListener(v -> startSensoryAtaxiaTest());
        btnSpeed.setOnClickListener(v -> startWSAndEnduranceTest());

        howTo.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), DemoSession.class);
            startActivity(intent);
        });
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
        if (!isAdded() || getActivity() == null) {
            return;
        }

        if (currentTestType == TestType.NONE) {
            Toast.makeText(requireContext(), "No test running", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cancel the auto-stop timer
        if (autoStopHandler != null && autoStopRunnable != null) {
            autoStopHandler.removeCallbacks(autoStopRunnable);
            autoStopHandler = null;
            autoStopRunnable = null;
        }
        try {
            Fragment fragment = requireActivity()
                    .getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);

            if (fragment instanceof CameraFragment) {
                CameraFragment cameraFragment = (CameraFragment) fragment;
                cameraFragment.stopCamera();
            }
        } catch (Exception e) {
        }

        // Clear test state
        TestType cancelledTest = currentTestType;
        currentTestType = TestType.NONE;

        if (angleHistories != null) {
            angleHistories.clear();
        }

        String testName = getTestName(cancelledTest);
        requireActivity().getSupportFragmentManager().popBackStack();
        Toast.makeText(requireContext(), testName + " test cancelled - results not saved", Toast.LENGTH_LONG).show();
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
        if (allAssessment.isEmpty()) {
            return;
        }

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

        /// CALL Speed
        Intent intent = new Intent(requireContext(), Speed.class);
        startActivityForResult(intent, SPEED_TEST_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        currentTestType = TestType.BASELINE;
        baselineTestDate = Calendar.getInstance();

        currentTestType = TestType.NONE;

        if (requestCode == SPEED_TEST_REQUEST && resultCode == RESULT_OK) {
            // Get the data from Speed activity
            float speed = data.getFloatExtra("currentSpeed", 0);
            float distance = data.getFloatExtra("totalDistance", 0);

            // Save to assessment
            try {
                saveWSAndEnduranceResults(speed, distance);  // ← Pass the data!
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void startARJAndBalanceTest() {
        if (currentTestType != TestType.NONE) { // If the user clicks another test while this test is running, discard this test data
            stopCurrentTestAndDiscardData();
            return;
        }
        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(requireContext());
        if (allAssessment.isEmpty()) {
            return;
        }

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

        if (arjDoneToday) {
            Toast.makeText(requireContext(), "ARJ & balance was already completed for today. Record again tomorrow!", Toast.LENGTH_LONG).show();
            return;
        }

        /// CALL CAMERA
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, new CameraFragment())
                .addToBackStack("camera")
                .commit();

        // Start baseline recording
        currentTestType = TestType.BASELINE;
        baselineTestDate = Calendar.getInstance();

        Toast.makeText(requireContext(), "ARJ and balance test starts after countdown - Walk normally for 10 seconds", Toast.LENGTH_LONG).show();

        var auto_test_handler = new Handler(Looper.getMainLooper());
        var auto_test_runner = new Runnable() {
            @Override
            public void run() {
                if (currentTestType == TestType.BASELINE) {
                    try {
                        saveARJAndBalanceResults();

                        CameraFragment cameraFragment = (CameraFragment) requireActivity()
                                .getSupportFragmentManager()
                                .findFragmentById(R.id.fragment_container);
                        cameraFragment.stopCamera();

                        requireActivity().getSupportFragmentManager().popBackStack();

                        Toast.makeText(requireContext(), "ARJ and balance test completed!",
                                Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    currentTestType = TestType.NONE;
                }
            }
        };
        auto_test_handler.postDelayed(auto_test_runner, BASELINE_DURATION_MS ); // 15 seconds = 15000ms
    }

    private void startSensoryAtaxiaTest() {
        if (currentTestType != TestType.NONE) { // If the user clicks another test while this test is running, discard this test data
            stopCurrentTestAndDiscardData();
            return;
        }

        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(requireContext());
        if (allAssessment.isEmpty()) {
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

        if (!arjDoneToday) {
            Toast.makeText(requireContext(), "Record ARJ & balance first!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sensoryDoneToday) {
            Toast.makeText(requireContext(), "Sensory ataxia test was already completed for today. Record again tomorrow!", Toast.LENGTH_SHORT).show();
            return;
        }
        /// CALL CAMERA
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, new CameraFragment())
                .addToBackStack("camera")
                .commit();

        // Create sensory ataxia test
        currentTestType = TestType.SENSORY_ATAXIA;

        Toast.makeText(requireContext(), "Sensory ataxia test starts after countdown - Close your eyes and walk in view for 10 seconds", Toast.LENGTH_LONG).show();

        autoStopHandler = new Handler(Looper.getMainLooper());
        autoStopRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentTestType == TestType.SENSORY_ATAXIA) {
                    try {
                        saveSensoryAtaxiaResults();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    CameraFragment cameraFragment = (CameraFragment) requireActivity()
                            .getSupportFragmentManager()
                            .findFragmentById(R.id.fragment_container);
                    cameraFragment.stopCamera();

                    requireActivity().getSupportFragmentManager().popBackStack();

                    Toast.makeText(requireContext(), "Sensory ataxia test completed!",
                            Toast.LENGTH_SHORT).show();

                    currentTestType = TestType.NONE;
                }
            }
        };

        autoStopHandler.postDelayed(autoStopRunnable, SENSORY_DURATION_MS ); // 15 seconds = 15000ms
    }

    private void startDualTaskTest() {
        if (currentTestType != TestType.NONE) { // If the user clicks another test while this test is running, discard this test data
            stopCurrentTestAndDiscardData();
            return;
        }
        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(requireContext());
        if (allAssessment.isEmpty()) {
            return;
        }

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

        if (!arjDoneToday) {
            Toast.makeText(requireContext(), "Record ARJ & balance first!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dualTaskDoneToday) {
            Toast.makeText(requireContext(), "Dual task test was already completed for today. Record again tomorrow!", Toast.LENGTH_LONG).show();
            return;
        }

        /// CALL CAMERA
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, new CameraFragment())
                .addToBackStack("camera")
                .commit();

        // Create dual task test
        currentTestType = TestType.DUAL_TASK;

        Toast.makeText(requireContext(), "Dual task test starts after count down - Walk while counting backwards from 30", Toast.LENGTH_LONG).show();

        autoStopHandler = new Handler(Looper.getMainLooper());
        autoStopRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentTestType == TestType.DUAL_TASK) {
                    try {
                        saveDualTaskResults();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    CameraFragment cameraFragment = (CameraFragment) requireActivity()
                            .getSupportFragmentManager()
                            .findFragmentById(R.id.fragment_container);
                    cameraFragment.stopCamera();

                    requireActivity().getSupportFragmentManager().popBackStack();

                    Toast.makeText(requireContext(), "Dual task test completed!",
                            Toast.LENGTH_SHORT).show();

                    currentTestType = TestType.NONE;
                }
            }
        };
        autoStopHandler.postDelayed(autoStopRunnable, DUAL_TASK_DURATION_MS );
    }

    public static void setFrameRate(float fps) {
        frameRate = fps;
    }

    private void saveSensoryAtaxiaResults() throws IOException {

//        float speed = Speed.currentSpeed;
//        float endurance = Speed.totalDistance;

        float balance = BalanceCalculator.calculateOffBalance(
                BalanceHistories.get("left_hip"),
                BalanceHistories.get("left_knee"));
        Map<String, Float> ARJScores = new HashMap<>();

        for (String joint : Arrays.asList("left_knee", "right_knee")) {
            float arj = AverageRectifiedJerk.calculateAverageRectifiedJerk(
                    angleHistories.get(joint), frameRate);
            ARJScores.put(joint, arj);
        }

//        currentAssessment.getSensoryAtaxia().setWalkingSpeed(speed);
//        currentAssessment.getSensoryAtaxia().setEnduranceScore(endurance);

        // Dual Task Cost (DTC): (Sensory Task Performance - Baseline Performance) / Baseline Performance × 100
        float baselineBalance = currentAssessment.getBaseline().getBalanceScore();
        if (baselineBalance > 0) {
            float balanceChange = ((balance - baselineBalance) / baselineBalance) * 100f;
            if (currentTestType == TestType.SENSORY_ATAXIA) {
                currentAssessment.getComparisons().setSensoryAtaxiaBalanceChange(balanceChange);
            }
        }
        Float baselineARJLeftObj = currentAssessment.getBaseline().getARJ().get("left_knee");
        Float baselineARJRightObj = currentAssessment.getBaseline().getARJ().get("right_knee");
        float baselineARJLeft = baselineARJLeftObj != null ? baselineARJLeftObj : 0f;
        float baselineARJRight = baselineARJRightObj != null ? baselineARJRightObj : 0f;
        if (baselineARJLeft > 0 && baselineARJRight > 0) {
            float ARJLeftChange = ((ARJScores.get("left_knee") - baselineARJLeft) / baselineARJLeft) * 100f;
            float ARJRightChange = ((ARJScores.get("right_knee") - baselineARJRight) / baselineARJRight) * 100f;
            if (currentTestType == TestType.SENSORY_ATAXIA) {
                currentAssessment.getComparisons().setSensoryAtaxiaARJLeftKneeChange(ARJLeftChange);
                currentAssessment.getComparisons().setSensoryAtaxiaARJRightKneeChange(ARJRightChange);
            }
        }
        currentAssessment.getSensoryAtaxia().setBalanceScore(balance);
        currentAssessment.getSensoryAtaxia().setARJ(ARJScores);

        currentAssessment.setStatus(AssessmentData.AssessmentStatus.SENSORY_ATAXIA_COMPLETED);
        currentAssessment.setSensoryAtaxiaTestDate(new Date());

        DataStorageManager.saveAssessment(requireContext(), currentAssessment);

        angleHistories.clear();
        BalanceHistories.clear();

        Toast.makeText(requireContext(),
                "Sensory Ataxia test saved. Next test available tomorrow.",
                Toast.LENGTH_LONG).show();
    }

    private void saveDualTaskResults() throws IOException {

//        float speed = Speed.currentSpeed;
//        float endurance = Speed.totalDistance;

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
            if (currentTestType == TestType.DUAL_TASK) {
                currentAssessment.getComparisons().setDualTaskARJLeftKneeChange(ARJLeftChange);
                currentAssessment.getComparisons().setDualTaskARJRightKneeChange(ARJRightChange);
            }
        }

        float baselineBalance = currentAssessment.getBaseline().getBalanceScore();
        if (baselineBalance > 0) {
            float balanceChange = ((balance - baselineBalance) / baselineBalance) * 100f;
            if (currentTestType == TestType.DUAL_TASK) {
                currentAssessment.getComparisons().setDualTaskBalanceChange(balanceChange);
            }
        }
//        currentAssessment.getDualTask().setWalkingSpeed(speed);
//        currentAssessment.getDualTask().setEnduranceScore(endurance);
        currentAssessment.getDualTask().setBalanceScore(balance);
        currentAssessment.getDualTask().setARJ(ARJScores);

        currentAssessment.setStatus(AssessmentData.AssessmentStatus.DUAL_TASK_COMPLETED);
        currentAssessment.setDualTaskTestDate(new Date());

        DataStorageManager.saveAssessment(requireContext(), currentAssessment);

        angleHistories.clear();
        BalanceHistories.clear();

        Toast.makeText(requireContext(),
                "Dual task test saved. Next test available tomorrow.",
                Toast.LENGTH_LONG).show();
    }

    private void saveComparisonResults(TestType currentTestType) throws IOException {

        TestComparisons comparisons = currentAssessment.getComparisons();
        float baselineSpeed = currentAssessment.getBaseline().getWalkingSpeed();
        if (baselineSpeed > 0) {
            float speedChange = ((Speed.currentSpeed - baselineSpeed) / baselineSpeed) * 100f;
            if (currentTestType == TestType.SENSORY_ATAXIA)
                comparisons.setSensoryAtaxiaSpeedChange(speedChange);
            else
                comparisons.setDualTaskSpeedChange(speedChange);
        }

        float balance = BalanceCalculator.calculateOffBalance(
                BalanceHistories.get("left_hip"),
                BalanceHistories.get("left_knee"));
        float baselineBalance = currentAssessment.getBaseline().getBalanceScore();
        if (baselineBalance > 0) {
            float balanceChange = ((balance - baselineBalance) / baselineBalance) * 100f;
            if (currentTestType == TestType.SENSORY_ATAXIA) {
                comparisons.setSensoryAtaxiaBalanceChange(balanceChange);
            } else {
                comparisons.setDualTaskBalanceChange(balanceChange);
            }
        }

        float baselineEndurance = currentAssessment.getBaseline().getEnduranceScore();
        if (baselineEndurance > 0) {
            float enduranceChange =
                    ((Speed.totalDistance - baselineEndurance) / baselineEndurance) * 100f;
            if (currentTestType == TestType.SENSORY_ATAXIA) {
                comparisons.setSensoryAtaxiaEnduranceChange(enduranceChange);
            } else {
                comparisons.setDualTaskEnduranceChange(enduranceChange);
            }
        }

        Map<String, Float> ARJScores = new HashMap<>();
        for (String joint : Arrays.asList("left_knee", "right_knee")) {
            float ARJ = AverageRectifiedJerk.calculateAverageRectifiedJerk(
                    angleHistories.get(joint), frameRate
            );
            ARJScores.put(joint, ARJ);
        }
        if (!currentAssessment.getBaseline().getARJ().isEmpty()) {
            currentAssessment.getBaseline().setARJ(ARJScores);
            float baselineARJForLeftKnee = currentAssessment.getBaseline().getARJ().get("left_knee");
            float baselineARJForRightKnee = currentAssessment.getBaseline().getARJ().get("right_knee");
            float stiffnessChangeForLeftKnee = (ARJScores.get("left_knee") - baselineARJForLeftKnee) / baselineARJForLeftKnee * 100;
            float stiffnessChangeForRightKnee = (ARJScores.get("right_knee") - baselineARJForRightKnee) / baselineARJForRightKnee * 100;
            if (currentTestType == TestType.SENSORY_ATAXIA) {
                comparisons.setSensoryAtaxiaARJLeftKneeChange(stiffnessChangeForLeftKnee);
                comparisons.setSensoryAtaxiaARJRightKneeChange(stiffnessChangeForRightKnee);
            } else {
                comparisons.setDualTaskARJLeftKneeChange(stiffnessChangeForLeftKnee);
                comparisons.setDualTaskARJRightKneeChange(stiffnessChangeForRightKnee);
            }
        }

        if (currentTestType == TestType.SENSORY_ATAXIA) {
            currentAssessment.setStatus(AssessmentData.AssessmentStatus.SENSORY_ATAXIA_COMPLETED);
            currentAssessment.setSensoryAtaxiaTestDate(new Date());
        } else {
            currentAssessment.setStatus(AssessmentData.AssessmentStatus.DUAL_TASK_COMPLETED);
            currentAssessment.setDualTaskTestDate(new Date());
        }
        DataStorageManager.saveAssessment(requireContext(), currentAssessment);

        angleHistories.clear();
        BalanceHistories.clear();
    }

    private void saveARJAndBalanceResults() throws IOException {

        float balance = BalanceCalculator.calculateOffBalance(
                BalanceHistories.get("left_hip"),
                BalanceHistories.get("left_knee"));
        Map<String, Float> ARJScores = new HashMap<>();

        for (String joint : Arrays.asList("left_knee", "right_knee")) {
            if (joint == null || joint.isEmpty()) {
                continue;
            }
            float arj = AverageRectifiedJerk.calculateAverageRectifiedJerk(
                    angleHistories.get(joint), frameRate);

            ARJScores.put(joint, arj);
        }

        currentAssessment.getBaseline().setBalanceScore(balance);
        currentAssessment.getBaseline().setARJ(ARJScores);

        currentAssessment.setStatus(AssessmentData.AssessmentStatus.ARJ_BALANCE_COMPLETED);
        currentAssessment.setARJAndBalanceTestDate(new Date());

        DataStorageManager.saveAssessment(requireContext(), currentAssessment);

        angleHistories.clear();
        BalanceHistories.clear();

        Toast.makeText(requireContext(),
                "ARJ and balance test saved. Next test available tomorrow.",
                Toast.LENGTH_LONG).show();
    }

    private void saveWSAndEnduranceResults(float speed, float endurance) throws IOException {

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
    }
}