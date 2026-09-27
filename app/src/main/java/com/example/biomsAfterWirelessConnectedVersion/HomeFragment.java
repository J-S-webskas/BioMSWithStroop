package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    Button recordButton;
    TextView todoListSWB, todoListWSE, reportPreview, todoListSA, todoListDT, latestProgression;
    private BottomNavigationView bottomNavigationView;


    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recordButton = view.findViewById(R.id.recordButton);
        todoListSWB = view.findViewById(R.id.todoListSWB);
        todoListWSE = view.findViewById(R.id.todoListWSE);
        todoListSA = view.findViewById(R.id.todoListSA);
        todoListDT = view.findViewById(R.id.todoListDT);
        reportPreview = view.findViewById(R.id.reportPreview);
        latestProgression = view.findViewById(R.id.latestProgression);
        recordButton.setHapticFeedbackEnabled(true);
        ImageView profile = view.findViewById(R.id.profile);

        setUpToDo();

        double mass = DataStorageManager.getMass(requireContext());
        String sex = DataStorageManager.getSex(requireContext());
        if (mass == 0) {
            showWeightDialog();
        }

        if (sex == null || sex.isEmpty() || sex.equals("Not Found")) {
            showSexDialog();
        }

        profile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), Profile.class);
            startActivity(intent);
        });

        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(requireContext());
        if (allAssessment != null && !allAssessment.isEmpty()) {
            // Filter to today
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            String todayStr = sdf.format(new Date());
//            Float bestARJLeft = null;
            Float bestARJRight = null;
            Float bestBalance = null;
            Float bestWalkingSpeed = null;
            Float bestEndurance = null;
            long latestTimestampForMetric = 0;

            for (AssessmentData assessment : allAssessment) {
                String assessmentDate = sdf.format(assessment.getTimestamp());
                if (!assessmentDate.equals(todayStr)) continue;

                // For each metric, take the most recent non-null/non-zero value
                if (assessment.getBaseline() != null && assessment.getBaseline().getARJ() != null) {
//                    Float arjLeft = assessment.getBaseline().getARJ().get("left_knee");
//                    if (arjLeft != null && arjLeft != 0) {
//                        if (bestARJLeft == null || assessment.getTimestamp().getTime() > latestTimestampForMetric) {
//                            bestARJLeft = arjLeft;
//                        }
//                    }

//                    Float arjRight = assessment.getBaseline().getARJ().get("right_knee");
//                    if (arjRight != null && arjRight != 0) {
//                        if (bestARJRight == null || assessment.getTimestamp().getTime() > latestTimestampForMetric) {
//                            bestARJRight = arjRight;
//                        }
//                    }

                    float balance = assessment.getBaseline().getBalanceScore();
                    if (balance != 0) {
                        if (bestBalance == null || assessment.getTimestamp().getTime() > latestTimestampForMetric) {
                            bestBalance = balance;
                            latestTimestampForMetric = assessment.getTimestamp().getTime();
                        }
                    }

                    float walkingSpeed = assessment.getBaseline().getWalkingSpeed();
                    if (walkingSpeed != 0) {
                        if (bestWalkingSpeed == null || assessment.getTimestamp().getTime() > latestTimestampForMetric) {
                            bestWalkingSpeed = walkingSpeed;
                            latestTimestampForMetric = assessment.getTimestamp().getTime();
                        }
                    }

                    float endurance = assessment.getBaseline().getEnduranceScore();
                    if (endurance != 0) {
                        if (bestEndurance == null || assessment.getTimestamp().getTime() > latestTimestampForMetric) {
                            bestEndurance = endurance;
                            latestTimestampForMetric = assessment.getTimestamp().getTime();
                        }
                    }
                }
            }

            // Use best values (fall back to 0 if null)
//            float arjLeft = bestARJLeft != null ? bestARJLeft : 0f;
//            float arjRight = bestARJRight != null ? bestARJRight : 0f;
            float balance = bestBalance != null ? bestBalance : 0f;
            float ws = bestWalkingSpeed != null ? bestWalkingSpeed : 0f;
            float endurance = bestEndurance != null ? bestEndurance : 0f;

            latestProgression.setText(String.format(Locale.US,
                    "Speed: %f m/s\nSway Area: %f mm²\nEndurance: %f s",
                    ws, balance, endurance));
        }

        List<ReportModel> allReport = DataStorageManager.loadAllReports(requireContext());
        ReportModel latestReport = null;
        if (allReport != null && !allReport.isEmpty()) {
            latestReport = allReport.get(0);
        }

        if (latestReport != null) {
            float speedToday = latestReport.getSpeedCV();
            float balanceToday = latestReport.getBalanceCV();
//            float arjLeftToday = latestReport.getARJLeftCV();
//            float arjRightToday = latestReport.getARJRightCV();
            float enduranceToday = latestReport.getEnduranceCV();
            reportPreview.setText(String.format(Locale.US,
                    "Speed: %f %%\nSway Area: %f %%\nEndurance: %f %%",
                    speedToday, balanceToday, enduranceToday));
        }

        // Navigating to record fragment
        recordButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                ReportFragment reportFragment = new ReportFragment();
                AssessmentFragment assessmentFragment = new AssessmentFragment();

                // UPDATE BOTTOM NAVIGATION TO ASSESSMENT TAB
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).selectBottomNavItem(R.id.nav_record);
                }
            }
        });
        // Inflate the layout for this fragment
        return view;
    }


    private void setUpToDo() {
        todoListSWB.setVisibility(View.VISIBLE);
        todoListWSE.setVisibility(View.VISIBLE);
        todoListDT.setVisibility(View.VISIBLE);
        todoListSA.setVisibility(View.VISIBLE);

        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(requireContext());
        if (allAssessment.isEmpty()) {;
            return;
        }

        AssessmentData newest = allAssessment.get(0);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        String todayStr = sdf.format(new Date());

        // Loading data
        for (AssessmentData assessment : allAssessment) {
            String currentDate = sdf.format(assessment.getTimestamp());
            if (!currentDate.equals(todayStr)) continue;

            // Check INDIVIDUAL test dates, not the combined status
            if (assessment.getSwayAreaAndBalanceTestDate() != null) {
                todoListSWB.setVisibility(View.GONE);
            }
            if (assessment.getWSAndEnduranceTestDate() != null) {
                todoListWSE.setVisibility(View.GONE);
            }
            if (assessment.getDualTaskTestDate() != null) {
                todoListDT.setVisibility(View.GONE);
            }
            if (assessment.getSensoryAtaxiaTestDate() != null) {
                todoListSA.setVisibility(View.GONE);
            }
        }
    }

    private void showSexDialog() {
        // Create the dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Update Gender")
                .setMessage("Select your gender")
                .setPositiveButton("Save", null); // Set null initially

        // Inflate custom view
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sex, null);
        RadioGroup sexRadioGroup = dialogView.findViewById(R.id.sexRadioGroup);
        RadioButton maleRadio = dialogView.findViewById(R.id.maleRadio);
        RadioButton femaleRadio = dialogView.findViewById(R.id.femaleRadio);
        RadioButton otherRadio = dialogView.findViewById(R.id.otherRadio);

        // Set current selection
        String currentSex = DataStorageManager.getSex(requireContext());
        if ("Male".equalsIgnoreCase(currentSex)) {
            maleRadio.setChecked(true);
        } else if ("Female".equalsIgnoreCase(currentSex)) {
            femaleRadio.setChecked(true);
        } else if ("Other".equalsIgnoreCase(currentSex)) {
            otherRadio.setChecked(true);
        }

        builder.setView(dialogView);

        // Create dialog to set custom button behavior
        AlertDialog dialog = builder.create();

        // Override positive button behavior to validate input
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                int selectedId = sexRadioGroup.getCheckedRadioButtonId();
                String selectedSex = "";

                if (selectedId == R.id.maleRadio) {
                    selectedSex = "Male";
                } else if (selectedId == R.id.femaleRadio) {
                    selectedSex = "Female";
                } else if (selectedId == R.id.otherRadio) {
                    selectedSex = "Other";
                }

                if (selectedSex.isEmpty()) {
                    Toast.makeText(requireContext(), "Please select a gender", Toast.LENGTH_SHORT).show();
                    return; // Keep dialog open
                }

                try {
                    DataStorageManager.setSex(requireContext(), selectedSex);
                    Toast.makeText(requireContext(), "Gender saved: " + selectedSex, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "Error saving gender", Toast.LENGTH_SHORT).show();
                }
            });
        });

        builder.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false); // Prevent cancel by touching outside
        dialog.show();
    }

    private void showWeightDialog() {
        // Create the dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Update Weight")
                .setMessage("Enter your current weight")
                .setPositiveButton("Save", null); // Set null initially

        // Inflate custom view
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_weight, null);
        EditText weightInput = dialogView.findViewById(R.id.weightInput);

        // Set current weight
        double currentWeight = DataStorageManager.getMass(requireContext());
        weightInput.setText(String.valueOf(currentWeight));

        builder.setView(dialogView);

        // Create dialog to set custom button behavior
        AlertDialog dialog = builder.create();

        // Override positive button behavior to validate input
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String weightStr = weightInput.getText().toString();
                if (weightStr.isEmpty()) {
                    weightInput.setError("Please enter a weight (kg)");
                    return;
                }
                try {
                    double newWeight = Double.parseDouble(weightStr);
                    if (newWeight <= 0) {
                        weightInput.setError("Weight must be positive");
                        return;
                    }
                    DataStorageManager.setMass(requireContext(), newWeight);
                    Toast.makeText(requireContext(), "Weight saved: " + newWeight + " kg", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    showWeightDialog();
                    weightInput.setError("Invalid number format");
                } catch (IOException e) {
                    e.printStackTrace();
                    showWeightDialog();
                }
            });
        });
        builder.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false); // Prevent cancel by touching outside
        dialog.show();
    }
}