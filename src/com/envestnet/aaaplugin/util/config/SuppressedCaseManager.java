package com.envestnet.aaaplugin.util.config;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SuppressedCaseManager {
    private List<String[]> suppressedCases;
    private String filePath;

    public SuppressedCaseManager(String filePath) {
        this.filePath = filePath;
        this.suppressedCases = new ArrayList<>();
        loadSuppressedCases();
    }

    private void loadSuppressedCases() {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            reader.readNext(); // Skip header row
            while ((nextLine = reader.readNext()) != null) {
                suppressedCases.add(nextLine);
            }
        } catch (IOException | CsvException e) {
            System.err.println("Error reading the suppressed cases file: " + e.getMessage());
        }
    }

    public void addSuppressedCase(String path, String caseName, String issueType, String Lines,String reason) {
        suppressedCases.add(new String[]{path, caseName, issueType, Lines, reason});
    }

    public void removeSuppressedCase(String path, String caseName, String issueType) {
        suppressedCases.removeIf(suppressedCase -> suppressedCase[0].equals(path) && suppressedCase[1].equals(caseName) && suppressedCase[2].equals(issueType));
    }

    private void removeDuplicateSuppressedCases() {
        List<String[]> uniqueSuppressedCases = new ArrayList<>();
        for (String[] suppressedCase : suppressedCases) {
            if (!uniqueSuppressedCases.contains(suppressedCase)) {
                uniqueSuppressedCases.add(suppressedCase);
            }
        }
        suppressedCases = uniqueSuppressedCases;
    }

    public void saveSuppressedCases() {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            String[] header = {"Path", "CaseName", "IssueType", "Lines", "Reason"}; // Define the header
            writer.writeNext(header); // Write header to the CSV file
            removeDuplicateSuppressedCases();
            for (String[] suppressedCase : suppressedCases) {
                writer.writeNext(suppressedCase);
            }
        } catch (IOException e) {
            System.err.println("Error writing to the suppressed cases file: " + e.getMessage());
        }
    }

    public List<String[]> getSuppressedCases() {
        return suppressedCases;
    }
}

