package com.envestnet.aaaplugin.util.config;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.envestnet.aaaplugin.util.config.data.SuppressedCase;

public class SuppressedCaseManager {
    private List<SuppressedCase> suppressedCases;
    private String filePath;

    public SuppressedCaseManager() {
        this.suppressedCases = new ArrayList<>();
    }

    public void loadSuppressedCases() {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            reader.readNext(); // Skip header row
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length != 6) {
                    System.err.println("Invalid suppressed case record: " + String.join(",", nextLine));
                    continue;
                } else {
                    suppressedCases.add(new SuppressedCase(
                            nextLine[0], nextLine[1], nextLine[2], nextLine[3], nextLine[4], nextLine[5]));
                }
            }
        } catch (IOException | CsvException e) {
            //if the file does not exist, it will be created when saving the suppressed cases
            System.err.println("Error reading the suppressed cases file: " + e.getMessage());
        }
    }

    public void addSuppressedCase(String className, String caseName, String issueType, String Lines,String reason, String path) {
        suppressedCases.add(new SuppressedCase(className, caseName, issueType, Lines, reason, path));
    }

    public void removeSuppressedCase(String caseName, String issueType, String filePath) {
        suppressedCases.removeIf(suppressedCase ->
                suppressedCase.getFilePath().equals(filePath) &&
                        suppressedCase.getCaseName().equals(caseName) &&
                        suppressedCase.getIssueType().equals(issueType)
        );
    }

    private void removeDuplicateSuppressedCases() {
        Set<SuppressedCase> uniqueSuppressedCases = new LinkedHashSet<>(suppressedCases);
        suppressedCases = new ArrayList<>(uniqueSuppressedCases);
    }


    public void saveSuppressedCases() {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            String[] header = {"ClassName", "CaseName", "IssueType", "Lines", "Reason", "Path"}; // Define the header
            writer.writeNext(header); // Write header to the CSV file
            removeDuplicateSuppressedCases();
            for (SuppressedCase sc : suppressedCases) {
                writer.writeNext(new String[]{
                        sc.getClassName(),
                        sc.getCaseName(),
                        sc.getIssueType(),
                        sc.getLines(),
                        sc.getReason(),
                        sc.getFilePath()
                });
            }
        } catch (IOException e) {
            System.err.println("Error writing to the suppressed cases file: " + e.getMessage());
        }
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<SuppressedCase> getSuppressedCases() {
        return suppressedCases;
    }
}

