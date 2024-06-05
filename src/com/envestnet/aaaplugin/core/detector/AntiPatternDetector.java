package com.envestnet.aaaplugin.core.detector;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AntiPatternDetector {

    private List<Integer> lineNumberMultipleAAA = new ArrayList<>();
    private List<Integer> lineNumberMissingAssert = new ArrayList<>();
    private List<Integer> lineNumberAssertPrecondition = new ArrayList<>();

    public Map<String, Boolean> detectAntiPatterns(String csvFilePath) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new FileReader(csvFilePath))) {
            List<String[]> allRecords = reader.readAll();

            boolean multipleAAA = false;
            boolean missingAssert = true;
            boolean assertPrecondition = false;

            if (allRecords.size() <= 1) { // Only header or no data
                missingAssert = false;
                return createResultMap(multipleAAA, missingAssert, assertPrecondition);
            }

            List<String[]> records = allRecords.subList(1, allRecords.size()); // Drop header row
            Map<String, Integer> actionFunctionNameCounts = new HashMap<>(); // Track counts of each action function name
            
            boolean actionExist = false; // Track the AAA value

            // Convert columns to lists for processing
            List<Integer> aaaColumn = new ArrayList<>();
            List<String> functionNameColumn = new ArrayList<>();
            List<String> lineNumberColumn = new ArrayList<>();
            List<String> qualifiedNameColumn = new ArrayList<>();
            List<Integer> levelColumn = new ArrayList<>();

            for (String[] record : records) {
                aaaColumn.add(Integer.parseInt(record[4]));
                functionNameColumn.add(record[2]);
                lineNumberColumn.add(record[5]);
                qualifiedNameColumn.add(record[3]);
                levelColumn.add(Integer.parseInt(record[8]));
            }

            // process the expanded method and replace with level 0 method for the submethods to keep line number simple
            // NOTE: This may lead to incorrect anti-pattern detection
//            int lastValidIndex = -1;
//            for (int i = 0; i < levelColumn.size(); i++) {
//                if (levelColumn.get(i) > 0) {
//                    if (lastValidIndex != -1) {
//                        aaaColumn.set(i, aaaColumn.get(lastValidIndex));
//                        functionNameColumn.set(i, functionNameColumn.get(lastValidIndex));
//                        lineNumberColumn.set(i, lineNumberColumn.get(lastValidIndex));
//                        qualifiedNameColumn.set(i, qualifiedNameColumn.get(lastValidIndex));
//                    }
//                    lastValidIndex = i;
//                }
//            }

            // Process the lists
            for (int i = 0; i < records.size(); i++) {
                int currentAAA = aaaColumn.get(i);
                if (currentAAA == 0) continue; // Ignoring Arrangement

                String functionName = functionNameColumn.get(i);
                List<Integer> lineNumbers = parseLineNumbers(lineNumberColumn.get(i));
                if (currentAAA == 1) { // Action
                    actionExist = true;
                    actionFunctionNameCounts.put(functionName, actionFunctionNameCounts.getOrDefault(functionName, 0) + 1);
                } else if (currentAAA == 2) { // Assertion
                    missingAssert = false;
                    if (!actionExist) {
                        lineNumberAssertPrecondition.addAll(lineNumbers);
                    }
                }
            }

            // Determine if it's multiple AAA based on actionFunctionNameCounts
            for (Map.Entry<String, Integer> entry : actionFunctionNameCounts.entrySet()) {
                if (entry.getValue() > 1) {
                    multipleAAA = true;
                    break;
                }
            }

            // Populate line numbers for multipleAAA and missingAssert
            if (multipleAAA || missingAssert) {
                for (int i = 0; i < records.size(); i++) {
                    List<Integer> lineNumbers = parseLineNumbers(lineNumberColumn.get(i));
                    String qualifiedName = qualifiedNameColumn.get(i);

                    // Check if the qualifiedName starts with 5 spaces(expanded method), if so skip the line number adding
                    if (qualifiedName.startsWith("     ")) {
                        continue; // Skip to the next iteration of the loop
                    }

                    if (multipleAAA) {
                        lineNumberMultipleAAA.addAll(lineNumbers);
                    }
                    if (missingAssert) {
                        lineNumberMissingAssert.addAll(lineNumbers);
                    }
                }
            }

            return createResultMap(multipleAAA, missingAssert, assertPrecondition);
        }
    }
    
    private List<Integer> parseLineNumbers(String lineNumberStr) {
        List<Integer> lineNumbers = new ArrayList<>();
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(lineNumberStr);
        while (m.find()) {
            lineNumbers.add(Integer.parseInt(m.group()));
        }
        return lineNumbers;
    }

    private Map<String, Boolean> createResultMap(boolean multipleAAA, boolean missingAssert, boolean assertPrecondition) {
        Map<String, Boolean> results = new HashMap<>();
        results.put("multipleAAA", multipleAAA);
        results.put("missingAssert", missingAssert);
        results.put("assertPrecondition", assertPrecondition);
        return results;
    }

    public List<Integer> getLineNumberMultipleAAA() {
        return lineNumberMultipleAAA;
    }

    public List<Integer> getLineNumberMissingAssert() {
        return lineNumberMissingAssert;
    }

    public List<Integer> getLineNumberAssertPrecondition() {
        return lineNumberAssertPrecondition;
    }
}
