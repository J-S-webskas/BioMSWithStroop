package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds the per-trial CSV export package for one finished test and zips
 * it for sharing (e.g. via Gmail):
 *
 *   &lt;PatientID&gt;_&lt;Condition&gt;_CoM_Trajectory.csv
 *   &lt;PatientID&gt;_&lt;Condition&gt;_JointAngles.csv
 *   &lt;PatientID&gt;_&lt;Condition&gt;_SystemLog.csv
 *   README.txt
 *
 * zipped as &lt;PatientID&gt;_&lt;yyyy-MM-dd&gt;.zip (with a condition
 * suffix when a second test is exported the same day). The patient is
 * identified ONLY by the anonymous ID from {@link AnonymousIdManager} —
 * never by name.
 *
 * Axis mapping for the trajectory: the app measures sway in the ground
 * plane — CoM_x_Pos is the mediolateral axis and CoM_z_Pos the
 * anteroposterior (depth) axis, matching MediaPipe's x/z convention.
 * CoM_y_Pos (vertical) is not tracked by the app, so the column is left
 * empty rather than filled with invented data; see README.txt in the zip.
 */
public class TrialCsvExporter {

    private static final String TAG = "TrialCsvExporter";
    public static final String PREFS = "csv_export";
    public static final String KEY_LAST_ZIP_PATH = "last_zip_path";

    private TrialCsvExporter() { /* static helpers only */ }

    /**
     * Writes the export zip into the app's files/exports directory and
     * remembers it as the pending export for the finish screen.
     *
     * @param conditionLabel Baseline / DualTask / SensoryAtaxia
     * @param metrics        the trial's computed sway metrics (trajectory + cleaning counters)
     * @param jointSeries    per-joint angle series recorded during the trial (degrees)
     * @param droppedFrames  frames where MediaPipe lost the patient during this trial
     * @param fovDegrees     the horizontal FOV used for this phone's scale conversion
     * @return the zip file, or null if it could not be written
     */
    public static File exportTrial(Context context, String conditionLabel, SwayMetrics metrics,
                                   Map<String, List<Float>> jointSeries,
                                   int droppedFrames, double fovDegrees) {
        try {
            String patientId = AnonymousIdManager.getId(context);
            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

            File dir = new File(context.getFilesDir(), "exports");
            if (!dir.exists() && !dir.mkdirs()) {
                Log.e(TAG, "Could not create exports directory");
                return null;
            }

            String base = patientId + "_" + conditionLabel;
            File zip = new File(dir, patientId + "_" + dateStr + ".zip");
            if (zip.exists()) {
                zip = new File(dir, patientId + "_" + dateStr + "_" + conditionLabel + ".zip");
                int n = 2;
                while (zip.exists()) {
                    zip = new File(dir, patientId + "_" + dateStr + "_" + conditionLabel + "_" + n + ".zip");
                    n++;
                }
            }

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
                writeEntry(zos, base + "_CoM_Trajectory.csv",
                        buildTrajectoryCsv(metrics));
                writeEntry(zos, base + "_JointAngles.csv",
                        buildJointAnglesCsv(jointSeries, metrics));
                writeEntry(zos, base + "_SystemLog.csv",
                        buildSystemLogCsv(patientId, conditionLabel, dateStr, metrics,
                                droppedFrames, fovDegrees));
                writeEntry(zos, "README.txt", buildReadme(patientId, conditionLabel, dateStr));
            }

            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString(KEY_LAST_ZIP_PATH, zip.getAbsolutePath()).apply();
            Log.d(TAG, "Export written: " + zip.getAbsolutePath());
            return zip;
        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            return null;
        }
    }

    // ------------------------------------------------------------------
    // On-demand exports, generated later from stored report data.
    // ------------------------------------------------------------------

    /** One stored trial, rebuilt from a saved assessment for re-export. */
    public static class StoredTrialData {
        public List<Float> trajX = new ArrayList<>();
        public List<Float> trajZ = new ArrayList<>();
        public float frameRate = 0f;
        public float swayAreaMm2 = 0f;
        public float pathLengthMm = 0f;
        public float meanVelocityMmS = 0f;
        public float rmsAPMm = 0f;
        public float rmsMLMm = 0f;
        public float cameraDistanceM = 0f;
        public boolean valid = false;
    }

    /**
     * Builds a ZIP for one condition of one stored day, from data saved in
     * assessments. Contains the trajectory + a system log per trial (files
     * gain a _T&lt;n&gt; suffix when the day has several trials of the
     * condition). Joint angles and frame counters are NOT part of stored
     * assessments, so they exist only in the at-save export; the README
     * in the zip says so.
     *
     * @param dateStr yyyy-MM-dd of the chosen day
     * @return the zip file, or null on failure / nothing to export
     */
    public static File exportStoredTrials(Context context, String dateStr, String conditionLabel,
                                          List<StoredTrialData> trials) {
        if (trials == null || trials.isEmpty()) return null;
        try {
            String patientId = AnonymousIdManager.getId(context);
            File dir = new File(context.getFilesDir(), "exports");
            if (!dir.exists() && !dir.mkdirs()) {
                Log.e(TAG, "Could not create exports directory");
                return null;
            }
            String base = patientId + "_" + conditionLabel;
            File zip = new File(dir, patientId + "_" + dateStr + "_" + conditionLabel + "_Report.zip");
            int n = 2;
            while (zip.exists()) {
                zip = new File(dir, patientId + "_" + dateStr + "_" + conditionLabel + "_Report_" + n + ".zip");
                n++;
            }
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
                for (int i = 0; i < trials.size(); i++) {
                    String suffix = trials.size() > 1 ? "_T" + (i + 1) : "";
                    writeEntry(zos, base + suffix + "_CoM_Trajectory.csv",
                            buildStoredTrajectoryCsv(trials.get(i)));
                    writeEntry(zos, base + suffix + "_SystemLog.csv",
                            buildStoredSystemLogCsv(patientId, conditionLabel, dateStr,
                                    trials.get(i), i + 1, trials.size()));
                }
                writeEntry(zos, "README.txt", buildStoredReadme(patientId, conditionLabel, dateStr));
            }
            Log.d(TAG, "Stored export written: " + zip.getAbsolutePath());
            return zip;
        } catch (Exception e) {
            Log.e(TAG, "Stored export failed", e);
            return null;
        }
    }

    private static String buildStoredTrajectoryCsv(StoredTrialData t) {
        StringBuilder sb = new StringBuilder();
        sb.append("time [s],CoM_x_Pos [m],CoM_y_Pos [m],CoM_z_Pos [m]\n");
        int n = Math.min(t.trajX.size(), t.trajZ.size());
        // Stored trajectories are on the uniform 30 Hz grid the metrics
        // were computed from.
        for (int i = 0; i < n; i++) {
            sb.append(String.format(Locale.US, "%.3f,%.6f,,%.6f\n",
                    i / 30.0, t.trajX.get(i), t.trajZ.get(i)));
        }
        return sb.toString();
    }

    private static String buildStoredSystemLogCsv(String patientId, String conditionLabel,
                                                  String dateStr, StoredTrialData t,
                                                  int trialIndex, int trialCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Metric,Value\n");
        sb.append("PatientID,").append(patientId).append('\n');
        sb.append("Condition,").append(conditionLabel).append('\n');
        sb.append("Date,").append(dateStr).append('\n');
        sb.append("Trial,").append(trialIndex).append(" of ").append(trialCount).append('\n');
        sb.append("Source,Stored report data (export generated on demand)\n");
        sb.append("SwayArea [mm^2],").append(String.format(Locale.US, "%.2f", t.swayAreaMm2)).append('\n');
        sb.append("SwayPathLength [mm],").append(String.format(Locale.US, "%.2f", t.pathLengthMm)).append('\n');
        sb.append("SwayMeanVelocity [mm/s],").append(String.format(Locale.US, "%.2f", t.meanVelocityMmS)).append('\n');
        sb.append("SwayRMS_AP [mm],").append(String.format(Locale.US, "%.2f", t.rmsAPMm)).append('\n');
        sb.append("SwayRMS_ML [mm],").append(String.format(Locale.US, "%.2f", t.rmsMLMm)).append('\n');
        sb.append("FrameRate [fps],").append(String.format(Locale.US, "%.2f", t.frameRate)).append('\n');
        sb.append("CameraDistance [m],");
        if (t.cameraDistanceM > 0) sb.append(String.format(Locale.US, "%.2f", t.cameraDistanceM));
        sb.append('\n');
        sb.append("MetricsValid,").append(t.valid).append('\n');
        return sb.toString();
    }

    private static String buildStoredReadme(String patientId, String conditionLabel, String dateStr) {
        return "BioMS trial export (generated on demand from stored report data)\n"
                + "PatientID: " + patientId + " (anonymous ID - no name is exported)\n"
                + "Condition: " + conditionLabel + "\n"
                + "Date: " + dateStr + "\n\n"
                + "This package was rebuilt from the trial data saved in the app's\n"
                + "reports. It contains the cleaned COM trajectory and the stored\n"
                + "sway metrics for each trial of the chosen condition that day.\n\n"
                + "Joint angles and the frame-quality counters (dropped /\n"
                + "interpolated / despiked frames) are recorded only in the export\n"
                + "created when the test was saved, because per-frame joint angles\n"
                + "are not stored in reports.\n\n"
                + "Axes: CoM_x_Pos = mediolateral, CoM_z_Pos = anteroposterior.\n"
                + "CoM_y_Pos (vertical) is not tracked by the app, so that column\n"
                + "is intentionally empty.\n";
    }

    /**
     * Builds a ZIP of the Stroop results shown in a report: a per-trial
     * summary CSV plus one row per spoken answer.
     *
     * @param dateStr yyyy-MM-dd of the report day
     * @return the zip file, or null on failure / no trials
     */
    public static File exportStroopResults(Context context, String dateStr,
                                           List<ReportModel.StroopTrialData> trials) {
        if (trials == null || trials.isEmpty()) return null;
        try {
            String patientId = AnonymousIdManager.getId(context);
            File dir = new File(context.getFilesDir(), "exports");
            if (!dir.exists() && !dir.mkdirs()) {
                Log.e(TAG, "Could not create exports directory");
                return null;
            }
            File zip = new File(dir, patientId + "_StroopResults_" + dateStr + ".zip");
            int n = 2;
            while (zip.exists()) {
                zip = new File(dir, patientId + "_StroopResults_" + dateStr + "_" + n + ".zip");
                n++;
            }
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
                writeEntry(zos, "StroopSummary.csv", buildStroopSummaryCsv(trials));
                writeEntry(zos, "StroopAnswers.csv", buildStroopAnswersCsv(trials));
                writeEntry(zos, "README.txt",
                        "BioMS Stroop results export\n"
                                + "PatientID: " + patientId + " (anonymous ID - no name is exported)\n"
                                + "Date: " + dateStr + "\n\n"
                                + "StroopSummary.csv - one row per dual-task trial: totals,\n"
                                + "accuracy, and mean reaction time (correct answers only).\n"
                                + "StroopAnswers.csv - one row per word shown: the word, its\n"
                                + "ink color, what the patient said, whether it matched,\n"
                                + "and the reaction time. Voice recognition is not 100%\n"
                                + "accurate; a 'wrong' answer can be a recognition error.\n");
            }
            Log.d(TAG, "Stroop export written: " + zip.getAbsolutePath());
            return zip;
        } catch (Exception e) {
            Log.e(TAG, "Stroop export failed", e);
            return null;
        }
    }

    private static String buildStroopSummaryCsv(List<ReportModel.StroopTrialData> trials) {
        StringBuilder sb = new StringBuilder();
        sb.append("Trial,TotalTrials,CorrectCount,AccuracyPercent,AvgReactionTimeMs\n");
        for (int i = 0; i < trials.size(); i++) {
            ReportModel.StroopTrialData t = trials.get(i);
            sb.append(String.format(Locale.US, "%d,%d,%d,%.1f,%.0f\n",
                    i + 1, t.totalTrials, t.correctCount, t.accuracyPercent, t.avgReactionMs));
        }
        return sb.toString();
    }

    private static String buildStroopAnswersCsv(List<ReportModel.StroopTrialData> trials) {
        StringBuilder sb = new StringBuilder();
        sb.append("Trial,Answer,Word,InkColor,Spoken,Correct,ReactionTimeMs\n");
        for (int i = 0; i < trials.size(); i++) {
            List<StroopAnswerRecord> answers = trials.get(i).answers;
            if (answers == null) continue;
            for (int j = 0; j < answers.size(); j++) {
                StroopAnswerRecord a = answers.get(j);
                sb.append(String.format(Locale.US, "%d,%d,%s,%s,%s,%s,%d\n",
                        i + 1, j + 1,
                        csvEscape(a.getWordText()), csvEscape(a.getInkColor()),
                        csvEscape(a.getSpokenText()), a.isCorrect(), a.getReactionTimeMs()));
            }
        }
        return sb.toString();
    }

    private static String csvEscape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /** The pending export recorded by the most recent test, if it still exists. */
    public static File getPendingExport(Context context) {
        String path = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LAST_ZIP_PATH, null);
        if (path == null) return null;
        File f = new File(path);
        return f.exists() ? f : null;
    }

    private static void writeEntry(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes("UTF-8"));
        zos.closeEntry();
    }

    private static String buildTrajectoryCsv(SwayMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("time [s],CoM_x_Pos [m],CoM_y_Pos [m],CoM_z_Pos [m]\n");
        if (metrics == null) return sb.toString();
        int n = Math.min(metrics.trajX.size(), metrics.trajZ.size());
        double dt = (metrics.effectiveDurationS > 0 && n > 1)
                ? metrics.effectiveDurationS / (n - 1.0) : 1.0 / 30.0;
        for (int i = 0; i < n; i++) {
            // x = mediolateral, y = vertical (not tracked -> empty), z = anteroposterior
            sb.append(String.format(Locale.US, "%.3f,%.6f,,%.6f\n",
                    i * dt, metrics.trajX.get(i), metrics.trajZ.get(i)));
        }
        return sb.toString();
    }

    /** The 10 joint angles the app records, in export column order. */
    private static final String[] JOINT_COLUMNS = {
            "left_ankle", "right_ankle", "left_knee", "right_knee",
            "left_hip", "right_hip", "left_shoulder", "right_shoulder",
            "left_elbow", "right_elbow"};

    private static String buildJointAnglesCsv(Map<String, List<Float>> jointSeries, SwayMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        // Always all 10 canonical columns, in a fixed order; any extra
        // recorded series are appended after them (sorted) so nothing is lost.
        List<String> joints = new ArrayList<>();
        if (jointSeries != null) {
            for (String j : JOINT_COLUMNS) {
                if (jointSeries.containsKey(j)) joints.add(j);
            }
            List<String> extra = new ArrayList<>(jointSeries.keySet());
            extra.removeAll(joints);
            Collections.sort(extra);
            joints.addAll(extra);
        }
        sb.append("time [s]");
        for (String j : JOINT_COLUMNS) sb.append(',').append(j).append(" [deg]");
        for (int i = JOINT_COLUMNS.length; i < joints.size(); i++) {
            sb.append(',').append(joints.get(i)).append(" [deg]");
        }
        sb.append('\n');
        if (jointSeries == null || jointSeries.isEmpty()) return sb.toString();

        List<String> columns = new ArrayList<>();
        Collections.addAll(columns, JOINT_COLUMNS);
        for (int i = JOINT_COLUMNS.length; i < joints.size(); i++) columns.add(joints.get(i));

        int rows = 0;
        for (String j : joints) rows = Math.max(rows, jointSeries.get(j).size());
        double fps = (metrics != null && metrics.frameRate > 0) ? metrics.frameRate : 30.0;
        for (int i = 0; i < rows; i++) {
            sb.append(String.format(Locale.US, "%.3f", i / fps));
            for (String j : columns) {
                List<Float> series = jointSeries.get(j);
                sb.append(',');
                if (series != null && i < series.size()) {
                    sb.append(String.format(Locale.US, "%.2f", series.get(i)));
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String buildSystemLogCsv(String patientId, String conditionLabel, String dateStr,
                                            SwayMetrics metrics, int droppedFrames, double fovDegrees) {
        StringBuilder sb = new StringBuilder();
        sb.append("Metric,Value\n");
        sb.append("PatientID,").append(patientId).append('\n');
        sb.append("Condition,").append(conditionLabel).append('\n');
        sb.append("Date,").append(dateStr).append('\n');
        sb.append("DroppedFramesCount,").append(droppedFrames).append('\n');
        sb.append("InterpolatedFramesCount,").append(metrics != null ? metrics.interpolatedFrames : 0).append('\n');
        sb.append("DespikedFramesCount,").append(metrics != null ? metrics.despikedFrames : 0).append('\n');
        sb.append("FOV_Used [deg],").append(String.format(Locale.US, "%.1f", fovDegrees)).append('\n');
        if (metrics != null) {
            sb.append("AnchorZ [m],").append(String.format(Locale.US, "%.3f", metrics.anchorZ)).append('\n');
            sb.append("AnchorSource,").append(metrics.anchorSource != null ? metrics.anchorSource : "none").append('\n');
            sb.append("CalibFramesUsed,").append(metrics.calibFramesUsed).append('\n');
            sb.append("CalibLowConfidence,").append(metrics.calibLowConfidence).append('\n');
            sb.append("AnchorDisagreement,").append(metrics.anchorDisagreement).append('\n');
            sb.append("PhoneRotationMax [deg],").append(String.format(Locale.US, "%.2f", metrics.phoneRotationDeg)).append('\n');
            sb.append("APRange [mm],").append(String.format(Locale.US, "%.1f", metrics.apRangeMm)).append('\n');
            sb.append("MLRange [mm],").append(String.format(Locale.US, "%.1f", metrics.mlRangeMm)).append('\n');
            sb.append("RangeFlag,").append(metrics.rangeFlag).append('\n');
        }
        sb.append("CameraDistance [m],");
        if (metrics != null && metrics.cameraDistanceM > 0) {
            sb.append(String.format(Locale.US, "%.2f", metrics.cameraDistanceM));
        }
        sb.append('\n');
        return sb.toString();
    }

    private static String buildReadme(String patientId, String conditionLabel, String dateStr) {
        return "BioMS trial export\n"
                + "PatientID: " + patientId + " (anonymous ID — no name is exported)\n"
                + "Condition: " + conditionLabel + "\n"
                + "Date: " + dateStr + "\n\n"
                + "CoM_Trajectory.csv — the cleaned COM trajectory the sway metrics were\n"
                + "computed from (despiked -> resampled to a uniform 30 Hz grid ->\n"
                + "zero-phase low-pass filtered). Axes follow the camera/MediaPipe\n"
                + "convention: CoM_x_Pos = mediolateral, CoM_z_Pos = anteroposterior\n"
                + "(depth). CoM_y_Pos (vertical) is not tracked by the app — sway is\n"
                + "measured in the ground plane — so that column is intentionally empty.\n\n"
                + "JointAngles.csv — per-frame joint angles in degrees for all 10\n"
                + "joints (ankles, knees, hips, shoulders, elbows — left and right),\n"
                + "sampled at the trial's mean frame rate (see SystemLog / report\n"
                + "for the exact rate).\n\n"
                + "SystemLog.csv — data-quality counters for this trial:\n"
                + "  DroppedFramesCount: frames where MediaPipe lost the patient.\n"
                + "  InterpolatedFramesCount: grid points the signal cleaner filled in\n"
                + "    across a dropout gap.\n"
                + "  DespikedFramesCount: frames where the Hampel filter replaced a\n"
                + "    glitch value.\n"
                + "  FOV_Used: horizontal field of view (degrees) calculated for this\n"
                + "    phone and used for the distance/scale conversion.\n"
                + "  AnchorZ / AnchorSource: the camera-distance anchor frozen at\n"
                + "    test start, and how it was obtained (manual tape distance,\n"
                + "    camera calibration median, or both in agreement).\n"
                + "  CalibFramesUsed / CalibLowConfidence / AnchorDisagreement:\n"
                + "    quality indicators for that calibration.\n"
                + "  PhoneRotationMax: largest rotation of the phone itself during\n"
                + "    the trial (gyroscope audit). Large values mean the camera\n"
                + "    moved, and part of the measured sway may be camera motion.\n"
                + "  APRange / MLRange / RangeFlag: total COM excursion. RangeFlag\n"
                + "    means the excursion exceeded the base of support for the\n"
                + "    stance protocol; treat the trial as invalid (it is flagged,\n"
                + "    never deleted).\n"
                + "  CameraDistance: the camera distance (meters) the user entered\n"
                + "    and confirmed for this trial. Blank when no distance was\n"
                + "    entered (the camera-estimated distance was used).\n";
    }
}
