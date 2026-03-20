package com.example.bioms1;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder>{

    private List<ReportModel> reportList;// Holding all lists

    private OnItemClickListener listener;  // Add this field

    public void updateData(List<ReportModel> newList) {
        this.reportList = newList;
        notifyDataSetChanged();
    }

    // 1. Create the interface
    public interface OnItemClickListener {
        void onItemClick(ReportModel report);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ReportAdapter(List<ReportModel> reportList) {
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.report_item, parent, false);
        return new ReportViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        ReportModel currentReport = reportList.get(position);

        holder.date.setText(currentReport.getDate());
        holder.preview.setText(currentReport.getPreview());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentReport);
            }
        });
    }

    // Returns total number of reports
    @Override
    public int getItemCount() {
        return reportList.size();
    }

    // Inner class that represents each card and handles its interactions
    class ReportViewHolder extends RecyclerView.ViewHolder {

        TextView date;
        TextView preview;
        Context context;

        // Contains references to all views in a card (title, date, buttons, etc.)
        // Handles click events for each card
        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);

            this.context = itemView.getContext();
            this.date = itemView.findViewById(R.id.date);
            this.preview = itemView.findViewById(R.id.preview);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        // Handle card click here
                        ReportModel clickedReport = reportList.get(position);

                        Intent intent = new Intent(context, ReportDetailActivity.class);
                        intent.putExtra("report", clickedReport);  // Pass the whole Report object
                        intent.putExtra("position", position);      // Pass position if needed
                        context.startActivity(intent);
                    }
                }
            });
        }
    }
}