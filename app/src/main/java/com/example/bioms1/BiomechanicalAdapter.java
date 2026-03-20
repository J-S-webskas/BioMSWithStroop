package com.example.bioms1;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bioms1.BiomechanicalChart;
import com.example.bioms1.ChartCardViewHolder;
import com.example.bioms1.EnduranceDetailFragment;
import com.example.bioms1.R;

import java.util.List;

public class BiomechanicalAdapter extends RecyclerView.Adapter<ChartCardViewHolder> {
    private List<BiomechanicalChart> charts;
    private Context context;
    private int selectedPosition = RecyclerView.NO_POSITION; // No selection initially
    private OnCardClickListener clickListener;
    private String timeRange;

    public interface OnCardClickListener {
        void onCardClick(int position);
    }
    //     Initializes with chart data and initial time range
    public BiomechanicalAdapter(List<BiomechanicalChart> charts, String timeRange, Context context) {
        this.context = context;
        this.charts = charts;
        this.timeRange = timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    @Override
    public void onBindViewHolder(@NonNull ChartCardViewHolder holder, int position) {
        BiomechanicalChart chart = charts.get(position);

        holder.bind(chart, timeRange, null);  // Pass to chartCardViewHolder bind

        String chartTitle = chart.getTitle();

        // Set click listener ONLY for endurance cards
        if (chartTitle != null && chartTitle.equalsIgnoreCase("Endurance")) {
            holder.cardView.setOnClickListener(v -> {
                // Use holder.getAdapterPosition() instead of position
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    openChartDetail(charts.get(adapterPosition), adapterPosition);
                }
            });
            holder.cardView.setClickable(true);
        } else {
            holder.cardView.setOnClickListener(null);
            holder.cardView.setClickable(false);
        }
    }

    private void openChartDetail(BiomechanicalChart chart, int position) {
        if (!(context instanceof FragmentActivity)) return;

        FragmentActivity activity = (FragmentActivity) context;

        EnduranceDetailFragment fragment = new EnduranceDetailFragment();

        // Create Bundle with arguments
        Bundle args = new Bundle();
        args.putString("chart_title", chart.getTitle());
        fragment.setArguments(args);

        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack("endurance_detail")
                .commit();
    }

    public void setOnItemClickListener(OnCardClickListener listener) {
        this.clickListener = listener;
    }

    public void updateData(List<BiomechanicalChart> newCharts) {
        this.charts = newCharts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChartCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chart_card, parent, false);
        return new ChartCardViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return charts.size();
    }
}