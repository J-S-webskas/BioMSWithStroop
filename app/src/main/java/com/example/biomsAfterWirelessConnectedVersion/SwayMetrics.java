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

    public List<Float> trajX = new ArrayList<>();  // filtered COM trajectory
    public List<Float> trajZ = new ArrayList<>();
}