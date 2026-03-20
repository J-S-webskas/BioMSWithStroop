package com.example.bioms1;

import android.os.Build;

import java.util.*;

public class AverageRectifiedJerk {

    // Higher AverageRectifiedJerk can mean higher ataxia and higher fall risk.
    // MS patients experience significant issues with gait smoothness and coordination (walking) and that gait issues, such as increased spasticity (which causes jerking movements) and reduced balance, are highly prevalent.
    // angleHistory is just the float array of one joint angle per time. Method calculates that joint angl's average re
    // Higher, more abrupt (jerkier) movements often mean impaired motor control, ataxia, or spasticity
    public static float calculateAverageRectifiedJerk(ArrayDeque<Float> angleHistory, float frameRate) {  // movement smoothness: jerk
        if (angleHistory.size() < 20) {
            return 0f;
        }
        float dt = 1.0f / frameRate;  // seconds per frame
        List<Float> angles = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            angles = new ArrayList<>(angleHistory);
        }

        // 1. Calculate angular velocity (degrees per second)
        List<Float> velocities = new ArrayList<>();
        // Starts at i = 1 (second frame) because need previous frame
        for (int i = 1; i < angles.size(); i++) {
            // angles.get(i) = current frame's angle, angles.get(i-1) = previous frame's angle, (angles.get(i) - angles.get(i-1)) = change in angle (degrees)
            float vel = (angles.get(i) - angles.get(i-1)) / dt;  // change in position / change in time:
            velocities.add(vel);
        }

        // 2. Calculate jerk (change in acceleration)
        List<Float> accelerations = new ArrayList<>();
        for (int i = 1; i < velocities.size(); i++) {
            float a = (velocities.get(i) - velocities.get(i-1)) / dt;  // Acceleration = derivative of velocity or velocity/time
            accelerations.add(a);
        }

        List<Float> jerks = new ArrayList<>();
        for (int i = 1; i < accelerations.size(); i++) {
            float j = (accelerations.get(i) - accelerations.get(i-1)) / dt;  // Jerk  = derivative of acceleration or acceleration/time
            // Take absolute value (rectified) of the jerk over the duration of a movement.
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