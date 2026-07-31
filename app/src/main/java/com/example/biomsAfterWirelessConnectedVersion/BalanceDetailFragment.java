package com.example.biomsAfterWirelessConnectedVersion;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.*;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.*;
import java.util.*;

public class BalanceDetailFragment extends Fragment {

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
        chart = view.findViewById(R.id.areaChart);

        Map<Date, Float> data =
                ChartDataExtractor.getBalanceData(getContext());

        BalanceChart balanceChart =
                new BalanceChart(2, "Balance", data);

        List<Entry> entries =
                balanceChart.getDataForTimeRangeSolid(
                        balanceChart.baselineData,
                        "max"
                );

        LineDataSet set =
                new LineDataSet(entries, "Sway Area (mm²)");

        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setDrawFilled(true);
        set.setDrawCircles(true);

        chart.setData(new LineData(set));
        chart.invalidate();

        return view;
    }
}