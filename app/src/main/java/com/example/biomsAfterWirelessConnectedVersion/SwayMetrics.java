package com.example.biomsAfterWirelessConnectedVersion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 Complete sway metric family computed from one COM trajectory.
 Units: area in mm^2, lengths in mm, velocity in mm/s.
 Trajectory points are stored in meters (as recorded).

 Axis convention (frontal camera facing the subject):
 x = mediolateral (ML), z = anteroposterior (AP, depth).
 If the camera orientation differs, swap the AP/ML labels in the study protocol notes.
 */
public class SwayMetrics implements Serializable {

    public float swayAreaMm2 = 0f;      // 95% confidence ellipse area
    public float pathLengthMm = 0f;     // total COM trajectory length
    public float meanVelocityMmS = 0f;  // path length / trial duration
    public float rmsAPMm = 0f;          // RMS displacement, anteroposterior (z)
    public float rmsMLMm = 0f;          // RMS displacement, mediolateral (x)

    public float frameRate = 0f;        // MEAN TRUE sampling rate of the raw trial (fps).
    // Filtering runs on a uniform 30 Hz resampled grid;
    // this field records what the camera actually delivered,
    // which is what the methods section must report.
    public int sampleCount = 0;
    public boolean valid = false;       // false when the trial had too few samples to measure
    public float validFraction = 0f;    // real samples / uniform grid points (1.0 = no interpolation)
    public float effectiveDurationS = 0f; // resampled grid span (s) — denominator of mean velocity
    public int despikedFrames = 0;      // samples where the Hampel despiker replaced a value (either axis)
    public int interpolatedFrames = 0;  // grid points interpolated across a dropout gap
    public List<Float> trajX = new ArrayList<>();  // filtered COM trajectory
    public List<Float> trajZ = new ArrayList<>();

    public float cameraDistanceM = 0f;  // camera distance (m) the user recorded
    // this trial with; 0 = none entered

    public float apRangeMm = 0f;        // max-min of the filtered AP (z) series
    public float mlRangeMm = 0f;        // max-min of the filtered ML (x) series
    public boolean rangeFlag = false;   // plausibility gate: range beyond the stance's base of support

    // Anchor audit, filled by RecordFragment from the camera/pose helper
    // at save time and written to the export SystemLog.
    public double anchorZ = 0;
    public String anchorSource = "none";
    public int calibFramesUsed = 0;
    public boolean calibLowConfidence = false;
    public boolean anchorDisagreement = false;
    public int anchorClampedFrames = 0; // frames the +/-10% anchor clamp corrected
    public float phoneRotationDeg = 0f; // max device rotation during the trial (gyro audit)
    public int droppedFrames = 0;       // frames where MediaPipe lost the patient (filled at export/save time)
    public double fovUsedDegrees = 0;   // horizontal FOV actually used for the scale conversion (filled at export/save
}