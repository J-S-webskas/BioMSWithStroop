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
            // Today
            intent.putExtra("date", report.getDate());  // Convert Date to long
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

        // Filter assessments for the selected date
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        String targetDateString = sdf.format(targetDate);

        // Track most recent values per metric for the selected date
//        Float bestARJLeft = null;
//        Float bestARJRight = null;
        Float bestBalance = null;
        Float bestWalkingSpeed = null;
        Float bestEndurance = null;
        // Extended sway metrics (latest assessment of the selected date wins)
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

        for (AssessmentData assessment : allAssessment) {
            String currentDate = sdf.format(assessment.getTimestamp());
            long currentTimestamp = assessment.getTimestamp().getTime();

            // Collect all data for SD/CV
//            Float arjLeftSDCV = assessment.getBaseline().getARJ().get("left_knee");
//            if (arjLeftSDCV != null && arjLeftSDCV != 0) {
//                allARJLeftValues.add(arjLeftSDCV);
//            }

//            Float arjRightSDCV = assessment.getBaseline().getARJ().get("right_knee");
//            if (arjRightSDCV != null && arjRightSDCV != 0) {
//                allARJRightValues.add(arjRightSDCV);
//            }

            float balanceSDCV = assessment.getBaseline().getBalanceScore();
            if (assessment.getSwayAreaAndBalanceTestDate() != null) {
                allBalanceValues.add(balanceSDCV);
            }

            float walkingSpeedSDCV = assessment.getBaseline().getWalkingSpeed();
            if (assessment.getWSAndEnduranceTestDate() != null) {
                allSpeedValues.add(walkingSpeedSDCV);
            }

            float enduranceSDCV = assessment.getBaseline().getEnduranceScore();
            if (assessment.getWSAndEnduranceTestDate() != null) {
                allEnduranceValues.add(enduranceSDCV);
            }

            // Process only data from the selected date
            if (!currentDate.equals(targetDateString)) continue;

            hasDataForDate = true;

            // Baseline data
//            Float arjLeft = assessment.getBaseline().getARJ().get("left_knee");
//            if (arjLeft != null && arjLeft != 0) {
//                if (bestARJLeft == null || currentTimestamp > latestTimestamp) {
//                    bestARJLeft = arjLeft;
//                }
//            }
//
//            Float arjRight = assessment.getBaseline().getARJ().get("right_knee");
//            if (arjRight != null && arjRight != 0) {
//                if (bestARJRight == null || currentTimestamp > latestTimestamp) {
//                    bestARJRight = arjRight;
//                }
//            }

            float balance = assessment.getBaseline().getBalanceScore();
            if (assessment.getSwayAreaAndBalanceTestDate() != null) {
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
            if (assessment.getWSAndEnduranceTestDate() != null) {
                if (bestWalkingSpeed == null || currentTimestamp > latestTimestamp) {
                    bestWalkingSpeed = walkingSpeed;
                }
            }

            float endurance = assessment.getBaseline().getEnduranceScore();
            if (assessment.getWSAndEnduranceTestDate() != null) {
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
//                Float dtArjLeft = dualTask.getARJ().get("left_knee");
//                if (dtArjLeft != null && dtArjLeft != 0) {
//                    if (bestDTARJLeft == null || currentTimestamp > latestDTTimestamp) {
//                        bestDTARJLeft = dtArjLeft;
//                    }
//                }
//
//                Float dtArjRight = dualTask.getARJ().get("right_knee");
//                if (dtArjRight != null && dtArjRight != 0) {
//                    if (bestDTARJRight == null || currentTimestamp > latestDTTimestamp) {
//                        bestDTARJRight = dtArjRight;
//                    }
//                }

                float dtBalance = dualTask.getBalanceScore();
                if (assessment.getDualTaskTestDate() != null) {
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
                Float saArjLeft = sensory.getARJ().get("left_knee");
//                if (saArjLeft != null && saArjLeft != 0) {
//                    if (bestSAARJLeft == null || currentTimestamp > latestSATimestamp) {
//                        bestSAARJLeft = saArjLeft;
//                    }
//                }
//
//                Float saArjRight = sensory.getARJ().get("right_knee");
//                if (saArjRight != null && saArjRight != 0) {
//                    if (bestSAARJRight == null || currentTimestamp > latestSATimestamp) {
//                        bestSAARJRight = saArjRight;
//                    }
//                }

                float saBalance = sensory.getBalanceScore();
                if (assessment.getSensoryAtaxiaTestDate() != null) {
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
//            DTARJLeftKneeDTC = comparisons.getDualTaskARJLeftKneeChange();
//            DTARJRightKneeDTC = comparisons.getDualTaskARJRightKneeChange();
            DTBalanceDTC = comparisons.getDualTaskBalanceChange();
            DTVelocityDTC = comparisons.getDualTaskVelocityChange();
            DTPathDTC = comparisons.getDualTaskPathLengthChange();
//            SAARJLeftKneeDTC = comparisons.getSensoryAtaxiaARJLeftKneeChange();
//            SAARJRightKneeDTC = comparisons.getSensoryAtaxiaARJRightKneeChange();
            SABalanceDTC = comparisons.getSensoryAtaxiaBalanceChange();
            SAVelocityDTC = comparisons.getSensoryAtaxiaVelocityChange();
            SAPathDTC = comparisons.getSensoryAtaxiaPathLengthChange();
        }

        if (!hasDataForDate) {
            Toast.makeText(requireContext(), "No assessment data found for " + targetDateStr, Toast.LENGTH_SHORT).show();
            return;
        }

        // Use best values for the selected date
//        ARJLeftKneeToday = bestARJLeft != null ? bestARJLeft : 0f;
//        ARJRightKneeToday = bestARJRight != null ? bestARJRight : 0f;
        balanceToday = bestBalance != null ? bestBalance : 0f;
        swayVelocityToday = bestSwayVelocity != null ? bestSwayVelocity : 0f;
        swayPathLengthToday = bestSwayPathLength != null ? bestSwayPathLength : 0f;
        swayRmsAPToday = bestSwayRmsAP != null ? bestSwayRmsAP : 0f;
        swayRmsMLToday = bestSwayRmsML != null ? bestSwayRmsML : 0f;
        speedToday = bestWalkingSpeed != null ? bestWalkingSpeed : 0f;
        enduranceToday = bestEndurance != null ? bestEndurance : 0f;
//        DTARJLeftKneeToday = bestDTARJLeft != null ? bestDTARJLeft : 0f;
//        DTARJRightKneeToday = bestDTARJRight != null ? bestDTARJRight : 0f;
        DTbalanceToday = bestDTBalance != null ? bestDTBalance : 0f;
        DTswayVelocityToday = bestDTSwayVelocity != null ? bestDTSwayVelocity : 0f;
        DTswayPathLengthToday = bestDTSwayPathLength != null ? bestDTSwayPathLength : 0f;
        DTswayRmsAPToday = bestDTSwayRmsAP != null ? bestDTSwayRmsAP : 0f;
        DTswayRmsMLToday = bestDTSwayRmsML != null ? bestDTSwayRmsML : 0f;
//        SAARJLeftKneeToday = bestSAARJLeft != null ? bestSAARJLeft : 0f;
//        SAARJRightKneeToday = bestSAARJRight != null ? bestSAARJRight : 0f;
        SAbalanceToday = bestSABalance != null ? bestSABalance : 0f;
        SASwayVelocityToday = bestSASwayVelocity != null ? bestSASwayVelocity : 0f;
        SASwayPathLengthToday = bestSASwayPathLength != null ? bestSASwayPathLength : 0f;
        SASwayRmsAPToday = bestSASwayRmsAP != null ? bestSASwayRmsAP : 0f;
        SASwayRmsMLToday = bestSASwayRmsML != null ? bestSASwayRmsML : 0f;

        // Calculate SD and CV
//        float ARJRightKneeSD = standardDeviation(allARJRightValues);
//        float ARJLeftKneeSD = standardDeviation(allARJLeftValues);
        float BalanceSD = standardDeviation(allBalanceValues);
        float EnduranceSD = standardDeviation(allEnduranceValues);
        float WSSD = standardDeviation(allSpeedValues);

//        float ARJLeftKneeCV = (ARJLeftKneeSD / getMean(allARJLeftValues)) * 100;
//        float ARJRightKneeCV = (ARJRightKneeSD / getMean(allARJRightValues)) * 100;
        float BalanceCV = (BalanceSD / getMean(allBalanceValues)) * 100;
        float WSCV = (WSSD / getMean(allSpeedValues)) * 100;
        float EnduranceCV = (EnduranceSD / getMean(allEnduranceValues)) * 100;

        // Create new Report
        ReportModel newReport = new ReportModel();
//        newReport.setArjLeftKneeToday(ARJLeftKneeToday);
//        newReport.setArjRightKneeToday(ARJRightKneeToday);
        newReport.setBalanceToday(balanceToday);
        newReport.setSwayVelocityToday(swayVelocityToday);
        newReport.setSwayPathLengthToday(swayPathLengthToday);
        newReport.setSwayRmsAPToday(swayRmsAPToday);
        newReport.setSwayRmsMLToday(swayRmsMLToday);
        newReport.setSpeedToday(speedToday);
        newReport.setEnduranceToday(enduranceToday);
//        newReport.setSensoryAtaxiaARJLeftToday(SAARJLeftKneeToday);
//        newReport.setSensoryAtaxiaARJRightToday(SAARJRightKneeToday);
        newReport.setSensoryAtaxiaBalanceToday(SAbalanceToday);
        newReport.setSensoryAtaxiaSwayVelocityToday(SASwayVelocityToday);
        newReport.setSensoryAtaxiaSwayPathLengthToday(SASwayPathLengthToday);
        newReport.setSensoryAtaxiaSwayRmsAPToday(SASwayRmsAPToday);
        newReport.setSensoryAtaxiaSwayRmsMLToday(SASwayRmsMLToday);
//        newReport.setDualTaskARJLeftToday(DTARJLeftKneeToday);
//        newReport.setDualTaskARJRightToday(DTARJRightKneeToday);
        newReport.setDualTaskBalanceToday(DTbalanceToday);
        newReport.setDualTaskSwayVelocityToday(DTswayVelocityToday);
        newReport.setDualTaskSwayPathLengthToday(DTswayPathLengthToday);
        newReport.setDualTaskSwayRmsAPToday(DTswayRmsAPToday);
        newReport.setDualTaskSwayRmsMLToday(DTswayRmsMLToday);
        // DTC
//        newReport.setDualTaskDTCARJLeft(DTARJLeftKneeDTC);
//        newReport.setDualTaskDTCARJRight(DTARJRightKneeDTC);
        newReport.setDualTaskDTCBalance(DTBalanceDTC);
        newReport.setDualTaskDTCSwayVelocity(DTVelocityDTC);
        newReport.setDualTaskDTCSwayPathLength(DTPathDTC);
//        newReport.setSensoryAtaxiaDTCARJLeft(SAARJLeftKneeDTC);
//        newReport.setSensoryAtaxiaDTCARJRight(SAARJRightKneeDTC);
        newReport.setSensoryAtaxiaDTCBalance(SABalanceDTC);
        newReport.setSensoryAtaxiaDTCSwayVelocity(SAVelocityDTC);
        newReport.setSensoryAtaxiaDTCSwayPathLength(SAPathDTC);
        // SD & CV
//        newReport.setARJLeftSD(ARJLeftKneeSD);
//        newReport.setARJRightSD(ARJRightKneeSD);
        newReport.setBalanceSD(BalanceSD);
        newReport.setEnduranceSD(EnduranceSD);
        newReport.setSpeedSD(WSSD);
//        newReport.setARJLeftCV(ARJLeftKneeCV);
//        newReport.setARJRightCV(ARJRightKneeCV);
        newReport.setBalanceCV(BalanceCV);
        newReport.setEnduranceCV(EnduranceCV);
        newReport.setSpeedCV(WSCV);

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

    private void createNewReport() throws IOException {

        float ARJLeftKneeSD = 0;
        float ARJRightKneeSD = 0;
        float BalanceSD = 0;
        float WSSD = 0;
        float EnduranceSD = 0;

        float ARJLeftKneeCV = 0;
        float ARJRightKneeCV = 0;
        float BalanceCV = 0;
        float WSCV = 0;
        float EnduranceCV = 0;

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

        // Track most recent values per metric (same as HomeFragment)
        Float bestARJLeft = null;
        Float bestARJRight = null;
        Float bestBalance = null;
        Float bestWalkingSpeed = null;
        Float bestEndurance = null;

        long latestTimestamp = 0;

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

        float swayVelocityToday = 0;
        float swayPathLengthToday = 0;
        float swayRmsAPToday = 0;
        float swayRmsMLToday = 0;
        float DTswayVelocityToday = 0;
        float DTswayPathLengthToday = 0;
        float DTswayRmsAPToday = 0;
        float DTswayRmsMLToday = 0;
        float SASwayVelocityToday = 0;
        float SASwayPathLengthToday = 0;
        float SASwayRmsAPToday = 0;
        float SASwayRmsMLToday = 0;

        float DTARJLeftKneeDTC = 0;
        float DTARJRightKneeDTC = 0;
        float DTBalanceDTC = 0;
        float DTVelocityDTC = 0;
        float DTPathDTC = 0;
        float SAARJLeftKneeDTC = 0;
        float SAARJRightKneeDTC = 0;
        float SABalanceDTC = 0;
        float SAVelocityDTC = 0;
        float SAPathDTC = 0;

        // Track best Dual Task values (most recent)
        Float bestDTARJLeft = null;
        Float bestDTARJRight = null;
        Float bestDTBalance = null;
        Float bestSAARJLeft = null;
        Float bestSAARJRight = null;
        Float bestSABalance = null;
        // Extended sway metrics (latest assessment wins, same pattern as balance)
        Float bestSwayVelocity = null;
        Float bestSwayPathLength = null;
        Float bestSwayRmsAP = null;
        Float bestSwayRmsML = null;
        Float bestDTSwayVelocity = null;
        Float bestDTSwayPathLength = null;
        Float bestDTSwayRmsAP = null;
        Float bestDTSwayRmsML = null;
        Float bestSASwayVelocity = null;
        Float bestSASwayPathLength = null;
        Float bestSASwayRmsAP = null;
        Float bestSASwayRmsML = null;

        long latestDTTimestamp = 0;
        long latestSATimestamp = 0;

        int sumARJRight = 0, sumARJLeft = 0, sumBalance = 0, sumWS = 0, sumEndurance = 0;
        ArrayList<Float> setARJRight = new ArrayList<>();
        ArrayList<Float> setARJLeft = new ArrayList<>();
        ArrayList<Float> setBalance = new ArrayList<>();
        ArrayList<Float> setWS = new ArrayList<>();
        ArrayList<Float> setEndurance = new ArrayList<>();

        // SD and Cv values
        ArrayList<Float> allARJLeftValues = new ArrayList<>();
        ArrayList<Float> allARJRightValues = new ArrayList<>();
        ArrayList<Float> allBalanceValues = new ArrayList<>();
        ArrayList<Float> allSpeedValues = new ArrayList<>();
        ArrayList<Float> allEnduranceValues = new ArrayList<>();

        for (AssessmentData assessment : allAssessment) {
            String currentDate = sdf.format(assessment.getTimestamp());

            Float arjLeftSDCV = assessment.getBaseline().getARJ().get("left_knee");
            if (arjLeftSDCV != null && arjLeftSDCV != 0) {
                allARJLeftValues.add(arjLeftSDCV);
            }

            Float arjRightSDCV = assessment.getBaseline().getARJ().get("right_knee");
            if (arjRightSDCV != null && arjRightSDCV != 0) {
                allARJRightValues.add(arjRightSDCV);
            }

            float balanceSDCV = assessment.getBaseline().getBalanceScore();
            if (assessment.getSwayAreaAndBalanceTestDate() != null) {
                allBalanceValues.add(balanceSDCV);
            }

            float walkingSpeedSDCV = assessment.getBaseline().getWalkingSpeed();
            if (assessment.getWSAndEnduranceTestDate() != null) {
                allSpeedValues.add(walkingSpeedSDCV);
            }

            float enduranceSDCV = assessment.getBaseline().getEnduranceScore();
            if (assessment.getWSAndEnduranceTestDate() != null) {
                allEnduranceValues.add(enduranceSDCV);
            }

            if (!currentDate.equals(todayStr)) continue;

            long currentTimestamp = assessment.getTimestamp().getTime();

            // Just like HomeFragment - directly access getBaseline()
            Float arjLeft = assessment.getBaseline().getARJ().get("left_knee");
            if (arjLeft != null && arjLeft != 0) {
                if (bestARJLeft == null || currentTimestamp > latestTimestamp) {
                    bestARJLeft = arjLeft;
                }
                setARJLeft.add(arjLeft);
            }

            Float arjRight = assessment.getBaseline().getARJ().get("right_knee");
            if (arjRight != null && arjRight != 0) {
                if (bestARJRight == null || currentTimestamp > latestTimestamp) {
                    bestARJRight = arjRight;
                }
                setARJRight.add(arjRight);
            }

            float balance = assessment.getBaseline().getBalanceScore();
            if (assessment.getSwayAreaAndBalanceTestDate() != null) {
                if (bestBalance == null || currentTimestamp > latestTimestamp) {
                    bestBalance = balance;
                }
                if (bestSwayVelocity == null || currentTimestamp > latestTimestamp) {
                    bestSwayVelocity = assessment.getBaseline().getSwayMeanVelocity();
                    bestSwayPathLength = assessment.getBaseline().getSwayPathLength();
                    bestSwayRmsAP = assessment.getBaseline().getSwayRmsAP();
                    bestSwayRmsML = assessment.getBaseline().getSwayRmsML();
                }
                setBalance.add(balance);
            }

            float walkingSpeed = assessment.getBaseline().getWalkingSpeed();
            if (assessment.getWSAndEnduranceTestDate() != null) {
                if (bestWalkingSpeed == null || currentTimestamp > latestTimestamp) {
                    bestWalkingSpeed = walkingSpeed;
                }
                setWS.add(walkingSpeed);
            }

            float endurance = assessment.getBaseline().getEnduranceScore();
            if (assessment.getWSAndEnduranceTestDate() != null) {
                if (bestEndurance == null || currentTimestamp > latestTimestamp) {
                    bestEndurance = endurance;
                }
                setEndurance.add(endurance);
            }

            if (currentTimestamp > latestTimestamp) {
                latestTimestamp = currentTimestamp;
            }

            DualTaskAssessment dualTask = assessment.getDualTask();
            if (dualTask != null) {
                Float dtArjLeft = dualTask.getARJ().get("left_knee");
                if (dtArjLeft != null && dtArjLeft != 0) {
                    if (bestDTARJLeft == null || currentTimestamp > latestDTTimestamp) {
                        bestDTARJLeft = dtArjLeft;
                    }
                }

                Float dtArjRight = dualTask.getARJ().get("right_knee");
                if (dtArjRight != null && dtArjRight != 0) {
                    if (bestDTARJRight == null || currentTimestamp > latestDTTimestamp) {
                        bestDTARJRight = dtArjRight;
                    }
                }

                float dtBalance = dualTask.getBalanceScore();
                if (assessment.getDualTaskTestDate() != null) {
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

            // === DTC (Dual Task Cost) from Comparisons ===
            TestComparisons comparisons = assessment.getComparisons();
            if (comparisons != null) {
                DTARJLeftKneeDTC = comparisons.getDualTaskARJLeftKneeChange();
                DTARJRightKneeDTC = comparisons.getDualTaskARJRightKneeChange();
                DTBalanceDTC = comparisons.getDualTaskBalanceChange();
                DTVelocityDTC = comparisons.getDualTaskVelocityChange();
                DTPathDTC = comparisons.getDualTaskPathLengthChange();

                SAARJLeftKneeDTC = comparisons.getSensoryAtaxiaARJLeftKneeChange();
                SAARJRightKneeDTC = comparisons.getSensoryAtaxiaARJRightKneeChange();
                SABalanceDTC = comparisons.getSensoryAtaxiaBalanceChange();
                SAVelocityDTC = comparisons.getSensoryAtaxiaVelocityChange();
                SAPathDTC = comparisons.getSensoryAtaxiaPathLengthChange();
            }

            SensoryAtaxiaAssessment sensory = assessment.getSensoryAtaxia();
            if (sensory != null) {
                // Similar logic for sensory data
                Float saArjLeft = sensory.getARJ().get("left_knee");
                if (saArjLeft != null && saArjLeft != 0) {
                    if (bestSAARJLeft == null || currentTimestamp > latestSATimestamp) {
                        bestSAARJLeft = saArjLeft;
                    }
                }

                // Add for right and balance similarly
                if (bestSASwayVelocity == null || currentTimestamp > latestSATimestamp) {
                    bestSASwayVelocity = sensory.getSwayMeanVelocity();
                    bestSASwayPathLength = sensory.getSwayPathLength();
                    bestSASwayRmsAP = sensory.getSwayRmsAP();
                    bestSASwayRmsML = sensory.getSwayRmsML();
                }
            }

            // === DTC DATA FROM COMPARISONS ===
            if (comparisons != null) {
                // For DTC, you usually want the most recent values
                if (currentTimestamp > latestTimestamp) {
                    DTARJLeftKneeDTC = comparisons.getDualTaskARJLeftKneeChange();
                    DTARJRightKneeDTC = comparisons.getDualTaskARJRightKneeChange();
                    DTBalanceDTC = comparisons.getDualTaskBalanceChange();
                    DTVelocityDTC = comparisons.getDualTaskVelocityChange();
                    DTPathDTC = comparisons.getDualTaskPathLengthChange();

                    SAARJLeftKneeDTC = comparisons.getSensoryAtaxiaARJLeftKneeChange();
                    SAARJRightKneeDTC = comparisons.getSensoryAtaxiaARJRightKneeChange();
                    SABalanceDTC = comparisons.getSensoryAtaxiaBalanceChange();
                    SAVelocityDTC = comparisons.getSensoryAtaxiaVelocityChange();
                    SAPathDTC = comparisons.getSensoryAtaxiaPathLengthChange();
                }
            }

            if (currentTimestamp > latestTimestamp) {
                latestTimestamp = currentTimestamp;
            }
        }

        // Use best values
        ARJLeftKneeToday = bestARJLeft != null ? bestARJLeft : 0f;
        ARJRightKneeToday = bestARJRight != null ? bestARJRight : 0f;
        balanceToday = bestBalance != null ? bestBalance : 0f;
        swayVelocityToday = bestSwayVelocity != null ? bestSwayVelocity : 0f;
        swayPathLengthToday = bestSwayPathLength != null ? bestSwayPathLength : 0f;
        swayRmsAPToday = bestSwayRmsAP != null ? bestSwayRmsAP : 0f;
        swayRmsMLToday = bestSwayRmsML != null ? bestSwayRmsML : 0f;
        speedToday = bestWalkingSpeed != null ? bestWalkingSpeed : 0f;
        enduranceToday = bestEndurance != null ? bestEndurance : 0f;

        // Set Dual Task values
        DTARJLeftKneeToday = bestDTARJLeft != null ? bestDTARJLeft : 0f;
        DTARJRightKneeToday = bestDTARJRight != null ? bestDTARJRight : 0f;
        DTbalanceToday = bestDTBalance != null ? bestDTBalance : 0f;
        DTswayVelocityToday = bestDTSwayVelocity != null ? bestDTSwayVelocity : 0f;
        DTswayPathLengthToday = bestDTSwayPathLength != null ? bestDTSwayPathLength : 0f;
        DTswayRmsAPToday = bestDTSwayRmsAP != null ? bestDTSwayRmsAP : 0f;
        DTswayRmsMLToday = bestDTSwayRmsML != null ? bestDTSwayRmsML : 0f;

        // Set Sensory Ataxia values
        SAARJLeftKneeToday = bestSAARJLeft != null ? bestSAARJLeft : 0f;
        SAARJRightKneeToday = bestSAARJRight != null ? bestSAARJRight : 0f;
        SAbalanceToday = bestSABalance != null ? bestSABalance : 0f;
        SASwayVelocityToday = bestSASwayVelocity != null ? bestSASwayVelocity : 0f;
        SASwayPathLengthToday = bestSASwayPathLength != null ? bestSASwayPathLength : 0f;
        SASwayRmsAPToday = bestSASwayRmsAP != null ? bestSASwayRmsAP : 0f;
        SASwayRmsMLToday = bestSASwayRmsML != null ? bestSASwayRmsML : 0f;

        // Calculating ARJ and balance
        // STANDARD DEVIATION (SD)
        ARJRightKneeSD = standardDeviation(allARJRightValues);
        ARJLeftKneeSD = standardDeviation(allARJLeftValues);
        BalanceSD = standardDeviation(allBalanceValues);
        EnduranceSD = standardDeviation(allEnduranceValues);
        WSSD = standardDeviation(allSpeedValues);

        // COEFFICIENT OF VARIATION (CV)
        ARJLeftKneeCV = (ARJLeftKneeSD / getMean(allARJLeftValues)) * 100;
        ARJRightKneeCV = (ARJRightKneeSD / getMean(allARJRightValues)) * 100;
        BalanceCV = (BalanceSD / getMean(allBalanceValues)) * 100;
        WSCV = (WSSD / getMean(allSpeedValues)) * 100;
        EnduranceCV = (EnduranceSD / getMean(allEnduranceValues)) * 100;

        // 2. Create a new Report object with the data
        ReportModel newReport = new ReportModel();
        newReport.setArjLeftKneeToday(ARJLeftKneeToday);
        newReport.setArjRightKneeToday(ARJRightKneeToday);
        newReport.setBalanceToday(balanceToday);
        newReport.setSwayVelocityToday(swayVelocityToday);
        newReport.setSwayPathLengthToday(swayPathLengthToday);
        newReport.setSwayRmsAPToday(swayRmsAPToday);
        newReport.setSwayRmsMLToday(swayRmsMLToday);
        newReport.setSpeedToday(speedToday);
        newReport.setEnduranceToday(enduranceToday);
        newReport.setSensoryAtaxiaARJLeftToday(SAARJLeftKneeToday);
        newReport.setSensoryAtaxiaARJRightToday(SAARJRightKneeToday);
        newReport.setSensoryAtaxiaBalanceToday(SAbalanceToday);
        newReport.setSensoryAtaxiaSwayVelocityToday(SASwayVelocityToday);
        newReport.setSensoryAtaxiaSwayPathLengthToday(SASwayPathLengthToday);
        newReport.setSensoryAtaxiaSwayRmsAPToday(SASwayRmsAPToday);
        newReport.setSensoryAtaxiaSwayRmsMLToday(SASwayRmsMLToday);
        newReport.setDualTaskARJLeftToday(DTARJLeftKneeToday);
        newReport.setDualTaskARJRightToday(DTARJRightKneeToday);
        newReport.setDualTaskBalanceToday(DTbalanceToday);
        newReport.setDualTaskSwayVelocityToday(DTswayVelocityToday);
        newReport.setDualTaskSwayPathLengthToday(DTswayPathLengthToday);
        newReport.setDualTaskSwayRmsAPToday(DTswayRmsAPToday);
        newReport.setDualTaskSwayRmsMLToday(DTswayRmsMLToday);
        // DTC
        newReport.setDualTaskDTCARJLeft(DTARJLeftKneeDTC);
        newReport.setDualTaskDTCARJRight(DTARJRightKneeDTC);
        newReport.setDualTaskDTCBalance(DTBalanceDTC);
        newReport.setDualTaskDTCSwayVelocity(DTVelocityDTC);
        newReport.setDualTaskDTCSwayPathLength(DTPathDTC);
        newReport.setSensoryAtaxiaDTCARJLeft(SAARJLeftKneeDTC);
        newReport.setSensoryAtaxiaDTCARJRight(SAARJRightKneeDTC);
        newReport.setSensoryAtaxiaDTCBalance(SABalanceDTC);
        newReport.setSensoryAtaxiaDTCSwayVelocity(SAVelocityDTC);
        newReport.setSensoryAtaxiaDTCSwayPathLength(SAPathDTC);
        // SD & CV
        newReport.setARJLeftSD(ARJLeftKneeSD);
        newReport.setARJRightSD(ARJRightKneeSD);
        newReport.setBalanceSD(BalanceSD);
        newReport.setEnduranceSD(EnduranceSD);
        newReport.setSpeedSD(WSSD);
        newReport.setARJLeftCV(ARJLeftKneeCV);
        newReport.setARJRightCV(ARJRightKneeCV);
        newReport.setBalanceCV(BalanceCV);
        newReport.setEnduranceCV(EnduranceCV);
        newReport.setSpeedCV(WSCV);

        Date now = new Date();
        newReport.setTimeStamp(now);

        // TODO: SAVE DATA TO DATA STORAGE MANAGER
        // Preview
        String preview = String.format(Locale.US,
                "Speed: %f m/s | Sway Area: %f mm² | ARJ: %f m/s³| Endurance: %f s",
                speedToday, balanceToday, ARJRightKneeToday, enduranceToday);
        newReport.setPreview(preview);

        Date date = new Date(); // current date
//        SimpleDateFormat sdf1 = new SimpleDateFormat("MMM dd yyyy", Locale.getDefault());
        SimpleDateFormat sdf1 = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        String formattedDate = sdf1.format(date); // "Oct 24 2025"
        newReport.setDate(formattedDate);

        // Save report
        DataStorageManager.saveReport(requireContext(), newReport);

        // Reload all reports to refresh the list
        loadAllReports();

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

    public float standardDeviation(ArrayList<Float> set) {
        if (set == null || set.size() < 2) {
            return 0f;
        }

        // Step 1: Calculate mean
        float sum = 0;
        for (float value : set) {
            sum += value;
        }
        float mean = sum / set.size();

        // Step 2: Calculate sum of squared differences
        float sumSquaredDiff = 0;
        for (float value : set) {
            float diff = value - mean;
            sumSquaredDiff += diff * diff;
        }

        // Step 3: Variance and standard deviation
        float variance = sumSquaredDiff / set.size(); // Population variance
        return (float) Math.sqrt(variance);
    }

    private void showDeleteReportDialog() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    // ✅ Use the SAME format as stored in reports
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