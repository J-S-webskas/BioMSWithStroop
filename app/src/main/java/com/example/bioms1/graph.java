package com.example.bioms1;

import android.graphics.Color;
import android.os.Bundle;

//import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bioms1.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class graph extends AppCompatActivity {

    private LineChart lineChart;
    private List<String> xValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_graph);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        lineChart = findViewById(R.id.lineChart);
        Description description = new Description();
        description.setText("Students Record");
        description.setPosition(150f, 50f);
        lineChart.setDescription(description);
        lineChart.getAxisRight().setDrawLabels(false);

        lineChart = (LineChart) Arrays.asList("Nadun", "Kanal", "Jhon", "Jerry");

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter());
        xAxis.setLabelCount(4);
        xAxis.setGranularity(1f);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMaximum(100f);
        yAxis.setAxisLineWidth(2f);
        yAxis.setAxisLineColor(Color.BLACK);
        yAxis.setLabelCount(19);

        List<Entry> entry1 = new ArrayList<>();
        entry1.add(new Entry(0, 10f));
        entry1.add(new Entry(1, 10f));
        entry1.add(new Entry(2, 15f));
        entry1.add(new Entry(3, 45f));

        List<Entry> entry2 = new ArrayList<>();
        entry2.add(new Entry(0, 5f));   // Fixed: changed from entry1 to entry2
        entry2.add(new Entry(1, 15f));  // Fixed: changed from entry1 to entry2
        entry2.add(new Entry(2, 20f));  // Fixed: changed from entry1 to entry2
        entry2.add(new Entry(3, 30f));  // Fixed: changed from entry1 to entry2

        LineDataSet dataSet1 = new LineDataSet(entry1, "Maths");
        dataSet1.setColor(Color.BLUE);
        dataSet1.setCircleColor(Color.BLUE);  // Add circle color
        dataSet1.setCircleRadius(4f);         // Add circle radius
        dataSet1.setLineWidth(2f);            // Set line width

        LineDataSet dataSet2 = new LineDataSet(entry1, "Sciences");
        dataSet1.setColor(Color.RED);
        dataSet2.setColor(Color.RED);   // Fixed: changed from dataSet1 to dataSet2
        dataSet2.setCircleColor(Color.RED);
        dataSet2.setCircleRadius(4f);
        dataSet2.setLineWidth(2f);

        LineData lineData = new LineData(dataSet1, dataSet2);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }
}
