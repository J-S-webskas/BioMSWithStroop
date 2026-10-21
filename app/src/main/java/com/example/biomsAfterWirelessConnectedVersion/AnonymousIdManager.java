package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Random;

/**
 * The patient's anonymous ID — an auto-generated code like "Patient4821"
 * that stands in for the patient everywhere the app identifies them
 * outside the device: report screens, exported file names, and shared
 * data. The patient's real name/username is never used for those.
 *
 * Generated once, stored in SharedPreferences, and stable for the life of
 * the install, so the same patient keeps the same ID across every test
 * and report.
 */
public class AnonymousIdManager {

    private static final String PREFS = "patient_identity";
    private static final String KEY_ANON_ID = "anonymous_id";

    public static String getId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String id = prefs.getString(KEY_ANON_ID, null);
        if (id == null || id.isEmpty()) {
            id = "Patient" + (1000 + new Random().nextInt(9000));
            prefs.edit().putString(KEY_ANON_ID, id).apply();
        }
        return id;
    }
}