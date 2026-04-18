package com.example.bioms1;

import com.github.mikephil.charting.data.Entry;
import java.util.Date;
import java.util.Map;

/**
 * Chart for displaying Endurance data (distance traveled)
 */
public class EnduranceChart extends BiomechanicalChart {
    private String condition;

    public EnduranceChart(int id, String title, Map<Date, Float> historicalData) {
        super(id, title, "km", historicalData);
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * Get formatted entries for endurance data
     */
    public Entry[] getFormattedEntries(String timeRange) {
        java.util.List<Entry> entries = getDataForTimeRangeWithGaps(baselineData, timeRange);
        return entries.toArray(new Entry[0]);
    }
}