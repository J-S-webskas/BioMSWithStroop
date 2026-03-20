package com.example.bioms1;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class YearAxisValueFormatter extends ValueFormatter {
    private final SimpleDateFormat dateFormat;
    private final Calendar calendar = Calendar.getInstance();
    private final Date currentDate = new Date();

    public YearAxisValueFormatter() {
        this.dateFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
    }

    @Override
    public String getFormattedValue(float value) {
        // value represents years offset from current date
        int yearsAgo = (int) value;

        // Calculate the date for this data point
        calendar.setTime(currentDate);
        calendar.add(Calendar.YEAR, -yearsAgo);

        // For yearly view, show year like "2024", "2023"
        return dateFormat.format(calendar.getTime());
    }

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        return getFormattedValue(value);
    }
}