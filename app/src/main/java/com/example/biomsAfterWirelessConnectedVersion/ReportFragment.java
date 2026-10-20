package com.example.biomsAfterWirelessConnectedVersion;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        Button btnDeleteReport = view.findViewById(R.id.btn_delete_report);
        btnDeleteReport.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            showDeleteReportDialog();
        });

        noData = view.findViewById(R.id.noData);

        // Load REAL data reports from storage
        List<ReportModel> reports = loadAllReports();

        // Create sample reports
        Button btnNew = view.findViewById(R.id.createNewReport);

        btnNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Generate new report...
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                showDatePickerForReport();
            }
        });

        // Connect adapter to RecyclerView
        adapter = new ReportAdapter(reportList);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(report -> {
            Intent intent = new Intent(requireContext(), ReportDetailActivity.class);

            // Pass the report data
            intent.putExtra("date", report.getDate());
            intent.putExtra("speed", report.getSpeedToday());
            intent.putExtra("arjRight", report.getArjRightKneeToday());
            intent.putExtra("arjLeft", report.getArjLeftKneeToday());
            intent.putExtra("balance", report.getBalanceToday());
            intent.putExtra("swayVelocity", report.getSwayVelocityToday());
            intent.putExtra("swayPathLength", report.getSwayPathLengthToday());
            intent.putExtra("swayRmsAP", report.getSwayRmsAPToday());
            intent.putExtra("swayRmsML", report.getSwayRmsMLToday());
            intent.putExtra("endurance", report.getEnduranceToday());
            intent.putExtra("sensoryAtaxiaARJLeft", report.getSensoryAtaxiaARJLeftToday());
            intent.putExtra("sensoryAtaxiaARJRight", report.getSensoryAtaxiaARJRightToday());
            intent.putExtra("sensoryAtaxiaBalance", report.getSensoryAtaxiaBalanceToday());
            intent.putExtra("sensoryAtaxiaSwayVelocity", report.getSensoryAtaxiaSwayVelocityToday());
            intent.putExtra("sensoryAtaxiaSwayPathLength", report.getSensoryAtaxiaSwayPathLengthToday());
            intent.putExtra("sensoryAtaxiaSwayRmsAP", report.getSensoryAtaxiaSwayRmsAPToday());
            intent.putExtra("sensoryAtaxiaSwayRmsML", report.getSensoryAtaxiaSwayRmsMLToday());
            intent.putExtra("dualTaskARJLeft", report.getDualTaskARJLeftToday());
            intent.putExtra("dualTaskARJRight", report.getDualTaskARJRightToday());
            intent.putExtra("dualTaskBalance", report.getDualTaskBalanceToday());
            intent.putExtra("dualTaskSwayVelocity", report.getDualTaskSwayVelocityToday());
            intent.putExtra("dualTaskSwayPathLength", report.getDualTaskSwayPathLengthToday());
            intent.putExtra("dualTaskSwayRmsAP", report.getDualTaskSwayRmsAPToday());
            intent.putExtra("dualTaskSwayRmsML", report.getDualTaskSwayRmsMLToday());
            // DTC
            intent.putExtra("dualTaskARJLeftDTC", report.getDualTaskDTCARJLeft());
            intent.putExtra("dualTaskARJRightDTC", report.getDualTaskDTCARJRight());
            intent.putExtra("dualTaskBalanceDTC", report.getDualTaskDTCBalance());
            intent.putExtra("dualTaskSwayVelocityDTC", report.getDualTaskDTCSwayVelocity());
            intent.putExtra("dualTaskSwayPathLengthDTC", report.getDualTaskDTCSwayPathLength());
            intent.putExtra("sensoryAtaxiaARJLeftDTC", report.getSensoryAtaxiaDTCARJLeft());
            intent.putExtra("sensoryAtaxiaARJRightDTC", report.getSensoryAtaxiaDTCARJRight());
            intent.putExtra("sensoryAtaxiaBalanceDTC", report.getSensoryAtaxiaDTCBalance());
            intent.putExtra("sensoryAtaxiaSwayVelocityDTC", report.getSensoryAtaxiaDTCSwayVelocity());
            intent.putExtra("sensoryAtaxiaSwayPathLengthDTC", report.getSensoryAtaxiaDTCSwayPathLength());
            // SD & CV
            intent.putExtra("ARJLeftSD", report.getARJLeftSD());
            intent.putExtra("ARJRightSD", report.getARJRightSD());
            intent.putExtra("BalanceSD", report.getBalanceSD());
            intent.putExtra("EnduranceSD", report.getEnduranceSD());
            intent.putExtra("WalkingSD", report.getSpeedSD());
            intent.putExtra("ARJLeftCV", report.getARJLeftCV());
            intent.putExtra("ARJRightCV", report.getARJRightCV());
            intent.putExtra("BalanceCV", report.getBalanceCV());
            intent.putExtra("EnduranceCV", report.getEnduranceCV());
            intent.putExtra("WalkingCV", report.getSpeedCV());

            // Trials
            intent.putExtra("reportModel", report);

            startActivity(intent);
        });

        return view;
    }

    private void createReportForDate(Date targetDate) throws IOException {
        String targetDateStr = new SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(targetDate);

        // Get data from storage
        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(requireContext());

        if (allAssessment.isEmpty()) {
            Toast.makeText(requireContext(), "No assessment data available to create report!", Toast.LENGTH_SHORT).show();
            return;
        }

        // DataStorageManager loads assessments sorted newest first. Sort chronologically (oldest first) so T1 is earliest trial.
        List<AssessmentData> chronologicalAssessments = new ArrayList<>(allAssessment);
        java.util.Collections.sort(chronologicalAssessments, (a1, a2) ->
                a1.getTimestamp().compareTo(a2.getTimestamp()));

        // Filter assessments for the selected date
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        String targetDateString = sdf.format(targetDate);

        // Track most recent values per metric for the selected date
//        Float bestARJLeft = null;
//        Float bestARJRight = null;
        Float bestBalance = null;
        Float bestWalkingSpeed = null;
        Float bestEndurance = null;
        Float bestSwayVelocity = null;
        Float bestSwayPathLength = null;
        Float bestSwayRmsAP = null;
        Float bestSwayRmsML = null;
//        Float bestDTARJLeft = null;
//        Float bestDTARJRight = null;
        Float bestDTBalance = null;
        Float bestDTSwayVelocity = null;
        Float bestDTSwayPathLength = null;
        Float bestDTSwayRmsAP = null;
        Float bestDTSwayRmsML = null;
//        Float bestSAARJLeft = null;
//        Float bestSAARJRight = null;
        Float bestSABalance = null;
        Float bestSASwayVelocity = null;
        Float bestSASwayPathLength = null;
        Float bestSASwayRmsAP = null;
        Float bestSASwayRmsML = null;

        long latestTimestamp = 0;
        long latestDTTimestamp = 0;
        long latestSATimestamp = 0;

        // Collections for SD/CV calculations - ALL data (not just selected date)
//        ArrayList<Float> allARJLeftValues = new ArrayList<>();
//        ArrayList<Float> allARJRightValues = new ArrayList<>();
        ArrayList<Float> allBalanceValues = new ArrayList<>();
        ArrayList<Float> allSpeedValues = new ArrayList<>();
        ArrayList<Float> allEnduranceValues = new ArrayList<>();

        // Variables for selected date data
//        float ARJRightKneeToday = 0;
//        float ARJLeftKneeToday = 0;
        float balanceToday = 0;
        float speedToday = 0;
        float enduranceToday = 0;
        float swayVelocityToday = 0;
        float swayPathLengthToday = 0;
        float swayRmsAPToday = 0;
        float swayRmsMLToday = 0;
//        float SAARJLeftKneeToday = 0;
//        float SAARJRightKneeToday = 0;
        float SAbalanceToday = 0;
        float SASwayVelocityToday = 0;
        float SASwayPathLengthToday = 0;
        float SASwayRmsAPToday = 0;
        float SASwayRmsMLToday = 0;
//        float DTARJLeftKneeToday = 0;
//        float DTARJRightKneeToday = 0;
        float DTbalanceToday = 0;
        float DTswayVelocityToday = 0;
        float DTswayPathLengthToday = 0;
        float DTswayRmsAPToday = 0;
        float DTswayRmsMLToday = 0;
//        float DTARJLeftKneeDTC = 0;
//        float DTARJRightKneeDTC = 0;
        float DTBalanceDTC = 0;
        float DTVelocityDTC = 0;
        float DTPathDTC = 0;
//        float SAARJLeftKneeDTC = 0;
//        float SAARJRightKneeDTC = 0;
        float SABalanceDTC = 0;
        float SAVelocityDTC = 0;
        float SAPathDTC = 0;

        boolean hasDataForDate = false;

        ArrayList<Float> balanceTrials = new ArrayList<>();
        ArrayList<Float> speedTrials = new ArrayList<>();
        ArrayList<Float> enduranceTrials = new ArrayList<>();
        ArrayList<Float> dtBalanceTrials = new ArrayList<>();
        ArrayList<ReportModel.StroopTrialData> dtStroopTrials = new ArrayList<>();
        ArrayList<Float> dtDTCTrials = new ArrayList<>();
        ArrayList<Float> saBalanceTrials = new ArrayList<>();
        ArrayList<Float> saDTCTrials = new ArrayList<>();
        // Sway metric family trials (chronological, same order as balanceTrials)
        ArrayList<Float> swayPathLengthTrials = new ArrayList<>();
        ArrayList<Float> swayVelocityTrials = new ArrayList<>();
        ArrayList<Float> swayRmsAPTrials = new ArrayList<>();
        ArrayList<Float> swayRmsMLTrials = new ArrayList<>();
        ArrayList<Float> dtSwayPathLengthTrials = new ArrayList<>();
        ArrayList<Float> dtSwayVelocityTrials = new ArrayList<>();
        ArrayList<Float> dtSwayRmsAPTrials = new ArrayList<>();
        ArrayList<Float> dtSwayRmsMLTrials = new ArrayList<>();
        ArrayList<Float> dtVelocityDTCTrials = new ArrayList<>();
        ArrayList<Float> dtPathLengthDTCTrials = new ArrayList<>();
        ArrayList<Float> saSwayPathLengthTrials = new ArrayList<>();
        ArrayList<Float> saSwayVelocityTrials = new ArrayList<>();
        ArrayList<Float> saSwayRmsAPTrials = new ArrayList<>();
        ArrayList<Float> saSwayRmsMLTrials = new ArrayList<>();
        ArrayList<Float> saVelocityDTCTrials = new ArrayList<>();
        ArrayList<Float> saPathLengthDTCTrials = new ArrayList<>();

        for (AssessmentData assessment : chronologicalAssessments) {
            String currentDate = sdf.format(assessment.getTimestamp());
            long currentTimestamp = assessment.getTimestamp().getTime();

            // Process only data from the selected date
            if (!currentDate.equals(targetDateString)) continue;

            hasDataForDate = true;

            float balance = assessment.getBaseline().getBalanceScore();
            if (assessment.getSwayAreaAndBalanceTestDate() != null || balance > 0) {
                if (balance > 0) {
                    balanceTrials.add(balance);
                    allBalanceValues.add(balance);
                }
                if (assessment.getBaseline().getSwayPathLength() > 0) {
                    swayPathLengthTrials.add(assessment.getBaseline().getSwayPathLength());
                    swayVelocityTrials.add(assessment.getBaseline().getSwayMeanVelocity());
                    swayRmsAPTrials.add(assessment.getBaseline().getSwayRmsAP());
                    swayRmsMLTrials.add(assessment.getBaseline().getSwayRmsML());
                }
                if (bestBalance == null || currentTimestamp > latestTimestamp) {
                    bestBalance = balance;
                }
                if (bestSwayVelocity == null || currentTimestamp > latestTimestamp) {
                    bestSwayVelocity = assessment.getBaseline().getSwayMeanVelocity();
                    bestSwayPathLength = assessment.getBaseline().getSwayPathLength();
                    bestSwayRmsAP = assessment.getBaseline().getSwayRmsAP();
                    bestSwayRmsML = assessment.getBaseline().getSwayRmsML();
                }
            }

            float walkingSpeed = assessment.getBaseline().getWalkingSpeed();
            if (assessment.getWSAndEnduranceTestDate() != null || walkingSpeed > 0) {
                if (walkingSpeed > 0) {
                    speedTrials.add(walkingSpeed);
                    allSpeedValues.add(walkingSpeed);
                }
                if (bestWalkingSpeed == null || currentTimestamp > latestTimestamp) {
                    bestWalkingSpeed = walkingSpeed;
                }
            }

            float endurance = assessment.getBaseline().getEnduranceScore();
            if (assessment.getWSAndEnduranceTestDate() != null || endurance > 0) {
                if (endurance > 0) {
                    enduranceTrials.add(endurance);
                    allEnduranceValues.add(endurance);
                }
                if (bestEndurance == null || currentTimestamp > latestTimestamp) {
                    bestEndurance = endurance;
                }
            }

            if (currentTimestamp > latestTimestamp) {
                latestTimestamp = currentTimestamp;
            }

            // Dual Task data
            DualTaskAssessment dualTask = assessment.getDualTask();
            if (dualTask != null) {
                if (dualTask.getStroopTotalTrials() > 0) {
                    dtStroopTrials.add(new ReportModel.StroopTrialData(
                            dualTask.getStroopTotalTrials(),
                            dualTask.getStroopCorrectCount(),
                            dualTask.getStroopAccuracyPercent(),
                            dualTask.getStroopAvgReactionMs(),
                            dualTask.getStroopAnswers()));
                }
                float dtBalance = dualTask.getBalanceScore();
                if (assessment.getDualTaskTestDate() != null || dtBalance > 0) {
                    if (dtBalance > 0) dtBalanceTrials.add(dtBalance);
                    if (dualTask.getSwayPathLength() > 0) {
                        dtSwayPathLengthTrials.add(dualTask.getSwayPathLength());
                        dtSwayVelocityTrials.add(dualTask.getSwayMeanVelocity());
                        dtSwayRmsAPTrials.add(dualTask.getSwayRmsAP());
                        dtSwayRmsMLTrials.add(dualTask.getSwayRmsML());
                    }
                    if (bestDTBalance == null || currentTimestamp > latestDTTimestamp) {
                        bestDTBalance = dtBalance;
                    }
                    if (bestDTSwayVelocity == null || currentTimestamp > latestDTTimestamp) {
                        bestDTSwayVelocity = dualTask.getSwayMeanVelocity();
                        bestDTSwayPathLength = dualTask.getSwayPathLength();
                        bestDTSwayRmsAP = dualTask.getSwayRmsAP();
                        bestDTSwayRmsML = dualTask.getSwayRmsML();
                    }
                }
                if (currentTimestamp > latestDTTimestamp) {
                    latestDTTimestamp = currentTimestamp;
                }
            }

            // Sensory Ataxia data
            SensoryAtaxiaAssessment sensory = assessment.getSensoryAtaxia();
            if (sensory != null) {
                float saBalance = sensory.getBalanceScore();
                if (assessment.getSensoryAtaxiaTestDate() != null || saBalance > 0) {
                    if (saBalance > 0) saBalanceTrials.add(saBalance);
                    if (sensory.getSwayPathLength() > 0) {
                        saSwayPathLengthTrials.add(sensory.getSwayPathLength());
                        saSwayVelocityTrials.add(sensory.getSwayMeanVelocity());
                        saSwayRmsAPTrials.add(sensory.getSwayRmsAP());
                        saSwayRmsMLTrials.add(sensory.getSwayRmsML());
                    }
                    if (bestSABalance == null || currentTimestamp > latestSATimestamp) {
                        bestSABalance = saBalance;
                    }
                    if (bestSASwayVelocity == null || currentTimestamp > latestSATimestamp) {
                        bestSASwayVelocity = sensory.getSwayMeanVelocity();
                        bestSASwayPathLength = sensory.getSwayPathLength();
                        bestSASwayRmsAP = sensory.getSwayRmsAP();
                        bestSASwayRmsML = sensory.getSwayRmsML();
                    }
                }
                if (currentTimestamp > latestSATimestamp) {
                    latestSATimestamp = currentTimestamp;
                }
            }

            TestComparisons comparisons = assessment.getComparisons();
            if (comparisons != null) {
                float dtcDT = comparisons.getDualTaskBalanceChange();
                if (dtcDT != 0) dtDTCTrials.add(dtcDT);
                float dtcSA = comparisons.getSensoryAtaxiaBalanceChange();
                if (dtcSA != 0) saDTCTrials.add(dtcSA);
                float dtcVelDT = comparisons.getDualTaskVelocityChange();
                if (dtcVelDT != 0) dtVelocityDTCTrials.add(dtcVelDT);
                float dtcPathDT = comparisons.getDualTaskPathLengthChange();
                if (dtcPathDT != 0) dtPathLengthDTCTrials.add(dtcPathDT);
                float dtcVelSA = comparisons.getSensoryAtaxiaVelocityChange();
                if (dtcVelSA != 0) saVelocityDTCTrials.add(dtcVelSA);
                float dtcPathSA = comparisons.getSensoryAtaxiaPathLengthChange();
                if (dtcPathSA != 0) saPathLengthDTCTrials.add(dtcPathSA);

                if (dtcDT != 0) DTBalanceDTC = dtcDT;
                if (dtcSA != 0) SABalanceDTC = dtcSA;
            }
        }

        if (!hasDataForDate) {
            Toast.makeText(requireContext(), "No assessment data found for " + targetDateStr, Toast.LENGTH_SHORT).show();
            return;
        }

        // Use best values for the selected date
        balanceToday = bestBalance != null ? bestBalance : 0f;
        speedToday = bestWalkingSpeed != null ? bestWalkingSpeed : 0f;
        enduranceToday = bestEndurance != null ? bestEndurance : 0f;
        DTbalanceToday = bestDTBalance != null ? bestDTBalance : 0f;
        SAbalanceToday = bestSABalance != null ? bestSABalance : 0f;

        // Calculate SD and CV across trials
        float BalanceSD = standardDeviation(allBalanceValues);
        float EnduranceSD = standardDeviation(allEnduranceValues);
        float WSSD = standardDeviation(allSpeedValues);

        float BalanceCV = (BalanceSD / getMean(allBalanceValues)) * 100;
        float WSCV = (WSSD / getMean(allSpeedValues)) * 100;
        float EnduranceCV = (EnduranceSD / getMean(allEnduranceValues)) * 100;

        // Create new Report
        ReportModel newReport = new ReportModel();
        newReport.setBalanceToday(balanceToday);
        newReport.setSpeedToday(speedToday);
        newReport.setEnduranceToday(enduranceToday);
        newReport.setSensoryAtaxiaBalanceToday(SAbalanceToday);
        newReport.setDualTaskBalanceToday(DTbalanceToday);
        // DTC
        newReport.setDualTaskDTCBalance(DTBalanceDTC);
        newReport.setSensoryAtaxiaDTCBalance(SABalanceDTC);
        // SD & CV
        newReport.setBalanceSD(BalanceSD);
        newReport.setEnduranceSD(EnduranceSD);
        newReport.setSpeedSD(WSSD);
        newReport.setBalanceCV(BalanceCV);
        newReport.setEnduranceCV(EnduranceCV);
        newReport.setSpeedCV(WSCV);

        // Set Trial Lists
        newReport.setBalanceTrials(balanceTrials);
        newReport.setSpeedTrials(speedTrials);
        newReport.setEnduranceTrials(enduranceTrials);
        newReport.setDualTaskBalanceTrials(dtBalanceTrials);
        newReport.setDualTaskStroopTrials(dtStroopTrials);
        newReport.setDualTaskDTCTrials(dtDTCTrials);
        newReport.setSensoryAtaxiaBalanceTrials(saBalanceTrials);
        newReport.setSensoryAtaxiaDTCTrials(saDTCTrials);
        newReport.setSwayPathLengthTrials(swayPathLengthTrials);
        newReport.setSwayVelocityTrials(swayVelocityTrials);
        newReport.setSwayRmsAPTrials(swayRmsAPTrials);
        newReport.setSwayRmsMLTrials(swayRmsMLTrials);
        newReport.setDualTaskSwayPathLengthTrials(dtSwayPathLengthTrials);
        newReport.setDualTaskSwayVelocityTrials(dtSwayVelocityTrials);
        newReport.setDualTaskSwayRmsAPTrials(dtSwayRmsAPTrials);
        newReport.setDualTaskSwayRmsMLTrials(dtSwayRmsMLTrials);
        newReport.setDualTaskVelocityDTCTrials(dtVelocityDTCTrials);
        newReport.setDualTaskPathLengthDTCTrials(dtPathLengthDTCTrials);
        newReport.setSensoryAtaxiaSwayPathLengthTrials(saSwayPathLengthTrials);
        newReport.setSensoryAtaxiaSwayVelocityTrials(saSwayVelocityTrials);
        newReport.setSensoryAtaxiaSwayRmsAPTrials(saSwayRmsAPTrials);
        newReport.setSensoryAtaxiaSwayRmsMLTrials(saSwayRmsMLTrials);
        newReport.setSensoryAtaxiaVelocityDTCTrials(saVelocityDTCTrials);
        newReport.setSensoryAtaxiaPathLengthDTCTrials(saPathLengthDTCTrials);

        newReport.setTimeStamp(targetDate);

        // Preview
        String preview = String.format(Locale.US,
                "Speed: %f m/s | Sway Area: %f mm² | Endurance: %f s",
                speedToday, balanceToday, enduranceToday);
        newReport.setPreview(preview);

        // Set date in display format
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
        String formattedDate = displayFormat.format(targetDate);
        newReport.setDate(formattedDate);

        // Save report
        DataStorageManager.saveReport(requireContext(), newReport);

        // Reload all reports to refresh the list
        loadAllReports();

        // Scroll to the new item
        recyclerView.scrollToPosition(0);

        Toast.makeText(requireContext(), "Report created for " + formattedDate, Toast.LENGTH_SHORT).show();
    }

    private void showDatePickerForReport() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth);
                    Date selectedDate = selectedCal.getTime();

                    try {
                        createReportForDate(selectedDate);
                    } catch (IOException e) {
                        Toast.makeText(requireContext(), "Error creating report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());

        datePicker.show();
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

    public float standardDeviation(ArrayList<Float> set) {
        if (set == null || set.size() < 2) {
            return 0f;
        }

        float sum = 0;
        for (float value : set) {
            sum += value;
        }
        float mean = sum / set.size();

        // Calculate sum of squared differences
        float sumSquaredDiff = 0;
        for (float value : set) {
            float diff = value - mean;
            sumSquaredDiff += diff * diff;
        }

        // Variance and standard deviation
        float variance = sumSquaredDiff / set.size(); // Population variance
        return (float) Math.sqrt(variance);
    }

    private void showDeleteReportDialog() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    // Use the SAME format as stored in reports
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth);
                    String selectedDate = new SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(selectedCal.getTime());
                    // This creates: "October 24, 2025" - MATCHES the report format!

                    deleteReportByDate(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void deleteReportByDate(String dateStr) {
        List<ReportModel> allReports = DataStorageManager.loadAllReports(requireContext());
        List<ReportModel> reportsOnDate = new ArrayList<>();

        for (ReportModel report : allReports) {
            String reportDate = report.getDate(); // "October 24, 2025"
            if (reportDate != null && reportDate.equals(dateStr)) { // Same format!
                reportsOnDate.add(report);
            }
        }

        if (reportsOnDate.isEmpty()) {
            Toast.makeText(requireContext(), "No report found for " + dateStr, Toast.LENGTH_SHORT).show();
            return;
        }

        // If only one report, delete directly
        if (reportsOnDate.size() == 1) {
            confirmAndDelete(reportsOnDate.get(0), dateStr);
            return;
        }

        // Multiple reports - show list to choose
        String[] reportNames = new String[reportsOnDate.size()];
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US); // "8:28 PM"

        for (int i = 0; i < reportsOnDate.size(); i++) {
            // Use timestamp or report type to differentiate
            ReportModel report = reportsOnDate.get(i);
            String timeStr = timeFormat.format(report.getTimeStamp());
            reportNames[i] = report.getTimestamp() != null ?
                    "Report " + (reportsOnDate.size() - i) + " (" + timeStr + ")" :
                    "Report " + (reportsOnDate.size() - i);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select report to delete for " + dateStr)
                .setItems(reportNames, (dialog, which) -> {
                    confirmAndDelete(reportsOnDate.get(which), dateStr);
                })
                .show();
    }

    private void confirmAndDelete(ReportModel report, String dateStr) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Deletion")
                .setMessage("Delete report for " + dateStr + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean deleted = DataStorageManager.deleteReport(requireContext(), report);
                    if (deleted) {
                        loadAllReports(); // Refresh list
                        Toast.makeText(requireContext(), "Report deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Helper method to convert "Oct 24 2025" to "10/24/2025"
    private String convertDateString(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("MMM dd yyyy", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return dateStr;
        }
    }
    private float getMean(ArrayList<Float> set) {
        if (set == null || set.isEmpty()) return 1f; // Avoid division by zero
        float sum = 0;
        for (float value : set) {
            sum += value;
        }
        return sum / set.size();
    }
    private final BroadcastReceiver reportUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("REPORT_UPDATED".equals(intent.getAction())) {
                // Refresh reports and reload RecyclerView
                loadAllReports();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        loadAllReports();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        // Register receiver using ContextCompat with RECEIVER_NOT_EXPORTED flag
        IntentFilter filter = new IntentFilter("REPORT_UPDATED");
        ContextCompat.registerReceiver(
                requireContext(),
                reportUpdateReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            requireContext().unregisterReceiver(reportUpdateReceiver);
        } catch (IllegalArgumentException e) {
        }
    }
}
