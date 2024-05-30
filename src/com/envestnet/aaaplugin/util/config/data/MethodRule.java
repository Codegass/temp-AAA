package com.envestnet.aaaplugin.util.config.data;

import java.util.regex.Pattern;

public class MethodRule {
    private Pattern classPattern; // Compiled regex for class matching
    private Pattern methodPattern; // Compiled regex for method matching

    public MethodRule(Pattern classPattern, Pattern methodPattern) {
        this.classPattern = classPattern;
        this.methodPattern = methodPattern;
    }

    /**
     * Checks if both the class and method match this rule's patterns.
     *
     * @param qualifiedName the fully qualified class name
     * @param methodName the method name to be checked
     * @return true if both the class and the method name match the patterns
     */
    public boolean matches(String qualifiedName, String methodName) {
        return classPattern.matcher(qualifiedName).find() && methodPattern.matcher(methodName).find();
    }
}