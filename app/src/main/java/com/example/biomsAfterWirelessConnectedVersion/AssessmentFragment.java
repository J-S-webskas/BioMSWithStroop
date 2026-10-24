package com.example.biomsAfterWirelessConnectedVersion;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.Locale;

public class AssessmentFragment extends Fragment {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_assessment, container, false);

        setupTestControls(view);

        return view;
    }

    private void setupTestControls(View view) {
        CardView startTest = view.findViewById(R.id.cardStartTest);
        Button openSeeInstructions = view.findViewById(R.id.openSeeInstructions);
        CardView explain = view.findViewById(R.id.explain);
//        Button previewTestMode = view.findViewById(R.id.previewTestButton);
        CardView delete = view.findViewById(R.id.delete);
        ImageButton goOnStartTest = view.findViewById(R.id.goOnStartTest);
        ImageButton startTestIcon = view.findViewById(R.id.startTestIcon);
        ImageButton goOnExplain = view.findViewById(R.id.goOnExplain);
        ImageButton explainIcon = view.findViewById(R.id.explainIcon);
        ImageButton deleteOpen = view.findViewById(R.id.deleteOpen);

        deleteOpen.setOnClickListener(v -> {
            showDeleteAssessmentDialog();
        });

        delete.setOnClickListener(v -> {
            showDeleteAssessmentDialog();
        });

        openSeeInstructions.setOnClickListener(v -> {
            // call  hw will you record, which navigates to MarkDistance, but need to stop it once it discovers that this is just instructions
            Intent intent = new Intent(requireContext(), HowWillYouRecord.class);
            intent.putExtra("seeInstructionsOnly", true);
            Log.d("Coming from assessment fragment, opening the instructions", "opening instructions, passed seeInstructions as key and value true");
            startActivity(intent);
        });

        startTest.setOnClickListener(v -> {
            // navigate to choose_test activity
            Intent intent = new Intent(requireContext(), ChooseTestActivity.class);
            startActivity(intent);
        });

        explain.setOnClickListener(v -> {
            // navigate to choose_test activity
            Intent intent = new Intent(requireContext(), TestMeaning.class);
            startActivity(intent);
        });

        goOnExplain.setOnClickListener(v -> {
            // navigate to choose_test activity
            Intent intent = new Intent(requireContext(), TestMeaning.class);
            startActivity(intent);
        });

        goOnStartTest.setOnClickListener(v -> {
            // navigate to choose_test activity
            Intent intent = new Intent(requireContext(), ChooseTestActivity.class);
            startActivity(intent);
        });

        explainIcon.setOnClickListener(v -> {
            // navigate to choose_test activity
            Intent intent = new Intent(requireContext(), TestMeaning.class);
            startActivity(intent);
        });

        startTestIcon.setOnClickListener(v -> {
            // navigate to choose_test activity
            Intent intent = new Intent(requireContext(), ChooseTestActivity.class);
            startActivity(intent);
        });
    }

    private void showDeleteAssessmentDialog() {
        // Create Calendar instance
        Calendar calendar = Calendar.getInstance();

        // Date picker first
        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    // Then show assessment type picker
                    showAssessmentTypePicker(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void showAssessmentTypePicker(String selectedDate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        // Get default dialog padding from theme
        TypedValue outValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(
                android.R.attr.dialogPreferredPadding,
                outValue,
                true
        );
        int defaultPadding = (int) outValue.getDimension(this.getResources().getDisplayMetrics());

        // Set title with your custom font
        TextView titleView = new TextView(requireContext());
        titleView.setText("Select assessment type to delete for " + selectedDate);
        titleView.setTextSize(20); // Your size
        titleView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.profile_pro_bold));
        titleView.setPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding / 2);
        titleView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        builder.setCustomTitle(titleView);

        String[] types = {"Sway area and balance", "Dual Task", "Sensory Ataxia", "Speed and distance"};

        // Custom adapter with your font
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                types
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.profile_pro));
                textView.setTextSize(18);
                return view;
            }
        };

        builder.setAdapter(adapter, (dialog, which) -> {
            String assessmentType = types[which];
            showTrialPickerForDeletion(selectedDate, assessmentType);
        });

        builder.show();
    }

    private void showTrialPickerForDeletion(String dateStr, String assessmentType) {
        java.util.List<AssessmentData> allAssessments = DataStorageManager.loadAllAssessments(requireContext());

        // Sort chronologically (oldest first) so index 0 = T1
        java.util.List<AssessmentData> chronologicalAssessments = new java.util.ArrayList<>(allAssessments);
        java.util.Collections.sort(chronologicalAssessments, (a1, a2) -> {
            if (a1.getTimestamp() == null || a2.getTimestamp() == null) return 0;
            return a1.getTimestamp().compareTo(a2.getTimestamp());
        });

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
        java.util.List<AssessmentData> matchingTrials = new java.util.ArrayList<>();

        for (AssessmentData assessment : chronologicalAssessments) {
            if (assessment.getTimestamp() == null) continue;
            String aDate = sdf.format(assessment.getTimestamp());
            if (!aDate.equals(dateStr)) continue;

            boolean matches = false;
            switch (assessmentType) {
                case "Sway area and balance":
                case "ARJ and balance":
                    if (assessment.getSwayAreaAndBalanceTestDate() != null ||
                            (assessment.getBaseline() != null && assessment.getBaseline().getBalanceScore() > 0)) {
                        matches = true;
                    }
                    break;
                case "Dual Task":
                    if (assessment.getDualTaskTestDate() != null ||
                            (assessment.getDualTask() != null && assessment.getDualTask().getBalanceScore() > 0)) {
                        matches = true;
                    }
                    break;
                case "Sensory Ataxia":
                    if (assessment.getSensoryAtaxiaTestDate() != null ||
                            (assessment.getSensoryAtaxia() != null && assessment.getSensoryAtaxia().getBalanceScore() > 0)) {
                        matches = true;
                    }
                    break;
                case "Speed and distance":
                    if (assessment.getWSAndEnduranceTestDate() != null ||
                            (assessment.getBaseline() != null && (assessment.getBaseline().getWalkingSpeed() > 0 || assessment.getBaseline().getEnduranceScore() > 0))) {
                        matches = true;
                    }
                    break;
            }

            if (matches) {
                matchingTrials.add(assessment);
            }
        }

        if (matchingTrials.isEmpty()) {
            android.widget.Toast.makeText(requireContext(), "No " + assessmentType + " trials found for " + dateStr, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // Build list of trial labels T1, T2, etc. with value
        String[] trialLabels = new String[matchingTrials.size()];
        for (int i = 0; i < matchingTrials.size(); i++) {
            AssessmentData trial = matchingTrials.get(i);
            String valStr = "";
            switch (assessmentType) {
                case "Sway area and balance":
                case "ARJ and balance":
                    valStr = String.format(Locale.US, "%.2f mm²", trial.getBaseline().getBalanceScore());
                    break;
                case "Dual Task":
                    valStr = String.format(Locale.US, "%.2f mm²", trial.getDualTask().getBalanceScore());
                    break;
                case "Sensory Ataxia":
                    valStr = String.format(Locale.US, "%.2f mm²", trial.getSensoryAtaxia().getBalanceScore());
                    break;
                case "Speed and distance":
                    valStr = String.format(Locale.US, "%.2f m/s, %.2f m", trial.getBaseline().getWalkingSpeed(), trial.getBaseline().getEnduranceScore());
                    break;
            }
            trialLabels[i] = "T" + (i + 1) + ": " + valStr;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        TypedValue outValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.dialogPreferredPadding, outValue, true);
        int defaultPadding = (int) outValue.getDimension(requireContext().getResources().getDisplayMetrics());

        TextView titleView = new TextView(requireContext());
        titleView.setText("Select trial to delete for " + dateStr);
        titleView.setTextSize(20);
        titleView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.profile_pro_bold));
        titleView.setPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding / 2);
        titleView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        builder.setCustomTitle(titleView);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                trialLabels
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.profile_pro));
                textView.setTextSize(18);
                return view;
            }
        };

        builder.setAdapter(adapter, (dialog, which) -> {
            AssessmentData targetTrial = matchingTrials.get(which);
            String trialName = trialLabels[which];
            showDeleteConfirmationDialog(targetTrial, trialName, dateStr);
        });

        builder.show();
    }

    private void showDeleteConfirmationDialog(AssessmentData targetTrial, String trialLabel, String dateStr) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        TypedValue outValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.dialogPreferredPadding, outValue, true);
        int defaultPadding = (int) outValue.getDimension(requireContext().getResources().getDisplayMetrics());

        TextView titleView = new TextView(requireContext());
        titleView.setText("Confirm Deletion");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        titleView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.profile_pro_bold));
        titleView.setPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding / 2);
        titleView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        builder.setCustomTitle(titleView);

        TextView messageView = new TextView(requireContext());
        messageView.setText("Delete " + trialLabel + " recorded on " + dateStr + "?");
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        messageView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.profile_pro));
        messageView.setPadding(defaultPadding, 0, defaultPadding, defaultPadding);
        messageView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        builder.setView(messageView);

        builder.setPositiveButton("Delete", (dialog, which) -> {
            boolean deleted = DataStorageManager.deleteAssessmentFile(requireContext(), targetTrial.getSessionId());
            if (deleted) {
                android.widget.Toast.makeText(requireContext(), trialLabel + " deleted successfully", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                android.widget.Toast.makeText(requireContext(), "Failed to delete trial", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.profile_pro));
            positiveButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        }
        if (negativeButton != null) {
            negativeButton.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.profile_pro));
            negativeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        }
    }
    @Override
    public void onResume() {
        super.onResume();

        // Intercept back press to exit
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),  // Use ViewLifecycleOwner for fragments
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        requireActivity().finishAffinity(); // Exits app completely
                    }
                }
        );
    }
}
