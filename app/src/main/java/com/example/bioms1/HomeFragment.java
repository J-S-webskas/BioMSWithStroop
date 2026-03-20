package com.example.bioms1;

import static android.view.View.GONE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    Button recordButton;
    TextView todoListARJB, todoListWSE, todoListB, todoListSA, todoListDT;


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
        todoListARJB = view.findViewById(R.id.todoListARJB);
        todoListWSE = view.findViewById(R.id.todoListWSE);
        todoListSA = view.findViewById(R.id.todoListSA);
        todoListDT = view.findViewById(R.id.todoListDT);

        setUpToDo();

        // Navigating to record fragment
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReportFragment reportFragment = new ReportFragment();

                // Get FragmentManager and begin transaction
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new RecordFragment()) // R.id.fragment_container is where your fragments are displayed
                        .commit();
            }
        });
        // Inflate the layout for this fragment
        return view;
    }

    private void setUpToDo() {

        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(requireContext());
        if (allAssessment.isEmpty()) {
            return;
        }

        AssessmentData newest = allAssessment.get(0);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
//        String newestDateStr = sdf.format(newest.getTimestamp());
        String todayStr = sdf.format(new Date());

        // Loading data
        for (int i = 0; i < allAssessment.size(); i++) {
            String currentDate = sdf.format(allAssessment.get(i).getTimestamp());

            AssessmentData assessment = allAssessment.get(i);
            String[] currentParts = currentDate.split("/");
            String[] todayParts = todayStr.split("/");

            // First data from last month
            if (Integer.parseInt(currentParts[1]) == Integer.parseInt(todayParts[1])) {

                if (assessment.getStatus() == null) {
                    continue; // Skip null status
                }

                switch (assessment.getStatus()) {
                    case ARJ_BALANCE_COMPLETED:
                        todoListARJB.setVisibility(View.GONE);
                        break;

                    case WS_ENDURANCE_COMPLETED:
                        todoListWSE.setVisibility(View.GONE);
                        break;

                    case DUAL_TASK_COMPLETED:
                        // This is a dual task test
                        todoListDT.setVisibility(View.GONE);
                        break;

                    case SENSORY_ATAXIA_COMPLETED:
                        // This is a sensory ataxia test
                        todoListSA.setVisibility(View.GONE);
                        break;
                }
            }
        }
    }
}