package com.example.biomsAfterWirelessConnectedVersion;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DataStorageManager {
    private static final String TAG = "DataStorageManager";
    private static final String FILE_EXTENSION = ".json";
    private static final String REGISTRATION_PREFIX = "registration_";
    private static final String EMAIL_PREFIX = "email_";
    private static final String PASSWORD_PREFIX = "password_"; // New prefix
    private static final String SALT_PREFIX = "salt_"; // New prefix for salt
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);


    /**
     * Save assessment to internal storage:
     * Only the application can access these files
     * other applications cannot read or write them
     * Users need root privileges to access them
     *
     *  Sample Json: (for visualization)
     *  {
     *   "timestamp": "2024-12-20T10:30:00Z",
     *   "patientId": "patient_001",
     *   "baseline": {
     *     "walkingSpeed": 1.2,
     *     "balanceScore": 78.5,
     *     "stiffness": {
     *       "knee": 45.2,
     *       "ankle": 32.1
     *     }
     *   },
     *   "dualTask": {
     *     "walkingSpeed": 0.9,
     *     // ...
     *   }
     * }
     *
     *
     JSON file (local storage)
     Load directly into memory
     Display chart
     */

    private static final String PREF_NAME = "app_preferences";
    private static final String KEY_CURRENT_USER = "current_username";

    /**
     * Set the current active user
     */
    public static void setCurrentUser(Context context, String username) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CURRENT_USER, username)
                .apply();
        Log.d(TAG, "Current user set to: " + username);
    }

    private static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.encodeToString(salt, Base64.DEFAULT);
    }

    private static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedPassword = password + salt;
            byte[] hash = digest.digest(saltedPassword.getBytes());
            return Base64.encodeToString(hash, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error hashing password: " + e.getMessage());
            return null;
        }
    }

    public static boolean savePassword(Context context, String username, String password) {
        if (username == null || password == null || password.isEmpty()) {
            Log.e(TAG, "Cannot save empty password for user: " + username);
            return false;
        }

        // Generate salt
        String salt = generateSalt();

        // Hash the password with salt
        String hashedPassword = hashPassword(password, salt);

        if (hashedPassword == null) {
            Log.e(TAG, "Failed to hash password for user: " + username);
            return false;
        }

        // Save salt
        String saltFilename = SALT_PREFIX + username + FILE_EXTENSION;
        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), saltFilename))) {
            writer.write(salt);
            Log.d(TAG, "Salt saved for user: " + username);
        } catch (IOException e) {
            Log.e(TAG, "Error saving salt: " + e.getMessage());
            return false;
        }

        // Save hashed password
        String passwordFilename = PASSWORD_PREFIX + username + FILE_EXTENSION;
        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), passwordFilename))) {
            writer.write(hashedPassword);
            Log.d(TAG, "Password saved securely for user: " + username);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error saving password: " + e.getMessage());
            return false;
        }
    }
    public static boolean verifyPassword(Context context, String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        // Load salt
        String salt = getSalt(context, username);
        if (salt == null) {
            Log.w(TAG, "No salt found for user: " + username);
            return false;
        }

        // Load stored hashed password
        String storedHash = getStoredPasswordHash(context, username);
        if (storedHash == null) {
            Log.w(TAG, "No stored password found for user: " + username);
            return false;
        }

        // Hash the input password with the stored salt
        String hashedInput = hashPassword(password, salt);

        // Compare hashes
        boolean isValid = hashedInput != null && hashedInput.equals(storedHash);
        Log.d(TAG, "Password verification for " + username + ": " + (isValid ? "SUCCESS" : "FAILED"));
        return isValid;
    }

    private static String getStoredPasswordHash(Context context, String username) {
        String filename = PASSWORD_PREFIX + username + FILE_EXTENSION;
        File file = new File(context.getFilesDir(), filename);

        if (!file.exists()) {
            Log.w(TAG, "Password file not found for user: " + username);
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.readLine();
        } catch (IOException e) {
            Log.e(TAG, "Error reading password: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get salt for a user
     */
    private static String getSalt(Context context, String username) {
        String filename = SALT_PREFIX + username + FILE_EXTENSION;
        File file = new File(context.getFilesDir(), filename);

        if (!file.exists()) {
            Log.w(TAG, "Salt file not found for user: " + username);
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.readLine();
        } catch (IOException e) {
            Log.e(TAG, "Error reading salt: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if a user has a password set
     */
    public static boolean hasPassword(Context context, String username) {
        String filename = PASSWORD_PREFIX + username + FILE_EXTENSION;
        File file = new File(context.getFilesDir(), filename);
        return file.exists();
    }

    public static boolean deletePasswordData(Context context, String username) {
        boolean success = true;

        String passwordFilename = PASSWORD_PREFIX + username + FILE_EXTENSION;
        File passwordFile = new File(context.getFilesDir(), passwordFilename);
        if (passwordFile.exists()) {
            success = success && passwordFile.delete();
        }

        String saltFilename = SALT_PREFIX + username + FILE_EXTENSION;
        File saltFile = new File(context.getFilesDir(), saltFilename);
        if (saltFile.exists()) {
            success = success && saltFile.delete();
        }

        if (success) {
            Log.d(TAG, "Password data deleted for user: " + username);
        } else {
            Log.e(TAG, "Error deleting password data for user: " + username);
        }

        return success;
    }

    /**
     * Get the current active user
     */
    public static String getCurrentUser(Context context) {
        String username = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CURRENT_USER, null);
        if (username == null || username.isEmpty()) {
            Log.w(TAG, "No current user set, using default");
            return "default_user";
        }
        return username;
    }

    /**
     * Check if a user is logged in
     */
    public static boolean isUserLoggedIn(Context context) {
        String username = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CURRENT_USER, null);
        return username != null && !username.isEmpty();
    }

    public static void saveEmail(Context context, String username, String email) {
        String filename = EMAIL_PREFIX + username + FILE_EXTENSION;

        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), filename))) {
            writer.write(email);
            Log.d(TAG, "Email saved for user: " + username + " on " + email);
        } catch (IOException e) {
            Log.e(TAG, "Error saving Email: " + e.getMessage());
        }
    }

    /**
     * Check if an email is already registered by any user
     */
    public static boolean emailExists(Context context, String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        File[] files = context.getFilesDir().listFiles();
        if (files == null) {
            return false;
        }

        // Check all email files
        for (File file : files) {
            String name = file.getName();
            if (name.startsWith(EMAIL_PREFIX) && name.endsWith(FILE_EXTENSION)) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String storedEmail = reader.readLine();
                    if (email.equals(storedEmail)) {
                        Log.d(TAG, "Email already exists: " + email);
                        return true;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading email file: " + e.getMessage());
                }
            }
        }
        return false;
    }

    public static String getEmail(Context context, String username) {
        String filename = EMAIL_PREFIX + username + FILE_EXTENSION;
        File file = new File(context.getFilesDir(), filename);

        if (!file.exists()) {
            Log.w(TAG, "Email not found for user: " + username);
            return "Unknown";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String email = reader.readLine();
            Log.d(TAG, "Registration date retrieved for user: " + username + " -> " + email);
            return email;
        } catch (IOException e) {
            Log.e(TAG, "Error reading Email: " + e.getMessage());
            return "Unknown";
        }
    }

    public static void saveRegistrationDate(Context context, String username, String registrationDate) {
        String filename = REGISTRATION_PREFIX + username + FILE_EXTENSION;

        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), filename))) {
            writer.write(registrationDate);
            Log.d(TAG, "Registration date saved for user: " + username + " on " + registrationDate);
        } catch (IOException e) {
            Log.e(TAG, "Error saving registration date: " + e.getMessage());
        }
    }

    /**
     * Get the registration date for a user
     */
    public static String getRegistrationDate(Context context, String username) {
        String filename = REGISTRATION_PREFIX + username + FILE_EXTENSION;
        File file = new File(context.getFilesDir(), filename);

        if (!file.exists()) {
            Log.w(TAG, "Registration date not found for user: " + username);
            return "Unknown";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String registrationDate = reader.readLine();
            Log.d(TAG, "Registration date retrieved for user: " + username + " -> " + registrationDate);
            return registrationDate;
        } catch (IOException e) {
            Log.e(TAG, "Error reading registration date: " + e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Get the registration date for the current user
     */
    public static String getCurrentUserRegistrationDate(Context context) {
        String username = getCurrentUser(context);
        return getRegistrationDate(context, username);
    }

    /**
     * Get formatted registration date for display (e.g., "Member since: January 15, 2026")
     */
    public static String getFormattedRegistrationDate(Context context, String username) {
        String regDateStr = getRegistrationDate(context, username);

        if (regDateStr.equals("Unknown")) {
            return "Member since: Unknown";
        }

        try {
            // Parse the stored date (format: "yyyy-MM-dd HH:mm:ss")
            SimpleDateFormat storedFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date date = storedFormat.parse(regDateStr);

            // Format for display (e.g., "January 15, 2026")
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
            return "Member since: " + displayFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting registration date: " + e.getMessage());
            return "Member since: " + regDateStr;
        }
    }

    /**
     * Clear current user session (without deleting data)
     */
    public static void clearCurrentUser(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_CURRENT_USER)
                .apply();
        Log.d(TAG, "Current user cleared");
    }

    public static void saveAssessment(Context context, AssessmentData assessment) throws IOException {
        String username = getCurrentUser(context);
        // Creating file name
        String filename = "assessment_" + username + "_" + assessment.getSessionId() + FILE_EXTENSION;
        // Gson instance
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")  // ISO 8601 standard Ex. 2024-12-20T10:30:00Z
                .serializeSpecialFloatingPointValues()
                .create();;

        // Convert the object to a JSON string.
        String json = gson.toJson(assessment);

        // write file
        // context.getFilesDir(): Returns the directory of files in the application's internal storage.
        // Example file path: /data/data/com.example.myappication/files/assessment_550e8400-e29b-41d4-a716-446655440000.json
        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), filename))) {
            writer.write(json);
            Toast.makeText(context,
                    "test saved. Next test available tomorrow.",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Load ALL assessments from storage
     */
    public static List<AssessmentData> loadAllAssessments(Context context) {
        String username = getCurrentUser(context);
        List<AssessmentData> allAssessments = new ArrayList<>();
        List<String> assessmentIds = listAssessments(context);  // Get a list of file names

        File[] files = context.getFilesDir().listFiles();

        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                // Only load files for this username
                if (name.startsWith("assessment_" + username + "_") && name.endsWith(FILE_EXTENSION)) {
                    String sessionId = name
                            .replace("assessment_" + username + "_", "")
                            .replace(FILE_EXTENSION, "");
                    AssessmentData assessment = loadAssessment(context, username, sessionId);
                    if (assessment != null) {
                        allAssessments.add(assessment);
                    }
                }
            }
        }

//        for (String sessionId : assessmentIds) {
//            String name = file.getName();
//            AssessmentData assessment = loadAssessment(context, sessionId);
//            if (assessment != null) {
//                allAssessments.add(assessment);
//            }
//        }

        // Sort by timestamp (newest first)
        Collections.sort(allAssessments, (a1, a2) ->
                a2.getTimestamp().compareTo(a1.getTimestamp()));

        return allAssessments;
    }

    /**
     * Load assessment from internal storage
     */
    public static AssessmentData loadAssessment(Context context, String username, String sessionId) {
        String filename = "assessment_" + username + "_" + sessionId + FILE_EXTENSION;

        try (FileReader reader = new FileReader(new File(context.getFilesDir(), filename))) {
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .serializeSpecialFloatingPointValues()
                    .create();

            return gson.fromJson(reader, AssessmentData.class);

        } catch (IOException e) {
            Log.e(TAG, "Error loading JSON assessment: " + e.getMessage());
            return null;
        }
    }

    /**
     * List all saved assessments
     */
    public static List<String> listAssessments(Context context) {
        String username = getCurrentUser(context);
        List<String> assessments = new ArrayList<>();
        File[] files = context.getFilesDir().listFiles();

        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.startsWith("assessment_" + username + "_") && name.endsWith(FILE_EXTENSION)) {
                    String sessionId = name
                            .replace("assessment_" + username + "_", "")
                            .replace(FILE_EXTENSION, "");
                    assessments.add(sessionId);
                }
            }
        }

        return assessments;
    }

    private static boolean isAssessmentEmpty(AssessmentData assessment) {
        if (assessment == null) return true;

        // Check if all test dates are null
        boolean hasARJDate = assessment.getARJAndBalanceTestDate() != null;
        boolean hasWSDate = assessment.getWSAndEnduranceTestDate() != null;
        boolean hasDualTaskDate = assessment.getDualTaskTestDate() != null;
        boolean hasSensoryDate = assessment.getSensoryAtaxiaTestDate() != null;

        // If any test date exists, assessment is not empty
        if (hasARJDate || hasWSDate || hasDualTaskDate || hasSensoryDate) {
            return false;
        }

        // Check if any scores exist (even if dates are null)
        BaselineAssessment baseline = assessment.getBaseline();
        DualTaskAssessment dualTask = assessment.getDualTask();
        SensoryAtaxiaAssessment sensoryAtaxia = assessment.getSensoryAtaxia();

        boolean hasBalance = baseline.getBalanceScore() != 0f;
        boolean hasWalkingSpeed = baseline.getWalkingSpeed() != 0f;
        boolean hasEndurance = baseline.getEnduranceScore() != 0f;
        boolean hasARJ = baseline.getARJ() != null && !baseline.getARJ().isEmpty();
        boolean hasDualTaskBalance = dualTask.getBalanceScore() != 0f;
        boolean hasDualTaskARJ = dualTask.getARJ() != null && !dualTask.getARJ().isEmpty();
        boolean hasSensoryBalance = sensoryAtaxia.getBalanceScore() != 0f;
        boolean hasSensoryARJ = sensoryAtaxia.getARJ() != null && !sensoryAtaxia.getARJ().isEmpty();

        // Also check comparisons
        TestComparisons comparisons = assessment.getComparisons();
        boolean hasComparisons = comparisons != null && (
                comparisons.getDualTaskBalanceChange() != 0f ||
                        comparisons.getDualTaskARJLeftKneeChange() != 0f ||
                        comparisons.getDualTaskARJRightKneeChange() != 0f ||
                        comparisons.getSensoryAtaxiaBalanceChange() != 0f ||
                        comparisons.getSensoryAtaxiaARJLeftKneeChange() != 0f ||
                        comparisons.getSensoryAtaxiaARJRightKneeChange() != 0f
        );

        return !(hasBalance || hasWalkingSpeed || hasEndurance || hasARJ ||
                hasDualTaskBalance || hasDualTaskARJ ||
                hasSensoryBalance || hasSensoryARJ ||
                hasComparisons);
    }

    public static boolean userExists(Context context, String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        // Check if password file exists (most reliable indicator)
        String passwordFilename = PASSWORD_PREFIX + username + FILE_EXTENSION;
        File passwordFile = new File(context.getFilesDir(), passwordFilename);

        // Alternative: Check for email or registration date
        // String emailFilename = EMAIL_PREFIX + username + FILE_EXTENSION;
        // File emailFile = new File(context.getFilesDir(), emailFilename);
        // return emailFile.exists() || passwordFile.exists();

        return passwordFile.exists();
    }

    /**
     * Delete an assessment
     */
    public static boolean deleteAssessment(Context context, String dateStr, String assessmentType) {
        List<AssessmentData> allAssessments = DataStorageManager.loadAllAssessments(context);
        AssessmentData targetAssessment = null;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        // Find the assessment for the selected date
//        for (AssessmentData assessment : allAssessments) {
//            String assessmentDate = sdf.format(assessment.getTimestamp());
//            if (assessmentDate.equals(dateStr)) {
//                targetAssessment = assessment;
//                break;
//            }
//        }

        for (AssessmentData assessment : allAssessments) {
            String assessmentDate = null;

            // Check which test type we're deleting and use the correct date
            // Find the latest date based on the stored test date for the assessments
            switch (assessmentType) {
                case "ARJ and balance":
                    if (assessment.getARJAndBalanceTestDate() != null) {
                        assessmentDate = sdf.format(assessment.getARJAndBalanceTestDate());
                    }
                    break;
                case "Dual Task":
                    if (assessment.getDualTaskTestDate() != null) {
                        assessmentDate = sdf.format(assessment.getDualTaskTestDate());
                    }
                    break;
                case "Sensory Ataxia":
                    if (assessment.getSensoryAtaxiaTestDate() != null) {
                        assessmentDate = sdf.format(assessment.getSensoryAtaxiaTestDate());
                    }
                    break;
                case "Speed and endurance":
                    if (assessment.getWSAndEnduranceTestDate() != null) {
                        assessmentDate = sdf.format(assessment.getWSAndEnduranceTestDate());
                    }
                    break;
            }

            if (assessmentDate != null && assessmentDate.equals(dateStr)) {
                targetAssessment = assessment;
                break;
            }
        }

        if (targetAssessment == null) {
            Toast.makeText(context, "No assessment found for " + dateStr, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "deleteAssessment: No assessment found for is entered and method returned");
            return false;
        }

        // ✅ Clear ONLY the selected test type data
        switch (assessmentType) {
            case "ARJ and balance":
                // Clear Baseline data
                targetAssessment.getBaseline().setBalanceScore(0f);
                targetAssessment.getBaseline().setARJ(new HashMap<>());
                targetAssessment.getBaseline().setWalkingSpeed(0f);
                targetAssessment.getBaseline().setEnduranceScore(0f);
                targetAssessment.setARJAndBalanceTestDate(null);

                // Clear comparisons
                if (targetAssessment.getComparisons() != null) {
                    targetAssessment.getComparisons().setDualTaskBalanceChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJRightKneeChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaBalanceChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJRightKneeChange(0f);
                }
                break;

            case "Dual Task":
                // ✅ Clear ONLY Dual Task data
                targetAssessment.getDualTask().setBalanceScore(0f);
                targetAssessment.getDualTask().setARJ(new HashMap<>());
                targetAssessment.setDualTaskTestDate(null);

                // Clear ONLY Dual Task comparisons
                if (targetAssessment.getComparisons() != null) {
                    targetAssessment.getComparisons().setDualTaskBalanceChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJRightKneeChange(0f);
                }
                break;

            case "Sensory Ataxia":
                // ✅ Clear ONLY Sensory Ataxia data
                targetAssessment.getSensoryAtaxia().setBalanceScore(0f);
                targetAssessment.getSensoryAtaxia().setARJ(new HashMap<>());
                targetAssessment.setSensoryAtaxiaTestDate(null);

                // Clear ONLY Sensory Ataxia comparisons
                if (targetAssessment.getComparisons() != null) {
                    targetAssessment.getComparisons().setSensoryAtaxiaBalanceChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJRightKneeChange(0f);
                }
                break;

            case "Speed and endurance":
                // ✅ Clear ONLY Speed data
                targetAssessment.getBaseline().setWalkingSpeed(0f);
                targetAssessment.getBaseline().setEnduranceScore(0f);
                targetAssessment.setWSAndEnduranceTestDate(null);
                break;
        }
        return true;
    }

    public static AssessmentData getOrCreateTodayAssessment(Context context) {
        String username = getCurrentUser(context);
        String todayDateString = DATE_FORMAT.format(new Date());

        // Load all assessments
        List<AssessmentData> allAssessments = loadAllAssessments(context);

        // Look for today's assessment
        for (AssessmentData assessment : allAssessments) {
            // Check by timestamp (session creation date)
            if (assessment.getTimestamp() != null) {
                String assessmentDate = DATE_FORMAT.format(assessment.getTimestamp());
                if (assessmentDate.equals(todayDateString)) {
                    return assessment; // Found today's assessment
                }
            }

            // Also check by any test date
            if (assessment.getARJAndBalanceTestDate() != null &&
                    DATE_FORMAT.format(assessment.getARJAndBalanceTestDate()).equals(todayDateString)) {
                return assessment;
            }
            if (assessment.getWSAndEnduranceTestDate() != null &&
                    DATE_FORMAT.format(assessment.getWSAndEnduranceTestDate()).equals(todayDateString)) {
                return assessment;
            }
            if (assessment.getDualTaskTestDate() != null &&
                    DATE_FORMAT.format(assessment.getDualTaskTestDate()).equals(todayDateString)) {
                return assessment;
            }
            if (assessment.getSensoryAtaxiaTestDate() != null &&
                    DATE_FORMAT.format(assessment.getSensoryAtaxiaTestDate()).equals(todayDateString)) {
                return assessment;
            }
        }

        // No assessment found for today - create new one
        AssessmentData newAssessment = new AssessmentData(username + "_" + todayDateString);
        Toast.makeText(context, "new assessment", Toast.LENGTH_SHORT).show();
        return newAssessment;
    }

    public static boolean setMass(Context context, double mass) throws IOException {
        String username = getCurrentUser(context);
        String filename = "mass_" + username + FILE_EXTENSION;

        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), filename))) {
            writer.write(String.valueOf(mass));
            Toast.makeText(context, "Saved weight for user: " + username, Toast.LENGTH_SHORT).show();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static double getMass(Context context) {
        String username = getCurrentUser(context);
        String filename = "mass_" + username + FILE_EXTENSION;
        File file = new File(context.getFilesDir(), filename);

        if (!file.exists()) {
            Toast.makeText(context, "Mass not found for user: " + username, Toast.LENGTH_SHORT).show();
            return 0;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return Double.parseDouble(reader.readLine());
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static boolean setSex(Context context, String sex) throws IOException {
        String username = getCurrentUser(context);
        String filename = "sex_" + username + FILE_EXTENSION;

        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), filename))) {
            writer.write(sex);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getSex(Context context) {
        String username = getCurrentUser(context);
        String filename = "sex_" + username + FILE_EXTENSION;
        File file = new File(context.getFilesDir(), filename);

        if (!file.exists()) {
            return "Not Found";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "Not Found";
        }
    }

    private static boolean deleteAssessmentFile(Context context, String sessionId) {
        String username = getCurrentUser(context);
        String filename = "assessment_" + username + "_" + sessionId + FILE_EXTENSION;
        File file = new File(context.getFilesDir(), filename);

        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Log.d(TAG, "Deleted empty assessment file: " + filename);
            } else {
                Log.e(TAG, "Failed to delete assessment file: " + filename);
            }
            return deleted;
        }
        return false;
    }
    public static boolean deleteAssessmentByDate(Context context, String dateStr, String assessmentType) {
        String username = getCurrentUser(context);
        List<AssessmentData> allAssessments = loadAllAssessments(context);
        AssessmentData targetAssessment = null;

        // Find the assessment for the selected date
        for (AssessmentData assessment : allAssessments) {
            String assessmentDate = DATE_FORMAT.format(assessment.getTimestamp());
            if (assessmentDate.equals(dateStr)) {
                targetAssessment = assessment;
                break;
            }
        }

        if (targetAssessment == null || (allAssessments.size() == 1 && isAssessmentEmpty(targetAssessment))) {
            Log.d(TAG, "No assessment found for date: " + dateStr);
            if (allAssessments.size() == 1) {
                deleteAssessmentFile(context, targetAssessment.getSessionId());
            }
            return false;
        }

        // Clear only the selected test type data
        switch (assessmentType) {
            case "ARJ and balance":
                targetAssessment.getBaseline().setBalanceScore(0f);
                targetAssessment.getBaseline().setARJ(new HashMap<>());
                targetAssessment.getBaseline().setWalkingSpeed(0f);
                targetAssessment.getBaseline().setEnduranceScore(0f);
                targetAssessment.setARJAndBalanceTestDate(null);

                // Clear comparisons
                if (targetAssessment.getComparisons() != null) {
                    targetAssessment.getComparisons().setDualTaskBalanceChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJRightKneeChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaBalanceChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJRightKneeChange(0f);
                }
                Toast.makeText(context, "ARJ and balance data cleared for " + dateStr, Toast.LENGTH_SHORT).show();
                break;

            case "Dual Task":
                targetAssessment.getDualTask().setBalanceScore(0f);
                targetAssessment.getDualTask().setARJ(new HashMap<>());
                targetAssessment.setDualTaskTestDate(null);

                if (targetAssessment.getComparisons() != null) {
                    targetAssessment.getComparisons().setDualTaskBalanceChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setDualTaskARJRightKneeChange(0f);
                }
                Toast.makeText(context, "Dual Task data cleared for " + dateStr, Toast.LENGTH_SHORT).show();
                break;

            case "Sensory Ataxia":
                targetAssessment.getSensoryAtaxia().setBalanceScore(0f);
                targetAssessment.getSensoryAtaxia().setARJ(new HashMap<>());
                targetAssessment.setSensoryAtaxiaTestDate(null);

                if (targetAssessment.getComparisons() != null) {
                    targetAssessment.getComparisons().setSensoryAtaxiaBalanceChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJLeftKneeChange(0f);
                    targetAssessment.getComparisons().setSensoryAtaxiaARJRightKneeChange(0f);
                }
                Toast.makeText(context, "Sensory Ataxia data cleared for " + dateStr, Toast.LENGTH_SHORT).show();
                break;

            case "Speed and endurance":
                targetAssessment.getBaseline().setWalkingSpeed(0f);
                targetAssessment.getBaseline().setEnduranceScore(0f);
                targetAssessment.setWSAndEnduranceTestDate(null);
                Toast.makeText(context, "Speed and endurance data cleared for " + dateStr, Toast.LENGTH_SHORT).show();
                break;

            default:
                return false;
        }

        // Save the updated assessment back
        try {
            saveAssessment(context, targetAssessment);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error saving assessment after deletion: " + e.getMessage());
            Toast.makeText(context, "Error saving changes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public static void saveReport(Context context, ReportModel report) throws IOException {
        String username = getCurrentUser(context);
        // Creating file name
        String filename = "report_" + username + "_" + report.getReportId() + FILE_EXTENSION;
        // Gson instance
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")  // ISO 8601 standard Ex. 2024-12-20T10:30:00Z
                .serializeSpecialFloatingPointValues() // handle Nan and Infinity
                .create();

        // Convert the object to a JSON string.
        String json = gson.toJson(report);

        // write file
        // context.getFilesDir(): Returns the directory of files in the application's internal storage.
        // Example file path: /data/data/com.example.myappication/files/assessment_550e8400-e29b-41d4-a716-446655440000.json
        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), filename))) {
            writer.write(json);
        }
    }
    public static List<ReportModel> loadAllReports(Context context) {
        String username = getCurrentUser(context);
        List<ReportModel> allReports = new ArrayList<>();
        File[] files = context.getFilesDir().listFiles();

        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.startsWith("report_" + username + "_") && name.endsWith(FILE_EXTENSION)) {
                    String reportId = name
                            .replace("report_" + username + "_", "")
                            .replace(FILE_EXTENSION, "");
                    ReportModel report = loadReport(context, username, reportId);
                    if (report != null) {
                        allReports.add(report);
                    }
                }
            }
        }

        // Sort by timestamp (newest first)
        Collections.sort(allReports, (r1, r2) ->
                r2.getTimeStamp().compareTo(r1.getTimeStamp()));

        return allReports;
    }

    public static List<String> listReports(Context context) {
        String username = getCurrentUser(context);
        List<String> reports = new ArrayList<>();
        File[] files = context.getFilesDir().listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("report_" + username + "_") && file.getName().endsWith(FILE_EXTENSION)) {
                    String reportId  = file.getName()
                            .replace("report_" + username + "_", "")
                            .replace(FILE_EXTENSION, "");
                    reports.add(reportId );
                }
            }
        }
        return reports;
    }

    public static ReportModel loadReport(Context context, String username, String reportId) {
        String filename = "report_" + username + "_" + reportId + FILE_EXTENSION;

        try (FileReader reader = new FileReader(new File(context.getFilesDir(), filename))) {
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .serializeSpecialFloatingPointValues()
                    .create();

            return gson.fromJson(reader, ReportModel.class);

        } catch (IOException e) {
            Log.e(TAG, "Error loading JSON assessment: " + e.getMessage());
            return null;
        }
    }

    public static boolean deleteReport(Context context, ReportModel report) {
        // Get the report ID or filename
        String username = getCurrentUser(context);
        String reportId = report.getReportId();
        String filename = "report_" + username + "_" + reportId + ".json";
        File file = new File(context.getFilesDir(), filename);

        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    public static boolean deleteAllUserData(Context context) {
        String username = getCurrentUser(context);
        boolean success = true;
        File[] files = context.getFilesDir().listFiles();

        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                // Delete all files belonging to this user
                if (name.startsWith("assessment_" + username + "_") ||
                        name.startsWith("mass_" + username) ||
                        name.startsWith("sex_" + username) ||
                        name.startsWith("report_" + username + "_")) {
                    if (!file.delete()) {
                        Log.e(TAG, "Failed to delete: " + name);
                        success = false;
                    }
                }
            }
        }

        if (success) {
            clearCurrentUser(context);
            Log.d(TAG, "All data deleted for user: " + username);
        }

        return success;
    }

    /**
     * List all users who have data on this device
     */
    public static List<String> getAllUsers(Context context) {
        Set<String> users = new HashSet<>();
        File[] files = context.getFilesDir().listFiles();

        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                // Extract username from filename
                if (name.startsWith("assessment_")) {
                    String[] parts = name.split("_");
                    if (parts.length >= 2) {
                        users.add(parts[1]); // username is the second part
                    }
                } else if (name.startsWith("mass_")) {
                    String username = name.replace("mass_", "").replace(FILE_EXTENSION, "");
                    users.add(username);
                } else if (name.startsWith("sex_")) {
                    String username = name.replace("sex_", "").replace(FILE_EXTENSION, "");
                    users.add(username);
                }
            }
        }

        return new ArrayList<>(users);
    }
}
