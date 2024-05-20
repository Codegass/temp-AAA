package com.envestnet.aaaplugin.util.config;

public class IssueType {
    private String severity;
    private boolean detect;

    public IssueType() {
        // Default values
        this.severity = "Info";
        this.detect = true;
    }

    // Constructor with parameters
    public IssueType(String severity, boolean detect) {
        this.severity = severity;
        this.detect = detect;
    }

    // Getters
    public String getSeverity() {
        return severity;
    }

    public boolean isDetect() {
        return detect;
    }

    // Setters
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setDetect(boolean detect) {
        this.detect = detect;
    }

    @Override
    public String toString() {
        return "IssueType{" +
                "severity='" + severity + '\'' +
                ", detect=" + detect +
                '}';
    }
}

