package com.example.bioms1;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.*;

import com.example.bioms1.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.*;
import java.util.*;

public class WalkingSpeedDetailFragment extends Fragment {

    private LineChart chart;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_endurance_detail,
                container,
                false);

        chart = view.findViewById(R.id.areaChart);

        Map<Date, Float> data =
                ChartDataExtractor.getWalkingSpeedData(getContext());

        WalkingSpeedChart speedChart =
                new WalkingSpeedChart(1, "Walking Speed", data, "No Data");

        List<Entry> entries =
                speedChart.getDataForTimeRangeWithGaps(
                        speedChart.baselineData,
                        "max"
                );

        LineDataSet set =
                new LineDataSet(entries, "Walking Speed (m/s)");

        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setDrawFilled(true);

        chart.setData(new LineData(set));
        chart.invalidate();

        return view;
    }
}