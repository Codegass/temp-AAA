package com.envestnet.aaaplugin.util.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PackageSupportConfigWriter {

    private Map<String, List<Map<String, List<String>>>> rules;
    private String filePath;

    public PackageSupportConfigWriter(String filePath) {
        this.filePath = filePath;
        loadConfig();
    }

    private void loadConfig() {
        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            Yaml yaml = new Yaml();
            rules = yaml.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YAML configuration", e);
        }
    }

    public void addPackageToRule(String sectionKey, String packageName) {
        List<Map<String, List<String>>> section = rules.get(sectionKey);
        if (section != null && !section.isEmpty()) {
            section.get(0).get("packages").add(packageName);
        } else {
            throw new IllegalArgumentException("Section " + sectionKey + " not found or is empty.");
        }
    }

    public void addMethodToRule(String sectionKey, String methodPattern) {
        List<Map<String, List<String>>> section = rules.get(sectionKey);
        if (section != null && !section.isEmpty()) {
            section.get(0).get("methods").add(methodPattern);
        } else {
            throw new IllegalArgumentException("Section " + sectionKey + " not found or is empty.");
        }
    }

    public void removePackageFromRule(String sectionKey, String packageName) {
        List<Map<String, List<String>>> section = rules.get(sectionKey);
        if (section != null && !section.isEmpty()) {
            section.get(0).get("packages").remove(packageName);
        } else {
            throw new IllegalArgumentException("Section " + sectionKey + " not found or is empty.");
        }
    }

    public void saveChanges() {
        try (FileWriter writer = new FileWriter(filePath)) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            yaml.dump(rules, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save YAML configuration", e);
        }
    }
}
