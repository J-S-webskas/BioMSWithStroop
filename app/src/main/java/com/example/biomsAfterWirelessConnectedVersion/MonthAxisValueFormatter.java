package com.example.biomsAfterWirelessConnectedVersion;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MonthAxisValueFormatter extends ValueFormatter {
    private final SimpleDateFormat dateFormat;
    private final Date startDate;

    public MonthAxisValueFormatter(Date startDate) {
        this.startDate = startDate;
        this.dateFormat = new SimpleDateFormat("MMM", Locale.getDefault());
    }

    @Override
    public String getFormattedValue(float value) {
        if (Float.isNaN(value)) return "";

        int daysSinceStart = (int) value;
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