package com.example.biomsAfterWirelessConnectedVersion;

import android.os.Build;

import java.util.*;

public class AverageRectifiedJerk {

    // Higher AverageRectifiedJerk can mean higher ataxia and higher fall risk.
    // MS patients experience significant issues with gait smoothness and coordination (walking) and that gait issues, such as increased spasticity (which causes jerking movements) and reduced balance, are highly prevalent.
    // angleHistory is just the float array of one joint angle per time. Method calculates that joint angle's average rectified jerk
    // Higher, more abrupt (jerkier) movements often mean impaired motor control, ataxia, or spasticity
    public static float calculateAverageRectifiedJerk(ArrayDeque<Float> angleHistory, float frameRate) {  // movement smoothness: jerk
        if (angleHistory == null || angleHistory.size() < 20) {
            return 0f;
        }

        // Fix division by zero / uninitialized frameRate
        if (frameRate <= 0) {
            frameRate = 30.0f;
        }

        float dt = 1.0f / frameRate;  // seconds per frame
        List<Float> rawAngles = new ArrayList<>(angleHistory);

        // Apply 2nd order low-pass Butterworth filter (6.0 Hz cutoff) to eliminate landmarker pixel jitter noise
        ButterworthFilter filter = new ButterworthFilter(6.0f, frameRate);
        List<Float> angles = filter.filter(rawAngles);

        // 1. Calculate angular velocity (degrees per second)
        List<Float> velocities = new ArrayList<>();
        for (int i = 1; i < angles.size(); i++) {
            // angles.get(i) = current frame's angle, angles.get(i-1) = previous frame's angle, (angles.get(i) - angles.get(i-1)) = change in angle (degrees)
            float vel = (angles.get(i) - angles.get(i-1)) / dt;
            velocities.add(vel);
        }

        // 2. Calculate acceleration (change in velocity)
        List<Float> accelerations = new ArrayList<>();
        for (int i = 1; i < velocities.size(); i++) {
            float a = (velocities.get(i) - velocities.get(i-1)) / dt;
            accelerations.add(a);
        }

        // 3. Calculate Jerk (change in acceleration)
        List<Float> jerks = new ArrayList<>();
        for (int i = 1; i < accelerations.size(); i++) {
            float j = (accelerations.get(i) - accelerations.get(i-1)) / dt;
            jerks.add(Math.abs(j));
        }

        if (jerks.isEmpty()) {
            return 0f;  // Units: °/s³
        }
        return calculateMean(jerks);
    }

    // Helper: Calculate mean of list
    private static float calculateMean(List<Float> values) {
        float sum = 0;
        for (float val : values) {
            sum += val;
        }
        return sum / values.size();
    }
}