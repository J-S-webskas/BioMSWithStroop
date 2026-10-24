package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private TrialCsvExporter() { /* static helpers only */ }

    /**
     * Writes the export zip into the app's files/exports directory.
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

            Log.d(TAG, "Export written: " + zip.getAbsolutePath());
            return zip;
        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Joint-angle sidecar files.
    //
    // Per-frame joint angles are deliberately NOT part of the stored
    // assessment: reports load every assessment at once, and angle
    // streams would bloat that load for data the reports never show.
    // Instead, at save time the angles are written to a sidecar file
    // per trial -- the exact JointAngles.csv content the at-save
    // export carries -- under files/joint_angles/, named
    // <sessionId>_<Condition>.csv (the session id is unique per
    // trial). Stored exports attach the sidecar verbatim when one
    // exists; trials recorded before sidecar storage have none.
    // Deleting a trial's data deletes its sidecar (see
    // DataStorageManager.deleteAssessment).
    // ------------------------------------------------------------------

    private static File jointAnglesDir(Context context) {
        File dir = new File(context.getFilesDir(), "joint_angles");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static File jointAnglesFile(Context context, String sessionId, String conditionLabel) {
        if (sessionId == null || conditionLabel == null) return null;
        return new File(jointAnglesDir(context), sessionId + "_" + conditionLabel + ".csv");
    }

    /** Writes a trial's joint angles as its sidecar file. */
    public static void saveJointAnglesSidecar(Context context, String sessionId,
                                              String conditionLabel,
                                              Map<String, List<Float>> jointSeries,
                                              SwayMetrics metrics) {
        try {
            File f = jointAnglesFile(context, sessionId, conditionLabel);
            if (f == null || jointSeries == null || jointSeries.isEmpty()) return;
            String csv = buildJointAnglesCsv(jointSeries, metrics);
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(csv.getBytes(StandardCharsets.UTF_8));
            }
            Log.d(TAG, "Joint-angle sidecar written: " + f.getName());
        } catch (Exception e) {
            Log.e(TAG, "Joint-angle sidecar write failed", e);
        }
    }

    /** Returns a trial's stored joint-angles CSV, or null if none exists. */
    public static String readJointAnglesSidecar(Context context, String sessionId,
                                                String conditionLabel) {
        try {
            File f = jointAnglesFile(context, sessionId, conditionLabel);
            if (f == null || !f.exists()) return null;
            // Plain stream read (java.nio.file.Files needs API 26; the
            // app supports API 24).
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(
                            new java.io.FileInputStream(f), StandardCharsets.UTF_8))) {
                char[] buf = new char[8192];
                int n;
                while ((n = reader.read(buf)) != -1) {
                    sb.append(buf, 0, n);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Joint-angle sidecar read failed", e);
            return null;
        }
    }

    /** Deletes a trial's joint-angle sidecar, if it exists. */
    public static void deleteJointAnglesSidecar(Context context, String sessionId,
                                                String conditionLabel) {
        try {
            File f = jointAnglesFile(context, sessionId, conditionLabel);
            if (f != null && f.exists() && !f.delete()) {
                Log.e(TAG, "Could not delete joint-angle sidecar: " + f.getName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Joint-angle sidecar delete failed", e);
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
        public String sessionId = null; // locates the trial's joint-angle sidecar
        // Trial diagnostics, persisted on the assessment at save time
        // so this export can reproduce the at-save SystemLog. Trials
        // saved before diagnostics were stored have
        // diagnosticsRecorded = false and say so in their SystemLog.
        public boolean diagnosticsRecorded = false;
        public int droppedFrames = 0;
        public int interpolatedFrames = 0;
        public int despikedFrames = 0;
        public double fovUsedDegrees = 0;
        public double anchorZ = 0;
        public String anchorSource = null;
        public int calibFramesUsed = 0;
        public boolean calibLowConfidence = false;
        public boolean anchorDisagreement = false;
        public int anchorClampedFrames = 0;
        public float phoneRotationDeg = 0f;
        public float apRangeMm = 0f;
        public float mlRangeMm = 0f;
        public boolean rangeFlag = false;
    }

    /** Copies the persisted diagnostics of a stored Baseline trial. */
    public static void fillDiagnostics(StoredTrialData t, BaselineAssessment c) {
        t.diagnosticsRecorded = c.isDiagnosticsRecorded();
        t.droppedFrames = c.getDroppedFrames();
        t.interpolatedFrames = c.getInterpolatedFrames();
        t.despikedFrames = c.getDespikedFrames();
        t.fovUsedDegrees = c.getFovUsedDegrees();
        t.anchorZ = c.getAnchorZ();
        t.anchorSource = c.getAnchorSource();
        t.calibFramesUsed = c.getCalibFramesUsed();
        t.calibLowConfidence = c.isCalibLowConfidence();
        t.anchorDisagreement = c.isAnchorDisagreement();
        t.anchorClampedFrames = c.getAnchorClampedFrames();
        t.phoneRotationDeg = c.getPhoneRotationDeg();
        t.apRangeMm = c.getApRangeMm();
        t.mlRangeMm = c.getMlRangeMm();
        t.rangeFlag = c.isSwayRangeFlag();
    }

    /** Copies the persisted diagnostics of a stored DualTask trial. */
    public static void fillDiagnostics(StoredTrialData t, DualTaskAssessment c) {
        t.diagnosticsRecorded = c.isDiagnosticsRecorded();
        t.droppedFrames = c.getDroppedFrames();
        t.interpolatedFrames = c.getInterpolatedFrames();
        t.despikedFrames = c.getDespikedFrames();
        t.fovUsedDegrees = c.getFovUsedDegrees();
        t.anchorZ = c.getAnchorZ();
        t.anchorSource = c.getAnchorSource();
        t.calibFramesUsed = c.getCalibFramesUsed();
        t.calibLowConfidence = c.isCalibLowConfidence();
        t.anchorDisagreement = c.isAnchorDisagreement();
        t.anchorClampedFrames = c.getAnchorClampedFrames();
        t.phoneRotationDeg = c.getPhoneRotationDeg();
        t.apRangeMm = c.getApRangeMm();
        t.mlRangeMm = c.getMlRangeMm();
        t.rangeFlag = c.isSwayRangeFlag();
    }

    /** Copies the persisted diagnostics of a stored SensoryAtaxia trial. */
    public static void fillDiagnostics(StoredTrialData t, SensoryAtaxiaAssessment c) {
        t.diagnosticsRecorded = c.isDiagnosticsRecorded();
        t.droppedFrames = c.getDroppedFrames();
        t.interpolatedFrames = c.getInterpolatedFrames();
        t.despikedFrames = c.getDespikedFrames();
        t.fovUsedDegrees = c.getFovUsedDegrees();
        t.anchorZ = c.getAnchorZ();
        t.anchorSource = c.getAnchorSource();
        t.calibFramesUsed = c.getCalibFramesUsed();
        t.calibLowConfidence = c.isCalibLowConfidence();
        t.anchorDisagreement = c.isAnchorDisagreement();
        t.anchorClampedFrames = c.getAnchorClampedFrames();
        t.phoneRotationDeg = c.getPhoneRotationDeg();
        t.apRangeMm = c.getApRangeMm();
        t.mlRangeMm = c.getMlRangeMm();
        t.rangeFlag = c.isSwayRangeFlag();
    }

    /**
     * Builds a ZIP for one condition of one stored day, from data saved in
     * assessments. Contains the trajectory + a system log per trial (files
     * gain a _T&lt;n&gt; suffix when the day has several trials of the
     * condition). Joint angles are stored per trial as sidecar files
     * and are attached when present (trials recorded before sidecar
     * storage have none); the trial diagnostics ARE stored in the
     * assessment and are reproduced in the system log. The README in
     * the zip says so.
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
                    // The trial's joint angles, attached verbatim from
                    // its sidecar file when one was stored.
                    String anglesCsv = readJointAnglesSidecar(context,
                            trials.get(i).sessionId, conditionLabel);
                    if (anglesCsv != null) {
                        writeEntry(zos, base + suffix + "_JointAngles.csv", anglesCsv);
                    }
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

    /** One selected trial inside a multi-day export. */
    public static class MultiExportEntry {
        public String dateStr;          // yyyy-MM-dd of the trial's day
        public String conditionLabel;   // Baseline / DualTask / SensoryAtaxia
        public int trialIndex;          // 1-based within its day + condition
        public int trialCount;          // trials of that day + condition
        public StoredTrialData trial;
    }

    /**
     * Builds one ZIP for an arbitrary selection of stored trials spanning
     * several days and conditions. Files are grouped into folders
     * "<date>_<Condition>/" and named like the single-day export, with a
     * _T&lt;n&gt; suffix that always counts within the trial's own day and
     * condition. Joint angles are attached from the sidecar when present;
     * the README lists everything included.
     *
     * @return the zip file, or null on failure / empty selection
     */
    public static File exportSelectedTrials(Context context, List<MultiExportEntry> entries) {
        if (entries == null || entries.isEmpty()) return null;
        try {
            String patientId = AnonymousIdManager.getId(context);
            File dir = new File(context.getFilesDir(), "exports");
            if (!dir.exists() && !dir.mkdirs()) {
                Log.e(TAG, "Could not create exports directory");
                return null;
            }
            SimpleDateFormat stampFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String stamp = stampFmt.format(new Date());
            File zip = new File(dir, patientId + "_MultiExport_" + stamp + ".zip");
            int n = 2;
            while (zip.exists()) {
                zip = new File(dir, patientId + "_MultiExport_" + stamp + "_" + n + ".zip");
                n++;
            }
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
                for (MultiExportEntry e : entries) {
                    String folder = e.dateStr + "_" + e.conditionLabel + "/";
                    String base = patientId + "_" + e.conditionLabel + "_T" + e.trialIndex;
                    writeEntry(zos, folder + base + "_CoM_Trajectory.csv",
                            buildStoredTrajectoryCsv(e.trial));
                    writeEntry(zos, folder + base + "_SystemLog.csv",
                            buildStoredSystemLogCsv(patientId, e.conditionLabel, e.dateStr,
                                    e.trial, e.trialIndex, e.trialCount));
                    String anglesCsv = readJointAnglesSidecar(context,
                            e.trial.sessionId, e.conditionLabel);
                    if (anglesCsv != null) {
                        writeEntry(zos, folder + base + "_JointAngles.csv", anglesCsv);
                    }
                }
                writeEntry(zos, "README.txt", buildMultiReadme(patientId, entries));
            }
            Log.d(TAG, "Multi export written: " + zip.getAbsolutePath());
            return zip;
        } catch (Exception e) {
            Log.e(TAG, "Multi export failed", e);
            return null;
        }
    }

    private static String buildMultiReadme(String patientId, List<MultiExportEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("BioMS multi-day export\n");
        sb.append("PatientID,").append(patientId).append('\n');
        sb.append("Source,Stored report data (export generated on demand)\n");
        sb.append("Included trials:\n");
        for (MultiExportEntry e : entries) {
            sb.append(e.dateStr).append(",").append(e.conditionLabel)
                    .append(",Trial ").append(e.trialIndex)
                    .append(" of ").append(e.trialCount).append('\n');
        }
        sb.append("\nEach trial contributes a CoM trajectory CSV and a system log; "
                + "joint angles are included only when a sidecar file was stored for "
                + "that trial. Trials saved before diagnostics were stored say so in "
                + "their system log.\n");
        return sb.toString();
    }

    /** One day's reports inside a multi-report export. */
    public static class ReportDayExport {
        public String dateKey; // yyyy-MM-dd, used for the folder name
        public List<ReportModel> reports;
    }

    /**
     * Builds one ZIP of report summaries for the chosen days: one folder
     * per day ("&lt;date&gt;/") holding that day's "Reports.csv" — one row
     * per saved report, headline metrics only — plus a README listing
     * the included days.
     *
     * @return the zip file, or null on failure / empty selection
     */
    public static File exportSelectedReports(Context context, List<ReportDayExport> days) {
        if (days == null || days.isEmpty()) return null;
        try {
            String patientId = AnonymousIdManager.getId(context);
            File dir = new File(context.getFilesDir(), "exports");
            if (!dir.exists() && !dir.mkdirs()) {
                Log.e(TAG, "Could not create exports directory");
                return null;
            }
            SimpleDateFormat stampFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String stamp = stampFmt.format(new Date());
            File zip = new File(dir, patientId + "_Reports_" + stamp + ".zip");
            int n = 2;
            while (zip.exists()) {
                zip = new File(dir, patientId + "_Reports_" + stamp + "_" + n + ".zip");
                n++;
            }
            SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.US);
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
                for (ReportDayExport d : days) {
                    if (d.reports == null || d.reports.isEmpty()) continue;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Date,Time,Sway Area [mm^2],Sway Velocity [mm/s],"
                            + "Sway Path Length [mm],RMS AP [mm],RMS ML [mm],"
                            + "Endurance [s],Walking Speed [m/s],"
                            + "Dual Task Balance DTC [%],Sensory Ataxia Balance DTC [%],"
                            + "Balance SD,Balance CV [%]\n");
                    for (ReportModel r : d.reports) {
                        sb.append(r.getDate()).append(',');
                        sb.append(r.getTimeStamp() != null ? timeFmt.format(r.getTimeStamp()) : "").append(',');
                        sb.append(fmt(r.getBalanceToday())).append(',');
                        sb.append(fmt(r.getSwayVelocityToday())).append(',');
                        sb.append(fmt(r.getSwayPathLengthToday())).append(',');
                        sb.append(fmt(r.getSwayRmsAPToday())).append(',');
                        sb.append(fmt(r.getSwayRmsMLToday())).append(',');
                        sb.append(fmt(r.getEnduranceToday())).append(',');
                        sb.append(fmt(r.getSpeedToday())).append(',');
                        sb.append(fmt(r.getDualTaskDTCBalance())).append(',');
                        sb.append(fmt(r.getSensoryAtaxiaDTCBalance())).append(',');
                        sb.append(fmt(r.getBalanceSD())).append(',');
                        sb.append(fmt(r.getBalanceCV())).append('\n');
                    }
                    writeEntry(zos, d.dateKey + "/Reports.csv", sb.toString());
                }
                writeEntry(zos, "README.txt", buildReportsReadme(patientId, days));
            }
            Log.d(TAG, "Reports export written: " + zip.getAbsolutePath());
            return zip;
        } catch (Exception e) {
            Log.e(TAG, "Reports export failed", e);
            return null;
        }
    }

    private static String fmt(float v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private static String buildReportsReadme(String patientId, List<ReportDayExport> days) {
        StringBuilder sb = new StringBuilder();
        sb.append("BioMS report export\n");
        sb.append("PatientID,").append(patientId).append('\n');
        sb.append("Source,Saved reports (export generated on demand)\n");
        sb.append("Included days:\n");
        for (ReportDayExport d : days) {
            int count = d.reports == null ? 0 : d.reports.size();
            sb.append(d.dateKey).append(",").append(count)
                    .append(count == 1 ? " report" : " reports").append('\n');
        }
        sb.append("\nEach day's folder holds that day's Reports.csv with one row "
                + "per saved report (headline metrics only).\n");
        return sb.toString();
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
        if (t.diagnosticsRecorded) {
            // The same diagnostic rows the at-save SystemLog carries,
            // reproduced from the values stored with the assessment.
            sb.append("DroppedFramesCount,").append(t.droppedFrames).append('\n');
            sb.append("InterpolatedFramesCount,").append(t.interpolatedFrames).append('\n');
            sb.append("DespikedFramesCount,").append(t.despikedFrames).append('\n');
            sb.append("FOV_Used [deg],").append(String.format(Locale.US, "%.1f", t.fovUsedDegrees)).append('\n');
            sb.append("AnchorZ [m],").append(String.format(Locale.US, "%.3f", t.anchorZ)).append('\n');
            sb.append("AnchorSource,").append(t.anchorSource != null ? t.anchorSource : "none").append('\n');
            sb.append("CalibFramesUsed,").append(t.calibFramesUsed).append('\n');
            sb.append("CalibLowConfidence,").append(t.calibLowConfidence).append('\n');
            sb.append("AnchorDisagreement,").append(t.anchorDisagreement).append('\n');
            sb.append("AnchorClampedFrames,").append(t.anchorClampedFrames).append('\n');
            sb.append("PhoneRotationMax [deg],").append(String.format(Locale.US, "%.2f", t.phoneRotationDeg)).append('\n');
            sb.append("APRange [mm],").append(String.format(Locale.US, "%.1f", t.apRangeMm)).append('\n');
            sb.append("MLRange [mm],").append(String.format(Locale.US, "%.1f", t.mlRangeMm)).append('\n');
            sb.append("RangeFlag,").append(t.rangeFlag).append('\n');
        } else {
            sb.append("TrialDiagnostics,not recorded (this trial was saved before diagnostics were stored)\n");
        }
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
                + "Joint angles are stored per trial as sidecar files (not inside\n"
                + "the report data) and are included here when the trial was\n"
                + "recorded after sidecar storage was added; older trials have\n"
                + "no stored angles -- theirs existed only in the export created\n"
                + "when the test was saved. The trial diagnostics (frame counters, FOV, anchor\n"
                + "and calibration details, phone rotation, sway ranges) ARE\n"
                + "stored with each trial and appear in the SystemLog above;\n"
                + "trials saved before diagnostics were stored say so there\n"
                + "instead.\n\n"
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
            sb.append("AnchorClampedFrames,").append(metrics.anchorClampedFrames).append('\n');
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
                + "  AnchorClampedFrames: frames where the working distance hit\n"
                + "    the +/-10% band around the frozen anchor and was held at\n"
                + "    the band edge. A large count means the distance estimate\n"
                + "    fought the anchor for much of the trial (for example the\n"
                + "    subject was still moving into position); treat the\n"
                + "    trial's absolute values with caution.\n"
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
