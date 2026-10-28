package com.example.biomsAfterWirelessConnectedVersion;

import java.io.Serializable;

/**
 * One center-of-mass sample: position (meters, camera space) plus the
 * frame's capture timestamp (milliseconds, SystemClock.uptimeMillis clock -
 * the same clock MediaPipe reports result timestamps on).
 *
 * Timestamps are first-class because the camera does not deliver frames at a
 * perfectly constant rate: pose dropouts and OS scheduling make the raw
 * series non-uniformly sampled. BalanceCalculator resamples onto a uniform
 * grid using these timestamps before any filtering or metric computation.
 */
public class COMSample implements Serializable {

    public final double x;           // mediolateral (m)
    public final double z;           // anteroposterior / depth axis (m)
    public final long timestampMs;   // capture time (uptimeMillis clock)

    public COMSample(double x, double z, long timestampMs) {
        this.x = x;
        this.z = z;
        this.timestampMs = timestampMs;
    }
}
