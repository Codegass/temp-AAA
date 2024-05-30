package com.envestnet.aaaplugin.util.config;

import com.envestnet.aaaplugin.util.config.data.IssueType;
import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssueTypeConfigReader {

    private Map<String, IssueType> issueTypes;
    private String[] excludedTestsPath; // disabled for now

    public IssueTypeConfigReader(String configPath) {
        // Initialize issueTypes with default values
        issueTypes = new HashMap<>();
        issueTypes.put("Multiple-AAA", new IssueType("Minor", true));
        issueTypes.put("Missing Assert", new IssueType("Blocker", true));
        issueTypes.put("Assert Precondition", new IssueType("Info", true));
        issueTypes.put("Obscure Assent", new IssueType("Major", true));
        issueTypes.put("Suppressed Exception", new IssueType("Major", true));
        issueTypes.put("Arrange and Quit", new IssueType("Info", true));

        Yaml yaml = new Yaml();
        FileInputStream inputStream = null;

        try {
            inputStream = new FileInputStream(configPath);

            System.out.println("Reading config file from " + configPath);
            Map<String, Object> configMap = yaml.load(inputStream);

            // Overwrite defaults if values are present in YAML
            Map<String, Map<String, Object>> yamlIssueTypes = (Map<String, Map<String, Object>>) configMap.get("AAAIssueType");

            yamlIssueTypes.forEach((key, value) -> {
                String severity = (String) value.get("Severity");
                Boolean detect = (Boolean) value.get("Detect");
                issueTypes.put(key, new IssueType(severity, detect));
            });

            excludedTestsPath = ((List<String>) configMap.get("ExcludedTestsPath")).toArray(new String[0]);
        } catch (FileNotFoundException e) {
            System.err.println("Error reading the config file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public IssueType getIssueType(String issueTypeName) {
        return issueTypes.getOrDefault(issueTypeName, new IssueType());
    }

    // main method remains the same
    public static void main(String[] args) {
        String projectRootPath = "/Users/chenhaowei/Documents/github/temp-AAA";
        IssueTypeConfigReader reader = new IssueTypeConfigReader(projectRootPath);
        IssueType issueType = reader.getIssueType("Missing Assert");

        if (issueType != null) {
            System.out.println("Severity: " + issueType.getSeverity());
            System.out.println("Detect: " + issueType.isDetect());
        }
    }
}
