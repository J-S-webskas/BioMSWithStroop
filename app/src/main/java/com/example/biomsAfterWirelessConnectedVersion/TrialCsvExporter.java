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
 *   &lt;PatientID&gt;_&lt;Condition&gt;_&lt;n&gt;_CoM_Trajectory.csv
 *   &lt;PatientID&gt;_&lt;Condition&gt;_&lt;n&gt;_JointAngles.csv
 *   &lt;PatientID&gt;_&lt;Condition&gt;_&lt;n&gt;_SystemLog.csv
 *   README.txt
 *
 * zipped as &lt;PatientID&gt;_&lt;yyyy-MM-dd&gt;_&lt;Condition&gt;_&lt;n&gt;.zip,
 * where n is the trial's number within its condition for that day,
 * counting from 1 (the first Baseline of a day is _Baseline_1, the
 * second _Baseline_2, and so on; DualTask and SensoryAtaxia are
 * numbered the same way, each in their own sequence). The number is
 * also recorded inside SystemLog.csv and README.txt, so exported
 * trials of one condition can live in one folder without overwriting
 * each other. The patient is identified ONLY by the anonymous ID from
 * {@link AnonymousIdManager} — never by name.
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

            // Number this trial within its condition for today, counting
            // from 1: the export runs before the just-finished trial is
            // saved, so it is (stored trials of this condition today) + 1.
            int trialNumber = countStoredTrialsToday(context, conditionLabel, dateStr) + 1;
            String base = patientId + "_" + conditionLabel + "_" + trialNumber;
            File zip = new File(dir, patientId + "_" + dateStr + "_" + conditionLabel
                    + "_" + trialNumber + ".zip");
            while (zip.exists()) {
                // An earlier export with this number is still on disk
                // (e.g. its assessment was deleted afterwards): move to
                // the next free number rather than overwriting it.
                trialNumber++;
                base = patientId + "_" + conditionLabel + "_" + trialNumber;
                zip = new File(dir, patientId + "_" + dateStr + "_" + conditionLabel
                        + "_" + trialNumber + ".zip");
            }

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
                writeEntry(zos, base + "_CoM_Trajectory.csv",
                        buildTrajectoryCsv(metrics));
                writeEntry(zos, base + "_JointAngles.csv",
                        buildJointAnglesCsv(jointSeries, metrics));
                writeEntry(zos, base + "_SystemLog.csv",
                        buildSystemLogCsv(patientId, conditionLabel, dateStr, trialNumber,
                                metrics, droppedFrames, fovDegrees));
                writeEntry(zos, "README.txt",
                        buildReadme(patientId, conditionLabel, dateStr, trialNumber));
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

    /**
     * How many trials of the given condition are already stored for the
     * given day (yyyy-MM-dd). Each save stores one assessment per trial,
     * dated by that condition's test date (falling back to the assessment
     * timestamp when the condition carries data but no test date).
     * Returns 0 if storage cannot be read, so numbering simply restarts.
     */
    private static int countStoredTrialsToday(Context context, String conditionLabel,
                                              String dateStr) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            int count = 0;
            for (AssessmentData a : DataStorageManager.loadAllAssessments(context)) {
                Date testDate = null;
                boolean hasData = false;
                if ("Baseline".equals(conditionLabel)) {
                    testDate = a.getSwayAreaAndBalanceTestDate();
                    hasData = a.getBaseline() != null && a.getBaseline().getBalanceScore() > 0;
                } else if ("DualTask".equals(conditionLabel)) {
                    testDate = a.getDualTaskTestDate();
                    hasData = a.getDualTask() != null && a.getDualTask().getBalanceScore() > 0;
                } else if ("SensoryAtaxia".equals(conditionLabel)) {
                    testDate = a.getSensoryAtaxiaTestDate();
                    hasData = a.getSensoryAtaxia() != null
                            && a.getSensoryAtaxia().getBalanceScore() > 0;
                }
                Date when = testDate != null ? testDate : (hasData ? a.getTimestamp() : null);
                if (when != null && dateStr.equals(fmt.format(when))) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            Log.e(TAG, "Could not count stored trials for export numbering", e);
            return 0;
        }
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

    private static String buildJointAnglesCsv(Map<String, List<Float>> jointSeries, SwayMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        if (jointSeries == null || jointSeries.isEmpty()) {
            sb.append("time [s]\n");
            return sb.toString();
        }
        List<String> joints = new ArrayList<>(jointSeries.keySet());
        Collections.sort(joints);
        sb.append("time [s]");
        for (String j : joints) sb.append(',').append(j).append(" [deg]");
        sb.append('\n');

        int rows = 0;
        for (String j : joints) rows = Math.max(rows, jointSeries.get(j).size());
        double fps = (metrics != null && metrics.frameRate > 0) ? metrics.frameRate : 30.0;
        for (int i = 0; i < rows; i++) {
            sb.append(String.format(Locale.US, "%.3f", i / fps));
            for (String j : joints) {
                List<Float> series = jointSeries.get(j);
                sb.append(',');
                if (i < series.size()) sb.append(String.format(Locale.US, "%.2f", series.get(i)));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String buildSystemLogCsv(String patientId, String conditionLabel, String dateStr,
                                            int trialNumber, SwayMetrics metrics,
                                            int droppedFrames, double fovDegrees) {
        StringBuilder sb = new StringBuilder();
        sb.append("Metric,Value\n");
        sb.append("PatientID,").append(patientId).append('\n');
        sb.append("Condition,").append(conditionLabel).append('\n');
        sb.append("TrialNumber,").append(trialNumber).append('\n');
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
        return sb.toString();
    }

    private static String buildReadme(String patientId, String conditionLabel, String dateStr,
                                      int trialNumber) {
        return "BioMS trial export\n"
                + "PatientID: " + patientId + " (anonymous ID — no name is exported)\n"
                + "Condition: " + conditionLabel + "\n"
                + "Trial: " + conditionLabel + " #" + trialNumber + " of " + dateStr
                + " (trials of a condition are numbered within their day, from 1)\n"
                + "Date: " + dateStr + "\n\n"
                + "CoM_Trajectory.csv — the cleaned COM trajectory the sway metrics were\n"
                + "computed from (despiked -> resampled to a uniform 30 Hz grid ->\n"
                + "zero-phase low-pass filtered). Axes follow the camera/MediaPipe\n"
                + "convention: CoM_x_Pos = mediolateral, CoM_z_Pos = anteroposterior\n"
                + "(depth). CoM_y_Pos (vertical) is not tracked by the app — sway is\n"
                + "measured in the ground plane — so that column is intentionally empty.\n\n"
                + "JointAngles.csv — per-frame joint angles in degrees, sampled at the\n"
                + "trial's mean frame rate (see SystemLog / report for the exact rate).\n\n"
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
                + "    never deleted).\n";
    }
}
