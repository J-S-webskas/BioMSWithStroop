package com.example.bioms1;

import com.github.mikephil.charting.data.Entry;
import java.util.Date;
import java.util.Map;

/**
 * Chart for displaying Walking Speed data
 */
public class WalkingSpeedChart extends BiomechanicalChart {
    private String condition;

    public WalkingSpeedChart(int id, String title, Map<Date, Float> historicalData, String condition) {
        super(id, title, "m/s", historicalData);
        this.condition = condition;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * Get formatted entries for walking speed data
     */
    public Entry[] getFormattedEntries(String timeRange) {
        java.util.List<Entry> entries = getDataForTimeRangeWithGaps(baselineData, timeRange);
        return entries.toArray(new Entry[0]);
    }
}