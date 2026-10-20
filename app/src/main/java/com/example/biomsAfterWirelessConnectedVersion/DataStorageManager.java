package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DataStorageManager {
    private static final String TAG = "DataStorageManager";
    private static final String FILE_EXTENSION = ".json";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static boolean firsttime = true;


    /**
     * Save assessment to internal storage:
     * Only the application can access these files
     * other applications cannot read or write them
     * Users need root privileges to access them
     *
     *  Sample Json: (for visualization)
     *  {
     *   "timestamp": "2024-12-20T10:30:00Z",
     *   "patientId": "patient_001",
     *   "baseline": {
     *     "walkingSpeed": 1.2,
     *     "balanceScore": 78.5,
     *     "stiffness": {
     *       "knee": 45.2,
     *       "ankle": 32.1
     *     }
     *   },
     *   "dualTask": {
     *     "walkingSpeed": 0.9,
     *     // ...
     *   }
     * }
     *
     *
     JSON file (local storage)
     Load directly into memory
     Display chart
     */

    public static boolean setFirsttime(Context context, boolean f) {
        String filename = "firstTime" + FILE_EXTENSION;
        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), filename))) {
            writer.write(String.valueOf(f));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean getFirsttime(Context context) {
        String filename = "firstTime" + FILE_EXTENSION;
        File file = new File(context.getFilesDir(), filename);

        if (!file.exists()) {
            return true;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line == null) {
                return false;
            }
            return Boolean.parseBoolean(line);
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return true;
        }
    }

    public static void saveAssessment(Context context, AssessmentData assessment) throws IOException {
        // Creating file name
        String filename = "assessment_" + assessment.getSessionId() + FILE_EXTENSION;
        // Gson instance
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")  // ISO 8601 standard Ex. 2024-12-20T10:30:00Z
                .serializeSpecialFloatingPointValues()
                .create();;

        // Convert the object to a JSON string.
        String json = gson.toJson(assessment);

        // write file
        // context.getFilesDir(): Returns the directory of files in the application's internal storage.
        // Example file path: /data/data/com.example.myappication/files/assessment_550e8400-e29b-41d4-a716-446655440000.json
        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), filename))) {
            writer.write(json);
        }
    }

    /**
     * Load ALL assessments from storage
     */
    public static List<AssessmentData> loadAllAssessments(Context context) {
        List<AssessmentData> allAssessments = new ArrayList<>();
        List<String> assessmentIds = listAssessments(context);  // Get a list of file names

        for (String sessionId : assessmentIds) {
            AssessmentData assessment = loadAssessment(context, sessionId);
            if (assessment != null) {
                allAssessments.add(assessment);
            }
        }

        // Sort by timestamp (newest first)
        Collections.sort(allAssessments, (a1, a2) ->
                a2.getTimestamp().compareTo(a1.getTimestamp()));

        return allAssessments;
    }

    /**
     * Load assessment from internal storage
     */
    public static AssessmentData loadAssessment(Context context, String sessionId) {
        String filename = "assessment_" + sessionId + FILE_EXTENSION;

        try (FileReader reader = new FileReader(new File(context.getFilesDir(), filename))) {
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .serializeSpecialFloatingPointValues()
                    .create();

            return gson.fromJson(reader, AssessmentData.class);

        } catch (IOException e) {
            Log.e(TAG, "Error loading JSON assessment: " + e.getMessage());
            return null;
        }
    }

    /**
     * List all saved assessments
     */
    public static List<String> listAssessments(Context context) {
        List<String> assessments = new ArrayList<>();
        File[] files = context.getFilesDir().listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("assessment_") && file.getName().endsWith(FILE_EXTENSION)) {
                    String sessionId = file.getName()
                            .replace("assessment_", "")
                            .replace(FILE_EXTENSION, "");
                    assessments.add(sessionId);
                }
            }
        }

        return assessments;
    }

    private static boolean isAssessmentEmpty(AssessmentData assessment) {
        if (assessment == null) return true;

        // Check if all test dates are null
        boolean hasARJDate = assessment.getSwayAreaAndBalanceTestDate() != null;
        boolean hasWSDate = assessment.getWSAndEnduranceTestDate() != null;
        boolean hasDualTaskDate = assessment.getDualTaskTestDate() != null;
        boolean hasSensoryDate = assessment.getSensoryAtaxiaTestDate() != null;

        // If any test date exists, assessment is not empty
        if (hasARJDate || hasWSDate || hasDualTaskDate || hasSensoryDate) {
            return false;
        }

        // Check if any scores exist (even if dates are null)
        BaselineAssessment baseline = assessment.getBaseline();
        DualTaskAssessment dualTask = assessment.getDualTask();
        SensoryAtaxiaAssessment sensoryAtaxia = assessment.getSensoryAtaxia();

        boolean hasBalance = baseline.getBalanceScore() != 0f;
        boolean hasWalkingSpeed = baseline.getWalkingSpeed() != 0f;
        boolean hasEndurance = baseline.getEnduranceScore() != 0f;
        boolean hasARJ = baseline.getARJ() != null && !baseline.getARJ().isEmpty();
        boolean hasDualTaskBalance = dualTask.getBalanceScore() != 0f;
        boolean hasDualTaskARJ = dualTask.getARJ() != null && !dualTask.getARJ().isEmpty();
        boolean hasSensoryBalance = sensoryAtaxia.getBalanceScore() != 0f;
        boolean hasSensoryARJ = sensoryAtaxia.getARJ() != null && !sensoryAtaxia.getARJ().isEmpty();

        // Also check comparisons
        TestComparisons comparisons = assessment.getComparisons();
        boolean hasComparisons = comparisons != null && (
                comparisons.getDualTaskBalanceChange() != 0f ||
                        comparisons.getDualTaskARJLeftKneeChange() != 0f ||
                        comparisons.getDualTaskARJRightKneeChange() != 0f ||
                        comparisons.getSensoryAtaxiaBalanceChange() != 0f ||
                        comparisons.getSensoryAtaxiaARJLeftKneeChange() != 0f ||
                        comparisons.getSensoryAtaxiaARJRightKneeChange() != 0f
        );

        return !(hasBalance || hasWalkingSpeed || hasEndurance || hasARJ ||
                hasDualTaskBalance || hasDualTaskARJ ||
                hasSensoryBalance || hasSensoryARJ ||
                hasComparisons);
    }

    /**
     * Delete an assessment
     */
    public static boolean deleteAssessment(Context context, String dateStr, String assessmentType) {
        List<AssessmentData> allAssessments = DataStorageManager.loadAllAssessments(context);
        AssessmentData targetAssessment = null;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        // Find the assessment for the selected date
//        for (AssessmentData assessment : allAssessments) {
//            String assessmentDate = sdf.format(assessment.getTimestamp());
//            if (assessmentDate.equals(dateStr)) {
//                targetAssessment = assessment;
//                break;
//            }
//        }

        for (AssessmentData assessment : allAssessments) {
            String assessmentDate = null;

            // Check which test type we're deleting and use the correct date
            // Find the latest date based on the stored test date for the assessments
            switch (assessmentType) {
                case "ARJ and balance":
                    if (assessment.getSwayAreaAndBalanceTestDate() != null) {
                        assessmentDate = sdf.format(assessment.getSwayAreaAndBalanceTestDate());
                    }
                    break;
                case "Dual Task":
                    if (assessment.getDualTaskTestDate() != null) {
                        assessmentDate = sdf.format(assessment.getDualTaskTestDate());
                    }
                    break;
                case "Sensory Ataxia":
                    if (assessment.getSensoryAtaxiaTestDate() != null) {
                        assessmentDate = sdf.format(assessment.getSensoryAtaxiaTestDate());
                    }
                    break;
                case "Speed and endurance":
                    if (assessment.getWSAndEnduranceTestDate() != null) {
                        assessmentDate = sdf.format(assessment.getWSAndEnduranceTestDate());
                    }
                    break;
            }

            if (assessmentDate != null && assessmentDate.equals(dateStr)) {
                targetAssessment = assessment;
                break;
            }
        }

        if (targetAssessment == null) {
            Toast.makeText(context, "No assessment found for " + dateStr, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "deleteAssessment: No assessment found for is entered and method returned");
            return false;
        }

        // ✅ Clear ONLY the selected test type data
        switch (assessmentType) {
            case "ARJ and balance":
                // Clear Baseline data
                targetAssessment.getBaseline().clearSwayMetrics();
                targetAssessment.getBaseline().setARJ(new HashMap<>());
                targetAssessment.getBaseline().setWalkingSpeed(0f);
                targetAssessment.getBaseline().setEnduranceScore(0f);
                targetAssessment.setSwayAreaAndBalanceTestDate(null);

                // Clear comparisons
                if (targetAssessment.getComparisons() != null) {
                    targetAssessment.getComparisons().setDualTaskBalanceChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJRightKneeChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaBalanceChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJRightKneeChange(0f);
                }
                break;

            case "Dual Task":
                // Clear ONLY Dual Task data
                targetAssessment.getDualTask().clearSwayMetrics();
                targetAssessment.getDualTask().setARJ(new HashMap<>());
                targetAssessment.setDualTaskTestDate(null);

                // Clear ONLY Dual Task comparisons
                if (targetAssessment.getComparisons() != null) {
                    targetAssessment.getComparisons().setDualTaskBalanceChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJRightKneeChange(0f);
                }
                break;

            case "Sensory Ataxia":
                // Clear ONLY Sensory Ataxia data
                targetAssessment.getSensoryAtaxia().clearSwayMetrics();
                targetAssessment.getSensoryAtaxia().setARJ(new HashMap<>());
                targetAssessment.setSensoryAtaxiaTestDate(null);

                // Clear ONLY Sensory Ataxia comparisons
                if (targetAssessment.getComparisons() != null) {
                    targetAssessment.getComparisons().setSensoryAtaxiaBalanceChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJRightKneeChange(0f);
                }
                break;

            case "Speed and endurance":
                // Clear ONLY Speed data
                targetAssessment.getBaseline().setWalkingSpeed(0f);
                targetAssessment.getBaseline().setEnduranceScore(0f);
                targetAssessment.setWSAndEnduranceTestDate(null);
                break;
        }
        return true;
    }

    public static AssessmentData getOrCreateTodayAssessment(Context context, String patientId) {
        String todayDateString = DATE_FORMAT.format(new Date());

        // Load all assessments
        List<AssessmentData> allAssessments = loadAllAssessments(context);

        // Look for today's assessment
        for (AssessmentData assessment : allAssessments) {
            // Check by timestamp (session creation date)
            if (assessment.getTimestamp() != null) {
                String assessmentDate = DATE_FORMAT.format(assessment.getTimestamp());
                if (assessmentDate.equals(todayDateString)) {
                    return assessment; // Found today's assessment
                }
            }

            // Also check by any test date
            if (assessment.getSwayAreaAndBalanceTestDate() != null &&
                    DATE_FORMAT.format(assessment.getSwayAreaAndBalanceTestDate()).equals(todayDateString)) {
                return assessment;
            }
            if (assessment.getWSAndEnduranceTestDate() != null &&
                    DATE_FORMAT.format(assessment.getWSAndEnduranceTestDate()).equals(todayDateString)) {
                return assessment;
            }
            if (assessment.getDualTaskTestDate() != null &&
                    DATE_FORMAT.format(assessment.getDualTaskTestDate()).equals(todayDateString)) {
                return assessment;
            }
            if (assessment.getSensoryAtaxiaTestDate() != null &&
                    DATE_FORMAT.format(assessment.getSensoryAtaxiaTestDate()).equals(todayDateString)) {
                return assessment;
            }
        }

        // No assessment found for today - create new one
        AssessmentData newAssessment = new AssessmentData(patientId);
        return newAssessment;
    }

    public static boolean setMass(Context context, double mass) throws IOException {
        String filename = "mass" + FILE_EXTENSION;
        // write file
        // context.getFilesDir(): Returns the directory of files in the application's internal storage.
        // Store as a simple text
        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), filename))) {
            writer.write(String.valueOf(mass));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static double getMass(Context context) {
        String filename = "mass" + FILE_EXTENSION;
        File file = new File(context.getFilesDir(), filename);
        if (!file.exists()) {
            Toast.makeText(context, "Mass not found", Toast.LENGTH_SHORT).show();
            return 0;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return Double.parseDouble(reader.readLine());
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static boolean setSex(Context context, String sex) throws IOException {
        String filename = "sex" + FILE_EXTENSION;
        // write file
        // context.getFilesDir(): Returns the directory of files in the application's internal storage.
        // Store as a simple text
        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), filename))) {
            writer.write(sex);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getSex(Context context) {
        String filename = "sex" + FILE_EXTENSION;
        File file = new File(context.getFilesDir(), filename);
        if (!file.exists()) {
            return "Not Found";
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.readLine(); // return the string directly
        } catch (IOException e) {
            e.printStackTrace();
            return "Not Found"; // Fixed: return a default value
        }
    }
    public static boolean deleteAssessmentFile(Context context, String sessionId) {
        String filename = "assessment_" + sessionId + FILE_EXTENSION;
        File file = new File(context.getFilesDir(), filename);

        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Log.d(TAG, "Deleted assessment file: " + filename);
            } else {
                Log.e(TAG, "Failed to delete assessment file: " + filename);
            }
            return deleted;
        }
        return false;
    }
    public static boolean deleteAssessmentByDate(Context context, String dateStr, String assessmentType) {
        List<AssessmentData> allAssessments = loadAllAssessments(context);
        AssessmentData targetAssessment = null;

        // Find the assessment for the selected date
        for (AssessmentData assessment : allAssessments) {
            String assessmentDate = DATE_FORMAT.format(assessment.getTimestamp());
            if (assessmentDate.equals(dateStr)) {
                targetAssessment = assessment;
                break;
            }
        }

        if (targetAssessment == null || (allAssessments.size() == 1 && isAssessmentEmpty(targetAssessment))) {
            Toast.makeText(context, assessmentType + " was not found for " + dateStr, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "No assessment found for date: " + dateStr);
            if (allAssessments.size() == 1) {
                deleteAssessmentFile(context, targetAssessment.getSessionId());
            }
            return false;
        }

        // Clear only the selected test type data
        switch (assessmentType) {
            case "Sway area and balance":
                targetAssessment.getBaseline().clearSwayMetrics();
                targetAssessment.getBaseline().setARJ(new HashMap<>());
                targetAssessment.setSwayAreaAndBalanceTestDate(null);

                // Clear comparisons
                if (targetAssessment.getComparisons() != null) {
                    targetAssessment.getComparisons().setDualTaskBalanceChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJRightKneeChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaBalanceChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJRightKneeChange(0f);
                }
                Toast.makeText(context, "Sway area and balance data cleared for " + dateStr, Toast.LENGTH_SHORT).show();
                break;

            case "Dual Task":
                targetAssessment.getDualTask().clearSwayMetrics();
                targetAssessment.getDualTask().setARJ(new HashMap<>());
                targetAssessment.setDualTaskTestDate(null);

                if (targetAssessment.getComparisons() != null) {
                    targetAssessment.getComparisons().setDualTaskBalanceChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJRightKneeChange(0f);
                }
                Toast.makeText(context, "Dual Task data cleared for " + dateStr, Toast.LENGTH_SHORT).show();
                break;

            case "Sensory Ataxia":
                targetAssessment.getSensoryAtaxia().clearSwayMetrics();
                targetAssessment.getSensoryAtaxia().setARJ(new HashMap<>());
                targetAssessment.setSensoryAtaxiaTestDate(null);

                if (targetAssessment.getComparisons() != null) {
                    targetAssessment.getComparisons().setSensoryAtaxiaBalanceChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJRightKneeChange(0f);
                }
                Toast.makeText(context, "Sensory Ataxia data cleared for " + dateStr, Toast.LENGTH_SHORT).show();
                break;

            case "Speed and endurance":
                targetAssessment.getBaseline().setWalkingSpeed(0f);
                targetAssessment.getBaseline().setEnduranceScore(0f);
                targetAssessment.setWSAndEnduranceTestDate(null);
                Toast.makeText(context, "Speed and endurance data cleared for " + dateStr, Toast.LENGTH_SHORT).show();
                break;

            default:
                return false;
        }

        // Save the updated assessment back
        try {
            saveAssessment(context, targetAssessment);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error saving assessment after deletion: " + e.getMessage());
            Toast.makeText(context, "Error saving changes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public static void saveReport(Context context, ReportModel report) throws IOException {
        // Creating file name
        String filename = "report_" + report.getReportId() + FILE_EXTENSION;
        // Gson instance
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")  // ISO 8601 standard Ex. 2024-12-20T10:30:00Z
                .serializeSpecialFloatingPointValues() // handle Nan and Infinity
                .create();

        // Convert the object to a JSON string.
        String json = gson.toJson(report);

        // write file
        // context.getFilesDir(): Returns the directory of files in the application's internal storage.
        // Example file path: /data/data/com.example.myappication/files/assessment_550e8400-e29b-41d4-a716-446655440000.json
        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), filename))) {
            writer.write(json);
        }
    }
    public static List<ReportModel> loadAllReports(Context context) {
        List<ReportModel> allReports = new ArrayList<>();
        List<String> reportIds = listReports(context);  // Get a list of file names

        for (String reportId : reportIds) {
            ReportModel report = loadReport(context, reportId);
            if (report != null) {
                allReports.add(report);
            }
        }

        // Sort by timestamp (newest first)
        Collections.sort(allReports, (r1, r2) ->
                r2.getTimeStamp().compareTo(r1.getTimeStamp()));

        return allReports;
    }

    public static List<String> listReports(Context context) {
        List<String> reports = new ArrayList<>();
        File[] files = context.getFilesDir().listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("report_") && file.getName().endsWith(FILE_EXTENSION)) {
                    String sessionId = file.getName()
                            .replace("report_", "")
                            .replace(FILE_EXTENSION, "");
                    reports.add(sessionId);
                }
            }
        }
        return reports;
    }

    public static ReportModel loadReport(Context context, String reportId) {
        String filename = "report_" + reportId + FILE_EXTENSION;

        try (FileReader reader = new FileReader(new File(context.getFilesDir(), filename))) {
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .serializeSpecialFloatingPointValues()
                    .create();

            return gson.fromJson(reader, ReportModel.class);

        } catch (IOException e) {
            Log.e(TAG, "Error loading JSON assessment: " + e.getMessage());
            return null;
        }
    }

    public static boolean deleteReport(Context context, ReportModel report) {
        // Get the report ID or filename
        String reportId = report.getReportId();
        String filename = "report_" + reportId + ".json";
        File file = new File(context.getFilesDir(), filename);

        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
}
