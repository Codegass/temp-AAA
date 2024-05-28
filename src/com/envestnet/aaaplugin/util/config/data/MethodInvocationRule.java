package com.envestnet.aaaplugin.util.config.data;

import java.util.List;
import java.util.regex.Pattern;

public class MethodInvocationRule {
    private List<Pattern> packagePatterns; // Patterns to match the package or class name
    private List<MethodRule> methodRules; // Rules for matching method names
    private List<MethodRule> excludeRules; // Rules for excluding methods

    public MethodInvocationRule(List<Pattern> packagePatterns, List<MethodRule> methodRules, List<MethodRule> excludeRules) {
        this.packagePatterns = packagePatterns;
        this.methodRules = methodRules;
        this.excludeRules = excludeRules;
    }

    /**
     * Determines if the qualified class name and method name match the rules defined.
     *
     * @param qualifiedName the fully qualified class name
     * @param methodName the method name to be checked
     * @return true if it matches any method rule but not excluded by any exclude rule.
     */
    public boolean matches(String qualifiedName, String methodName) {
        // Check if qualifiedName matches any of the package patterns
        if (packagePatterns.stream().anyMatch(pattern -> pattern.matcher(qualifiedName).matches())) {
            // Ensure it does not match any of the exclude rules
            if (excludeRules.stream().noneMatch(rule -> rule.matches(qualifiedName, methodName))) {
                // Check if it matches any of the method rules
                return methodRules.stream().anyMatch(rule -> rule.matches(qualifiedName, methodName));
            }
        }
        return false;
    }
}
