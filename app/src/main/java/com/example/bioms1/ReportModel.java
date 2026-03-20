package com.example.bioms1;

import java.io.Serializable;
import java.util.Date;
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
    private String arjTrendRight, arjTrendLeft;  // "improved", "declined", "stable"

    // Balance section
    private String balanceTitle;
    private float balanceToday;
    private String balanceTrend;

    // Speed section
    private String speedTitle;
    private float speedToday;
    private String speedTrend;

    // Endurance section
    private String enduranceTitle;
    private float enduranceToday;
    private String enduranceTrend;

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
    private String sensoryAtaxiaTrend;

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
    private String dualTaskTrend;

    private String currentValueBaseline;
    private String reportId;
    public ReportModel() {
        this.reportId = UUID.randomUUID().toString();
        this.timestamp = new Date();
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
}