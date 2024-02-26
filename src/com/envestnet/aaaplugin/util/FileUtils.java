package com.envestnet.aaaplugin.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
	private static final String[] testKeyWords = {"tests", "test", "testing", "tester"};

	public static boolean isTest(File file) {
		for (String keyword : testKeyWords)
			if (file.getAbsolutePath().toLowerCase().contains(keyword + File.separatorChar) && file.getAbsolutePath().endsWith(".java"))
				return true;
		return false;
	}

	public static boolean isProduction(File file) {
		for (String keyword : testKeyWords)
			if (file.getAbsolutePath().toLowerCase().contains(keyword + File.separatorChar)) return false;
		return file.getAbsolutePath().endsWith(".java");
	}
	
	public static List<String> findCsvFiles(String directoryPath) {
        File folder = new File(directoryPath);
        File[] listOfFiles = folder.listFiles();
        List<String> csvFiles = new ArrayList<>();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile() && file.getName().endsWith(".csv")) {
                    csvFiles.add(file.getAbsolutePath());
                }
            }
        }

        return csvFiles;
    }

    public static String extractPart(String fileName) {

        String[] parts = fileName.split("_");
        if (parts.length > 1) {

            String[] subParts = parts[1].split("\\.");
            return subParts[0];
        }
        return "";
    }

}
