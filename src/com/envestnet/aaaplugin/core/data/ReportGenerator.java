package com.envestnet.aaaplugin.core.data;

import com.envestnet.aaaplugin.util.config.IssueTypeConfigReader;
import com.envestnet.aaaplugin.util.config.IssueType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportGenerator {
    private ProjectStats projectStats;
    private Map<String, List<AntiPattern>> antiPatterns = new HashMap<>();
    private Map<String, List<DesignFlaw>> designFlaws = new HashMap<>();
    private IssueTypeConfigReader configReader;

    public ReportGenerator(String configPath) {
        configReader = new IssueTypeConfigReader(configPath);
    }

    public void setProjectStats(ProjectStats projectStats) {
        this.projectStats = projectStats;
    }

    public void addAntiPattern(AntiPattern antiPattern) {
        IssueType issueType = configReader.getIssueType(antiPattern.getIssueType());
        if (issueType != null && issueType.isDetect()) {
            antiPattern.setSeverity(issueType.getSeverity());
            antiPatterns.computeIfAbsent(antiPattern.getIssueType(), k -> new ArrayList<>()).add(antiPattern);
        }
    }

    public void addDesignFlaw(DesignFlaw designFlaw) {
        IssueType issueType = configReader.getIssueType(designFlaw.getIssueType());
        if (issueType != null && issueType.isDetect()) {
            designFlaw.setSeverity(issueType.getSeverity());
            designFlaws.computeIfAbsent(designFlaw.getIssueType(), k -> new ArrayList<>()).add(designFlaw);
        }
    }
    
    public void checkDefault() {
        String[] antiPatternTypes = {"multipleAAA", "missingAssert", "assertPrecondition"};
        String[] designFlawTypes = {"suppressedException", "arrangeAndQuit", "ObscureAssert"};

        for (String type : antiPatternTypes) {
            antiPatterns.putIfAbsent(type, new ArrayList<>());
        }
        for (String type : designFlawTypes) {
            designFlaws.putIfAbsent(type, new ArrayList<>());
        }
    }


    public void generateReport(String fileName) {
    	System.out.println("Generating report at " + fileName);

        try (FileWriter writer = new FileWriter(fileName)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Report report = new Report();
            report.setProjectStats(projectStats != null ? projectStats : new ProjectStats());
            report.setAntiPatterns(antiPatterns);
            report.setDesignFlaws(designFlaws);
            
            gson.toJson(report, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Report {
        private ProjectStats projectStats;
        private Map<String, List<AntiPattern>> antiPatterns;
        private Map<String, List<DesignFlaw>> designFlaws;

        public ProjectStats getProjectStats() {
            return projectStats;
        }

        public void setProjectStats(ProjectStats projectStats) {
            this.projectStats = projectStats;
        }

        public Map<String, List<AntiPattern>> getAntiPatterns() {
            return antiPatterns;
        }

        public void setAntiPatterns(Map<String, List<AntiPattern>> antiPatterns) {
            this.antiPatterns = antiPatterns;
        }

        public Map<String, List<DesignFlaw>> getDesignFlaws() {
            return designFlaws;
        }

        public void setDesignFlaws(Map<String, List<DesignFlaw>> designFlaws) {
            this.designFlaws = designFlaws;
        }
    }

    // main method for testing
//    public static void main(String[] args) {
//        ReportGenerator generator = new ReportGenerator();
//        // Set project stats and add anti-patterns and design flaws
//        List<Integer> lineNumbers = new ArrayList<>();
//        lineNumbers.add(1);
//        generator.addAntiPattern(new AntiPattern("sourcefilePath", "methodName",
//                "classname", lineNumbers,
//                "Missing Assert", "description", "severity"));
//
//        // Finally, generate the report
//        generator.generateReport("results.json");
//    }
}
