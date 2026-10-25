package com.example.biomsAfterWirelessConnectedVersion;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.*;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.*;
import java.util.*;

public class EnduranceDetailFragment extends Fragment {

    private LineChart chart;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_endurance_detail_not_used,
                container,
                false);

        chart = view.findViewById(R.id.areaChart);

        Map<Date, Float> data =
                ChartDataExtractor.getEnduranceData(getContext());

        EnduranceChart enduranceChart =
                new EnduranceChart(3, "Distance", data);

        List<Entry> entries =
                enduranceChart.getDataForTimeRangeSolid(
                        enduranceChart.baselineData,
                        "max"
                );

        LineDataSet set =
                new LineDataSet(entries, "Distance (m)");

        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setDrawFilled(true);

        chart.setData(new LineData(set));
        chart.invalidate();

        return view;
    }
}