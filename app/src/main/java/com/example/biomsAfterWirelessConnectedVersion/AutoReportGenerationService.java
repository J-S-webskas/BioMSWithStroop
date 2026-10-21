package com.example.biomsAfterWirelessConnectedVersion;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AutoReportGenerationService extends Service {

    private static final String TAG = "AutoReportService";
    private static final String CHANNEL_ID = "AutoReportChannel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start foreground service with required type for Android 14+ (API 34+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            );
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }

        // Run the check and report generation
        checkAndGenerateReport();

        // Stop service after checking
        stopSelf();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void checkAndGenerateReport() {
        try {
            // 1. Load all assessments from storage
            List<AssessmentData> allAssessments = DataStorageManager.loadAllAssessments(this);
            if (allAssessments == null || allAssessments.isEmpty()) {
                Log.d(TAG, "No assessment data available.");
                return;
            }

            // 2. Find today's assessment strictly by today's date ("MM/dd/yyyy")
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            String todayStr = sdf.format(new Date());

            AssessmentData todayAssessment = null;
            for (AssessmentData assessment : allAssessments) {
                if (assessment.getTimestamp() != null) {
                    String assessmentDate = sdf.format(assessment.getTimestamp());
                    if (assessmentDate.equals(todayStr)) {
                        todayAssessment = assessment;
                        break;
                    }
                }
            }

            // If no assessment exists for today, DO NOT create anything!
            if (todayAssessment == null) {
                Log.d(TAG, "No assessment recorded for today (" + todayStr + "). Skipping.");
                return;
            }

            // If today's assessment doesn't contain actual completed test data, still generate
            // Load all existing reports to check if today's report already exists
            List<ReportModel> allReports = DataStorageManager.loadAllReports(this);
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
            String todayReportDateStr = displayFormat.format(new Date());

            ReportModel existingTodayReport = null;
            for (ReportModel report : allReports) {
                if (report.getDate() != null && report.getDate().equals(todayReportDateStr)) {
                    existingTodayReport = report;
                    break;
                }
            }

            // 5. If today's report already exists AND data matches, skip generation
            if (existingTodayReport != null && dataMatches(existingTodayReport, todayAssessment)) {
                Log.d(TAG, "Today's report already exists and is up to date.");
                return;
            }

            // 7. Generate new report for today
            generateReportForToday(new Date(), allAssessments);

            sendBroadcast(new Intent("REPORT_UPDATED"));

            // 8. Show notification
            showNotification("Report Generated", "A new report has been generated for " + todayReportDateStr);

        } catch (Exception e) {
            Log.e(TAG, "Error in checkAndGenerateReport: " + e.getMessage(), e);
        }
    }

    // Full data matching implementation comparing all key metrics
    private boolean dataMatches(ReportModel report, AssessmentData assessment) {
        if (report == null || assessment == null || assessment.getBaseline() == null) return false;

        float tolerance = 0.001f;

        // Baseline Metrics
        float reportSpeed = report.getSpeedToday();
        float assessmentSpeed = assessment.getBaseline().getWalkingSpeed();
        if (Math.abs(reportSpeed - assessmentSpeed) >= tolerance) return false;

        float reportBalance = report.getBalanceToday();
        float assessmentBalance = assessment.getBaseline().getBalanceScore();
        if (Math.abs(reportBalance - assessmentBalance) >= tolerance) return false;

        float reportEndurance = report.getEnduranceToday();
        float assessmentEndurance = assessment.getBaseline().getEnduranceScore();
        if (Math.abs(reportEndurance - assessmentEndurance) >= tolerance) return false;

        Float assessmentARJRight = assessment.getBaseline().getARJ() != null ?
                assessment.getBaseline().getARJ().get("right_knee") : null;
        float reportARJRight = report.getArjRightKneeToday();
        if (assessmentARJRight != null && Math.abs(reportARJRight - assessmentARJRight) >= tolerance) {
            return false;
        }

        Float assessmentARJLeft = assessment.getBaseline().getARJ() != null ?
                assessment.getBaseline().getARJ().get("left_knee") : null;
        float reportARJLeft = report.getArjLeftKneeToday();
        if (assessmentARJLeft != null && Math.abs(reportARJLeft - assessmentARJLeft) >= tolerance) {
            return false;
        }

        // Dual Task Metrics
        if (assessment.getDualTask() != null) {
            float reportDTBalance = report.getDualTaskBalanceToday();
            float assessmentDTBalance = assessment.getDualTask().getBalanceScore();
            if (Math.abs(reportDTBalance - assessmentDTBalance) >= tolerance) return false;
        }

        // Sensory Ataxia Metrics
        if (assessment.getSensoryAtaxia() != null) {
            float reportSABalance = report.getSensoryAtaxiaBalanceToday();
            float assessmentSABalance = assessment.getSensoryAtaxia().getBalanceScore();
            if (Math.abs(reportSABalance - assessmentSABalance) >= tolerance) return false;
        }

        return true;
    }

    // Self-contained report generator for Service context
    private void generateReportForToday(Date targetDate, List<AssessmentData> allAssessments) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        String targetDateString = sdf.format(targetDate);

        Float bestARJLeft = null, bestARJRight = null, bestBalance = null;
        Float bestSwayVelocity = null, bestSwayPathLength = null, bestSwayRmsAP = null, bestSwayRmsML = null;
        Float bestWalkingSpeed = null, bestEndurance = null;
        Float bestDTARJLeft = null, bestDTARJRight = null, bestDTBalance = null;
        Float bestDTSwayVelocity = null, bestDTSwayPathLength = null, bestDTSwayRmsAP = null, bestDTSwayRmsML = null;
        Float bestSAARJLeft = null, bestSAARJRight = null, bestSABalance = null;
        Float bestSASwayVelocity = null, bestSASwayPathLength = null, bestSASwayRmsAP = null, bestSASwayRmsML = null;

        long latestTimestamp = 0, latestDTTimestamp = 0, latestSATimestamp = 0;

        ArrayList<Float> allARJLeftValues = new ArrayList<>();
        ArrayList<Float> allARJRightValues = new ArrayList<>();
        ArrayList<Float> allBalanceValues = new ArrayList<>();
        ArrayList<Float> allSpeedValues = new ArrayList<>();
        ArrayList<Float> allEnduranceValues = new ArrayList<>();

        float DTARJLeftKneeDTC = 0, DTARJRightKneeDTC = 0, DTBalanceDTC = 0;
        float DTVelocityDTC = 0, DTPathDTC = 0;
        float SAARJLeftKneeDTC = 0, SAARJRightKneeDTC = 0, SABalanceDTC = 0;
        float SAVelocityDTC = 0, SAPathDTC = 0;

        for (AssessmentData assessment : allAssessments) {
            String currentDate = sdf.format(assessment.getTimestamp());
            long currentTimestamp = assessment.getTimestamp().getTime();

            // SDCV Collection across all dates
            Float arjLeftSDCV = assessment.getBaseline().getARJ().get("left_knee");
            if (arjLeftSDCV != null && arjLeftSDCV != 0) allARJLeftValues.add(arjLeftSDCV);

            Float arjRightSDCV = assessment.getBaseline().getARJ().get("right_knee");
            if (arjRightSDCV != null && arjRightSDCV != 0) allARJRightValues.add(arjRightSDCV);

            float balanceSDCV = assessment.getBaseline().getBalanceScore();
            if (balanceSDCV != 0) allBalanceValues.add(balanceSDCV);

            float walkingSpeedSDCV = assessment.getBaseline().getWalkingSpeed();
            if (walkingSpeedSDCV != 0) allSpeedValues.add(walkingSpeedSDCV);

            float enduranceSDCV = assessment.getBaseline().getEnduranceScore();
            if (enduranceSDCV != 0) allEnduranceValues.add(enduranceSDCV);

            if (!currentDate.equals(targetDateString)) continue;

            // Extract Baseline
            Float arjLeft = assessment.getBaseline().getARJ().get("left_knee");
            if (arjLeft != null && arjLeft != 0 && (bestARJLeft == null || currentTimestamp > latestTimestamp)) {
                bestARJLeft = arjLeft;
            }
            Float arjRight = assessment.getBaseline().getARJ().get("right_knee");
            if (arjRight != null && arjRight != 0 && (bestARJRight == null || currentTimestamp > latestTimestamp)) {
                bestARJRight = arjRight;
            }
            float balance = assessment.getBaseline().getBalanceScore();
            if (balance != 0 && (bestBalance == null || currentTimestamp > latestTimestamp)) {
                bestBalance = balance;
            }
            if (bestSwayVelocity == null || currentTimestamp > latestTimestamp) {
                bestSwayVelocity = assessment.getBaseline().getSwayMeanVelocity();
                bestSwayPathLength = assessment.getBaseline().getSwayPathLength();
                bestSwayRmsAP = assessment.getBaseline().getSwayRmsAP();
                bestSwayRmsML = assessment.getBaseline().getSwayRmsML();
            }
            float walkingSpeed = assessment.getBaseline().getWalkingSpeed();
            if (walkingSpeed != 0 && (bestWalkingSpeed == null || currentTimestamp > latestTimestamp)) {
                bestWalkingSpeed = walkingSpeed;
            }
            float endurance = assessment.getBaseline().getEnduranceScore();
            if (endurance != 0 && (bestEndurance == null || currentTimestamp > latestTimestamp)) {
                bestEndurance = endurance;
            }
            if (currentTimestamp > latestTimestamp) latestTimestamp = currentTimestamp;

            // Dual Task
            DualTaskAssessment dualTask = assessment.getDualTask();
            if (dualTask != null) {
                Float dtArjLeft = dualTask.getARJ().get("left_knee");
                if (dtArjLeft != null && dtArjLeft != 0 && (bestDTARJLeft == null || currentTimestamp > latestDTTimestamp)) {
                    bestDTARJLeft = dtArjLeft;
                }
                Float dtArjRight = dualTask.getARJ().get("right_knee");
                if (dtArjRight != null && dtArjRight != 0 && (bestDTARJRight == null || currentTimestamp > latestDTTimestamp)) {
                    bestDTARJRight = dtArjRight;
                }
                float dtBalance = dualTask.getBalanceScore();
                if (dtBalance != 0 && (bestDTBalance == null || currentTimestamp > latestDTTimestamp)) {
                    bestDTBalance = dtBalance;
                }
                if (bestDTSwayVelocity == null || currentTimestamp > latestDTTimestamp) {
                    bestDTSwayVelocity = dualTask.getSwayMeanVelocity();
                    bestDTSwayPathLength = dualTask.getSwayPathLength();
                    bestDTSwayRmsAP = dualTask.getSwayRmsAP();
                    bestDTSwayRmsML = dualTask.getSwayRmsML();
                }
                if (currentTimestamp > latestDTTimestamp) latestDTTimestamp = currentTimestamp;
            }

            // Sensory Ataxia
            SensoryAtaxiaAssessment sensory = assessment.getSensoryAtaxia();
            if (sensory != null) {
                Float saArjLeft = sensory.getARJ().get("left_knee");
                if (saArjLeft != null && saArjLeft != 0 && (bestSAARJLeft == null || currentTimestamp > latestSATimestamp)) {
                    bestSAARJLeft = saArjLeft;
                }
                Float saArjRight = sensory.getARJ().get("right_knee");
                if (saArjRight != null && saArjRight != 0 && (bestSAARJRight == null || currentTimestamp > latestSATimestamp)) {
                    bestSAARJRight = saArjRight;
                }
                float saBalance = sensory.getBalanceScore();
                if (saBalance != 0 && (bestSABalance == null || currentTimestamp > latestSATimestamp)) {
                    bestSABalance = saBalance;
                }
                if (bestSASwayVelocity == null || currentTimestamp > latestSATimestamp) {
                    bestSASwayVelocity = sensory.getSwayMeanVelocity();
                    bestSASwayPathLength = sensory.getSwayPathLength();
                    bestSASwayRmsAP = sensory.getSwayRmsAP();
                    bestSASwayRmsML = sensory.getSwayRmsML();
                }
                if (currentTimestamp > latestSATimestamp) latestSATimestamp = currentTimestamp;
            }

            // Comparisons / DTC
            TestComparisons comparisons = assessment.getComparisons();
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

        // Calculate SD & CV
        float ARJRightKneeSD = standardDeviation(allARJRightValues);
        float ARJLeftKneeSD = standardDeviation(allARJLeftValues);
        float BalanceSD = standardDeviation(allBalanceValues);
        float EnduranceSD = standardDeviation(allEnduranceValues);
        float WSSD = standardDeviation(allSpeedValues);

        float ARJLeftKneeCV = (ARJLeftKneeSD / getMean(allARJLeftValues)) * 100;
        float ARJRightKneeCV = (ARJRightKneeSD / getMean(allARJRightValues)) * 100;
        float BalanceCV = (BalanceSD / getMean(allBalanceValues)) * 100;
        float WSCV = (WSSD / getMean(allSpeedValues)) * 100;
        float EnduranceCV = (EnduranceSD / getMean(allEnduranceValues)) * 100;

        // Sort chronological (oldest first) so T1 = earliest trial
        List<AssessmentData> chronologicalAssessments = new ArrayList<>(allAssessments);
        java.util.Collections.sort(chronologicalAssessments, (a1, a2) ->
                a1.getTimestamp().compareTo(a2.getTimestamp()));

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
            if (!currentDate.equals(targetDateString)) continue;

            float balance = assessment.getBaseline().getBalanceScore();
            if (balance > 0) balanceTrials.add(balance);

            if (assessment.getBaseline().getSwayPathLength() > 0) {
                swayPathLengthTrials.add(assessment.getBaseline().getSwayPathLength());
                swayVelocityTrials.add(assessment.getBaseline().getSwayMeanVelocity());
                swayRmsAPTrials.add(assessment.getBaseline().getSwayRmsAP());
                swayRmsMLTrials.add(assessment.getBaseline().getSwayRmsML());
            }

            float speed = assessment.getBaseline().getWalkingSpeed();
            if (speed > 0) speedTrials.add(speed);

            float endurance = assessment.getBaseline().getEnduranceScore();
            if (endurance > 0) enduranceTrials.add(endurance);

            if (assessment.getDualTask() != null && assessment.getDualTask().getBalanceScore() > 0) {
                dtBalanceTrials.add(assessment.getDualTask().getBalanceScore());
            }

            if (assessment.getDualTask() != null && assessment.getDualTask().getStroopTotalTrials() > 0) {
                DualTaskAssessment dtA = assessment.getDualTask();
                dtStroopTrials.add(new ReportModel.StroopTrialData(
                        dtA.getStroopTotalTrials(),
                        dtA.getStroopCorrectCount(),
                        dtA.getStroopAccuracyPercent(),
                        dtA.getStroopAvgReactionMs(),
                        dtA.getStroopAnswers()));
            }

            if (assessment.getDualTask() != null && assessment.getDualTask().getSwayPathLength() > 0) {
                dtSwayPathLengthTrials.add(assessment.getDualTask().getSwayPathLength());
                dtSwayVelocityTrials.add(assessment.getDualTask().getSwayMeanVelocity());
                dtSwayRmsAPTrials.add(assessment.getDualTask().getSwayRmsAP());
                dtSwayRmsMLTrials.add(assessment.getDualTask().getSwayRmsML());
            }

            if (assessment.getSensoryAtaxia() != null && assessment.getSensoryAtaxia().getBalanceScore() > 0) {
                saBalanceTrials.add(assessment.getSensoryAtaxia().getBalanceScore());
            }

            if (assessment.getSensoryAtaxia() != null && assessment.getSensoryAtaxia().getSwayPathLength() > 0) {
                saSwayPathLengthTrials.add(assessment.getSensoryAtaxia().getSwayPathLength());
                saSwayVelocityTrials.add(assessment.getSensoryAtaxia().getSwayMeanVelocity());
                saSwayRmsAPTrials.add(assessment.getSensoryAtaxia().getSwayRmsAP());
                saSwayRmsMLTrials.add(assessment.getSensoryAtaxia().getSwayRmsML());
            }

            if (assessment.getComparisons() != null) {
                if (assessment.getComparisons().getDualTaskBalanceChange() != 0) {
                    dtDTCTrials.add(assessment.getComparisons().getDualTaskBalanceChange());
                }
                if (assessment.getComparisons().getSensoryAtaxiaBalanceChange() != 0) {
                    saDTCTrials.add(assessment.getComparisons().getSensoryAtaxiaBalanceChange());
                }
                if (assessment.getComparisons().getDualTaskVelocityChange() != 0) {
                    dtVelocityDTCTrials.add(assessment.getComparisons().getDualTaskVelocityChange());
                }
                if (assessment.getComparisons().getDualTaskPathLengthChange() != 0) {
                    dtPathLengthDTCTrials.add(assessment.getComparisons().getDualTaskPathLengthChange());
                }
                if (assessment.getComparisons().getSensoryAtaxiaVelocityChange() != 0) {
                    saVelocityDTCTrials.add(assessment.getComparisons().getSensoryAtaxiaVelocityChange());
                }
                if (assessment.getComparisons().getSensoryAtaxiaPathLengthChange() != 0) {
                    saPathLengthDTCTrials.add(assessment.getComparisons().getSensoryAtaxiaPathLengthChange());
                }
            }
        }

        // Build Report object
        ReportModel newReport = new ReportModel();
        newReport.setArjLeftKneeToday(bestARJLeft != null ? bestARJLeft : 0f);
        newReport.setArjRightKneeToday(bestARJRight != null ? bestARJRight : 0f);
        newReport.setBalanceToday(bestBalance != null ? bestBalance : 0f);
        newReport.setSwayVelocityToday(bestSwayVelocity != null ? bestSwayVelocity : 0f);
        newReport.setSwayPathLengthToday(bestSwayPathLength != null ? bestSwayPathLength : 0f);
        newReport.setSwayRmsAPToday(bestSwayRmsAP != null ? bestSwayRmsAP : 0f);
        newReport.setSwayRmsMLToday(bestSwayRmsML != null ? bestSwayRmsML : 0f);
        newReport.setSpeedToday(bestWalkingSpeed != null ? bestWalkingSpeed : 0f);
        newReport.setEnduranceToday(bestEndurance != null ? bestEndurance : 0f);
        newReport.setSensoryAtaxiaARJLeftToday(bestSAARJLeft != null ? bestSAARJLeft : 0f);
        newReport.setSensoryAtaxiaARJRightToday(bestSAARJRight != null ? bestSAARJRight : 0f);
        newReport.setSensoryAtaxiaBalanceToday(bestSABalance != null ? bestSABalance : 0f);
        newReport.setSensoryAtaxiaSwayVelocityToday(bestSASwayVelocity != null ? bestSASwayVelocity : 0f);
        newReport.setSensoryAtaxiaSwayPathLengthToday(bestSASwayPathLength != null ? bestSASwayPathLength : 0f);
        newReport.setSensoryAtaxiaSwayRmsAPToday(bestSASwayRmsAP != null ? bestSASwayRmsAP : 0f);
        newReport.setSensoryAtaxiaSwayRmsMLToday(bestSASwayRmsML != null ? bestSASwayRmsML : 0f);
        newReport.setDualTaskARJLeftToday(bestDTARJLeft != null ? bestDTARJLeft : 0f);
        newReport.setDualTaskARJRightToday(bestDTARJRight != null ? bestDTARJRight : 0f);
        newReport.setDualTaskBalanceToday(bestDTBalance != null ? bestDTBalance : 0f);
        newReport.setDualTaskSwayVelocityToday(bestDTSwayVelocity != null ? bestDTSwayVelocity : 0f);
        newReport.setDualTaskSwayPathLengthToday(bestDTSwayPathLength != null ? bestDTSwayPathLength : 0f);
        newReport.setDualTaskSwayRmsAPToday(bestDTSwayRmsAP != null ? bestDTSwayRmsAP : 0f);
        newReport.setDualTaskSwayRmsMLToday(bestDTSwayRmsML != null ? bestDTSwayRmsML : 0f);

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

        newReport.setBalanceTrials(balanceTrials);
        newReport.setSpeedTrials(speedTrials);
        newReport.setEnduranceTrials(enduranceTrials);
        newReport.setDualTaskBalanceTrials(dtBalanceTrials);
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

        String preview = String.format(Locale.US,
                "Speed: %f m/s | Sway Area: %f mm² | ARJ: %f m/s³ | Endurance: %f s",
                newReport.getSpeedToday(), newReport.getBalanceToday(),
                newReport.getArjRightKneeToday(), newReport.getEnduranceToday());
        newReport.setPreview(preview);

        SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
        newReport.setDate(displayFormat.format(targetDate));

        DataStorageManager.saveReport(this, newReport);
    }

    private float standardDeviation(ArrayList<Float> set) {
        if (set == null || set.size() < 2) return 0f;
        float sum = 0;
        for (float value : set) sum += value;
        float mean = sum / set.size();

        float sumSquaredDiff = 0;
        for (float value : set) {
            float diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        return (float) Math.sqrt(sumSquaredDiff / set.size());
    }

    private float getMean(ArrayList<Float> set) {
        if (set == null || set.isEmpty()) return 1f;
        float sum = 0;
        for (float value : set) sum += value;
        return sum / set.size();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Auto Report Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Auto Report Generator")
                .setContentText("Checking and generating reports...")
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void showNotification(String title, String message) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build();

            manager.notify(NOTIFICATION_ID + 1, notification);
        }
    }
}
