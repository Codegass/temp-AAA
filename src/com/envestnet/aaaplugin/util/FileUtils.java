package com.envestnet.aaaplugin.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public static String getGitCommitId(String projectRootPath) {
        String gitCommitId = "nogitinfo"; // Default value
        try {
            Path path = Paths.get(projectRootPath, ".git", "logs", "HEAD");
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                if (!lines.isEmpty()) {
                    String lastLine = lines.get(lines.size() - 1);
                    String[] parts = lastLine.split(" ");
                    if (parts.length > 1) {
                        gitCommitId = parts[1];
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gitCommitId;
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
