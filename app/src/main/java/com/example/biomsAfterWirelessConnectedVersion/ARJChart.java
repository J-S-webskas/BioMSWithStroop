package com.example.biomsAfterWirelessConnectedVersion;

import java.util.Date;
import java.util.Map;

public class ARJChart extends BiomechanicalChart {
    // Step width variability during gait
    public ARJChart(int id, String title, Map<Date, Float> historicalData) {
        super(id, "The average sum of ARJ from both knees", "Intensity", historicalData);
    }

    //    @Override public String getTitle() { return "Joint Spasticity"; }
    @Override public String getUnit() { return "Intensity"; }
//    @Override public String getCondition() {
//        float stiffnessScore = getCurrentValue();  // 0-100 scale from Python
//
//        // Match Python's stiffness interpretation
//        if (stiffnessScore < 20) return "Excellent";
//        if (stiffnessScore < 40) return "Good";
//        if (stiffnessScore < 60) return "Fair";
//        if (stiffnessScore < 80) return "Poor";
//        return "Critical";
//    }
//    // TODO: Need doctors to verify that this measurement is correct
//    public String getJointCondition(String jointType) {
//        float asymmetryPercent = getCurrentValue();
//
//        // TODO: not supposed to show asymmetry. Supposed to show joint spasticity level
//        // Joint-specific thresholds based on clinical norms
//        switch (jointType.toLowerCase()) {
//            case "knee":
//                if (asymmetryPercent < 10) return "Normal knee symmetry";
//                if (asymmetryPercent < 25) return "Mild knee asymmetry";
//                if (asymmetryPercent < 40) return "Moderate knee asymmetry";
//                return "Severe knee asymmetry";
//
//            case "ankle":
//                if (asymmetryPercent < 8) return "Normal ankle symmetry";
//                if (asymmetryPercent < 20) return "Mild ankle asymmetry";
//                if (asymmetryPercent < 35) return "Moderate ankle asymmetry";
//                return "Severe ankle asymmetry";
//
//            case "hip":
//                if (asymmetryPercent < 12) return "Normal hip symmetry";
//                if (asymmetryPercent < 30) return "Mild hip asymmetry";
//                if (asymmetryPercent < 45) return "Moderate hip asymmetry";
//                return "Severe hip asymmetry";
//
//            default:
//                return getCondition();
//        }
//    }

    private float getCurrentValue() {
        return 0;
    }

}