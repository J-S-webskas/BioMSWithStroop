package com.example.bioms1;

import com.github.mikephil.charting.data.Entry;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class BiomechanicalChart {
    protected int id;
    protected String title;
    protected String unit;
    // Data for each TEST TYPE (not each graph)
    public Map<Date, Float> baselineData;     // Date = X, Float = Y
    public Map<Date, Float> dualTaskData;     // Counting while walking data
    public Map<Date, Float> sensoryAtaxiaData; // Eyes closed walking data

    protected String emptyMessage;

    public BiomechanicalChart(int id, String title, String unit) {
        this.id = id;
        this.title = title;
        this.unit = unit;
        this.baselineData = new HashMap<>();
        this.dualTaskData = new HashMap<>();
        this.sensoryAtaxiaData = new HashMap<>();
        this.emptyMessage = "Record walking tests to see data";
    }

    public BiomechanicalChart(int id, String title, String unit,
                              Map<Date, Float> baselineData) {
        this.id = id;
        this.title = title;
        this.unit = unit;
        this.baselineData = baselineData != null ? baselineData : new HashMap<>();
        this.dualTaskData = new HashMap<>();
        this.sensoryAtaxiaData = new HashMap<>();
        this.emptyMessage = null;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getUnit() { return unit; }
    public String getEmptyMessage() { return emptyMessage; }

    // Set data for each test type
    public void setBaselineData(Map<Date, Float> data) {
        this.baselineData = data != null ? data : new HashMap<>();
    }

    public void setDualTaskData(Map<Date, Float> data) {
        this.dualTaskData = data != null ? data : new HashMap<>();
    }

    public void setSensoryAtaxiaData(Map<Date, Float> data) {
        this.sensoryAtaxiaData = data != null ? data : new HashMap<>();
    }

    public void setEmptyMessage(String message) {
        this.emptyMessage = message;
    }

    // Check which test types have data for THIS METRIC
    public boolean hasBaselineData() { return !baselineData.isEmpty(); }
    public boolean hasDualTaskData() { return !dualTaskData.isEmpty(); }
    public boolean hasSensoryAtaxiaData() { return !sensoryAtaxiaData.isEmpty(); }

    public boolean hasAnyData() {
        return hasBaselineData() || hasDualTaskData() || hasSensoryAtaxiaData();
    }

    // Get available test types for this metric
    public List<String> getAvailableTestTypes() {
        List<String> available = new ArrayList<>();
        if (hasBaselineData()) available.add("Baseline");
        if (hasDualTaskData()) available.add("Dual Task");
        if (hasSensoryAtaxiaData()) available.add("Sensory Ataxia");
        return available;
    }

    // Get the latest date from all test types
    public Date getLatestDate() {
        Date latest = null;

        // Check all test types
        for (Map<Date, Float> dataMap : getAllDataMaps()) {
            for (Date date : dataMap.keySet()) {
                if (latest == null || date.after(latest)) {
                    latest = date;
                }
            }
        }

        return latest != null ? latest : new Date();
    }

    public String getFormattedLatestDate() {
//        if (!hasAnyData()) return "No Data";

        Date latest = getLatestDate();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(latest);
    }

    // Get data for a specific test type filtered by time range WITH GAPS
    public List<Entry> getDataForTimeRange(Map<Date, Float> dataMap, String timeRange) {
        if (dataMap == null || dataMap.isEmpty()) {
            return new ArrayList<>();
        }

        // Get all data points sorted by date
        List<Map.Entry<Date, Float>> sortedEntries = new ArrayList<>(dataMap.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());

        // Filter by time range
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        Date startDate;

        switch (timeRange) {
            case "1month":
                cal.setTime(now);
                cal.add(Calendar.MONTH, -1);
                startDate = cal.getTime();
                break;
            case "6month":
                cal.setTime(now);
                cal.add(Calendar.MONTH, -6);
                startDate = cal.getTime();
                break;
            case "1year":
                cal.setTime(now);
                cal.add(Calendar.YEAR, -1);
                startDate = cal.getTime();
                break;
            case "max":
                // For max range, use the earliest date in data
                if (!sortedEntries.isEmpty()) {
                    startDate = sortedEntries.get(0).getKey();
                } else {
                    startDate = now;
                }
                break;
            default:
                cal.setTime(now);
                cal.add(Calendar.MONTH, -1);
                startDate = cal.getTime();
        }

        // Convert to entries with days since start
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<Date, Float> entry : sortedEntries) {
            Date date = entry.getKey();
            // Only include dates within range
            if (!date.before(startDate) && !date.after(now)) {
                float daysSinceStart = getDaysBetween(startDate, date);
                entries.add(new Entry(daysSinceStart, entry.getValue()));
            }
        }

        return entries;
    }

    // NEW: Get data with gaps properly handled for line chart
    public List<Entry> getDataForTimeRangeWithGaps(Map<Date, Float> dataMap, String timeRange) {
        if (dataMap == null || dataMap.isEmpty()) {
            return new ArrayList<>();
        }

        // Get all data points sorted by date (oldest to newest)
        List<Map.Entry<Date, Float>> sortedEntries = new ArrayList<>(dataMap.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());

        // Get cutoff date
        Date startDate = getStartDateForRange(timeRange, sortedEntries);
        Date now = new Date();

        List<Entry> entries = new ArrayList<>();

        // Loop from oldest to newest
        for (Map.Entry<Date, Float> entry : sortedEntries) {
            Date date = entry.getKey();

            if (!date.before(startDate) && !date.after(now)) {
                // Use daysSinceStart (not daysSinceNow)
                float daysSinceStart = getDaysBetween(startDate, date);
                float currentValue = entry.getValue();

                entries.add(new Entry(daysSinceStart, currentValue));
            }
        }

        return entries;
    }

    public Date getStartDateForRange(String timeRange, List<Map.Entry<Date, Float>> sortedEntries) {
        Calendar cal = Calendar.getInstance();
        Date now = new Date();

        switch (timeRange) {
            case "1month":
                cal.setTime(now);
                cal.add(Calendar.MONTH, -1);
                return cal.getTime();
            case "6month":
                cal.setTime(now);
                cal.add(Calendar.MONTH, -6);
                return cal.getTime();
            case "1year":
                cal.setTime(now);
                cal.add(Calendar.YEAR, -1);
                return cal.getTime();
            case "max":
                // Use earliest data point if available, otherwise 1 year back
                if (!sortedEntries.isEmpty()) {
                    return sortedEntries.get(0).getKey();
                } else {
                    cal.setTime(now);
                    cal.add(Calendar.YEAR, -1);
                    return cal.getTime();
                }
            default:
                cal.setTime(now);
                cal.add(Calendar.MONTH, -1);
                return cal.getTime();
        }
    }

    private int getGapThresholdForRange(String timeRange) {
        switch (timeRange) {
            case "1month": return 7;    // 7 days gap for 1 month view
            case "6month": return 30;   // 30 days gap for 6 month view
            case "1year": return 60;    // 60 days gap for 1 year view
            case "max": return 90;      // 90 days gap for max view
            default: return 30;
        }
    }

    // Get average value for stats display
    public float getAverageValue() {
        if (!hasBaselineData()) return 0f;

        return calculateAverage(baselineData.values());
    }

    // Get the date of the last baseline test
    public Date getLastBaselineDate() {
        if (!hasBaselineData()) return null;

        Date latest = null;
        for (Date date : baselineData.keySet()) {
            if (latest == null || date.after(latest)) {
                latest = date;
            }
        }
        return latest;
    }

    // Helper to get all data maps
    private List<Map<Date, Float>> getAllDataMaps() {
        List<Map<Date, Float>> allMaps = new ArrayList<>();
        allMaps.add(baselineData);
        allMaps.add(dualTaskData);
        allMaps.add(sensoryAtaxiaData);
        return allMaps;
    }

    // Helper methods
    private float getDaysBetween(Date startDate, Date endDate) {
        long diff = endDate.getTime() - startDate.getTime();
        return (float) (diff / (1000.0 * 60 * 60 * 24));
    }

    private float calculateAverage(Collection<Float> values) {
        if (values == null || values.isEmpty()) return 0f;
        float sum = 0;
        for (float value : values) {
            sum += value;
        }
        return sum / values.size();
    }
}