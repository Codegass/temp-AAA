package com.envestnet.aaaplugin.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Formater {

    public static String formatLineNumber(String lineNumberStr) {
        try {
            String[] parts = lineNumberStr.replace("[", "").replace("]", "").split(",");
            List<Integer> lineNumbers = new ArrayList<>();
            for (String part : parts) {
                lineNumbers.add(Integer.parseInt(part.trim()));
            }
            Collections.sort(lineNumbers);

            List<int[]> mergedIntervals = new ArrayList<>();
            int start = lineNumbers.get(0), end = lineNumbers.get(0);

            for (int i = 1; i < lineNumbers.size(); i++) {
                if (lineNumbers.get(i) == end || lineNumbers.get(i) == end + 1) {
                    end = lineNumbers.get(i);
                } else {
                    mergedIntervals.add(new int[]{start, end});
                    start = end = lineNumbers.get(i);
                }
            }
            mergedIntervals.add(new int[]{start, end});

            StringBuilder formatted = new StringBuilder();
            for (int[] interval : mergedIntervals) {
                if (interval[0] == interval[1]) {
                    formatted.append(interval[0]).append(", ");
                } else {
                    formatted.append(interval[0]).append("-").append(interval[1]).append(", ");
                }
            }

            if (formatted.length() > 2) {
                formatted.setLength(formatted.length() - 2);
            }

            return formatted.toString();
        } catch (Exception e) {
            System.err.println("Error formatting line numbers: " + e.getMessage());
            return lineNumberStr;
        }
    }
}
