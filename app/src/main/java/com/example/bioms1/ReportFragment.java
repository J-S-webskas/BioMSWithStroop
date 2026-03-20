package com.example.bioms1;

import static android.view.View.GONE;
import static com.example.bioms1.AssessmentData.AssessmentStatus.ALL_COMPLETED;
import static com.example.bioms1.AssessmentData.AssessmentStatus.DUAL_TASK_COMPLETED;
import static com.example.bioms1.AssessmentData.AssessmentStatus.SENSORY_ATAXIA_COMPLETED;

import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ReportFragment extends Fragment {

    private RecyclerView recyclerView;      // The container for cards
    private ReportAdapter adapter;          // Connects data to cards
    private List<ReportModel> reportList = new ArrayList<>();        // Stores all reports
    private Button createNewReport;          // Button to make new reports
    private TextView noData;

    public ReportFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_report, container, false);

        // Initialize the RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        noData = view.findViewById(R.id.noData);

        // Load REAL data reports from storage
        List<ReportModel> reports = loadAllReports();

        // Create sample reports
        Button btnNew = view.findViewById(R.id.createNewReport);

        btnNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Generate new report...
                try {
                    createNewReport();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // Connect adapter to RecyclerView
        adapter = new ReportAdapter(reportList);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(report -> {
            Intent intent = new Intent(requireContext(), ReportDetailActivity.class);

            // Pass the report data
            intent.putExtra("date", report.getDate());  // Convert Date to long
            intent.putExtra("speed", report.getSpeedToday());
            intent.putExtra("speedTrend", report.getSpeedTrend());
            intent.putExtra("arjRight", report.getArjRightKneeToday());
            intent.putExtra("arjRightTrend", report.getArjTrendRight());
            intent.putExtra("arjLeft", report.getArjLeftKneeToday());
            intent.putExtra("arjLeftTrend", report.getArjTrendLeft());
            intent.putExtra("balance", report.getBalanceToday());
            intent.putExtra("balanceTrend", report.getBalanceTrend());
            intent.putExtra("endurance", report.getEnduranceToday());
            intent.putExtra("enduranceTrend", report.getEnduranceTrend());
            intent.putExtra("sensoryAtaxiaARJLeft", report.getSensoryAtaxiaARJLeftToday());
            intent.putExtra("sensoryAtaxiaARJRight", report.getSensoryAtaxiaARJRightToday());
            intent.putExtra("sensoryAtaxiaBalance", report.getSensoryAtaxiaBalanceToday());
            intent.putExtra("sensoryAtaxiaTrend", report.getSensoryAtaxiaTrend());
            intent.putExtra("dualTaskARJLeft", report.getDualTaskARJLeftToday());
            intent.putExtra("dualTaskARJRight", report.getDualTaskARJRightToday());
            intent.putExtra("dualTaskBalance", report.getDualTaskBalanceToday());
            intent.putExtra("dualTaskTrend", report.getDualTaskTrend());

            startActivity(intent);
        });

        return view;
    }

    // todo: get currentAssessment.get sensory ataxia, get dual task, get baseline and such...
    private void createNewReport() throws IOException {

        // 1. Get data from storage
        // today's result: data storage manager loadAllAssessment
        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(requireContext());

        if (allAssessment.isEmpty()) {
            Toast.makeText(requireContext(), "Finish a recording to create data!", Toast.LENGTH_SHORT).show();
            return;
        }
        AssessmentData newest = allAssessment.get(0);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
//        String newestDateStr = sdf.format(newest.getTimestamp());
        String todayStr = sdf.format(new Date());

        float ARJRightKneeToday = 0;
        float ARJLeftKneeToday = 0;
        float balanceToday = 0;
        float speedToday = 0;
        float enduranceToday = 0;

        float SAARJLeftKneeToday = 0;
        float SAARJRightKneeToday = 0;
        float SAbalanceToday = 0;

        float DTARJLeftKneeToday = 0;
        float DTARJRightKneeToday = 0;
        float DTbalanceToday = 0;

        for (int i = 0; i < allAssessment.size(); i++) {
            String currentDate = sdf.format(allAssessment.get(i).getTimestamp());

            AssessmentData assessment = allAssessment.get(i);
            String[] currentParts = currentDate.split("/");
            String[] todayParts = todayStr.split("/");

            // First data from last month
            if (Integer.parseInt(currentParts[0]) == Integer.parseInt(todayParts[0]) - 1 || Integer.parseInt(currentParts[0]) == 12 && Integer.parseInt(todayParts[0]) == 1) {
                switch (assessment.getStatus()) {
                    case ARJ_BALANCE_COMPLETED:
                        // This is a baseline test
                        BaselineAssessment baseline = assessment.getBaseline();
                        ARJRightKneeToday = baseline.getARJ().get("right_knee");
                        ARJLeftKneeToday = baseline.getARJ().get("left_knee");
                        balanceToday = baseline.getBalanceScore();
                        break;

                    case WS_ENDURANCE_COMPLETED:
                        // This is a baseline test
                        BaselineAssessment baselines = assessment.getBaseline();
                        speedToday = baselines.getWalkingSpeed();
                        enduranceToday = baselines.getEnduranceScore();
                        break;

                    case DUAL_TASK_COMPLETED:
                        // This is a dual task test
                        TestComparisons comparisons = assessment.getComparisons();
                        DTARJLeftKneeToday = comparisons.getDualTaskARJLeftKneeChange();
                        DTARJRightKneeToday = comparisons.getDualTaskARJRightKneeChange();
                        DTbalanceToday = comparisons.getDualTaskBalanceChange();
                        break;

                    case SENSORY_ATAXIA_COMPLETED:
                        // This is a sensory ataxia test
                        TestComparisons comparison = assessment.getComparisons();
                        SAARJLeftKneeToday = comparison.getSensoryAtaxiaARJLeftKneeChange();
                        SAARJRightKneeToday = comparison.getSensoryAtaxiaARJRightKneeChange();
                        SAbalanceToday = comparison.getSensoryAtaxiaBalanceChange();
                        break;
                }
            }
        }

        // todo: need to test if this is baseline or test comparisons
        // todo: look at the update status thing in assessmentData
        // Found an assessment today
//        if (newestDateStr.equals(todayStr)) {
//            ARJRightKneeToday = newest.getBaseline().getARJ().get("right_knee");
//            ARJLeftKneeToday = newest.getBaseline().getARJ().get("left_knee");
//            SAARJRightKneeToday = newest.getSensoryAtaxia().getARJ().get("right_knee");
//            SAARJLeftKneeToday = newest.getSensoryAtaxia().getARJ().get("left_knee");
//
//            balanceToday = newest.getBaseline().getBalanceScore();
//            speedToday = newest.getBaseline().getWalkingSpeed();
//            enduranceToday = newest.getBaseline().getEnduranceScore();
//        }

        // 2. Create a new Report object with the data
        ReportModel newReport = new ReportModel();
        newReport.setArjLeftKneeToday(ARJLeftKneeToday);
        newReport.setArjRightKneeToday(ARJRightKneeToday);
        newReport.setBalanceToday(balanceToday);
        newReport.setSpeedToday(speedToday);
        newReport.setEnduranceToday(enduranceToday);
        newReport.setSensoryAtaxiaARJLeftToday(SAARJLeftKneeToday);
        newReport.setSensoryAtaxiaARJRightToday(SAARJRightKneeToday);
        newReport.setSensoryAtaxiaBalanceToday(SAbalanceToday);
        newReport.setDualTaskARJLeftToday(DTARJLeftKneeToday);
        newReport.setDualTaskARJRightToday(DTARJRightKneeToday);
        newReport.setDualTaskBalanceToday(DTbalanceToday);

        // TODO: SAVE DATA TO DATA STORAGE MANAGER
        // TODO: SET PREVIEW
        String preview = String.format(Locale.US,
                "Speed: %.1f m/s | Balance: %.1f | ARJ: %.1f° | Endurance: %.1f s",
                speedToday, balanceToday, ARJRightKneeToday, enduranceToday);
        newReport.setPreview(preview);

        Date date = new Date(); // current date
        SimpleDateFormat sdf1 = new SimpleDateFormat("MMM dd yyyy", Locale.getDefault());
        String formattedDate = sdf1.format(date); // "Oct 24 2025"
        newReport.setDate(formattedDate);

        // Save report
        DataStorageManager.saveReport(requireContext(), newReport);

        // Reload all reports to refresh the list
        loadAllReports();

        // 3. Add it to the list
//        reportList.add(0, newReport);

        // Scroll to the new item
        recyclerView.scrollToPosition(0);

        Toast.makeText(requireContext(), "Report created successfully!", Toast.LENGTH_SHORT).show();
    }

    private List<ReportModel> loadAllReports() {

        // Try to load assessments from storage
        List<ReportModel> reports = DataStorageManager.loadAllReports(requireContext());

        // Update the reportList
        reportList.clear();
        reportList.addAll(reports);

        // Update adapter if it exists
        if (adapter != null) {
            adapter.updateData(reportList);
        }

        // HAS DATA - Create report from real data
        if (!reports.isEmpty()) {
            noData.setVisibility(View.GONE);
        } else {
            noData.setVisibility(View.VISIBLE);
        }

        return reports;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh list when returning to this fragment
        loadAllReports();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}