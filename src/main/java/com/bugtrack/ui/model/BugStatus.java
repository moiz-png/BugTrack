package com.bugtrack.ui.model;

/**
 * The stages of the bug lifecycle (proposal section 10).
 */
public enum BugStatus {
    NEW("New"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    QA_VERIFICATION("QA Verification"),
    CLOSED("Closed"),
    REOPENED("Reopened"),
    REJECTED("Rejected / Duplicate");

    private final String label;

    BugStatus(String label) {
        this.label = label;
    }

    /**
     * @return the name shown in the UI
     */
    public String label() {
        return label;
    }

    /**
     * @return {@code true} while the bug still needs work (not closed or rejected)
     */
    public boolean isOpen() {
        return this != CLOSED && this != REJECTED;
    }

    /**
     * @return the CSS style class used to colour this status, e.g. {@code status-in-progress}
     */
    public String styleClass() {
        return "status-" + name().toLowerCase().replace('_', '-');
    }

    @Override
    public String toString() {
        return label;
    }
}
