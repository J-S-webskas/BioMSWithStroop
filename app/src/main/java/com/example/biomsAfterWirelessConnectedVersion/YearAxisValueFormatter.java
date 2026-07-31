package com.example.biomsAfterWirelessConnectedVersion;

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
    private final Date startDate;  // The cutoff date from getStartDateForRange

    public YearAxisValueFormatter(Date startDate) {
        this.startDate = startDate;
        this.dateFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
    }

    @Override
    public String getFormattedValue(float value) {
        // value represents years offset from current date
        if (Float.isNaN(value)) return "";

        int yearsSinceStart = (int) value;
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.YEAR, yearsSinceStart);

        return dateFormat.format(cal.getTime());
    }

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        return getFormattedValue(value);
    }
}