package com.example.bioms1;

import com.github.mikephil.charting.data.Entry;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class BalanceChart extends BiomechanicalChart {
    private String condition;

    public BalanceChart(int id, String title, Map<Date, Float> historicalData) {
        super(id, title, "degrees", historicalData);
        this.condition = "No Data";
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }


    /**
     * Get formatted entries for BASELINE data
     */
    public Entry[] getBaselineEntries(String timeRange) {
        List<Entry> entries = getDataForTimeRangeWithGaps(baselineData, timeRange);
        return entries.toArray(new Entry[0]);
    }

    /**
     * Get formatted entries for DUAL TASK data
     */
    public Entry[] getDualTaskEntries(String timeRange) {
        List<Entry> entries = getDataForTimeRangeWithGaps(dualTaskData, timeRange);
        return entries.toArray(new Entry[0]);
    }

    /**
     * Get formatted entries for SENSORY ATAXIA data
     */
    public Entry[] getSensoryAtaxiaEntries(String timeRange) {
        List<Entry> entries = getDataForTimeRangeWithGaps(sensoryAtaxiaData, timeRange);
        return entries.toArray(new Entry[0]);
    }

    /**
     * Check which test types have data
     */
    public boolean hasDualTaskData() {
        return dualTaskData != null && !dualTaskData.isEmpty();
    }

    public boolean hasSensoryAtaxiaData() {
        return sensoryAtaxiaData != null && !sensoryAtaxiaData.isEmpty();
    }

    // Optional: Keep this for backward compatibility if needed
    @Deprecated
    public Entry[] getFormattedEntries(String timeRange) {
        return getBaselineEntries(timeRange);
    }
}