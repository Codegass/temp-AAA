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
            
            for (String[] record : records) {
                int currentAAA = Integer.parseInt(record[4]); // AAA column
                if (currentAAA == 0) continue; // Ignoring Arrangement
                
                String functionName = record[3]; // Function invocation name TODO:Check Action name are the same
                List<Integer> lineNumbers = parseLineNumbers(record[5]);
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
            
            //BUG! This add all the line number to multiple AAA and missing assert case which does not remove the expanded invocations.
            // Populate line numbers for multipleAAA and missingAssert
            if (multipleAAA || missingAssert) {
                for (String[] record : records) {
                    List<Integer> lineNumbers = parseLineNumbers(record[5]);
                    String qualifiedName = record[4];

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
