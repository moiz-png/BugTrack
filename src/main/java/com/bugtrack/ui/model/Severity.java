package com.bugtrack.ui.model;

/**
 * How badly a bug affects the software. Set by the reporter.
 */
public enum Severity {
    COSMETIC("Cosmetic"),
    MINOR("Minor"),
    MAJOR("Major"),
    CRITICAL("Critical"),
    BLOCKER("Blocker");

    private final String label;

    Severity(String label) {
        this.label = label;
    }

    /**
     * @return the name shown in the UI
     */
    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
