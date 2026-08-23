package com.example.biomsAfterWirelessConnectedVersion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoReportReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, AutoReportGenerationService.class);
        context.startService(serviceIntent);
    }
}