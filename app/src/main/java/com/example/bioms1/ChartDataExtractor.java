package com.example.bioms1;

import android.content.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartDataExtractor {

    public static Map<Date, Float> getWalkingSpeedData(Context context) {

        Map<Date, Float> data = new HashMap<>();

        List<AssessmentData> assessments =
                DataStorageManager.loadAllAssessments(context);

        for (AssessmentData assessment : assessments) {

            if (assessment.getBaseline() != null &&
                    assessment.getBaseline().getWalkingSpeed() != 0) {

                Float value =
                        assessment.getBaseline()
                                .getWalkingSpeed();

                if (value != null) {
                    data.put(assessment.getTimestamp(), value);
                }
            }
        }

        return data;
    }

    public static Map<Date, Float> getBalanceData(Context context) {

        Map<Date, Float> data = new HashMap<>();

        List<AssessmentData> assessments =
                DataStorageManager.loadAllAssessments(context);

        for (AssessmentData assessment : assessments) {

            if (assessment.getBaseline() != null &&
                    assessment.getBaseline().getBalanceScore() != 0) {

                Float value =
                        assessment.getBaseline()
                                .getBalanceScore();

                if (value != null) {
                    data.put(assessment.getTimestamp(), value);
                }
            }
        }

        return data;
    }

    public static Map<Date, Float> getEnduranceData(Context context) {

        Map<Date, Float> data = new HashMap<>();

        List<AssessmentData> assessments =
                DataStorageManager.loadAllAssessments(context);

        for (AssessmentData assessment : assessments) {

            if (assessment.getBaseline() != null &&
                    assessment.getBaseline().getEnduranceScore() != 0) {

                Float value =
                        assessment.getBaseline()
                                .getEnduranceScore();

                if (value != null) {
                    data.put(assessment.getTimestamp(), value);
                }
            }
        }

        return data;
    }
}