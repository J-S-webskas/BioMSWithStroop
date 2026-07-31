package com.example.biomsAfterWirelessConnectedVersion;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;

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
                try {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
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
            // Today
            intent.putExtra("date", report.getDate());  // Convert Date to long
            intent.putExtra("speed", report.getSpeedToday());
            intent.putExtra("arjRight", report.getArjRightKneeToday());
            intent.putExtra("arjLeft", report.getArjLeftKneeToday());
            intent.putExtra("balance", report.getBalanceToday());
            intent.putExtra("endurance", report.getEnduranceToday());
            intent.putExtra("sensoryAtaxiaARJLeft", report.getSensoryAtaxiaARJLeftToday());
            intent.putExtra("sensoryAtaxiaARJRight", report.getSensoryAtaxiaARJRightToday());
            intent.putExtra("sensoryAtaxiaBalance", report.getSensoryAtaxiaBalanceToday());
            intent.putExtra("dualTaskARJLeft", report.getDualTaskARJLeftToday());
            intent.putExtra("dualTaskARJRight", report.getDualTaskARJRightToday());
            intent.putExtra("dualTaskBalance", report.getDualTaskBalanceToday());
            // DTC
            intent.putExtra("dualTaskARJLeftDTC", report.getDualTaskDTCARJLeft());
            intent.putExtra("dualTaskARJRightDTC", report.getDualTaskDTCARJRight());
            intent.putExtra("dualTaskBalanceDTC", report.getDualTaskDTCBalance());
            intent.putExtra("sensoryAtaxiaARJLeftDTC", report.getSensoryAtaxiaDTCARJLeft());
            intent.putExtra("sensoryAtaxiaARJRightDTC", report.getSensoryAtaxiaDTCARJRight());
            intent.putExtra("sensoryAtaxiaBalanceDTC", report.getSensoryAtaxiaDTCBalance());
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

        float DTARJLeftKneeDTC = 0;
        float DTARJRightKneeDTC = 0;
        float DTBalanceDTC = 0;
        float SAARJLeftKneeDTC = 0;
        float SAARJRightKneeDTC = 0;
        float SABalanceDTC = 0;

        // Track best Dual Task values (most recent)
        Float bestDTARJLeft = null;
        Float bestDTARJRight = null;
        Float bestDTBalance = null;
        Float bestSAARJLeft = null;
        Float bestSAARJRight = null;
        Float bestSABalance = null;

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
            if (balanceSDCV != 0) {
                allBalanceValues.add(balanceSDCV);
            }

            float walkingSpeedSDCV = assessment.getBaseline().getWalkingSpeed();
            if (walkingSpeedSDCV != 0) {
                allSpeedValues.add(walkingSpeedSDCV);
            }

            float enduranceSDCV = assessment.getBaseline().getEnduranceScore();
            if (enduranceSDCV != 0) {
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
            if (balance != 0) {
                if (bestBalance == null || currentTimestamp > latestTimestamp) {
                    bestBalance = balance;
                }
                setBalance.add(balance);
            }

            float walkingSpeed = assessment.getBaseline().getWalkingSpeed();
            if (walkingSpeed != 0) {
                if (bestWalkingSpeed == null || currentTimestamp > latestTimestamp) {
                    bestWalkingSpeed = walkingSpeed;
                }
                setWS.add(walkingSpeed);
            }

            float endurance = assessment.getBaseline().getEnduranceScore();
            if (endurance != 0) {
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
                if (dtBalance != 0) {
                    if (bestDTBalance == null || currentTimestamp > latestDTTimestamp) {
                        bestDTBalance = dtBalance;
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

                SAARJLeftKneeDTC = comparisons.getSensoryAtaxiaARJLeftKneeChange();
                SAARJRightKneeDTC = comparisons.getSensoryAtaxiaARJRightKneeChange();
                SABalanceDTC = comparisons.getSensoryAtaxiaBalanceChange();
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
            }

            // === DTC DATA FROM COMPARISONS ===
            if (comparisons != null) {
                // For DTC, you usually want the most recent values
                if (currentTimestamp > latestTimestamp) {
                    DTARJLeftKneeDTC = comparisons.getDualTaskARJLeftKneeChange();
                    DTARJRightKneeDTC = comparisons.getDualTaskARJRightKneeChange();
                    DTBalanceDTC = comparisons.getDualTaskBalanceChange();

                    SAARJLeftKneeDTC = comparisons.getSensoryAtaxiaARJLeftKneeChange();
                    SAARJRightKneeDTC = comparisons.getSensoryAtaxiaARJRightKneeChange();
                    SABalanceDTC = comparisons.getSensoryAtaxiaBalanceChange();
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
        speedToday = bestWalkingSpeed != null ? bestWalkingSpeed : 0f;
        enduranceToday = bestEndurance != null ? bestEndurance : 0f;

        // Set Dual Task values
        DTARJLeftKneeToday = bestDTARJLeft != null ? bestDTARJLeft : 0f;
        DTARJRightKneeToday = bestDTARJRight != null ? bestDTARJRight : 0f;
        DTbalanceToday = bestDTBalance != null ? bestDTBalance : 0f;

        // Set Sensory Ataxia values
        SAARJLeftKneeToday = bestSAARJLeft != null ? bestSAARJLeft : 0f;
        SAARJRightKneeToday = bestSAARJRight != null ? bestSAARJRight : 0f;
        SAbalanceToday = bestSABalance != null ? bestSABalance : 0f;

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
        newReport.setSpeedToday(speedToday);
        newReport.setEnduranceToday(enduranceToday);
        newReport.setSensoryAtaxiaARJLeftToday(SAARJLeftKneeToday);
        newReport.setSensoryAtaxiaARJRightToday(SAARJRightKneeToday);
        newReport.setSensoryAtaxiaBalanceToday(SAbalanceToday);
        newReport.setDualTaskARJLeftToday(DTARJLeftKneeToday);
        newReport.setDualTaskARJRightToday(DTARJRightKneeToday);
        newReport.setDualTaskBalanceToday(DTbalanceToday);
        // DTC
        newReport.setDualTaskDTCARJLeft(DTARJLeftKneeDTC);
        newReport.setDualTaskDTCARJRight(DTARJRightKneeDTC);
        newReport.setDualTaskDTCBalance(DTBalanceDTC);
        newReport.setSensoryAtaxiaDTCARJLeft(SAARJLeftKneeDTC);
        newReport.setSensoryAtaxiaDTCARJRight(SAARJRightKneeDTC);
        newReport.setSensoryAtaxiaDTCBalance(SABalanceDTC);
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

    @Override
    public void onResume() {
        super.onResume();
        // Refresh list when returning to this fragment
        loadAllReports();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
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
}