package com.bugtrack.ui.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * A user's personal settings: theme, default project, page size and saved searches.
 * <p>Serializable so the backend can save it to the user's profile folder on exit
 * and restore it at the next login (proposal section 13).</p>
 */
public class SettingsView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Theme name for light mode. */
    public static final String LIGHT = "light";
    /** Theme name for dark mode. */
    public static final String DARK = "dark";

    private String theme = LIGHT;
    private Long defaultProjectId;
    private int pageSize = 20;
    private LinkedHashMap<String, BugFilter> savedSearches = new LinkedHashMap<>();

    /**
     * @return {@link #LIGHT} or {@link #DARK}
     */
    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    /**
     * @return the project pre-selected on the report-bug form, or {@code null}
     */
    public Long getDefaultProjectId() {
        return defaultProjectId;
    }

    public void setDefaultProjectId(Long defaultProjectId) {
        this.defaultProjectId = defaultProjectId;
    }

    /**
     * @return how many bugs the bug list shows per page
     */
    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * @return saved searches by name, in the order they were saved; can be changed directly
     */
    public LinkedHashMap<String, BugFilter> getSavedSearches() {
        return savedSearches;
    }
}
