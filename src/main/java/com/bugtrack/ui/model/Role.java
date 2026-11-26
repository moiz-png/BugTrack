package com.bugtrack.ui.model;

/**
 * User roles from the proposal's permission matrix.
 */
public enum Role {
    ADMIN("Admin / PM"),
    DEVELOPER("Developer"),
    QA_TESTER("QA Tester");

    private final String label;

    Role(String label) {
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
