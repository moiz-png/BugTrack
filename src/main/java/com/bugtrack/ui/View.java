package com.bugtrack.ui;

/**
 * Every screen shown in the main window's content area, with its FXML file and page title.
 */
public enum View {
    DASHBOARD("dashboard.fxml", "Dashboard"),
    BUG_LIST("bug-list.fxml", "Bugs"),
    QA_QUEUE("bug-list.fxml", "QA Verification Queue"),
    BUG_FORM("bug-form.fxml", "Report a Bug"),
    BUG_DETAIL("bug-detail.fxml", "Bug Details"),
    PROJECTS("projects.fxml", "Projects"),
    USERS("users.fxml", "Users"),
    REPORTS("reports.fxml", "Reports"),
    NOTIFICATIONS("notifications.fxml", "Notifications"),
    SETTINGS("settings.fxml", "Settings");

    private final String fxml;
    private final String title;

    View(String fxml, String title) {
        this.fxml = fxml;
        this.title = title;
    }

    /**
     * @return the FXML file name in {@code /com/bugtrack/ui/view/}
     */
    public String fxml() {
        return fxml;
    }

    /**
     * @return the title shown in the top bar
     */
    public String title() {
        return title;
    }
}
