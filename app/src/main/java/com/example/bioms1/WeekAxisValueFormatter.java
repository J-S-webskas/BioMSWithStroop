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
    private final Date startDate;  // The cutoff date from getStartDateForRange

    public WeekAxisValueFormatter(Date startDate) {
        this.startDate = startDate;
        this.dateFormat = new SimpleDateFormat("MMM", Locale.getDefault());
    }

    @Override
    public String getFormattedValue(float value) {
        // value represents weeks offset from current date
        if (Float.isNaN(value)) return "";

        int weeksSinceStart = (int) value;
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.WEEK_OF_YEAR, weeksSinceStart);

        return dateFormat.format(cal.getTime());
    }

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        return getFormattedValue(value);
    }
}