package com.example.biomsAfterWirelessConnectedVersion;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;


/*
When user clicks on TrendFragment, onCreateView automatically triggers the fragment to load
 */

public class TrendFragment extends Fragment {

    private RecyclerView recyclerView;
    private MaterialButtonToggleGroup toggleGroup;
    private BiomechanicalAdapter adapter;
    private String currentTimeRange = "1month";

    public TrendFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout
        View view = inflater.inflate(R.layout.fragment_trend, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView);
        toggleGroup = view.findViewById(R.id.toggleGroup);

        // Uses requireContext() instead of this (Fragments need context from parent Activity)
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Load REAL data charts from storage
        List<BiomechanicalChart> charts = loadRealDataCharts();

        // Connects adapter to recyclerView
        adapter = new BiomechanicalAdapter(charts, currentTimeRange, getContext());
        recyclerView.setAdapter(adapter);

        // Time range selection
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.oneMonth) {
                    currentTimeRange = "1month";
                } else if (checkedId == R.id.sixMonth) {
                    currentTimeRange = "6month";
                } else if (checkedId == R.id.oneYear) {
                    currentTimeRange = "1year";
                } else if (checkedId == R.id.MAX) {
                    currentTimeRange = "max";
                }

                // Update adapter with new time range
                adapter.setTimeRange(currentTimeRange);
                adapter.notifyDataSetChanged();
            }
        });
        return view;
    }

    private List<BiomechanicalChart> loadRealDataCharts() {
        List<BiomechanicalChart> charts = new ArrayList<>();

        // Try to load assessments from storage
        // TODO: NEED SOME OPTIMIZATION. Only need to loadAllAssessment the first time the user use the app.
        // TODO: Shouldn't load assessment data everytime the fragment refreshes: Like screen rotates

        List<AssessmentData> assessments = DataStorageManager.loadAllAssessments(requireContext());

        if (assessments == null || assessments.isEmpty()) {
            // NO DATA - Create empty charts
            charts.add(createEmptyBalanceChart());
//            charts.add(createEmptySpasticityChart());
            charts.add(createEmptyEnduranceChart());
            charts.add(createEmptyWalkingSpeedChart());
        } else {
            // Create chart from mock data
//            charts.add(createBalanceChartMockData(assessments));
//            charts.add(createARJChartMockData(assessments));
//            charts.add(createWalkingSpeedChartMockData(assessments));
//            charts.add(createEnduranceChartMockData(assessments));
            // HAS DATA - Create charts from real data
            charts.add(createBalanceChartFromData(assessments));
//            charts.add(createARJChartFromData(assessments));
            charts.add(createEnduranceChartFromData(assessments));
            charts.add(createWalkingSpeedChartFromData(assessments));
        }

        return charts;
    }

    private BiomechanicalChart createEnduranceChartMockData(List<AssessmentData> assessments) {
        Map<Date, Float> baselineData = new TreeMap<>(); // Use TreeMap for ordering
        Map<Date, Float> dualTaskData = new TreeMap<>();
        Map<Date, Float> sensoryAtaxiaData = new TreeMap<>();

        Calendar cal = Calendar.getInstance();
        Random rand = new Random();

        // Start from 364 days ago to have 365 days including today
        cal.add(Calendar.DAY_OF_YEAR, -364);

        float startValue = 45.0f;
        float targetValue = 85.0f;

        for (int i = 0; i < 365; i++) {
            Date date = cal.getTime();

            // Progress from 0 to 1 over the year
            float progress = (float) i / 364;

            // Base value improves over time (higher is better for endurance)
            float baseValue = startValue + (targetValue - startValue) * progress;

            // Add random variation
            float variation = 3.0f;
            float randomVar = (rand.nextFloat() * 2 - 1) * variation;
            float value = baseValue + randomVar;

            // Clamp to valid range (0-100)
            value = Math.max(0, Math.min(100, value));

            baselineData.put(date, value);

            // Dual task data (slightly lower)
            float dualValue = value - 5.0f + (rand.nextFloat() * 2 - 1) * variation;
            dualValue = Math.max(0, Math.min(100, dualValue));
            dualTaskData.put(date, dualValue);

            // Sensory ataxia data (even lower)
            float sensoryValue = value - 8.0f + (rand.nextFloat() * 2 - 1) * variation;
            sensoryValue = Math.max(0, Math.min(100, sensoryValue));
            sensoryAtaxiaData.put(date, sensoryValue);

            // Move to next day
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        EnduranceChart chart = new EnduranceChart(3, "Endurance", baselineData);
        chart.setDualTaskData(dualTaskData);
        chart.setSensoryAtaxiaData(sensoryAtaxiaData);

        return chart;

    }

    private BiomechanicalChart createWalkingSpeedChartMockData(List<AssessmentData> assessments) {
        float startValue = 0.8f;       // Starting walking speed (m/s) - slow
        float targetValue = 1.4f;      // Target walking speed - normal range
        float dailyChange = 0.00165f;  // Gradual increase (about 0.6 m/s over 365 days)
        float variation = 0.05f;       // Random variation
        float maxValue = 2.0f;         // Max possible speed (m/s)
        float minValue = 0.0f;         // Min possible speed
        boolean increasingIsBad = false; // For walking speed, higher is better

        Map<Date, Float> baselineData = generateTrendData(startValue, targetValue, dailyChange,
                variation, maxValue, minValue, increasingIsBad);

        // Dual task walking speed is typically lower than baseline
        Map<Date, Float> dualTaskData = generateTrendData(startValue - 0.15f, targetValue - 0.15f,
                dailyChange, variation, maxValue, minValue, increasingIsBad);

        // Sensory ataxia walking speed is also typically lower
        Map<Date, Float> sensoryAtaxiaData = generateTrendData(startValue - 0.2f, targetValue - 0.2f,
                dailyChange, variation, maxValue, minValue, increasingIsBad);

        WalkingSpeedChart chart = new WalkingSpeedChart(4, "Walking Speed", baselineData);

        if (!dualTaskData.isEmpty()) {
            chart.setDualTaskData(dualTaskData);
        }
        if (!sensoryAtaxiaData.isEmpty()) {
            chart.setSensoryAtaxiaData(sensoryAtaxiaData);
        }

        return chart;
    }

//    private BiomechanicalChart createARJChartMockData(List<AssessmentData> assessments) {
//        float startValue = 2.5f;       // Starting ARJ value (high = poor quality)
//        float targetValue = 0.8f;      // Target ARJ value (low = good quality)
//        float dailyChange = 0.0047f;   // Gradual decrease (about 1.7 over 365 days)
//        float variation = 0.15f;       // Random variation
//        float maxValue = 5.0f;         // Max possible ARJ
//        float minValue = 0.2f;         // Min possible ARJ
//        boolean increasingIsBad = true; // For ARJ, higher is worse (decreasing trend is good)
//
//        Map<Date, Float> baselineData = generateTrendData(startValue, targetValue, dailyChange,
//                variation, maxValue, minValue, increasingIsBad);
//
//        // Dual task ARJ is typically higher (worse) than baseline
//        Map<Date, Float> dualTaskData = generateTrendData(startValue + 0.4f, targetValue + 0.3f,
//                dailyChange, variation, maxValue, minValue, increasingIsBad);
//
//        // Sensory ataxia ARJ is typically higher (worse) than baseline
//        Map<Date, Float> sensoryAtaxiaData = generateTrendData(startValue + 0.6f, targetValue + 0.4f,
//                dailyChange, variation, maxValue, minValue, increasingIsBad);
//
////        ARJChart chart = new ARJChart(2, "Average Rectified Jerk (ARJ)", baselineData);
//
//        if (!dualTaskData.isEmpty()) {
//            chart.setDualTaskData(dualTaskData);
//        }
//        if (!sensoryAtaxiaData.isEmpty()) {
//            chart.setSensoryAtaxiaData(sensoryAtaxiaData);
//        }
//
//        return chart;
//    }

    private BiomechanicalChart createBalanceChartMockData(List<AssessmentData> assessments) {
        Map<Date, Float> baselineData = new TreeMap<>();
        Map<Date, Float> dualTaskData = new TreeMap<>();
        Map<Date, Float> sensoryAtaxiaData = new TreeMap<>();

        Calendar cal = Calendar.getInstance();
        Random rand = new Random();

        // Clear time components
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_YEAR, -364);

        // For balance in degrees: LOWER is better
        float startValue = 8.5f;   // Starting balance: 8.5 degrees sway (poor)
        float targetValue = 2.0f;  // Target balance: 2.0 degrees sway (good)

        for (int i = 0; i < 365; i++) {
            Date currentDate = cal.getTime();

            // Progress from 0 to 1 over the year
            float progress = (float) i / 364;

            // Balance IMPROVES (decreases) over time
            float baseValue = startValue - (startValue - targetValue) * progress;

            // Add random variation
            float variation = 0.5f;  // ±0.5 degrees variation
            float randomVar = (rand.nextFloat() * 2 - 1) * variation;
            float value = baseValue + randomVar;

            // Clamp to realistic range (0-15 degrees)
            value = Math.max(0.5f, Math.min(15.0f, value));

            baselineData.put(currentDate, value);

            // Dual task balance is worse (higher degrees)
            float dualValue = value + 1.5f + (rand.nextFloat() * 2 - 1) * variation;
            dualValue = Math.max(0.5f, Math.min(15.0f, dualValue));
            dualTaskData.put(currentDate, dualValue);

            // Sensory ataxia balance is even worse
            float sensoryValue = value + 2.5f + (rand.nextFloat() * 2 - 1) * variation;
            sensoryValue = Math.max(0.5f, Math.min(15.0f, sensoryValue));
            sensoryAtaxiaData.put(currentDate, sensoryValue);

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        BalanceChart chart = new BalanceChart(1, "Balance", baselineData);
        chart.setDualTaskData(dualTaskData);
        chart.setSensoryAtaxiaData(sensoryAtaxiaData);

        return chart;
    }

    // EMPTY CHARTS (when no data exists)
    private BalanceChart createEmptyBalanceChart() {
        Map<Date, Float> emptyData = new HashMap<>();
        BalanceChart chart = new BalanceChart(1, "Balance", emptyData);
        return chart;
    }

//    private ARJChart createEmptySpasticityChart() {
//        Map<Date, Float> emptyData = new HashMap<>();
//        ARJChart chart = new ARJChart(2, "Average Rectified Jerk (ARJ)", emptyData);
//        return chart;
//    }

    private EnduranceChart createEmptyEnduranceChart() {
        Map<Date, Float> emptyData = new HashMap<>();
        EnduranceChart chart = new EnduranceChart(3, "Endurance", emptyData);
        return chart;
    }

    private WalkingSpeedChart createEmptyWalkingSpeedChart() {
        Map<Date, Float> emptyData = new HashMap<>();
        WalkingSpeedChart chart = new WalkingSpeedChart(4, "Walking Speed", emptyData);
        return chart;
    }

    // REAL DATA CHARTS
    private BalanceChart createBalanceChartFromData(List<AssessmentData> assessments) {
        // Extract data from all three test types
        Map<Date, Float> baselineData = new TreeMap<>(); // TreeMap ensures that dates are ordered: important for time series charts.
        Map<Date, Float> dualTaskData = new TreeMap<>();
        Map<Date, Float> sensoryAtaxiaData = new TreeMap<>();

        for (AssessmentData assessment : assessments) {
            Date date = assessment.getTimestamp();

            // Data from the three test types is stored separately to display 3 lines in charts.
            // Check if the test type exists. Check if the score is valid (>0).
            // BASELINE data
            if (assessment.getBaseline() != null && assessment.getBaseline().getBalanceScore() > 0) {
                baselineData.put(date, assessment.getBaseline().getBalanceScore());
            }

            // DUAL TASK data
            if (assessment.getDualTask() != null && assessment.getDualTask().getBalanceScore() > 0) {
                dualTaskData.put(date, assessment.getDualTask().getBalanceScore());
            }

            // SENSORY ATAXIA data
            if (assessment.getSensoryAtaxia() != null && assessment.getSensoryAtaxia().getBalanceScore() > 0) {
                sensoryAtaxiaData.put(date, assessment.getSensoryAtaxia().getBalanceScore());
            }
        }

        // Determine condition based on latest baseline data
        if (!baselineData.isEmpty()) {
            // Get the latest baseline value
            Date latestDate = baselineData.keySet().iterator().next(); // TreeMap is sorted, first is earliest
            for (Date date : baselineData.keySet()) {
                if (date.after(latestDate)) {
                    latestDate = date;
                }
            }
            float latestValue = baselineData.get(latestDate);
        }

        // Create chart with baseline data
        BalanceChart chart = new BalanceChart(1, "Balance", baselineData);

        // Add other test type data
        if (!dualTaskData.isEmpty()) {
            chart.setDualTaskData(dualTaskData);
        }
        if (!sensoryAtaxiaData.isEmpty()) {
            chart.setSensoryAtaxiaData(sensoryAtaxiaData);
        }

        return chart;
    }

//    private ARJChart createARJChartFromData(List<AssessmentData> assessments) {
//        Map<Date, Float> baselineData = new TreeMap<>();
//        Map<Date, Float> dualTaskData = new TreeMap<>();
//        Map<Date, Float> sensoryAtaxiaData = new TreeMap<>();
//
//        for (AssessmentData assessment : assessments) {
//            Date date = assessment.getTimestamp();
//
//            // BASELINE data - calculate average stiffness
//            if (assessment.getBaseline() != null && assessment.getBaseline().getARJ() != null) {
//                float avgARJ = calculateARV(assessment.getBaseline().getARJ());
//                if (avgARJ > 0) {
//                    baselineData.put(date, avgARJ);
//                }
//            }
//
//            // DUAL TASK data
//            if (assessment.getDualTask() != null && assessment.getDualTask().getARJ() != null) {
//                float avgStiffness = calculateARV(assessment.getDualTask().getARJ());
//                if (avgStiffness > 0) {
//                    dualTaskData.put(date, avgStiffness);
//                }
//            }
//
//            // SENSORY ATAXIA data
//            if (assessment.getSensoryAtaxia() != null && assessment.getSensoryAtaxia().getARJ() != null) {
//                float avgStiffness = calculateARV(assessment.getSensoryAtaxia().getARJ());
//                if (avgStiffness > 0) {
//                    sensoryAtaxiaData.put(date, avgStiffness);
//                }
//            }
//        }
//
//        // Determine condition
//        String condition = "No Data";
//        if (!baselineData.isEmpty()) {
//            Date latestDate = baselineData.keySet().iterator().next();
//            for (Date date : baselineData.keySet()) {
//                if (date.after(latestDate)) {
//                    latestDate = date;
//                }
//            }
//            float latestValue = baselineData.get(latestDate);
//            condition = determineSpasticityCondition(latestValue);
//        }
//
//        // id used to distinguish between different charts
//        ARJChart chart = new ARJChart(2, "Average Rectified Jerk", baselineData);
//
//        if (!dualTaskData.isEmpty()) {
//            chart.setDualTaskData(dualTaskData);
//        }
//        if (!sensoryAtaxiaData.isEmpty()) {
//            chart.setSensoryAtaxiaData(sensoryAtaxiaData);
//        }
//
//        return chart;
//    }

    private EnduranceChart createEnduranceChartFromData(List<AssessmentData> assessments) {
        Map<Date, Float> baselineData = new TreeMap<>();
        Map<Date, Float> dualTaskData = new TreeMap<>();
        Map<Date, Float> sensoryAtaxiaData = new TreeMap<>();

        for (AssessmentData assessment : assessments) {
            Date date = assessment.getTimestamp();

            // BASELINE data
            if (assessment.getBaseline() != null && assessment.getBaseline().getEnduranceScore() > 0) {
                baselineData.put(date, assessment.getBaseline().getEnduranceScore());
            }

            // DUAL TASK data
            if (assessment.getDualTask() != null && assessment.getDualTask().getEnduranceScore() > 0) {
                dualTaskData.put(date, assessment.getDualTask().getEnduranceScore());
            }

            // SENSORY ATAXIA data
            if (assessment.getSensoryAtaxia() != null && assessment.getSensoryAtaxia().getEnduranceScore() > 0) {
                sensoryAtaxiaData.put(date, assessment.getSensoryAtaxia().getEnduranceScore());
            }
        }

        // Determine condition
        String condition = "No Data";
        if (!baselineData.isEmpty()) {
            Date latestDate = baselineData.keySet().iterator().next();
            for (Date date : baselineData.keySet()) {
                if (date.after(latestDate)) {
                    latestDate = date;
                }
            }
            float latestValue = baselineData.get(latestDate);
            condition = determineEnduranceCondition(latestValue);
        }

        EnduranceChart chart = new EnduranceChart(3, "Endurance", baselineData);

        if (!dualTaskData.isEmpty()) {
            chart.setDualTaskData(dualTaskData);
        }
        if (!sensoryAtaxiaData.isEmpty()) {
            chart.setSensoryAtaxiaData(sensoryAtaxiaData);
        }

        return chart;
    }

    private WalkingSpeedChart createWalkingSpeedChartFromData(List<AssessmentData> assessments) {
        Map<Date, Float> baselineData = new TreeMap<>();
        Map<Date, Float> dualTaskData = new TreeMap<>();
        Map<Date, Float> sensoryAtaxiaData = new TreeMap<>();

        for (AssessmentData assessment : assessments) {
            Date date = assessment.getTimestamp();

            // BASELINE data
            if (assessment.getBaseline() != null && assessment.getBaseline().getWalkingSpeed() > 0) {
                baselineData.put(date, assessment.getBaseline().getWalkingSpeed());
            }

            // DUAL TASK data
            if (assessment.getDualTask() != null && assessment.getDualTask().getWalkingSpeed() > 0) {
                dualTaskData.put(date, assessment.getDualTask().getWalkingSpeed());
            }

            // SENSORY ATAXIA data
            if (assessment.getSensoryAtaxia() != null && assessment.getSensoryAtaxia().getWalkingSpeed() > 0) {
                sensoryAtaxiaData.put(date, assessment.getSensoryAtaxia().getWalkingSpeed());
            }
        }

        // Determine condition
        String condition = "No Data";
        if (!baselineData.isEmpty()) {
            Date latestDate = baselineData.keySet().iterator().next();
            for (Date date : baselineData.keySet()) {
                if (date.after(latestDate)) {
                    latestDate = date;
                }
            }
            float latestValue = baselineData.get(latestDate);
            condition = determineWalkingSpeedCondition(latestValue);
        }

        WalkingSpeedChart chart = new WalkingSpeedChart(4, "Walking Speed", baselineData);

        if (!dualTaskData.isEmpty()) {
            chart.setDualTaskData(dualTaskData);
        }
        if (!sensoryAtaxiaData.isEmpty()) {
            chart.setSensoryAtaxiaData(sensoryAtaxiaData);
        }

        return chart;
    }

    // caculateAverageRectifiedJerk averages the left and right knee separately, and then in trend fragment, this method averages left + right knee together to create a single ARJ value to plot on the graph
    private float calculateARV(Map<String, Float> stiffnessMap) {
        if (stiffnessMap == null || stiffnessMap.isEmpty()) {
            return 0f;
        }

        float sum = 0f;
        int count = 0;

        for (Float stiffness : stiffnessMap.values()) {
            if (stiffness != null && stiffness > 0) {
                sum += stiffness;
                count++;
            }
        }

        return count > 0 ? sum / count : 0f;
    }

    private static Map<Date, Float> generateTrendData(float startValue, float targetValue,
                                                      float dailyChange, float variation,
                                                      float maxValue, float minValue,
                                                      boolean increasingIsBad) {
        Map<Date, Float> data = new TreeMap<>();
        Random rand = new Random();

        // Start from 365 days ago
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -364); // Go back 364 days to have 365 days including today

        for (int i = 0; i < 365; i++) {
            Date date = cal.getTime();

            float base = startValue + (dailyChange * i);
            if (!increasingIsBad) {
                base = startValue - (dailyChange * i); // Decrease over time if lower is better
            }

            float randomVar = (rand.nextFloat() * 2 - 1) * variation;
            float value = base + randomVar;

            // Clamp to min/max
            value = Math.max(minValue, Math.min(maxValue, value));

            data.put(date, value);

            // Move to next day
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return data;
    }


    // Condition determination methods
    private String determineBalanceCondition(float value) {
        if (value > 85) return "Excellent";
        if (value > 70) return "Good";
        if (value > 50) return "Fair";
        return "Poor";
    }

    private String determineSpasticityCondition(float value) {
        // Lower is better for spasticity
        if (value < 20) return "Excellent";
        if (value < 40) return "Good";
        if (value < 60) return "Fair";
        return "Poor";
    }

    private String determineEnduranceCondition(float value) {
        // Higher is better for endurance (seconds)
        if (value > 180) return "Excellent";
        if (value > 120) return "Good";
        if (value > 60) return "Fair";
        return "Poor";
    }

    private String determineWalkingSpeedCondition(float value) {
        // Higher is better for walking speed (m/s)
        if (value > 1.4) return "Excellent";
        if (value > 1.0) return "Good";
        if (value > 0.6) return "Fair";
        return "Poor";
    }
}