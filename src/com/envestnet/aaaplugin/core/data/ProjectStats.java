package com.envestnet.aaaplugin.core.data;

public class ProjectStats {
    private String projectName;
    private int totalTestSuite;
    private int totalTestCase;
    private int totalStatement;

    public ProjectStats(String projectName, int totalTestSuite, int totalTestCase, int totalStatement) {
        this.projectName = projectName;
        this.totalTestSuite = totalTestSuite;
        this.totalTestCase = totalTestCase;
        this.totalStatement = totalStatement;
    }

    public ProjectStats() {
        this.projectName = ""; // or some default value
        this.totalTestSuite = 0;
        this.totalTestCase = 0;
        this.totalStatement = 0;
    }


    // Getters
    public String getProjectName() {
        return projectName;
    }

    public int getTotalTestSuite() {
        return totalTestSuite;
    }

    public int getTotalTestCase() {
        return totalTestCase;
    }

    public int getTotalStatement() {
        return totalStatement;
    }
}

