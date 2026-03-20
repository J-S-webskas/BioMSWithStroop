package com.example.bioms1;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MonthAxisValueFormatter extends ValueFormatter {
    private final SimpleDateFormat dateFormat;
    private final Calendar calendar = Calendar.getInstance();
    private final Date currentDate = new Date();

    public MonthAxisValueFormatter() {
        this.dateFormat = new SimpleDateFormat("MMM", Locale.getDefault());
    }

    @Override
    public String getFormattedValue(float value) {

        if (Float.isNaN(value)) return "";

        // value represents months offset from current date
        int monthsAgo = (int) value;

        // Calculate the date for this data point
        calendar.setTime(currentDate);
        calendar.add(Calendar.MONTH, -monthsAgo);

        // For monthly view, show month abbreviation
        return dateFormat.format(calendar.getTime());
    }

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        return getFormattedValue(value);
    }
}