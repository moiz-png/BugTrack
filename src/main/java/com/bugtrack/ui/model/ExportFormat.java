package com.bugtrack.ui.model;

/**
 * File formats a report can be exported to.
 */
public enum ExportFormat {
    CSV("CSV file", "*.csv"),
    PDF("PDF document", "*.pdf");

    private final String description;
    private final String pattern;

    ExportFormat(String description, String pattern) {
        this.description = description;
        this.pattern = pattern;
    }

    /**
     * @return a description for the file chooser, e.g. "CSV file"
     */
    public String description() {
        return description;
    }

    /**
     * @return the file-name pattern for the file chooser, e.g. {@code *.csv}
     */
    public String pattern() {
        return pattern;
    }

    /**
     * @return the file extension including the dot, e.g. {@code .csv}
     */
    public String extension() {
        return pattern.substring(1);
    }
}
