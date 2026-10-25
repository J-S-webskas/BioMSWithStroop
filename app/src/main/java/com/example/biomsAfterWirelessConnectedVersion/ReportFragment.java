package com.example.biomsAfterWirelessConnectedVersion;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class ReportFragment extends Fragment {

    private RecyclerView recyclerView;      // The container for cards
    private ReportAdapter adapter;          // Connects data to cards
    private List<ReportModel> reportList = new ArrayList<>();        // Stores all reports
    private Button createNewReport;          // Button to make new reports
    private TextView noData;
    private Button btnFilterDay;
    private Button btnClearFilter;
    private TextView filterDayLabel;
    private String filterDateStr = null; // "MMMM dd, yyyy" while a day filter is active, else null

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

        Button btnExportCsv = view.findViewById(R.id.btn_export_csv);
        btnExportCsv.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            showExportCsvDialog();
        });

        Button btnExportMultiple = view.findViewById(R.id.btn_export_multiple);
        btnExportMultiple.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            showMultiExportDialog();
        });

        Button btnExportReports = view.findViewById(R.id.btn_export_reports);
        btnExportReports.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            showMultiReportDialog();
        });

        noData = view.findViewById(R.id.noData);

        btnFilterDay = view.findViewById(R.id.btn_filter_day);
        btnFilterDay.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            showDayFilterPicker();
        });
        btnClearFilter = view.findViewById(R.id.btn_clear_filter);
        btnClearFilter.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            applyDayFilter(null);
        });
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
        // Latest step values of the day (null = no walk with step data)
        Float bestStepCount = null;
        Float bestCadence = null;
        Float bestStepLength = null;
        Float bestStepTimeCv = null;
        Float bestStepTimeN = null;
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
        // Step trials (only walks with step data contribute, so these can
        // be shorter than speedTrials; stepTimeNTrials parallels stepTimeCvTrials)
        ArrayList<Float> stepCountTrials = new ArrayList<>();
        ArrayList<Float> cadenceTrials = new ArrayList<>();
        ArrayList<Float> stepLengthTrials = new ArrayList<>();
        ArrayList<Float> stepTimeCvTrials = new ArrayList<>();
        ArrayList<Float> stepTimeNTrials = new ArrayList<>();
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

        // Timestamped trial points for report-time DTC computation
        // (see computeDtcTrials). Chronological, like the trial lists.
        ArrayList<Long> baseTimes = new ArrayList<>();
        ArrayList<Float> baseBalPts = new ArrayList<>();
        ArrayList<Float> baseVelPts = new ArrayList<>();
        ArrayList<Float> basePathPts = new ArrayList<>();
        ArrayList<Long> dtTimes = new ArrayList<>();
        ArrayList<Float> dtBalPts = new ArrayList<>();
        ArrayList<Float> dtVelPts = new ArrayList<>();
        ArrayList<Float> dtPathPts = new ArrayList<>();
        ArrayList<Long> saTimes = new ArrayList<>();
        ArrayList<Float> saBalPts = new ArrayList<>();
        ArrayList<Float> saVelPts = new ArrayList<>();
        ArrayList<Float> saPathPts = new ArrayList<>();

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
                    baseTimes.add(currentTimestamp);
                    baseBalPts.add(balance);
                    baseVelPts.add(assessment.getBaseline().getSwayMeanVelocity());
                    basePathPts.add(assessment.getBaseline().getSwayPathLength());
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

            // Step trials: only walks whose detection produced steps. The
            // has-data marker is stepCount > 0 (unset trials store 0).
            if (assessment.getBaseline().getStepCount() > 0) {
                stepCountTrials.add((float) assessment.getBaseline().getStepCount());
                cadenceTrials.add(assessment.getBaseline().getCadence());
                stepLengthTrials.add(assessment.getBaseline().getMeanStepLength());
                stepTimeCvTrials.add(assessment.getBaseline().getStepTimeCVPct());
                stepTimeNTrials.add((float) assessment.getBaseline().getStepTimeN());
                if (bestStepCount == null || currentTimestamp > latestTimestamp) {
                    bestStepCount = (float) assessment.getBaseline().getStepCount();
                    bestCadence = assessment.getBaseline().getCadence();
                    bestStepLength = assessment.getBaseline().getMeanStepLength();
                    bestStepTimeCv = assessment.getBaseline().getStepTimeCVPct();
                    bestStepTimeN = (float) assessment.getBaseline().getStepTimeN();
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
                    if (dtBalance > 0 || dualTask.getSwayPathLength() > 0) {
                        dtTimes.add(currentTimestamp);
                        dtBalPts.add(dtBalance);
                        dtVelPts.add(dualTask.getSwayMeanVelocity());
                        dtPathPts.add(dualTask.getSwayPathLength());
                    }
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
                    if (saBalance > 0 || sensory.getSwayPathLength() > 0) {
                        saTimes.add(currentTimestamp);
                        saBalPts.add(saBalance);
                        saVelPts.add(sensory.getSwayMeanVelocity());
                        saPathPts.add(sensory.getSwayPathLength());
                    }
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

        // Report-time DTC: compute each condition trial's percent change
        // against the most recent baseline trial at or before it, from
        // the stored trial values themselves. The save-time comparisons
        // collected above are only a fallback (used when a trial has no
        // pairable baseline this day, e.g. its baseline was recorded on
        // another day): builds before PR #25 never stored velocity/path
        // DTC at all, so reading the stored comparisons alone left
        // those DTCs at 0 for every older trial.
        ArrayList<Float> computedDtBalDTC = new ArrayList<>();
        ArrayList<Float> computedDtVelDTC = new ArrayList<>();
        ArrayList<Float> computedDtPathDTC = new ArrayList<>();
        ArrayList<Float> computedSaBalDTC = new ArrayList<>();
        ArrayList<Float> computedSaVelDTC = new ArrayList<>();
        ArrayList<Float> computedSaPathDTC = new ArrayList<>();
        computeDtcTrials(baseTimes, baseBalPts, baseVelPts, basePathPts,
                dtTimes, dtBalPts, dtVelPts, dtPathPts,
                computedDtBalDTC, computedDtVelDTC, computedDtPathDTC);
        computeDtcTrials(baseTimes, baseBalPts, baseVelPts, basePathPts,
                saTimes, saBalPts, saVelPts, saPathPts,
                computedSaBalDTC, computedSaVelDTC, computedSaPathDTC);
        if (!computedDtBalDTC.isEmpty()) dtDTCTrials = computedDtBalDTC;
        if (!computedDtVelDTC.isEmpty()) dtVelocityDTCTrials = computedDtVelDTC;
        if (!computedDtPathDTC.isEmpty()) dtPathLengthDTCTrials = computedDtPathDTC;
        if (!computedSaBalDTC.isEmpty()) saDTCTrials = computedSaBalDTC;
        if (!computedSaVelDTC.isEmpty()) saVelocityDTCTrials = computedSaVelDTC;
        if (!computedSaPathDTC.isEmpty()) saPathLengthDTCTrials = computedSaPathDTC;
        // The headline DTC for each metric is the most recent trial's
        // value in the final list (computed where available, stored
        // fallback otherwise). The velocity/path headline values were
        // previously never assigned here at all, so they always read 0.
        DTBalanceDTC = dtDTCTrials.isEmpty() ? 0 : dtDTCTrials.get(dtDTCTrials.size() - 1);
        DTVelocityDTC = dtVelocityDTCTrials.isEmpty() ? 0 : dtVelocityDTCTrials.get(dtVelocityDTCTrials.size() - 1);
        DTPathDTC = dtPathLengthDTCTrials.isEmpty() ? 0 : dtPathLengthDTCTrials.get(dtPathLengthDTCTrials.size() - 1);
        SABalanceDTC = saDTCTrials.isEmpty() ? 0 : saDTCTrials.get(saDTCTrials.size() - 1);
        SAVelocityDTC = saVelocityDTCTrials.isEmpty() ? 0 : saVelocityDTCTrials.get(saVelocityDTCTrials.size() - 1);
        SAPathDTC = saPathLengthDTCTrials.isEmpty() ? 0 : saPathLengthDTCTrials.get(saPathLengthDTCTrials.size() - 1);

        if (!hasDataForDate) {
            Toast.makeText(requireContext(), "No assessment data found for " + targetDateStr, Toast.LENGTH_SHORT).show();
            return;
        }

        // Use best values for the selected date
        balanceToday = bestBalance != null ? bestBalance : 0f;
        speedToday = bestWalkingSpeed != null ? bestWalkingSpeed : 0f;
        enduranceToday = bestEndurance != null ? bestEndurance : 0f;
        float stepCountToday = bestStepCount != null ? bestStepCount : 0f;
        float cadenceToday = bestCadence != null ? bestCadence : 0f;
        float stepLengthToday = bestStepLength != null ? bestStepLength : 0f;
        float stepTimeCvToday = bestStepTimeCv != null ? bestStepTimeCv : 0f;
        float stepTimeNToday = bestStepTimeN != null ? bestStepTimeN : 0f;
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
        newReport.setStepCountToday(stepCountToday);
        newReport.setCadenceToday(cadenceToday);
        newReport.setStepLengthToday(stepLengthToday);
        newReport.setStepTimeCvToday(stepTimeCvToday);
        newReport.setStepTimeNToday(stepTimeNToday);
        newReport.setSensoryAtaxiaBalanceToday(SAbalanceToday);
        newReport.setDualTaskBalanceToday(DTbalanceToday);
        // DTC
        newReport.setDualTaskDTCBalance(DTBalanceDTC);
        newReport.setDualTaskDTCSwayVelocity(DTVelocityDTC);
        newReport.setDualTaskDTCSwayPathLength(DTPathDTC);
        newReport.setSensoryAtaxiaDTCBalance(SABalanceDTC);
        newReport.setSensoryAtaxiaDTCSwayVelocity(SAVelocityDTC);
        newReport.setSensoryAtaxiaDTCSwayPathLength(SAPathDTC);
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
        newReport.setStepCountTrials(stepCountTrials);
        newReport.setCadenceTrials(cadenceTrials);
        newReport.setStepLengthTrials(stepLengthTrials);
        newReport.setStepTimeCvTrials(stepTimeCvTrials);
        newReport.setStepTimeNTrials(stepTimeNTrials);
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

        // Re-apply the active day filter (or show everything) so every
        // reload — new report, deletion, resume, broadcast — respects it.
        applyDayFilter(filterDateStr);

        return reports;
    }

    /** Opens a date picker (same pattern as the New-report picker) and
     *  filters the report list to that day. */
    private void showDayFilterPicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth);
                    String selectedDateStr = new SimpleDateFormat("MMMM dd, yyyy", Locale.US)
                            .format(selectedCal.getTime());
                    applyDayFilter(selectedDateStr);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());

        datePicker.show();
    }

    /** Shows only the reports for {@code dateStr} ("MMMM dd, yyyy"), or all
     *  reports when {@code dateStr} is null. The stored order is preserved,
     *  so all of a day's reports appear together in their usual order. */
    private void applyDayFilter(String dateStr) {
        filterDateStr = dateStr;
        List<ReportModel> shown = new ArrayList<>();
        if (dateStr == null) {
            shown.addAll(reportList);
        } else {
            for (ReportModel report : reportList) {
                if (dateStr.equals(report.getDate())) {
                    shown.add(report);
                }
            }
        }
        if (adapter != null) {
            adapter.updateData(shown);
        }
        if (filterDayLabel != null) {
            filterDayLabel.setText(dateStr == null
                    ? "Showing all reports"
                    : "Showing reports for " + dateStr);
        }
        if (btnClearFilter != null) {
            btnClearFilter.setVisibility(dateStr == null ? View.GONE : View.VISIBLE);
        }
        if (noData != null) {
            if (shown.isEmpty()) {
                noData.setText(dateStr == null
                        ? "Finish a recording to create data!"
                        : "No reports for " + dateStr);
                noData.setVisibility(View.VISIBLE);
            } else {
                noData.setVisibility(View.GONE);
            }
        }
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

    /**
     * Report-time dual task cost. For each condition trial (chronological),
     * finds the most recent baseline trial recorded at or before it and
     * appends the percent change ((trial - baseline) / baseline) x 100 for
     * each metric both sides recorded (> 0). Trials with no pairable
     * baseline contribute nothing; callers fall back to the save-time
     * comparisons in that case. Computing DTC here, from the stored trial
     * values, keeps reports correct for trials whose save-time
     * comparisons were never stored (builds before PR #25 stored no
     * velocity/path DTC) and makes the result independent of the order
     * in which assessment files happen to be read.
     */
    private static void computeDtcTrials(
            ArrayList<Long> baseTimes, ArrayList<Float> baseBal,
            ArrayList<Float> baseVel, ArrayList<Float> basePath,
            ArrayList<Long> condTimes, ArrayList<Float> condBal,
            ArrayList<Float> condVel, ArrayList<Float> condPath,
            ArrayList<Float> outBal, ArrayList<Float> outVel,
            ArrayList<Float> outPath) {
        for (int i = 0; i < condTimes.size(); i++) {
            int b = -1;
            for (int j = 0; j < baseTimes.size(); j++) {
                if (baseTimes.get(j) <= condTimes.get(i)) {
                    b = j;
                } else {
                    break;
                }
            }
            if (b < 0) continue;
            if (condBal.get(i) > 0 && baseBal.get(b) > 0) {
                outBal.add(((condBal.get(i) - baseBal.get(b)) / baseBal.get(b)) * 100f);
            }
            if (condVel.get(i) > 0 && baseVel.get(b) > 0) {
                outVel.add(((condVel.get(i) - baseVel.get(b)) / baseVel.get(b)) * 100f);
            }
            if (condPath.get(i) > 0 && basePath.get(b) > 0) {
                outPath.add(((condPath.get(i) - basePath.get(b)) / basePath.get(b)) * 100f);
            }
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

    private void showExportCsvDialog() {
        List<AssessmentData> all = DataStorageManager.loadAllAssessments(requireContext());
        Map<String, List<AssessmentData>> byDate = new TreeMap<>(Collections.reverseOrder());
        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        for (AssessmentData a : all) {
            if (a.getTimestamp() == null) continue;
            String key = keyFmt.format(a.getTimestamp());
            byDate.computeIfAbsent(key, k -> new ArrayList<>()).add(a);
        }
        if (byDate.isEmpty()) {
            Toast.makeText(requireContext(), "No recorded tests to export yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> keys = new ArrayList<>(byDate.keySet());
        String todayKey = keyFmt.format(new Date());
        SimpleDateFormat dispFmt = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
        String[] labels = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            String label = keys.get(i);
            try {
                label = dispFmt.format(keyFmt.parse(keys.get(i)));
            } catch (Exception ignored) {
            }
            labels[i] = (keys.get(i).equals(todayKey) ? "Today \u2014 " : "") + label;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Export CSV \u2014 choose a day")
                .setItems(labels, (d, which) ->
                        showExportConditionDialog(keys.get(which), byDate.get(keys.get(which))))
                .show();
    }

    private void showExportConditionDialog(String dateKey, List<AssessmentData> day) {
        int nBaseline = 0, nDual = 0, nSensory = 0;
        for (AssessmentData a : day) {
            if (hasBaselineTrial(a)) nBaseline++;
            if (hasDualTaskTrial(a)) nDual++;
            if (hasSensoryTrial(a)) nSensory++;
        }
        List<String> labels = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        if (nBaseline > 0) {
            labels.add("Baseline (" + nBaseline + (nBaseline == 1 ? " trial" : " trials") + ")");
            conditions.add("Baseline");
        }
        if (nDual > 0) {
            labels.add("Dual Task (" + nDual + (nDual == 1 ? " trial" : " trials") + ")");
            conditions.add("DualTask");
        }
        if (nSensory > 0) {
            labels.add("Sensory Ataxia (" + nSensory + (nSensory == 1 ? " trial" : " trials") + ")");
            conditions.add("SensoryAtaxia");
        }
        if (conditions.isEmpty()) {
            Toast.makeText(requireContext(), "No completed tests on this day.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Export CSV \u2014 choose a test")
                .setItems(labels.toArray(new String[0]), (d, which) ->
                        exportStoredCondition(dateKey, conditions.get(which), day))
                .show();
    }

    private boolean hasBaselineTrial(AssessmentData a) {
        return a.getSwayAreaAndBalanceTestDate() != null || a.getBaseline().getBalanceScore() > 0;
    }

    private boolean hasDualTaskTrial(AssessmentData a) {
        return a.getDualTask() != null && (a.getDualTaskTestDate() != null
                || a.getDualTask().getBalanceScore() > 0
                || a.getDualTask().getStroopTotalTrials() > 0);
    }

    private boolean hasSensoryTrial(AssessmentData a) {
        return a.getSensoryAtaxia() != null && (a.getSensoryAtaxiaTestDate() != null
                || a.getSensoryAtaxia().getBalanceScore() > 0);
    }

    private void exportStoredCondition(String dateKey, String condition, List<AssessmentData> day) {
        List<AssessmentData> chronological = new ArrayList<>(day);
        Collections.sort(chronological, (a1, a2) -> a1.getTimestamp().compareTo(a2.getTimestamp()));
        List<TrialCsvExporter.StoredTrialData> trials = new ArrayList<>();
        for (AssessmentData a : chronological) {
            TrialCsvExporter.StoredTrialData t = buildStoredTrialFor(a, condition);
            if (t != null) trials.add(t);
        }
        if (trials.isEmpty()) {
            Toast.makeText(requireContext(), "Nothing to export for that test.", Toast.LENGTH_SHORT).show();
            return;
        }
        File zip = TrialCsvExporter.exportStoredTrials(requireContext(), dateKey, condition, trials);
        if (zip == null) {
            Toast.makeText(requireContext(), "Export failed.", Toast.LENGTH_SHORT).show();
            return;
        }
        shareZip(zip);
    }

    /** Hands a finished export ZIP to the system share sheet. */
    private void shareZip(File zip) {
        Uri uri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", zip);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/zip");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_SUBJECT, zip.getName());
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Export CSV data"));
    }

    /** Builds the stored-trial record for one assessment's one condition,
     *  or null when that assessment holds no such trial. Shared by the
     *  single-condition export and the multi-export below. */
    private TrialCsvExporter.StoredTrialData buildStoredTrialFor(AssessmentData a, String condition) {
        TrialCsvExporter.StoredTrialData t = null;
        if ("Baseline".equals(condition) && hasBaselineTrial(a)) {
            t = toStoredTrial(a.getBaseline().getComTrajX(), a.getBaseline().getComTrajZ(),
                    a.getBaseline().getComFrameRate(), a.getBaseline().getBalanceScore(),
                    a.getBaseline().getSwayPathLength(), a.getBaseline().getSwayMeanVelocity(),
                    a.getBaseline().getSwayRmsAP(), a.getBaseline().getSwayRmsML(),
                    a.getBaseline().getCameraDistanceM(), a.getBaseline().isSwayMetricsValid());
            TrialCsvExporter.fillDiagnostics(t, a.getBaseline());
            t.sessionId = a.getSessionId();
        } else if ("DualTask".equals(condition) && hasDualTaskTrial(a)) {
            t = toStoredTrial(a.getDualTask().getComTrajX(), a.getDualTask().getComTrajZ(),
                    a.getDualTask().getComFrameRate(), a.getDualTask().getBalanceScore(),
                    a.getDualTask().getSwayPathLength(), a.getDualTask().getSwayMeanVelocity(),
                    a.getDualTask().getSwayRmsAP(), a.getDualTask().getSwayRmsML(),
                    a.getDualTask().getCameraDistanceM(), a.getDualTask().isSwayMetricsValid());
            TrialCsvExporter.fillDiagnostics(t, a.getDualTask());
            t.sessionId = a.getSessionId();
        } else if ("SensoryAtaxia".equals(condition) && hasSensoryTrial(a)) {
            t = toStoredTrial(a.getSensoryAtaxia().getComTrajX(), a.getSensoryAtaxia().getComTrajZ(),
                    a.getSensoryAtaxia().getComFrameRate(), a.getSensoryAtaxia().getBalanceScore(),
                    a.getSensoryAtaxia().getSwayPathLength(), a.getSensoryAtaxia().getSwayMeanVelocity(),
                    a.getSensoryAtaxia().getSwayRmsAP(), a.getSensoryAtaxia().getSwayRmsML(),
                    a.getSensoryAtaxia().getCameraDistanceM(), a.getSensoryAtaxia().isSwayMetricsValid());
            TrialCsvExporter.fillDiagnostics(t, a.getSensoryAtaxia());
            t.sessionId = a.getSessionId();
        }
        return t;
    }

    // ------------------------------------------------------------------
    // Multi-day, multi-trial export: a separate button from the single
    // Export CSV. Days and individual trials are picked with checkboxes;
    // everything selected goes into one ZIP, grouped by day and test.
    // ------------------------------------------------------------------

    /** One checkbox row in the multi-export dialog. */
    private static class MultiExportRow {
        CheckBox box;
        String dateKey;                 // yyyy-MM-dd
        String condition;               // Baseline / DualTask / SensoryAtaxia, null for a day row
        AssessmentData assessment;      // null for a day row
        ReportModel report;             // null except for a report row under a day row
        int trialNumber;                // 1-based within its day + condition
        List<MultiExportRow> children = new ArrayList<>(); // trial/report rows under a day row
    }

    private void showMultiExportDialog() {
        List<AssessmentData> all = DataStorageManager.loadAllAssessments(requireContext());
        Map<String, List<AssessmentData>> byDate = new TreeMap<>(Collections.reverseOrder());
        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        for (AssessmentData a : all) {
            if (a.getTimestamp() == null) continue;
            String key = keyFmt.format(a.getTimestamp());
            byDate.computeIfAbsent(key, k -> new ArrayList<>()).add(a);
        }
        SimpleDateFormat dispFmt = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
        String todayKey = keyFmt.format(new Date());
        float density = requireContext().getResources().getDisplayMetrics().density;
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * density);
        container.setPadding(pad, pad, pad, pad);
        // Guard against listener cascades: a programmatic setChecked must
        // not re-enter the day/trial sync below.
        final boolean[] syncing = {false};

        CheckBox exportAllBox = new CheckBox(requireContext());
        exportAllBox.setText("Export all CSV");
        exportAllBox.setTextSize(17);
        exportAllBox.setTypeface(Typeface.DEFAULT_BOLD);
        container.addView(exportAllBox);

        List<MultiExportRow> dayRows = new ArrayList<>();
        boolean firstDayAdded = false;
        for (Map.Entry<String, List<AssessmentData>> entry : byDate.entrySet()) {
            String dateKey = entry.getKey();
            List<AssessmentData> chronological = new ArrayList<>(entry.getValue());
            Collections.sort(chronological, (a1, a2) -> a1.getTimestamp().compareTo(a2.getTimestamp()));
            String label = dateKey;
            try {
                label = dispFmt.format(keyFmt.parse(dateKey));
            } catch (Exception ignored) {
            }
            if (dateKey.equals(todayKey)) label = "Today \u2014 " + label;

            MultiExportRow dayRow = new MultiExportRow();
            dayRow.dateKey = dateKey;
            CheckBox dayBox = new CheckBox(requireContext());
            dayBox.setText(label);
            dayBox.setTextSize(17);
            dayRow.box = dayBox;

            int nBaseline = 0, nDual = 0, nSensory = 0;
            for (AssessmentData a : chronological) {
                if (hasBaselineTrial(a)) {
                    dayRow.children.add(makeTrialRow(dateKey,
                            "Baseline", "Baseline", ++nBaseline, a, density));
                }
                if (hasDualTaskTrial(a)) {
                    dayRow.children.add(makeTrialRow(dateKey,
                            "DualTask", "Dual Task", ++nDual, a, density));
                }
                if (hasSensoryTrial(a)) {
                    dayRow.children.add(makeTrialRow(dateKey,
                            "SensoryAtaxia", "Sensory Ataxia", ++nSensory, a, density));
                }
            }
            if (dayRow.children.isEmpty()) {
                continue;
            }
            if (firstDayAdded) {
                View spacer = new View(requireContext());
                spacer.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, (int) (12 * density)));
                container.addView(spacer);
            }
            container.addView(dayBox);
            firstDayAdded = true;
            for (MultiExportRow child : dayRow.children) container.addView(child.box);
            for (MultiExportRow child : dayRow.children) {
                child.box.setOnCheckedChangeListener((b, checked) -> {
                    if (syncing[0]) return;
                    syncing[0] = true;
                    boolean allChecked = true;
                    for (MultiExportRow c : dayRow.children) allChecked &= c.box.isChecked();
                    dayRow.box.setChecked(allChecked);
                    syncing[0] = false;
                    syncExportAllBox(exportAllBox, dayRows, syncing);
                });
            }
            dayBox.setOnCheckedChangeListener((b, checked) -> {
                if (syncing[0]) return;
                syncing[0] = true;
                for (MultiExportRow child : dayRow.children) child.box.setChecked(checked);
                syncing[0] = false;
                syncExportAllBox(exportAllBox, dayRows, syncing);
            });
            dayRows.add(dayRow);
        }
        exportAllBox.setOnCheckedChangeListener((b, checked) -> {
            if (syncing[0]) return;
            syncing[0] = true;
            for (MultiExportRow dayRow : dayRows) {
                dayRow.box.setChecked(checked);
                for (MultiExportRow child : dayRow.children) child.box.setChecked(checked);
            }
            syncing[0] = false;
        });
        if (dayRows.isEmpty()) {
            Toast.makeText(requireContext(), "No recorded tests to export yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        ScrollView scroll = new ScrollView(requireContext());
        scroll.addView(container);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Export multiple \u2014 choose days and trials")
                .setView(scroll)
                .setPositiveButton("Export CSV", null)
                .setNegativeButton("Cancel", null)
                .show();
        // Override the positive button so an empty selection toasts and
        // keeps the dialog open instead of dismissing it.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            List<TrialCsvExporter.MultiExportEntry> entries = collectMultiExportEntries(dayRows);
            if (entries.isEmpty()) {
                Toast.makeText(requireContext(), "Select at least one trial.", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            exportMultiSelection(entries);
        });
    }

    private MultiExportRow makeTrialRow(String dateKey, String condition, String displayName, int trialNumber, AssessmentData assessment, float density) {
        MultiExportRow row = new MultiExportRow();
        row.dateKey = dateKey;
        row.condition = condition;
        row.assessment = assessment;
        row.trialNumber = trialNumber;
        CheckBox box = new CheckBox(requireContext());
        box.setText(displayName + " T" + trialNumber);
        box.setTextSize(16);
        box.setPadding((int) (32 * density), 0, 0, 0);
        row.box = box;
        return row;
    }


    private List<TrialCsvExporter.MultiExportEntry> collectMultiExportEntries(
            List<MultiExportRow> dayRows) {
        List<TrialCsvExporter.MultiExportEntry> entries = new ArrayList<>();
        for (MultiExportRow dayRow : dayRows) {
            Map<String, Integer> totals = new HashMap<>();
            for (MultiExportRow child : dayRow.children) {
                totals.put(child.condition, totals.getOrDefault(child.condition, 0) + 1);
            }
            for (MultiExportRow child : dayRow.children) {
                if (!child.box.isChecked()) continue;
                TrialCsvExporter.StoredTrialData t = buildStoredTrialFor(child.assessment, child.condition);
                if (t == null) continue;
                TrialCsvExporter.MultiExportEntry e = new TrialCsvExporter.MultiExportEntry();
                e.dateStr = child.dateKey;
                e.conditionLabel = child.condition;
                e.trialIndex = child.trialNumber;
                e.trialCount = totals.get(child.condition);
                e.trial = t;
                entries.add(e);
            }
        }
        return entries;
    }

    private void exportMultiSelection(List<TrialCsvExporter.MultiExportEntry> entries) {
        File zip = TrialCsvExporter.exportSelectedTrials(requireContext(), entries);
        if (zip == null) {
            Toast.makeText(requireContext(), "Export failed.", Toast.LENGTH_SHORT).show();
            return;
        }
        shareZip(zip);
    }

    /** Keeps the master "Export all CSV" checkbox in step with the day
     *  boxes. Called with the sync guard released; it re-takes the guard
     *  while setting the master box so the master listener cannot fire
     *  and clobber the selection it is reflecting. */
    private void syncExportAllBox(CheckBox exportAllBox, List<MultiExportRow> dayRows,
                                  boolean[] syncing) {
        boolean all = !dayRows.isEmpty();
        for (MultiExportRow dayRow : dayRows) all &= dayRow.box.isChecked();
        syncing[0] = true;
        exportAllBox.setChecked(all);
        syncing[0] = false;
    }

    private void exportReportSelection(List<TrialCsvExporter.ReportDayExport> days) {
        File zip = TrialCsvExporter.exportSelectedReports(requireContext(), days);
        if (zip == null) {
            Toast.makeText(requireContext(), "Export failed.", Toast.LENGTH_SHORT).show();
            return;
        }
        shareZip(zip);
    }

    private TrialCsvExporter.StoredTrialData toStoredTrial(List<Float> trajX, List<Float> trajZ,
                                                           float frameRate, float area, float path, float vel, float rmsAP, float rmsML,
                                                           float cameraDistanceM, boolean valid) {
        TrialCsvExporter.StoredTrialData t = new TrialCsvExporter.StoredTrialData();
        if (trajX != null) t.trajX = trajX;
        if (trajZ != null) t.trajZ = trajZ;
        t.frameRate = frameRate;
        t.swayAreaMm2 = area;
        t.pathLengthMm = path;
        t.meanVelocityMmS = vel;
        t.rmsAPMm = rmsAP;
        t.rmsMLMm = rmsML;
        t.cameraDistanceM = cameraDistanceM;
        t.valid = valid;
        return t;
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


    /** Report picker behind the screen's Export Reports button: the same
     * checkbox format as the trial picker (Export-all master, bold dates,
     * a blank line between dates), but one child per saved report of the
     * day, numbered chronologically, with its time to tell regenerations
     * apart. Exports exactly the checked reports. */
    private void showMultiReportDialog() {
        List<ReportModel> all = DataStorageManager.loadAllReports(requireContext());
        if (all == null || all.isEmpty()) {
            Toast.makeText(requireContext(), "No reports to export yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat dispFmt = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
        SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.US);
        String todayKey = keyFmt.format(new Date());

        Map<String, List<ReportModel>> byDate = new TreeMap<>(Collections.reverseOrder());
        Map<String, String> labelByKey = new HashMap<>();
        for (ReportModel r : all) {
            if (r.getDate() == null) continue;
            String key = r.getDate();
            try {
                key = keyFmt.format(dispFmt.parse(r.getDate()));
            } catch (Exception ignored) {
            }
            byDate.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
            labelByKey.put(key, r.getDate());
        }
        for (List<ReportModel> day : byDate.values()) {
            day.sort((a, b) -> {
                if (a.getTimeStamp() == null && b.getTimeStamp() == null) return 0;
                if (a.getTimeStamp() == null) return 1;
                if (b.getTimeStamp() == null) return -1;
                return a.getTimeStamp().compareTo(b.getTimeStamp());
            });
        }

        float density = requireContext().getResources().getDisplayMetrics().density;
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * density);
        container.setPadding(pad, pad, pad, pad);
        final boolean[] syncing = {false};

        CheckBox exportAllBox = new CheckBox(requireContext());
        exportAllBox.setText("Export all reports");
        exportAllBox.setTextSize(17);
        exportAllBox.setTypeface(Typeface.DEFAULT_BOLD);
        container.addView(exportAllBox);

        List<MultiExportRow> dayRows = new ArrayList<>();
        boolean firstDayAdded = false;
        for (Map.Entry<String, List<ReportModel>> entry : byDate.entrySet()) {
            String dateKey = entry.getKey();
            String label = labelByKey.get(dateKey);
            if (dateKey.equals(todayKey)) label = "Today \u2014 " + label;

            MultiExportRow dayRow = new MultiExportRow();
            dayRow.dateKey = dateKey;
            CheckBox dayBox = new CheckBox(requireContext());
            dayBox.setText(label);
            dayBox.setTextSize(17);
            dayRow.box = dayBox;

            int n = 0;
            for (ReportModel r : entry.getValue()) {
                n++;
                MultiExportRow child = new MultiExportRow();
                child.dateKey = dateKey;
                child.report = r;
                CheckBox box = new CheckBox(requireContext());
                String time = r.getTimeStamp() != null ? timeFmt.format(r.getTimeStamp()) : "";
                box.setText("Report " + n + (time.isEmpty() ? "" : " (" + time + ")"));
                box.setTextSize(16);
                box.setPadding((int) (32 * density), 0, 0, 0);
                child.box = box;
                dayRow.children.add(child);
            }
            if (firstDayAdded) {
                View spacer = new View(requireContext());
                spacer.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, (int) (12 * density)));
                container.addView(spacer);
            }
            container.addView(dayBox);
            firstDayAdded = true;
            for (MultiExportRow child : dayRow.children) container.addView(child.box);
            for (MultiExportRow child : dayRow.children) {
                child.box.setOnCheckedChangeListener((b, checked) -> {
                    if (syncing[0]) return;
                    syncing[0] = true;
                    boolean allChecked = true;
                    for (MultiExportRow c : dayRow.children) allChecked &= c.box.isChecked();
                    dayRow.box.setChecked(allChecked);
                    syncing[0] = false;
                    syncExportAllBox(exportAllBox, dayRows, syncing);
                });
            }
            dayBox.setOnCheckedChangeListener((b, checked) -> {
                if (syncing[0]) return;
                syncing[0] = true;
                for (MultiExportRow child : dayRow.children) child.box.setChecked(checked);
                syncing[0] = false;
                syncExportAllBox(exportAllBox, dayRows, syncing);
            });
            dayRows.add(dayRow);
        }
        exportAllBox.setOnCheckedChangeListener((b, checked) -> {
            if (syncing[0]) return;
            syncing[0] = true;
            for (MultiExportRow dayRow : dayRows) {
                dayRow.box.setChecked(checked);
                for (MultiExportRow child : dayRow.children) child.box.setChecked(checked);
            }
            syncing[0] = false;
        });

        ScrollView scroll = new ScrollView(requireContext());
        scroll.addView(container);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Export reports \u2014 choose days and reports")
                .setView(scroll)
                .setPositiveButton("Export Reports", null)
                .setNegativeButton("Cancel", null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            List<TrialCsvExporter.ReportDayExport> days = collectReportSelection(dayRows);
            if (days.isEmpty()) {
                Toast.makeText(requireContext(), "Select at least one report.", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            exportReportSelection(days);
        });
    }

    /** Checked reports only: a checked day box checks all its children
     *  through the listener, so collecting checked children covers both
     *  day-level and single-report selection. */
    private List<TrialCsvExporter.ReportDayExport> collectReportSelection(List<MultiExportRow> dayRows) {
        List<TrialCsvExporter.ReportDayExport> days = new ArrayList<>();
        for (MultiExportRow dayRow : dayRows) {
            List<ReportModel> checked = new ArrayList<>();
            for (MultiExportRow child : dayRow.children) {
                if (child.box.isChecked() && child.report != null) checked.add(child.report);
            }
            if (checked.isEmpty()) continue;
            TrialCsvExporter.ReportDayExport d = new TrialCsvExporter.ReportDayExport();
            d.dateKey = dayRow.dateKey;
            d.reports = checked;
            days.add(d);
        }
        return days;
    }
}
