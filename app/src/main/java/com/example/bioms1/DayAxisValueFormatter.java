// Update DayAxisValueFormatter to skip labels for gaps
package com.example.bioms1;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DayAxisValueFormatter extends ValueFormatter {
    private final SimpleDateFormat dateFormat;
    private final Calendar calendar = Calendar.getInstance();
    private final Date currentDate = new Date();
    private final Date startDate;  // The cutoff date from getStartDateForRange

    public DayAxisValueFormatter(Date startDate) {
        this.startDate = startDate;
        this.dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
    }

    @Override
    public String getFormattedValue(float value) {
        // If value is NaN or invalid, return empty string
        if (Float.isNaN(value)) return "";

        // value represents days offset from current date
        int daysSinceStart = (int) value;
        // Add days to startDate (go FORWARD)
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.DAY_OF_YEAR, daysSinceStart);

        return dateFormat.format(cal.getTime());
    }

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        return getFormattedValue(value);
    }
}