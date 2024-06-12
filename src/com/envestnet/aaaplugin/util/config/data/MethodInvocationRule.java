package com.envestnet.aaaplugin.util.config.data;

import java.util.List;
import java.util.regex.Pattern;

public class MethodInvocationRule {
    private List<String> packageNames; // Patterns to match the package or class name
    private List<MethodRule> methodRules; // Rules for matching method names
    private List<MethodRule> excludeRules; // Rules for excluding methods

    public MethodInvocationRule(List<String> packageNames, List<MethodRule> methodRules, List<MethodRule> excludeRules) {
        this.packageNames = packageNames;
        this.methodRules = methodRules;
        this.excludeRules = excludeRules;
    }

    /**
     * Determines if the given qualified class name and method name match the rules defined in this MethodInvocationRule.
     *
     * @param qualifiedName the fully qualified class name
     * @param methodName the name of the method to be checked
     * @return true if the qualified class name matches any of the package names (if not empty) and the method name is not excluded,
     *         or if the method name matches any of the method rules (if not empty); false otherwise
     */
    public boolean matches(String qualifiedName, String methodName) {
        boolean packageMatch = !packageNames.isEmpty() && packageNames.stream().anyMatch(qualifiedName::contains);
        boolean methodMatch = !methodRules.isEmpty() && methodRules.stream().anyMatch(rule -> rule.matches(qualifiedName, methodName));
        boolean isExcluded = !excludeRules.isEmpty() && excludeRules.stream().anyMatch(rule -> rule.matches(qualifiedName, methodName));

        return (packageMatch && !isExcluded) || methodMatch;
    }
}
