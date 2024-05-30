package com.envestnet.aaaplugin.util.config.data;

import java.util.Objects;

public class SuppressedCase {
    private String className;
    private String caseName;
    private String filePath;
    private String issueType;
    private String lines;
    private String reason;

    public SuppressedCase(String className, String caseName, String issueType, String Lines,String reason, String path) {
        this.filePath = path;
        this.caseName = caseName;
        this.issueType = issueType;
        this.className = className;
        this.lines = Lines;
        this.reason = reason;
    }

    // Getters
    public String getFilePath() { return filePath; }
    public String getCaseName() { return caseName; }
    public String getIssueType() { return issueType; }
    public String getClassName() { return className; }
    public String getLines() { return lines; }
    public String getReason() { return reason; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SuppressedCase that = (SuppressedCase) obj;
        return Objects.equals(filePath, that.filePath) &&
                Objects.equals(caseName, that.caseName) &&
                Objects.equals(issueType, that.issueType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, caseName, issueType);
    }

}

