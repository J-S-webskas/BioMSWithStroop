package com.example.bioms1;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class WeekAxisValueFormatter extends ValueFormatter {
    private final SimpleDateFormat dateFormat;
    private final Calendar calendar = Calendar.getInstance();
    private final Date currentDate = new Date();

    public WeekAxisValueFormatter() {
        this.dateFormat = new SimpleDateFormat("MMM", Locale.getDefault());
    }

    @Override
    public String getFormattedValue(float value) {
        // value represents weeks offset from current date
        int weeksAgo = (int) value;

        // Calculate the date for this data point
        calendar.setTime(currentDate);
        calendar.add(Calendar.WEEK_OF_YEAR, -weeksAgo);

        // For week view, show month abbreviation like "Dec", "Nov"
        return dateFormat.format(calendar.getTime());
    }

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        return getFormattedValue(value);
    }
}