package com.envestnet.aaaplugin.util.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class PackageSupportConfigReader {
    private Map<String, List<Map<String, List<String>>>> config;

    public PackageSupportConfigReader(String filePath) {
        loadConfig(filePath);
    }

    private void loadConfig(String filePath) {
        try {
            InputStream inputStream = new FileInputStream(filePath);
            Yaml yaml = new Yaml();
            this.config = yaml.load(inputStream);
            validateConfig();
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

    public List<Map<String, List<String>>> getConfigurationSection(String sectionKey) {
        return config.get(sectionKey);
    }
}

