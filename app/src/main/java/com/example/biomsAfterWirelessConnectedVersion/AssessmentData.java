package com.example.biomsAfterWirelessConnectedVersion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",  // Unique ID
  "timestamp": "2024-12-15T10:30:00Z",                 // When recorded
  "patientId": "patient_001",                          // Who it's for
  "frames": [                                          // 36 landmarks per frame
    {
      "timestamp": 0,                                  // Time since start (ms)
      "landmarks": [[x,y,z], [x,y,z], ...36 times],    // Body coordinates
      "confidence": 0.9                                // Detection confidence
    },
    // ... hundreds more frames
  ],
  "scores": {                                          // Final 4 clinical scores
    "ARJ": 45.2,
    "balance": 78.5,
    "walkingSpeed": 1.2,
    "endurance": 65.0
  }
}
 */

public class AssessmentData implements Serializable {
    private String sessionId;
    private Date timestamp;  // This should be the time that the user finishes recording, because they can start recording but then cancel
    private String patientId;
    private static final String TAG = "AssessmentStorage";  // For logging
    private static final String ASSESSMENTS_DIR = "assessments"; // Folder name
    private TestComparisons comparisons;
    // 1. BASELINE DATA (Normal walking)
    private BaselineAssessment baseline;

    // 2. DUAL TASK DATA (Counting while walking)
    private DualTaskAssessment dualTask;

    // 3. SENSORY ATAXIA DATA (Eyes closed walking)
    private SensoryAtaxiaAssessment sensoryAtaxia;

    // Status tracking
    private AssessmentStatus status;

    public Date getSwayAreaAndBalanceTestDate() {
        return SwayAreaAndBalanceTestDate;
    }

    public void setSwayAreaAndBalanceTestDate(Date SwayAreaAndBalanceTestDate) {
        this.SwayAreaAndBalanceTestDate = SwayAreaAndBalanceTestDate;
        updateStatus();
    }

    public Date getWSAndEnduranceTestDate() {
        return WSAndEnduranceTestDate;
    }

    public void setWSAndEnduranceTestDate(Date WSAndEnduranceTestDate) {
        this.WSAndEnduranceTestDate = WSAndEnduranceTestDate;
        updateStatus();
    }

    // Storage dates
    private Date SwayAreaAndBalanceTestDate;
    private Date WSAndEnduranceTestDate;
    private Date dualTaskTestDate;
    private Date sensoryAtaxiaTestDate;

    public void setStatus(AssessmentStatus assessmentStatus) {
        this.status = assessmentStatus;
    }

    public enum AssessmentStatus {
        NOT_STARTED,
        SWAY_AREA_BALANCE_COMPLETED,
        WS_ENDURANCE_COMPLETED,
        DUAL_TASK_COMPLETED,
        SENSORY_ATAXIA_COMPLETED,
        ALL_COMPLETED,
    }

    public AssessmentStatus getStatus() {
        return status;
    }

    public AssessmentData(String patientId) {
        this.sessionId = UUID.randomUUID().toString(); // UUID stands for Universally Unique Identifier. It generates a 128-bit value that is practically guaranteed to be unique across time and space
        this.timestamp = new Date();
        this.patientId = patientId;
        this.baseline = new BaselineAssessment();
        this.dualTask = new DualTaskAssessment();
        this.sensoryAtaxia = new SensoryAtaxiaAssessment();
        this.comparisons = new TestComparisons();
    }
    public String getSessionId() { return sessionId; }
    public Date getTimestamp() { return timestamp; }
    public Date getDualTaskTestDate() { return dualTaskTestDate; }
    public void setDualTaskTestDate(Date date) {
        this.dualTaskTestDate = date;
        updateStatus();
    }
    public Date getSensoryAtaxiaTestDate() { return sensoryAtaxiaTestDate; }
    public void setSensoryAtaxiaTestDate(Date date) {
        this.sensoryAtaxiaTestDate = date;
        updateStatus();
    }
    public boolean canTakeSwayAreaAndBalanceTest() {
        if (SwayAreaAndBalanceTestDate == null) return true; // Never taken

        Calendar now = Calendar.getInstance();
        Calendar lastTest = Calendar.getInstance();
        lastTest.setTime(SwayAreaAndBalanceTestDate);

        // Check if 24 hours have passed
        lastTest.add(Calendar.DAY_OF_YEAR, 1);
        return now.after(lastTest);
    }

    public boolean canTakeWSAndEnduranceTest() {
        if (WSAndEnduranceTestDate == null) return true; // Never taken

        Calendar now = Calendar.getInstance();
        Calendar lastTest = Calendar.getInstance();
        lastTest.setTime(WSAndEnduranceTestDate);

        // Check if 24 hours have passed
        lastTest.add(Calendar.DAY_OF_YEAR, 1);
        return now.after(lastTest);
    }

    public boolean canTakeDualTaskTest() {
        if (dualTaskTestDate == null) return true;

        Calendar now = Calendar.getInstance();
        Calendar lastTest = Calendar.getInstance();
        lastTest.setTime(dualTaskTestDate);

        lastTest.add(Calendar.DAY_OF_YEAR, 1);
        return now.after(lastTest);
    }

    public boolean canTakeSensoryAtaxiaTest() {
        if (sensoryAtaxiaTestDate == null) return true;

        Calendar now = Calendar.getInstance();
        Calendar lastTest = Calendar.getInstance();
        lastTest.setTime(sensoryAtaxiaTestDate);

        lastTest.add(Calendar.DAY_OF_YEAR, 1);
        return now.after(lastTest);
    }
    private void updateStatus() {
        boolean hasSwayAreaAndBalance = SwayAreaAndBalanceTestDate != null;
        boolean hasWSAndEndurance = WSAndEnduranceTestDate != null;
        boolean hasDualTask = dualTaskTestDate != null;
        boolean hasSensory = sensoryAtaxiaTestDate != null;

        if (hasSwayAreaAndBalance && hasWSAndEndurance && hasDualTask && hasSensory) {
            status = AssessmentStatus.ALL_COMPLETED;
        } else if (hasDualTask) {
            status = AssessmentStatus.DUAL_TASK_COMPLETED;
        } else if (hasSensory) {
            status = AssessmentStatus.SENSORY_ATAXIA_COMPLETED;
        } else if (hasSwayAreaAndBalance) {
            status = AssessmentStatus.SWAY_AREA_BALANCE_COMPLETED;
        } else if (hasWSAndEndurance) {
            status = AssessmentStatus.WS_ENDURANCE_COMPLETED;
        } else {
            status = AssessmentStatus.NOT_STARTED;
        }
    }

    public BaselineAssessment getBaseline() {
        return baseline;
    }

    public DualTaskAssessment getDualTask() {
        return dualTask;
    }

    public SensoryAtaxiaAssessment getSensoryAtaxia() {
        return sensoryAtaxia;
    }

    public TestComparisons getComparisons() {
        return comparisons;
    }

}

class JointAngleData implements Serializable {
    // angles[frameIndex][jointIndex] in degrees
    private float[][] angles;
    private float[] timestamps; // seconds

    // 12 key joints for gait analysis..maybe we'll do 33 if we have time to create 3D skeletons
    private static final String[] JOINT_NAMES = {
            "pelvis_tilt", "pelvis_list", "pelvis_rotation",
            "hip_flexion_r", "hip_adduction_r", "hip_rotation_r",
            "knee_angle_r", "ankle_angle_r",
            "hip_flexion_l", "hip_adduction_l", "hip_rotation_l",
            "knee_angle_l", "ankle_angle_l"
    };

    public JointAngleData(int numFrames) {
        angles = new float[numFrames][JOINT_NAMES.length];
        timestamps = new float[numFrames];
    }

    public void setFrame(int frameIndex, float time, float[] frameAngles) {
        if (frameIndex < angles.length) {
            timestamps[frameIndex] = time;
            System.arraycopy(frameAngles, 0, angles[frameIndex], 0,
                    Math.min(frameAngles.length, JOINT_NAMES.length));
        }
    }

    public float[][] getAngles() { return angles; }
    public float[] getTimestamps() { return timestamps; }
    public static String[] getJointNames() { return JOINT_NAMES; }
}

// BaselineAssessment
class BaselineAssessment implements Serializable {

    private JointAngleData jointAngles;

    private float walkingSpeed;
    private float balanceScore;
    private float enduranceScore;

    private float swayPathLength;   // mm
    private float swayMeanVelocity; // mm/s
    private float swayRmsAP;        // mm (z/depth axis, frontal camera)
    private float swayRmsML;        // mm (x axis, frontal camera)
    private List<Float> comTrajX;   // filtered COM trajectory, meters
    private List<Float> comTrajZ;
    private float comFrameRate;     // mean true sampling rate of the raw trial (fps)
    private boolean swayMetricsValid; // false = too few samples; sway zeros then mean "no measurement"
    private float cameraDistanceM; // camera distance (m) entered for this trial; 0 = none entered
    private boolean swayRangeFlag; // plausibility gate: COM range exceeded the base-of-support bounds

    // Trial diagnostics, persisted so an export generated later from
    // stored data can reproduce the at-save SystemLog in full. These
    // used to exist only in the export created when the test was
    // saved: they were read from live objects (the just-computed
    // metrics and the pose helper's end-of-test snapshot) and were
    // never written to storage, so on-demand exports could not show
    // them. Joint angles remain at-save-only by design (per-frame
    // streams for 10 joints are an export artifact, not report data).
    private boolean diagnosticsRecorded; // false for trials saved before diagnostics were stored
    private int droppedFrames;
    private int interpolatedFrames;
    private int despikedFrames;
    private double fovUsedDegrees;
    private double anchorZ;              // frozen camera-distance anchor (m)
    private String anchorSource;
    private int calibFramesUsed;
    private boolean calibLowConfidence;
    private boolean anchorDisagreement;
    private int anchorClampedFrames;
    private float phoneRotationDeg;      // max phone rotation during the trial (gyro audit)
    private float apRangeMm;
    private float mlRangeMm;

    private Map<String, Float> ARJ = new HashMap<>();
    private Map<String, Float> asymmetry = new HashMap<>();

    public float getWalkingSpeed() { return walkingSpeed; }
    public void setWalkingSpeed(float walkingSpeed) { this.walkingSpeed = walkingSpeed; }

    public Map<String, Float> getARJ() { return ARJ; }
    public void setARJ(Map<String, Float> ARJ) { this.ARJ = ARJ; }

    public float getBalanceScore() { return balanceScore; }
    public void setBalanceScore(float balanceScore) { this.balanceScore = balanceScore; }

    public float getEnduranceScore() { return enduranceScore; }
    public void setEnduranceScore(float enduranceScore) { this.enduranceScore = enduranceScore; }

    public float getSwayPathLength() { return swayPathLength; }
    public void setSwayPathLength(float v) { this.swayPathLength = v; }

    public float getSwayMeanVelocity() { return swayMeanVelocity; }
    public void setSwayMeanVelocity(float v) { this.swayMeanVelocity = v; }

    public float getSwayRmsAP() { return swayRmsAP; }
    public void setSwayRmsAP(float v) { this.swayRmsAP = v; }

    public float getSwayRmsML() { return swayRmsML; }
    public void setSwayRmsML(float v) { this.swayRmsML = v; }

    public List<Float> getComTrajX() { return comTrajX; }
    public void setComTrajX(List<Float> v) { this.comTrajX = v; }

    public List<Float> getComTrajZ() { return comTrajZ; }
    public void setComTrajZ(List<Float> v) { this.comTrajZ = v; }

    public float getComFrameRate() { return comFrameRate; }
    public void setComFrameRate(float v) { this.comFrameRate = v; }
    public boolean isSwayMetricsValid() { return swayMetricsValid; }
    public void setSwayMetricsValid(boolean v) { this.swayMetricsValid = v; }

    public float getCameraDistanceM() { return cameraDistanceM; }
    public void setCameraDistanceM(float v) { this.cameraDistanceM = v; }

    public boolean isSwayRangeFlag() { return swayRangeFlag; }
    public void setSwayRangeFlag(boolean v) { this.swayRangeFlag = v; }

    public boolean isDiagnosticsRecorded() { return diagnosticsRecorded; }
    public void setDiagnosticsRecorded(boolean v) { this.diagnosticsRecorded = v; }
    public int getDroppedFrames() { return droppedFrames; }
    public void setDroppedFrames(int v) { this.droppedFrames = v; }
    public int getInterpolatedFrames() { return interpolatedFrames; }
    public void setInterpolatedFrames(int v) { this.interpolatedFrames = v; }
    public int getDespikedFrames() { return despikedFrames; }
    public void setDespikedFrames(int v) { this.despikedFrames = v; }
    public double getFovUsedDegrees() { return fovUsedDegrees; }
    public void setFovUsedDegrees(double v) { this.fovUsedDegrees = v; }
    public double getAnchorZ() { return anchorZ; }
    public void setAnchorZ(double v) { this.anchorZ = v; }
    public String getAnchorSource() { return anchorSource; }
    public void setAnchorSource(String v) { this.anchorSource = v; }
    public int getCalibFramesUsed() { return calibFramesUsed; }
    public void setCalibFramesUsed(int v) { this.calibFramesUsed = v; }
    public boolean isCalibLowConfidence() { return calibLowConfidence; }
    public void setCalibLowConfidence(boolean v) { this.calibLowConfidence = v; }
    public boolean isAnchorDisagreement() { return anchorDisagreement; }
    public void setAnchorDisagreement(boolean v) { this.anchorDisagreement = v; }
    public int getAnchorClampedFrames() { return anchorClampedFrames; }
    public void setAnchorClampedFrames(int v) { this.anchorClampedFrames = v; }
    public float getPhoneRotationDeg() { return phoneRotationDeg; }
    public void setPhoneRotationDeg(float v) { this.phoneRotationDeg = v; }
    public float getApRangeMm() { return apRangeMm; }
    public void setApRangeMm(float v) { this.apRangeMm = v; }
    public float getMlRangeMm() { return mlRangeMm; }
    public void setMlRangeMm(float v) { this.mlRangeMm = v; }

    /** Reset all sway fields (used when this test's data is deleted). */
    public void clearSwayMetrics() {
        balanceScore = 0f;
        swayPathLength = 0f;
        swayMeanVelocity = 0f;
        swayRmsAP = 0f;
        swayRmsML = 0f;
        comTrajX = null;
        comTrajZ = null;
        comFrameRate = 0f;
        swayMetricsValid = false;
        cameraDistanceM = 0f;
        swayRangeFlag = false;
        diagnosticsRecorded = false;
        droppedFrames = 0;
        interpolatedFrames = 0;
        despikedFrames = 0;
        fovUsedDegrees = 0;
        anchorZ = 0;
        anchorSource = null;
        calibFramesUsed = 0;
        calibLowConfidence = false;
        anchorDisagreement = false;
        anchorClampedFrames = 0;
        phoneRotationDeg = 0f;
        apRangeMm = 0f;
        mlRangeMm = 0f;
    }
}

// DualTaskAssessment
class DualTaskAssessment implements Serializable {

    private JointAngleData jointAngles;

    private float walkingSpeed;
    private float balanceScore;
    private float enduranceScore;

    private float swayPathLength;   // mm
    private float swayMeanVelocity; // mm/s
    private float swayRmsAP;        // mm (z/depth axis, frontal camera)
    private float swayRmsML;        // mm (x axis, frontal camera)
    private List<Float> comTrajX;   // filtered COM trajectory, meters
    private List<Float> comTrajZ;
    private float comFrameRate;     // mean true sampling rate of the raw trial (fps)
    private boolean swayMetricsValid; // false = too few samples; sway zeros then mean "no measurement"
    private float cameraDistanceM; // camera distance (m) entered for this trial; 0 = none entered
    private boolean swayRangeFlag; // plausibility gate: COM range exceeded the base-of-support bounds

    // Trial diagnostics, persisted so an export generated later from
    // stored data can reproduce the at-save SystemLog in full. These
    // used to exist only in the export created when the test was
    // saved: they were read from live objects (the just-computed
    // metrics and the pose helper's end-of-test snapshot) and were
    // never written to storage, so on-demand exports could not show
    // them. Joint angles remain at-save-only by design (per-frame
    // streams for 10 joints are an export artifact, not report data).
    private boolean diagnosticsRecorded; // false for trials saved before diagnostics were stored
    private int droppedFrames;
    private int interpolatedFrames;
    private int despikedFrames;
    private double fovUsedDegrees;
    private double anchorZ;              // frozen camera-distance anchor (m)
    private String anchorSource;
    private int calibFramesUsed;
    private boolean calibLowConfidence;
    private boolean anchorDisagreement;
    private int anchorClampedFrames;
    private float phoneRotationDeg;      // max phone rotation during the trial (gyro audit)
    private float apRangeMm;
    private float mlRangeMm;
    private Map<String, Float> ARJ = new HashMap<>();
    private Map<String, Float> asymmetry = new HashMap<>();

    public float getWalkingSpeed() { return walkingSpeed; }
    public void setWalkingSpeed(float walkingSpeed) { this.walkingSpeed = walkingSpeed; }

    public Map<String, Float> getARJ() { return ARJ; }
    public void setARJ(Map<String, Float> ARJ) { this.ARJ = ARJ; }

    public float getBalanceScore() { return balanceScore; }
    public void setBalanceScore(float balanceScore) { this.balanceScore = balanceScore; }

    public float getEnduranceScore() { return enduranceScore; }
    public void setEnduranceScore(float enduranceScore) { this.enduranceScore = enduranceScore; }

    public float getSwayPathLength() { return swayPathLength; }
    public void setSwayPathLength(float v) { this.swayPathLength = v; }

    public float getSwayMeanVelocity() { return swayMeanVelocity; }
    public void setSwayMeanVelocity(float v) { this.swayMeanVelocity = v; }

    public float getSwayRmsAP() { return swayRmsAP; }
    public void setSwayRmsAP(float v) { this.swayRmsAP = v; }

    public float getSwayRmsML() { return swayRmsML; }
    public void setSwayRmsML(float v) { this.swayRmsML = v; }

    public List<Float> getComTrajX() { return comTrajX; }
    public void setComTrajX(List<Float> v) { this.comTrajX = v; }

    public List<Float> getComTrajZ() { return comTrajZ; }
    public void setComTrajZ(List<Float> v) { this.comTrajZ = v; }

    public float getComFrameRate() { return comFrameRate; }
    public void setComFrameRate(float v) { this.comFrameRate = v; }
    public boolean isSwayMetricsValid() { return swayMetricsValid; }
    public void setSwayMetricsValid(boolean v) { this.swayMetricsValid = v; }

    public float getCameraDistanceM() { return cameraDistanceM; }
    public void setCameraDistanceM(float v) { this.cameraDistanceM = v; }

    public boolean isSwayRangeFlag() { return swayRangeFlag; }
    public void setSwayRangeFlag(boolean v) { this.swayRangeFlag = v; }

    public boolean isDiagnosticsRecorded() { return diagnosticsRecorded; }
    public void setDiagnosticsRecorded(boolean v) { this.diagnosticsRecorded = v; }
    public int getDroppedFrames() { return droppedFrames; }
    public void setDroppedFrames(int v) { this.droppedFrames = v; }
    public int getInterpolatedFrames() { return interpolatedFrames; }
    public void setInterpolatedFrames(int v) { this.interpolatedFrames = v; }
    public int getDespikedFrames() { return despikedFrames; }
    public void setDespikedFrames(int v) { this.despikedFrames = v; }
    public double getFovUsedDegrees() { return fovUsedDegrees; }
    public void setFovUsedDegrees(double v) { this.fovUsedDegrees = v; }
    public double getAnchorZ() { return anchorZ; }
    public void setAnchorZ(double v) { this.anchorZ = v; }
    public String getAnchorSource() { return anchorSource; }
    public void setAnchorSource(String v) { this.anchorSource = v; }
    public int getCalibFramesUsed() { return calibFramesUsed; }
    public void setCalibFramesUsed(int v) { this.calibFramesUsed = v; }
    public boolean isCalibLowConfidence() { return calibLowConfidence; }
    public void setCalibLowConfidence(boolean v) { this.calibLowConfidence = v; }
    public boolean isAnchorDisagreement() { return anchorDisagreement; }
    public void setAnchorDisagreement(boolean v) { this.anchorDisagreement = v; }
    public int getAnchorClampedFrames() { return anchorClampedFrames; }
    public void setAnchorClampedFrames(int v) { this.anchorClampedFrames = v; }
    public float getPhoneRotationDeg() { return phoneRotationDeg; }
    public void setPhoneRotationDeg(float v) { this.phoneRotationDeg = v; }
    public float getApRangeMm() { return apRangeMm; }
    public void setApRangeMm(float v) { this.apRangeMm = v; }
    public float getMlRangeMm() { return mlRangeMm; }
    public void setMlRangeMm(float v) { this.mlRangeMm = v; }

    // Stroop voice test recorded during this dual-task trial. Until now the
    // result of CameraFragment.stopVoiceStroop() was discarded; it is now
    // persisted so reports can present the Stroop test as its own result.
    private int stroopTotalTrials;
    private int stroopCorrectCount;
    private float stroopAccuracyPercent;
    private float stroopAvgReactionMs;
    private List<StroopAnswerRecord> stroopAnswers = new ArrayList<>();

    public int getStroopTotalTrials() { return stroopTotalTrials; }
    public void setStroopTotalTrials(int v) { this.stroopTotalTrials = v; }
    public int getStroopCorrectCount() { return stroopCorrectCount; }
    public void setStroopCorrectCount(int v) { this.stroopCorrectCount = v; }
    public float getStroopAccuracyPercent() { return stroopAccuracyPercent; }
    public void setStroopAccuracyPercent(float v) { this.stroopAccuracyPercent = v; }
    public float getStroopAvgReactionMs() { return stroopAvgReactionMs; }
    public void setStroopAvgReactionMs(float v) { this.stroopAvgReactionMs = v; }
    public List<StroopAnswerRecord> getStroopAnswers() { return stroopAnswers; }
    public void setStroopAnswers(List<StroopAnswerRecord> v) { this.stroopAnswers = v != null ? v : new ArrayList<>(); }

    /** Reset all sway fields (used when this test's data is deleted). */
    public void clearSwayMetrics() {
        balanceScore = 0f;
        swayPathLength = 0f;
        swayMeanVelocity = 0f;
        swayRmsAP = 0f;
        swayRmsML = 0f;
        comTrajX = null;
        comTrajZ = null;
        comFrameRate = 0f;
        swayMetricsValid = false;
        cameraDistanceM = 0f;
        swayRangeFlag = false;
        diagnosticsRecorded = false;
        droppedFrames = 0;
        interpolatedFrames = 0;
        despikedFrames = 0;
        fovUsedDegrees = 0;
        anchorZ = 0;
        anchorSource = null;
        calibFramesUsed = 0;
        calibLowConfidence = false;
        anchorDisagreement = false;
        anchorClampedFrames = 0;
        phoneRotationDeg = 0f;
        apRangeMm = 0f;
        mlRangeMm = 0f;
        stroopTotalTrials = 0;
        stroopCorrectCount = 0;
        stroopAccuracyPercent = 0f;
        stroopAvgReactionMs = 0f;
        stroopAnswers = new ArrayList<>();
    }
}

// SensoryAtaxiaAssessment
class SensoryAtaxiaAssessment implements Serializable {

    private JointAngleData jointAngles;

    private float walkingSpeed;
    private float balanceScore;
    private float enduranceScore;

    // Extended sway metric family (same COM trajectory as balanceScore)
    private float swayPathLength;   // mm
    private float swayMeanVelocity; // mm/s
    private float swayRmsAP;        // mm (z/depth axis, frontal camera)
    private float swayRmsML;        // mm (x axis, frontal camera)
    private List<Float> comTrajX;   // filtered COM trajectory, meters
    private List<Float> comTrajZ;
    private float comFrameRate;     // mean true sampling rate of the raw trial (fps)
    private boolean swayMetricsValid; // false = too few samples; sway zeros then mean "no measurement"
    private float cameraDistanceM; // camera distance (m) entered for this trial; 0 = none entered
    private boolean swayRangeFlag; // plausibility gate: COM range exceeded the base-of-support bounds

    // Trial diagnostics, persisted so an export generated later from
    // stored data can reproduce the at-save SystemLog in full. These
    // used to exist only in the export created when the test was
    // saved: they were read from live objects (the just-computed
    // metrics and the pose helper's end-of-test snapshot) and were
    // never written to storage, so on-demand exports could not show
    // them. Joint angles remain at-save-only by design (per-frame
    // streams for 10 joints are an export artifact, not report data).
    private boolean diagnosticsRecorded; // false for trials saved before diagnostics were stored
    private int droppedFrames;
    private int interpolatedFrames;
    private int despikedFrames;
    private double fovUsedDegrees;
    private double anchorZ;              // frozen camera-distance anchor (m)
    private String anchorSource;
    private int calibFramesUsed;
    private boolean calibLowConfidence;
    private boolean anchorDisagreement;
    private int anchorClampedFrames;
    private float phoneRotationDeg;      // max phone rotation during the trial (gyro audit)
    private float apRangeMm;
    private float mlRangeMm;

    private Map<String, Float> ARJ = new HashMap<>();
    private Map<String, Float> asymmetry = new HashMap<>();

    public float getWalkingSpeed() { return walkingSpeed; }
    public void setWalkingSpeed(float walkingSpeed) { this.walkingSpeed = walkingSpeed; }

    public Map<String, Float> getARJ() { return ARJ; }
    public void setARJ(Map<String, Float> ARJ) { this.ARJ = ARJ; }

    public float getBalanceScore() { return balanceScore; }
    public void setBalanceScore(float balanceScore) { this.balanceScore = balanceScore; }

    public float getEnduranceScore() { return enduranceScore; }
    public void setEnduranceScore(float enduranceScore) { this.enduranceScore = enduranceScore; }


    public float getSwayPathLength() { return swayPathLength; }
    public void setSwayPathLength(float v) { this.swayPathLength = v; }

    public float getSwayMeanVelocity() { return swayMeanVelocity; }
    public void setSwayMeanVelocity(float v) { this.swayMeanVelocity = v; }

    public float getSwayRmsAP() { return swayRmsAP; }
    public void setSwayRmsAP(float v) { this.swayRmsAP = v; }

    public float getSwayRmsML() { return swayRmsML; }
    public void setSwayRmsML(float v) { this.swayRmsML = v; }

    public List<Float> getComTrajX() { return comTrajX; }
    public void setComTrajX(List<Float> v) { this.comTrajX = v; }

    public List<Float> getComTrajZ() { return comTrajZ; }
    public void setComTrajZ(List<Float> v) { this.comTrajZ = v; }

    public float getComFrameRate() { return comFrameRate; }
    public void setComFrameRate(float v) { this.comFrameRate = v; }
    public boolean isSwayMetricsValid() { return swayMetricsValid; }
    public void setSwayMetricsValid(boolean v) { this.swayMetricsValid = v; }

    public float getCameraDistanceM() { return cameraDistanceM; }
    public void setCameraDistanceM(float v) { this.cameraDistanceM = v; }
    public boolean isSwayRangeFlag() { return swayRangeFlag; }
    public void setSwayRangeFlag(boolean v) { this.swayRangeFlag = v; }

    public boolean isDiagnosticsRecorded() { return diagnosticsRecorded; }
    public void setDiagnosticsRecorded(boolean v) { this.diagnosticsRecorded = v; }
    public int getDroppedFrames() { return droppedFrames; }
    public void setDroppedFrames(int v) { this.droppedFrames = v; }
    public int getInterpolatedFrames() { return interpolatedFrames; }
    public void setInterpolatedFrames(int v) { this.interpolatedFrames = v; }
    public int getDespikedFrames() { return despikedFrames; }
    public void setDespikedFrames(int v) { this.despikedFrames = v; }
    public double getFovUsedDegrees() { return fovUsedDegrees; }
    public void setFovUsedDegrees(double v) { this.fovUsedDegrees = v; }
    public double getAnchorZ() { return anchorZ; }
    public void setAnchorZ(double v) { this.anchorZ = v; }
    public String getAnchorSource() { return anchorSource; }
    public void setAnchorSource(String v) { this.anchorSource = v; }
    public int getCalibFramesUsed() { return calibFramesUsed; }
    public void setCalibFramesUsed(int v) { this.calibFramesUsed = v; }
    public boolean isCalibLowConfidence() { return calibLowConfidence; }
    public void setCalibLowConfidence(boolean v) { this.calibLowConfidence = v; }
    public boolean isAnchorDisagreement() { return anchorDisagreement; }
    public void setAnchorDisagreement(boolean v) { this.anchorDisagreement = v; }
    public int getAnchorClampedFrames() { return anchorClampedFrames; }
    public void setAnchorClampedFrames(int v) { this.anchorClampedFrames = v; }
    public float getPhoneRotationDeg() { return phoneRotationDeg; }
    public void setPhoneRotationDeg(float v) { this.phoneRotationDeg = v; }
    public float getApRangeMm() { return apRangeMm; }
    public void setApRangeMm(float v) { this.apRangeMm = v; }
    public float getMlRangeMm() { return mlRangeMm; }
    public void setMlRangeMm(float v) { this.mlRangeMm = v; }

    /** Reset all sway fields (used when this test's data is deleted). */
    public void clearSwayMetrics() {
        balanceScore = 0f;
        swayPathLength = 0f;
        swayMeanVelocity = 0f;
        swayRmsAP = 0f;
        swayRmsML = 0f;
        comTrajX = null;
        comTrajZ = null;
        comFrameRate = 0f;
        swayMetricsValid = false;
        cameraDistanceM = 0f;
        swayRangeFlag = false;
        diagnosticsRecorded = false;
        droppedFrames = 0;
        interpolatedFrames = 0;
        despikedFrames = 0;
        fovUsedDegrees = 0;
        anchorZ = 0;
        anchorSource = null;
        calibFramesUsed = 0;
        calibLowConfidence = false;
        anchorDisagreement = false;
        anchorClampedFrames = 0;
        phoneRotationDeg = 0f;
        apRangeMm = 0f;
        mlRangeMm = 0f;
    }
}

class TestComparisons implements Serializable {
    // DUAL TASK vs BASELINE changes
    private float dualTaskSpeedChange;       // e.g., -25% (slower with counting)
    private float dualTaskBalanceChange;     // e.g., -15% (worse balance with counting)

    public float getDualTaskARJLeftKneeChange() {
        return dualTaskARJLeftKneeChange;
    }

    public void setDualTaskARJLeftKneeChange(float dualTaskARJLeftKneeChange) {
        this.dualTaskARJLeftKneeChange = dualTaskARJLeftKneeChange;
    }

    public float getDualTaskARJRightKneeChange() {
        return dualTaskARJRightKneeChange;
    }

    public void setDualTaskARJRightKneeChange(float dualTaskARJRightKneeChange) {
        this.dualTaskARJRightKneeChange = dualTaskARJRightKneeChange;
    }

    private float dualTaskARJLeftKneeChange;   // e.g., +30% (more stiff with counting)
    private float dualTaskARJRightKneeChange;   // e.g., +30% (more stiff with counting)
    private float dualTaskEnduranceChange; // e.g., -50% (much worse balance eyes closed)

    // SENSORY ATAXIA vs BASELINE changes
    private float sensoryAtaxiaSpeedChange;  // e.g., -40% (much slower eyes closed)
    private float sensoryAtaxiaBalanceChange; // e.g., -50% (much worse balance eyes closed)

    public float getSensoryAtaxiaARJLeftKneeChange() {
        return sensoryAtaxiaARJLeftKneeChange;
    }

    public void setSensoryAtaxiaARJLeftKneeChange(float sensoryAtaxiaARJLeftKneeChange) {
        this.sensoryAtaxiaARJLeftKneeChange = sensoryAtaxiaARJLeftKneeChange;
    }

    public float getSensoryAtaxiaARJRightKneeChange() {
        return sensoryAtaxiaARJRightKneeChange;
    }

    public void setSensoryAtaxiaARJRightKneeChange(float sensoryAtaxiaARJRightKneeChange) {
        this.sensoryAtaxiaARJRightKneeChange = sensoryAtaxiaARJRightKneeChange;
    }

    private float sensoryAtaxiaARJLeftKneeChange; // e.g., -50% (much worse balance eyes closed)
    private float sensoryAtaxiaARJRightKneeChange; // e.g., -50% (much worse balance eyes closed)
    private float sensoryAtaxiaEnduranceChange; // e.g., -50% (much worse balance eyes closed)

    // Dual-task / sensory-ataxia costs on the extended sway metrics (% change vs baseline)
    private float dualTaskVelocityChange;
    private float dualTaskPathLengthChange;
    private float sensoryAtaxiaVelocityChange;
    private float sensoryAtaxiaPathLengthChange;

    // ASYMMETRY SCORES
    private Map<String, Float> asymmetryScores;  // hip, knee, ankle

    public TestComparisons() {
        this.asymmetryScores = new HashMap<>();
    }
    public void setDualTaskSpeedChange(float value) {
        this.dualTaskSpeedChange = value;
    }

    public void setDualTaskBalanceChange(float value) {
        this.dualTaskBalanceChange = value;
    }
    public void setDualTaskEnduranceChange(float value) {
        this.dualTaskEnduranceChange = value;
    }

    public void setSensoryAtaxiaSpeedChange(float value) {
        this.sensoryAtaxiaSpeedChange = value;
    }

    public void setSensoryAtaxiaBalanceChange(float value) {
        this.sensoryAtaxiaBalanceChange = value;
    }
    public void setSensoryAtaxiaEnduranceChange(float value) {
        this.sensoryAtaxiaEnduranceChange = value;
    }

    public float getDualTaskVelocityChange() { return dualTaskVelocityChange; }
    public void setDualTaskVelocityChange(float v) { this.dualTaskVelocityChange = v; }

    public float getDualTaskPathLengthChange() { return dualTaskPathLengthChange; }
    public void setDualTaskPathLengthChange(float v) { this.dualTaskPathLengthChange = v; }

    public float getSensoryAtaxiaVelocityChange() { return sensoryAtaxiaVelocityChange; }
    public void setSensoryAtaxiaVelocityChange(float v) { this.sensoryAtaxiaVelocityChange = v; }

    public float getSensoryAtaxiaPathLengthChange() { return sensoryAtaxiaPathLengthChange; }
    public void setSensoryAtaxiaPathLengthChange(float v) { this.sensoryAtaxiaPathLengthChange = v; }


    public float getDualTaskSpeedChange() {
        return dualTaskSpeedChange;
    }

    public float getDualTaskBalanceChange() {
        return dualTaskBalanceChange;
    }

    public float getDualTaskEnduranceChange() {
        return dualTaskEnduranceChange;
    }

    public float getSensoryAtaxiaSpeedChange() {
        return sensoryAtaxiaSpeedChange;
    }

    public float getSensoryAtaxiaBalanceChange() {
        return sensoryAtaxiaBalanceChange;
    }

    public float getSensoryAtaxiaEnduranceChange() {
        return sensoryAtaxiaEnduranceChange;
    }

    public Map<String, Float> getAsymmetryScores() {
        return asymmetryScores;
    }
}

/** One judged Stroop answer, stored with the dual-task trial that recorded
 *  it (the storage form of StroopVoiceManager.StroopAnswer). */
class StroopAnswerRecord implements Serializable {

    private String wordText;
    private String inkColor;
    private String spokenText;
    private boolean correct;
    private long reactionTimeMs;

    public StroopAnswerRecord() {
    }

    public StroopAnswerRecord(String wordText, String inkColor, String spokenText,
                              boolean correct, long reactionTimeMs) {
        this.wordText = wordText;
        this.inkColor = inkColor;
        this.spokenText = spokenText;
        this.correct = correct;
        this.reactionTimeMs = reactionTimeMs;
    }

    public String getWordText() { return wordText; }
    public void setWordText(String v) { this.wordText = v; }
    public String getInkColor() { return inkColor; }
    public void setInkColor(String v) { this.inkColor = v; }
    public String getSpokenText() { return spokenText; }
    public void setSpokenText(String v) { this.spokenText = v; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean v) { this.correct = v; }
    public long getReactionTimeMs() { return reactionTimeMs; }
    public void setReactionTimeMs(long v) { this.reactionTimeMs = v; }
}
