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

//        previewTestMode.setOnClickListener(v -> {
//            // Check if permission is granted
//            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
//                    != PackageManager.PERMISSION_GRANTED) {
//                // Directly request permission
//                ActivityCompat.requestPermissions(requireActivity(),
//                        new String[]{Manifest.permission.CAMERA},
//                        CAMERA_PERMISSION_REQUEST_CODE);
//                return;
//            }
//            // open camera fragment and let it see preview test mode
//            Bundle bundle = new Bundle();
//            bundle.putString("SOURCE_ACTIVITY", "AssessmentFragment");
//            bundle.putBoolean("preview", true);
//
//            CameraFragment fragment = new CameraFragment();
//            fragment.setArguments(bundle);
//
//            requireActivity().getSupportFragmentManager()
//                    .beginTransaction()
//                    .add(R.id.fragment_container, fragment)
//                    .commit();
//        });

        openSeeInstructions.setOnClickListener(v -> {
            // call  hw will you record, which navigates to MarkDistance, but need to stop it once it discovers that this is just instructions
            Intent intent = new Intent(requireContext(), HowWillYouRecord.class);
            intent.putExtra("seeInstructionsOnly", true);
            Log.d("Coming from assessmentfragment, opening the instructions", "opening instructions, passed seeInstructions as key and value true");
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

        String[] types = {"ARJ and balance", "Dual Task", "Sensory Ataxia", "Speed and endurance"};

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
            showDeleteConfirmationDialog(selectedDate, assessmentType);
        });

        builder.show();
    }

    private void showDeleteConfirmationDialog(String dateStr, String assessmentType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        // Get default dialog padding from theme
        TypedValue outValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(
                android.R.attr.dialogPreferredPadding,
                outValue,
                true
        );
        int defaultPadding = (int) outValue.getDimension(requireContext().getResources().getDisplayMetrics());

        // Custom title with your font
        TextView titleView = new TextView(requireContext());
        titleView.setText("Confirm Deletion");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        titleView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.profile_pro_bold));
        titleView.setPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding / 2);
        titleView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        builder.setCustomTitle(titleView);

        // Custom message with your font
        TextView messageView = new TextView(requireContext());
        messageView.setText("Delete " + assessmentType + " report for " + dateStr + "?");
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        messageView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.profile_pro));
        messageView.setPadding(defaultPadding, 0, defaultPadding, defaultPadding);
        messageView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        builder.setView(messageView);

        // Buttons with your font
        builder.setPositiveButton("Delete", (dialog, which) -> {
            DataStorageManager.deleteAssessmentByDate(requireContext(), dateStr, assessmentType);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.show();

        // Apply font to buttons after dialog is shown
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
