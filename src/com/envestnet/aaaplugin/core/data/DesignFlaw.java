package com.envestnet.aaaplugin.core.data;

import java.util.List;

public class DesignFlaw {
    private String filePath;
    private String testName;
    private String testSuite;
    private List<Integer> lineNumber;
    private String issueType;
    private String description;
    private String severity;

    public DesignFlaw(String filePath, String testName, String testSuite, List<Integer> lineNumber, String issueType, String description, String severity) {
        this.filePath = filePath;
        this.testName = testName;
        this.testSuite = testSuite;
        this.lineNumber = lineNumber;
        this.issueType = issueType;
        this.description = description;
        this.severity = severity;
    }

    // Setters
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public void setTestSuite(String testSuite) {
        this.testSuite = testSuite;
    }

    public void setLineNumber(List<Integer> lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    // Getters
    public String getFilePath() {
        return filePath;
    }

    public String getTestName() {
        return testName;
    }

    public String getTestSuite() {
        return testSuite;
    }

    public List<Integer> getLineNumber() {
        return lineNumber;
    }

    public String getIssueType() {
        return issueType;
    }

    public String getDescription() {
        return description;
    }

    public String getSeverity() {
        return severity;
    }
}

