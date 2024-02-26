package com.envestnet.aaaplugin.core.detector;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

            if (allRecords.size() == 1) { // Only header, no data
                missingAssert = false;
                return createResultMap(multipleAAA, missingAssert, assertPrecondition);
            }

            int lastAAA = 0;
            int actionQuality = 0;
            
            List<String[]> records = allRecords.subList(1, allRecords.size()); // Drop header row

            for (int i = 0; i < records.size(); i++) {
                String[] record = records.get(i);
                int currentAAA = Integer.parseInt(record[4]); // AAA col
                List<Integer> lineNumbers = parseLineNumbers(record[5]); // Line number col

                if (currentAAA == 0) continue;
                
                if (currentAAA == 1) {
                    actionQuality++;
                }

                if (currentAAA == 2) {
                    missingAssert = false;
                }

                // Multiple AAA
                if (lastAAA == 2 && currentAAA == 1 && actionQuality > 1) {
                    multipleAAA = true;
                    lineNumberMultipleAAA.addAll(lineNumbers);
                }


                if (currentAAA == 2 && lastAAA == 1) {
                    missingAssert = false;
                    if (i > 0) {
                        lineNumberAssertPrecondition.addAll(parseLineNumbers(records.get(i - 1)[5]));
                    }
                }

                lastAAA = currentAAA;
            }

            if (missingAssert) {
                lineNumberMissingAssert.clear();
                for (String[] record : records) {
                    lineNumberMissingAssert.addAll(parseLineNumbers(record[5]));
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
