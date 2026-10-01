package com.example.biomsAfterWirelessConnectedVersion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ReportModel implements Serializable {
    private Date timestamp;

    // TODO: NO TREND YET

    private String preview;       // Short preview text
    // Date
    private String date;

    // Rectified Jerk section
    private String rectifiedJerkTitle;
    private float arjLeftKneeToday;
    private float arjRightKneeToday;
    private float ARJRightSD;
    private float ARJRightCV;
    private float ARJLeftSD;
    private float ARJLeftCV;
    private String arjTrendRight, arjTrendLeft;  // "improved", "declined", "stable"

    // Balance section
    private String balanceTitle;
    private float balanceToday;
    private String balanceTrend;
    private float balanceSD;
    private float balanceCV;

    // Speed section
    private String speedTitle;
    private float speedToday;
    private String speedTrend;
    private float speedSD;
    private float speedCV;

    // Endurance section
    private String enduranceTitle;
    private float enduranceToday;
    private String enduranceTrend;
    private float enduranceSD;
    private float enduranceCV;

    // Sensory Ataxia section
    private String sensoryAtaxiaTitle;

    public float getSensoryAtaxiaARJLeftToday() {
        return sensoryAtaxiaARJLeftToday;
    }

    public void setSensoryAtaxiaARJLeftToday(float sensoryAtaxiaARJLeftToday) {
        this.sensoryAtaxiaARJLeftToday = sensoryAtaxiaARJLeftToday;
    }

    public float getSensoryAtaxiaARJRightToday() {
        return sensoryAtaxiaARJRightToday;
    }

    public void setSensoryAtaxiaARJRightToday(float sensoryAtaxiaARJRightToday) {
        this.sensoryAtaxiaARJRightToday = sensoryAtaxiaARJRightToday;
    }

    private float sensoryAtaxiaARJLeftToday;
    private float sensoryAtaxiaARJRightToday;
    private float sensoryAtaxiaBalanceToday;
    private float sensoryAtaxiaDTCARJLeft;
    private float sensoryAtaxiaDTCARJRight;
    private float sensoryAtaxiaDTCBalance;
    private String sensoryAtaxiaTrend;

    public float getSensoryAtaxiaDTCARJLeft() {
        return sensoryAtaxiaDTCARJLeft;
    }

    public void setSensoryAtaxiaDTCARJLeft(float sensoryAtaxiaDTCARJLeft) {
        this.sensoryAtaxiaDTCARJLeft = sensoryAtaxiaDTCARJLeft;
    }

    public float getSensoryAtaxiaDTCARJRight() {
        return sensoryAtaxiaDTCARJRight;
    }

    public void setSensoryAtaxiaDTCARJRight(float sensoryAtaxiaDTCARJRight) {
        this.sensoryAtaxiaDTCARJRight = sensoryAtaxiaDTCARJRight;
    }

    public float getSensoryAtaxiaDTCBalance() {
        return sensoryAtaxiaDTCBalance;
    }

    public void setSensoryAtaxiaDTCBalance(float sensoryAtaxiaDTCBalance) {
        this.sensoryAtaxiaDTCBalance = sensoryAtaxiaDTCBalance;
    }

    // Dual Task section
    private String dualTaskTitle;

    public float getDualTaskARJLeftToday() {
        return dualTaskARJLeftToday;
    }

    public void setDualTaskARJLeftToday(float dualTaskARJLeftToday) {
        this.dualTaskARJLeftToday = dualTaskARJLeftToday;
    }

    public float getDualTaskARJRightToday() {
        return dualTaskARJRightToday;
    }

    public void setDualTaskARJRightToday(float dualTaskARJRightToday) {
        this.dualTaskARJRightToday = dualTaskARJRightToday;
    }

    private float dualTaskARJLeftToday;
    private float dualTaskARJRightToday;
    private float dualTaskBalanceToday;
    private float dualTaskDTCARJLeft;
    private float dualTaskDTCARJRight;
    private float dualTaskDTCBalance;
    private String dualTaskTrend;
    private String currentValueBaseline;
    private String reportId;

    // Trial Lists
    private List<Float> balanceTrials = new ArrayList<>();
    private List<Float> speedTrials = new ArrayList<>();
    private List<Float> enduranceTrials = new ArrayList<>();
    private List<Float> dualTaskBalanceTrials = new ArrayList<>();
    private List<Float> dualTaskDTCTrials = new ArrayList<>();
    private List<Float> sensoryAtaxiaBalanceTrials = new ArrayList<>();
    private List<Float> sensoryAtaxiaDTCTrials = new ArrayList<>();

    public List<Float> getBalanceTrials() { return balanceTrials; }
    public void setBalanceTrials(List<Float> trials) { this.balanceTrials = trials != null ? trials : new ArrayList<>(); }

    public List<Float> getSpeedTrials() { return speedTrials; }
    public void setSpeedTrials(List<Float> trials) { this.speedTrials = trials != null ? trials : new ArrayList<>(); }

    public List<Float> getEnduranceTrials() { return enduranceTrials; }
    public void setEnduranceTrials(List<Float> trials) { this.enduranceTrials = trials != null ? trials : new ArrayList<>(); }

    public List<Float> getDualTaskBalanceTrials() { return dualTaskBalanceTrials; }
    public void setDualTaskBalanceTrials(List<Float> trials) { this.dualTaskBalanceTrials = trials != null ? trials : new ArrayList<>(); }

    public List<Float> getDualTaskDTCTrials() { return dualTaskDTCTrials; }
    public void setDualTaskDTCTrials(List<Float> trials) { this.dualTaskDTCTrials = trials != null ? trials : new ArrayList<>(); }

    public List<Float> getSensoryAtaxiaBalanceTrials() { return sensoryAtaxiaBalanceTrials; }
    public void setSensoryAtaxiaBalanceTrials(List<Float> trials) { this.sensoryAtaxiaBalanceTrials = trials != null ? trials : new ArrayList<>(); }

    public List<Float> getSensoryAtaxiaDTCTrials() { return sensoryAtaxiaDTCTrials; }
    public void setSensoryAtaxiaDTCTrials(List<Float> trials) { this.sensoryAtaxiaDTCTrials = trials != null ? trials : new ArrayList<>(); }

    public float getDualTaskDTCARJLeft() {
        return dualTaskDTCARJLeft;
    }

    public void setDualTaskDTCARJLeft(float dualTaskDTCARJLeft) {
        this.dualTaskDTCARJLeft = dualTaskDTCARJLeft;
    }

    public float getDualTaskDTCARJRight() {
        return dualTaskDTCARJRight;
    }

    public void setDualTaskDTCARJRight(float dualTaskDTCARJRight) {
        this.dualTaskDTCARJRight = dualTaskDTCARJRight;
    }

    public float getDualTaskDTCBalance() {
        return dualTaskDTCBalance;
    }

    public void setDualTaskDTCBalance(float dualTaskDTCBalance) {
        this.dualTaskDTCBalance = dualTaskDTCBalance;
    }

    public ReportModel() {
        this.reportId = UUID.randomUUID().toString();
        this.timestamp = new Date();
    }

    public float getARJRightSD() {
        return ARJRightSD;
    }

    public void setARJRightSD(float ARJRightSD) {
        this.ARJRightSD = ARJRightSD;
    }

    public float getARJRightCV() {
        return ARJRightCV;
    }

    public void setARJRightCV(float ARJRightCV) {
        this.ARJRightCV = ARJRightCV;
    }

    public float getARJLeftSD() {
        return ARJLeftSD;
    }

    public void setARJLeftSD(float ARJLeftSD) {
        this.ARJLeftSD = ARJLeftSD;
    }

    public float getARJLeftCV() {
        return ARJLeftCV;
    }

    public void setARJLeftCV(float ARJLeftCV) {
        this.ARJLeftCV = ARJLeftCV;
    }

    public float getBalanceSD() {
        return balanceSD;
    }

    public void setBalanceSD(float balanceSD) {
        this.balanceSD = balanceSD;
    }

    public float getBalanceCV() {
        return balanceCV;
    }

    public void setBalanceCV(float balanceCV) {
        this.balanceCV = balanceCV;
    }

    public float getSpeedSD() {
        return speedSD;
    }

    public void setSpeedSD(float speedSD) {
        this.speedSD = speedSD;
    }

    public float getSpeedCV() {
        return speedCV;
    }

    public void setSpeedCV(float speedCV) {
        this.speedCV = speedCV;
    }

    public float getEnduranceSD() {
        return enduranceSD;
    }

    public void setEnduranceSD(float enduranceSD) {
        this.enduranceSD = enduranceSD;
    }

    public float getEnduranceCV() {
        return enduranceCV;
    }

    public void setEnduranceCV(float enduranceCV) {
        this.enduranceCV = enduranceCV;
    }

    public Date getTimeStamp() { return timestamp; }
    public String getReportId() { return reportId; }
    public String getPreview() { return preview; }
    public String getDate() { return date; }
    public void setPreview(String preview) {
        this.preview = preview;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRectifiedJerkTitle() {
        return rectifiedJerkTitle;
    }

    public void setRectifiedJerkTitle(String rectifiedJerkTitle) {
        this.rectifiedJerkTitle = rectifiedJerkTitle;
    }
    public float getArjRightKneeToday() {
        return arjRightKneeToday;
    }

    public void setArjRightKneeToday(float arjToday) {
        this.arjRightKneeToday = arjToday;
    }
    public float getArjLeftKneeToday() {
        return arjLeftKneeToday;
    }

    public void setArjLeftKneeToday(float arjToday) {
        this.arjLeftKneeToday = arjToday;
    }

    public String getArjTrendRight() {
        return arjTrendRight;
    }

    public String getArjTrendLeft() {
        return arjTrendLeft;
    }

    public void setArjTrendRight(String arjTrend) {
        this.arjTrendRight = arjTrend;
    }

    public void setArjTrendLeft(String arjTrend) {
        this.arjTrendLeft = arjTrend;
    }

    public String getBalanceTitle() {
        return balanceTitle;
    }

    public void setBalanceTitle(String balanceTitle) {
        this.balanceTitle = balanceTitle;
    }

    public float getBalanceToday() {
        return balanceToday;
    }

    public void setBalanceToday(float balanceToday) {
        this.balanceToday = balanceToday;
    }

    public String getBalanceTrend() {
        return balanceTrend;
    }

    public void setBalanceTrend(String balanceTrend) {
        this.balanceTrend = balanceTrend;
    }

    public String getSpeedTitle() {
        return speedTitle;
    }

    public void setSpeedTitle(String speedTitle) {
        this.speedTitle = speedTitle;
    }

    public float getSpeedToday() {
        return speedToday;
    }

    public void setSpeedToday(float speedToday) {
        this.speedToday = speedToday;
    }

    public String getSpeedTrend() {
        return speedTrend;
    }

    public void setSpeedTrend(String speedTrend) {
        this.speedTrend = speedTrend;
    }

    public String getEnduranceTitle() {
        return enduranceTitle;
    }

    public void setEnduranceTitle(String enduranceTitle) {
        this.enduranceTitle = enduranceTitle;
    }

    public float getEnduranceToday() {
        return enduranceToday;
    }

    public void setEnduranceToday(float enduranceToday) {
        this.enduranceToday = enduranceToday;
    }

    public String getEnduranceTrend() {
        return enduranceTrend;
    }

    public void setEnduranceTrend(String enduranceTrend) {
        this.enduranceTrend = enduranceTrend;
    }

    public String getSensoryAtaxiaTitle() {
        return sensoryAtaxiaTitle;
    }
    public void setSensoryAtaxiaTitle(String sensoryAtaxiaTitle) {
        this.sensoryAtaxiaTitle = sensoryAtaxiaTitle;
    }

    public void setSensoryAtaxiaBalanceToday(float sensoryAtaxiaToday) {
        this.sensoryAtaxiaBalanceToday = sensoryAtaxiaToday;
    }

    public float getSensoryAtaxiaBalanceToday() {
       return sensoryAtaxiaBalanceToday;
    }

    public String getSensoryAtaxiaTrend() {
        return sensoryAtaxiaTrend;
    }

    public void setSensoryAtaxiaTrend(String sensoryAtaxiaTrend) {
        this.sensoryAtaxiaTrend = sensoryAtaxiaTrend;
    }

    public String getDualTaskTitle() {
        return dualTaskTitle;
    }

    public void setDualTaskTitle(String dualTaskTitle) {
        this.dualTaskTitle = dualTaskTitle;
    }

    public float getDualTaskBalanceToday() {
        return dualTaskBalanceToday;
    }


    public void setDualTaskBalanceToday(float dualTaskToday) {
        this.dualTaskBalanceToday = dualTaskToday;
    }

    public String getDualTaskTrend() {
        return dualTaskTrend;
    }

    public void setDualTaskTrend(String dualTaskTrend) {
        this.dualTaskTrend = dualTaskTrend;
    }

    public String getCurrentValueBaseline() {
        return currentValueBaseline;
    }

    public void setCurrentValueBaseline(String currentValueBaseline) {
        this.currentValueBaseline = currentValueBaseline;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimeStamp(Date now) {
        this.timestamp = now;
    }
}