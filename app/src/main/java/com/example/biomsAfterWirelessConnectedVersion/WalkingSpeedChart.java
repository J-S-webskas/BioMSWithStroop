package com.example.biomsAfterWirelessConnectedVersion;

import com.github.mikephil.charting.data.Entry;
import java.util.Date;
import java.util.Map;

/**
 * Chart for displaying Walking Speed data
 */
public class WalkingSpeedChart extends BiomechanicalChart {
    private String condition;

    public WalkingSpeedChart(int id, String title, Map<Date, Float> historicalData) {
        super(id, title, "m/s", historicalData);
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
        java.util.List<Entry> entries = getDataForTimeRangeSolid(baselineData, timeRange);
        return entries.toArray(new Entry[0]);
    }
}