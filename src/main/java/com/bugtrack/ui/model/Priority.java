package com.bugtrack.ui.model;

/**
 * How urgently a bug should be fixed. Set by the Admin / PM.
 */
public enum Priority {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    /**
     * @return the name shown in the UI
     */
    public String label() {
        return label;
    }

    /**
     * @return the CSS style class used to colour this priority, e.g. {@code priority-high}
     */
    public String styleClass() {
        return "priority-" + name().toLowerCase();
    }

    @Override
    public String toString() {
        return label;
    }
}
