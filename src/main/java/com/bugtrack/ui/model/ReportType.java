package com.bugtrack.ui.model;

/**
 * The reports offered on the Reports screen (proposal section 9.7).
 */
public enum ReportType {
    BUGS_BY_STATUS("Bugs by status"),
    OPEN_BY_PRIORITY("Open bugs by priority"),
    DEVELOPER_WORKLOAD("Developer workload (open bugs)"),
    DEFECTS_PER_MODULE("Defects per module"),
    QUALITY_SUMMARY("Quality summary (resolution time, reopen rate)");

    private final String label;

    ReportType(String label) {
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
