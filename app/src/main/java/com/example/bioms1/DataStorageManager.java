package com.example.bioms1;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataStorageManager {
    private static final String TAG = "DataStorageManager";
    private static final String FILE_EXTENSION = ".json";

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
    public static void saveAssessment(Context context, AssessmentData assessment) throws IOException {
        // Creating file name
        String filename = "assessment_" + assessment.getSessionId() + FILE_EXTENSION;
        // Gson instance
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")  // ISO 8601 standard Ex. 2024-12-20T10:30:00Z
                .serializeSpecialFloatingPointValues()     // Handle NaN and Infinity
                .create();

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

    /**
     * Delete an assessment
     */
    public static boolean deleteAssessment(Context context, String sessionId) {
        String filename = "assessment_" + sessionId + FILE_EXTENSION;
        File file = new File(context.getFilesDir(), filename);

        if (file.exists()) {
            boolean deleted = file.delete();
            Log.d(TAG, "Assessment deleted: " + sessionId + " - " + deleted);
            return deleted;
        }

        return false;
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
                    .create();

            return gson.fromJson(reader, ReportModel.class);

        } catch (IOException e) {
            Log.e(TAG, "Error loading JSON assessment: " + e.getMessage());
            return null;
        }
    }
}