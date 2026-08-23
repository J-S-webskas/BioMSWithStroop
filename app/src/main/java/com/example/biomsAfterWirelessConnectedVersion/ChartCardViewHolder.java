package com.example.biomsAfterWirelessConnectedVersion;

import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.*;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChartCardViewHolder extends RecyclerView.ViewHolder {

    // Stores references to all views in item_chart_card.xml
    TextView chartTitle;
    TextView chartDate;
    TextView averageValue;
    TextView maxValue;
    TextView conditionIndicator;
    LineChart lineChart;
    CardView cardView;
    TextView emptyStateText;

    public ChartCardViewHolder(@NonNull View itemView) {
        super(itemView);
        chartTitle = itemView.findViewById(R.id.chartTitle);
        averageValue = itemView.findViewById(R.id.averageValue);
        maxValue = itemView.findViewById(R.id.maxValue);
        lineChart = itemView.findViewById(R.id.lineChart);
        cardView = itemView.findViewById(R.id.pelvicSwayCardView);
        emptyStateText = itemView.findViewById(R.id.emptyStateText);

        configureChartSettings(lineChart);
    }

    private void configureChartSettings(LineChart chart) {
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawMarkers(false);
        chart.setHighlightPerDragEnabled(false);
        chart.setHighlightPerTapEnabled(false);
    }

    public void bind(BiomechanicalChart chart, String timeRange, BiomechanicalAdapter.OnCardClickListener listener, Date startDate) {
        // Set basic text
        chartTitle.setText(chart.getTitle());
//        chartDate.setText(chart.getFormattedLatestDate());

        // DEBUG: Check what data exists
        Log.d("CHART_DEBUG", "Chart: " + chart.getTitle());
        Log.d("CHART_DEBUG", "  Baseline data size: " +
                (chart.baselineData != null ? chart.baselineData.size() : 0));
        Log.d("CHART_DEBUG", "  Dual Task data size: " +
                (chart.dualTaskData != null ? chart.dualTaskData.size() : 0));
        Log.d("CHART_DEBUG", "  Sensory Ataxia data size: " +
                (chart.sensoryAtaxiaData != null ? chart.sensoryAtaxiaData.size() : 0));

        // Prepare data for all three test types
        Map<String, List<Entry>> allData = new LinkedHashMap<>();

        if (chart.hasBaselineData()) {
            allData.put("Baseline", chart.getDataForTimeRangeSolid(chart.baselineData, timeRange));
        }
        if (chart.hasDualTaskData()) {
            allData.put("Dual Task", chart.getDataForTimeRangeSolid(chart.dualTaskData, timeRange));
        }
        if (chart.hasSensoryAtaxiaData()) {
            allData.put("Sensory Ataxia", chart.getDataForTimeRangeSolid(chart.sensoryAtaxiaData, timeRange));
        }

        if (!allData.isEmpty()) {
            // HAS DATA - Show chart with available test types
            showChartState(chart, allData, timeRange, startDate);
        } else {
            // NO DATA - Show empty state
            showEmptyState(chart);
        }

        // Set click listener if needed
        if (listener != null) {
            cardView.setOnClickListener(v -> listener.onCardClick(chart.getId()));
        }
    }

    private void showChartState(BiomechanicalChart chart, Map<String, List<Entry>> allData, String timeRange, Date startDate) {
        // Hide empty state
        if (emptyStateText != null) {
            emptyStateText.setVisibility(View.GONE);
        }

        // Show chart
        lineChart.setVisibility(View.VISIBLE);

        // Clear any previous data
        lineChart.clear();

        // Setup chart with available test types AND GAPS
        setupMultiTestChartWithGaps(lineChart, allData, chart.getTitle(), chart.getUnit(), timeRange, startDate);

        // Update stats
        updateChartStats(chart, allData);
    }

    private void showEmptyState(BiomechanicalChart chart) {
        // Hide chart
        lineChart.setVisibility(View.GONE);

        // Clear any existing data
        lineChart.clear();

        // Set "No Data" message on chart
        lineChart.setNoDataText("No test data available");
        lineChart.setNoDataTextColor(Color.GRAY);

        // Show empty message (Probably not needed)
//        if (emptyStateText != null) {
//            emptyStateText.setVisibility(View.VISIBLE);
//            String message = chart.getEmptyMessage();
//            if (message == null || message.isEmpty()) {
//                message = "Start a recording test to see graph!";
//            }
//            emptyStateText.setText(message);
//        }

        // Update stats to show no data
        averageValue.setText("No data available");
        maxValue.setText("Record tests to see trends");

        // Invalidate to refresh
        lineChart.invalidate();
    }

    private void setupMultiTestChartWithGaps(LineChart chart, Map<String, List<Entry>> dataSeries,
                                             String title, String unit, String timeRange, Date startDate) {
        // Create datasets
        List<ILineDataSet> dataSets = new ArrayList<>();

        // Colors for each test type
        int[] lineColors = {
                Color.parseColor("#2196F3"),  // Blue - Baseline
                Color.parseColor("#FF9800"),  // Orange - Dual Task
                Color.parseColor("#F44336")   // Red - Sensory Ataxia
        };

        // Create datasets for each available test type
        int index = 0;
        for (Map.Entry<String, List<Entry>> series : dataSeries.entrySet()) {
            List<Entry> entries = series.getValue();

            if (entries != null && !entries.isEmpty()) {
                String testType = series.getKey();
                int colorIndex = getColorIndexForTestType(testType);

                LineDataSet dataSet = createTestTypeDataSetWithGaps(
                        entries,
                        testType,
                        lineColors[colorIndex],
                        testType.equals("Baseline") ? 2.5f : 2f // Thicker line for baseline
                );
                dataSets.add(dataSet);
                index++;
            }
        }

        if (!dataSets.isEmpty()) {
            LineData lineData = new LineData(dataSets);
            chart.setData(lineData);

            // Configure chart appearance
            configureChartAppearance(chart, dataSeries.size() > 1, unit, dataSeries, timeRange, startDate);
        }
    }

    private LineDataSet createTestTypeDataSetWithGaps(List<Entry> entries, String label,
                                                      int lineColor, float lineWidth) {
        LineDataSet dataSet = new LineDataSet(entries, label);

        // Line styling
        dataSet.setColor(lineColor);
        dataSet.setLineWidth(lineWidth);

        // Disable circles and values for cleaner look with gaps
        dataSet.setDrawCircles(false);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawValues(false);

        // Make line dashed to visually indicate gaps
        dataSet.enableDashedLine(10f, 5f, 0f);

        // Linear mode works better with gaps
        dataSet.setMode(LineDataSet.Mode.LINEAR);

        // Don't fill area with gaps
        dataSet.setDrawFilled(false);

        return dataSet;
    }

    private void configureChartAppearance(LineChart chart, boolean showLegend,
                                          String unit, Map<String, List<Entry>> dataSeries,
                                          String timeRange, Date startDate) {
        chart.getDescription().setEnabled(false);

        // Configure legend
        Legend legend = chart.getLegend();
        legend.setEnabled(showLegend);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setYOffset(10f);
        legend.setTextSize(10f);
        legend.setForm(Legend.LegendForm.LINE);

        // Configure X axis
        XAxis xAxis = chart.getXAxis();
        setupXAxis(xAxis, timeRange, startDate);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#22000000"));
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.DKGRAY);

        // Configure Y axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#22000000"));
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setTextSize(10f);
        leftAxis.setTextColor(Color.DKGRAY);

        // Set Y axis range
        configureYAxisRange(leftAxis, dataSeries, unit);

        // Hide right axis
        chart.getAxisRight().setEnabled(false);

        // Add animation
        chart.animateX(800);
    }

    private void setupXAxis(XAxis xAxis, String timeRange, Date startDate) {
        switch (timeRange) {
            case "1month":
                xAxis.setValueFormatter(new DayAxisValueFormatter(startDate));
                xAxis.setLabelCount(6, true);
                break;

            case "6month":
                // Use MonthAxisValueFormatter with 6 labels for the 6 months
                xAxis.setValueFormatter(new MonthAxisValueFormatter(startDate));
                xAxis.setLabelCount(6, true);
                break;

            case "1year":
                xAxis.setValueFormatter(new MonthAxisValueFormatter(startDate));
                xAxis.setLabelCount(12, true);
                break;

            case "max":
                xAxis.setValueFormatter(new YearAxisValueFormatter(startDate));
                xAxis.setLabelCount(6, true);
                break;
        }
    }

    private void configureYAxisRange(YAxis yAxis, Map<String, List<Entry>> dataSeries, String unit) {
        // Find min and max across all test types, ignoring NaN values
        float minValue = Float.MAX_VALUE;
        float maxValue = Float.MIN_VALUE;

        for (List<Entry> entries : dataSeries.values()) {
            for (Entry entry : entries) {
                float value = entry.getY();
                // Skip NaN values
                if (!Float.isNaN(value)) {
                    if (value < minValue) minValue = value;
                    if (value > maxValue) maxValue = value;
                }
            }
        }

        // Set reasonable defaults if no valid data
        if (minValue == Float.MAX_VALUE || maxValue == Float.MIN_VALUE) {
            minValue = 0;
            switch (unit.toLowerCase()) {
                case "%": maxValue = 100; break;
                case "seconds":
                case "s": maxValue = 200; break;
                case "m/s": maxValue = 2.0f; break;
                case "score": maxValue = 100; break;
                default: maxValue = 100;
            }
        } else {
            // Add padding
            float range = maxValue - minValue;
            float padding = range * 0.1f;

            minValue = Math.max(0, minValue - padding);
            maxValue += padding;
        }

        yAxis.setAxisMinimum(minValue);
        yAxis.setAxisMaximum(maxValue);
        yAxis.setLabelCount(6, true);

        // Format based on unit
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (Float.isNaN(value)) return "";

                switch (unit.toLowerCase()) {
                    case "%": return String.format("%.0f%%", value);
                    case "seconds":
                    case "s": return String.format("%.0f", value);
                    case "m/s": return String.format("%.2f", value);
                    case "score": return String.format("%.0f", value);
                    default: return String.format("%.1f", value);
                }
            }
        });
    }

    private void updateChartStats(BiomechanicalChart chart, Map<String, List<Entry>> allData) {
        StringBuilder stats = new StringBuilder();

        // Count number of tests for each type
        if (chart.hasBaselineData()) {
            int baselineCount = chart.baselineData.size();
            stats.append(baselineCount).append(" baseline");
        }

        if (chart.hasDualTaskData()) {
            if (stats.length() > 0) stats.append(", ");
            int dualTaskCount = chart.dualTaskData.size();
            stats.append(dualTaskCount).append(" dual");
        }

        if (chart.hasSensoryAtaxiaData()) {
            if (stats.length() > 0) stats.append(", ");
            int sensoryCount = chart.sensoryAtaxiaData.size();
            stats.append(sensoryCount).append(" sensory");
        }

        if (stats.length() > 0) {
            averageValue.setText("Tests: " + stats.toString());
        } else {
            averageValue.setText("No tests recorded");
        }

        // Show date range of data
        Date earliest = getEarliestDate(chart);
        Date latest = chart.getLatestDate();

        if (earliest != null && latest != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
            maxValue.setText(sdf.format(earliest) + " - " + sdf.format(latest));
        } else {
            maxValue.setText("Record tests to see timeline");
        }
    }

    private Date getEarliestDate(BiomechanicalChart chart) {
        Date earliest = null;

        for (Map<Date, Float> dataMap : Arrays.asList(
                chart.baselineData,
                chart.dualTaskData,
                chart.sensoryAtaxiaData)) {

            for (Date date : dataMap.keySet()) {
                if (earliest == null || date.before(earliest)) {
                    earliest = date;
                }
            }
        }

        return earliest;
    }

    private int getColorIndexForTestType(String testType) {
        switch (testType.toLowerCase()) {
            case "baseline": return 0;
            case "dual task": return 1;
            case "sensory ataxia": return 2;
            default: return 0;
        }
    }

    private void setConditionIndicator(TextView indicator, String condition) {
        switch (condition.toLowerCase()) {
            case "excellent":
                indicator.setBackgroundResource(R.drawable.bg_normal_indicator);
                indicator.setTextColor(Color.WHITE);
                break;
            case "good":
                indicator.setBackgroundResource(R.drawable.bg_elevated_indicator);
                indicator.setTextColor(Color.WHITE);
                break;
            case "fair":
                indicator.setBackgroundResource(R.drawable.bg_critical_indicator);
                indicator.setTextColor(Color.WHITE);
                break;
            case "poor":
                indicator.setBackgroundResource(R.drawable.bg_critical_indicator);
                indicator.setTextColor(Color.WHITE);
                break;
            case "no data":
                indicator.setBackgroundResource(R.drawable.bg_gray_indicator);
                indicator.setTextColor(Color.DKGRAY);
                break;
            default:
                indicator.setBackgroundResource(R.drawable.bg_gray_indicator);
                indicator.setTextColor(Color.DKGRAY);
                break;
        }
        indicator.setText(condition.toUpperCase());
    }
}