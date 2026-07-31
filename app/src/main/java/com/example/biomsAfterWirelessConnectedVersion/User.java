package com.example.biomsAfterWirelessConnectedVersion;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class User {
    private String name;
    private String username;
    private String password;
    private String email;
    private String registrationDate;

    public User() {}

    public User(String name, String username, String password, String email) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.registrationDate = getCurrentDateTime();
        this.email = email;
    }
    public User(String name, String username, String password, String registrationDate, String email) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.registrationDate = registrationDate;
        this.email = email;
    }

    public String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public String getEmail() { return email;}
    public void setPassword(String password) { this.password = password; }
}
