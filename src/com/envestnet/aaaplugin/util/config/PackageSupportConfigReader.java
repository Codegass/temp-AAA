package com.envestnet.aaaplugin.util.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.envestnet.aaaplugin.util.config.data.MethodInvocationRule;
import com.envestnet.aaaplugin.util.config.data.MethodRule;

public class PackageSupportConfigReader {
    private Map<String, List<Map<String, List<String>>>> config;
    private Map<String, MethodInvocationRule> ruleSections;

    public PackageSupportConfigReader(String filePath) {
        loadConfig(filePath);
    }

    private void loadConfig(String filePath) {
        try {
            InputStream inputStream = new FileInputStream(filePath);
            Yaml yaml = new Yaml();
            this.config = yaml.load(inputStream);
            validateConfig();
            processConfig();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Configuration file not found: " + filePath, e);
        } catch (YAMLException e) {
            throw new RuntimeException("Error parsing YAML configuration", e);
        }
    }

    private void validateConfig() {
        if (config == null) {
            throw new RuntimeException("No configuration data found in YAML");
        }
        // validation - check for required top-level keys
        if (!config.containsKey("asserts") || !config.containsKey("junitExpectedException") || !config.containsKey("mock")) {
            throw new RuntimeException("Configuration is missing required sections");
        }
    }

    private void processConfig() {
        ruleSections = config.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> createRuleFromSection(entry.getValue())
        ));
    }

    private MethodInvocationRule createRuleFromSection(List<Map<String, List<String>>> section) {
        List<Pattern> packagePatterns = new ArrayList<>();
        List<MethodRule> methodRules = new ArrayList<>();
        List<MethodRule> excludeRules = new ArrayList<>();

        for (Map<String, List<String>> rule : section) {
            if (rule.containsKey("packages")) {
                packagePatterns.addAll(rule.get("packages").stream()
                        .map(Pattern::compile)
                        .collect(Collectors.toList()));
            }
            if (rule.containsKey("methods")) {
                methodRules.addAll(rule.get("methods").stream()
                        .map(this::createMethodRule)
                        .collect(Collectors.toList()));
            }
            if (rule.containsKey("excludeMethods")) {
                excludeRules.addAll(rule.get("excludeMethods").stream()
                        .map(this::createMethodRule)
                        .collect(Collectors.toList()));
            }
        }

        return new MethodInvocationRule(packagePatterns, methodRules, excludeRules);
    }

    private MethodRule createMethodRule(String methodPattern) {
        String[] parts = methodPattern.split("#");
        return new MethodRule(Pattern.compile(parts[0].replaceAll("\\*", ".*")), Pattern.compile(parts[1].replaceAll("\\*", ".*")));
    }

    public boolean matchesRule(String qualifiedClassName, String methodName, String sectionKey) {
        MethodInvocationRule rule = ruleSections.get(sectionKey);
        return rule != null && rule.matches(qualifiedClassName, methodName);
    }

    public List<Map<String, List<String>>> getConfigurationSection(String sectionKey) {
        return config.get(sectionKey);
    }
}

