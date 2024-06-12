package com.envestnet.aaaplugin.util.config.data;

public class MethodRule {
    private String className; // Class name for matching
    private String methodName; // Method name for matching

    /**
     * Constructs a MethodRule with the specified class name and method name.
     *
     * @param className the class name to match
     * @param methodName the method name to match
     */
    public MethodRule(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    /**
     * Checks if both the class and method match this rule's Strings.
     *
     * @param qualifiedName the fully qualified class name
     * @param methodName the method name to be checked
     * @return true if both the class and the method name contains the Strings
     */
    public boolean matches(String qualifiedName, String methodName) {
        return (className != null && !className.isEmpty() && qualifiedName.contains(className))
                && (this.methodName != null && !this.methodName.isEmpty() && methodName.contains(this.methodName));
    }
}