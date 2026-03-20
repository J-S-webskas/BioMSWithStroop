package com.example.bioms1;

import java.util.ArrayDeque;

public class BalanceCalculator {

    public static float calculateOffBalance(
            ArrayDeque<Float> hipAngles,
            ArrayDeque<Float> kneeAngles) {

        if (hipAngles == null || kneeAngles == null)
            return 0f;

        float minAngle = Float.MAX_VALUE;
        float maxAngle = Float.MIN_VALUE;

        int size = Math.min(hipAngles.size(), kneeAngles.size());

        Float[] hipArray = hipAngles.toArray(new Float[0]);
        Float[] kneeArray = kneeAngles.toArray(new Float[0]);

        for (int i = 0; i < size; i++) {

            float hip_angle = hipArray[i];
            float knee_angle = kneeArray[i];

            if (hip_angle > 95f || hip_angle < 90f) {

                if (minAngle > hip_angle)
                    minAngle = hip_angle;

                if (maxAngle < hip_angle)
                    maxAngle = hip_angle;

            } else if (knee_angle > 200f || knee_angle < 150f) {

                if (minAngle > knee_angle)
                    minAngle = knee_angle;

                if (maxAngle < knee_angle)
                    maxAngle = knee_angle;
            }
        }

        if (minAngle == Float.MAX_VALUE)
            return 0f; // balanced entire session

        return (maxAngle + minAngle) / 2f;
    }
}