package com.bugtrack.ui.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Search and filter criteria for the bug list. Every field is optional: {@code null} means "any".
 * <p>Serializable so saved searches can be stored with the user's settings.</p>
 *
 * @param text        words to find in the title or description
 * @param projectId   only bugs in this project
 * @param module      only bugs in this module
 * @param status      only bugs with this status
 * @param priority    only bugs with this priority
 * @param severity    only bugs with this severity
 * @param assigneeId  only bugs assigned to this user
 * @param reporterId  only bugs reported by this user
 * @param createdFrom only bugs reported on or after this date
 * @param createdTo   only bugs reported on or before this date
 */
public record BugFilter(String text, Long projectId, String module, BugStatus status, Priority priority,
                        Severity severity, Long assigneeId, Long reporterId,
                        LocalDate createdFrom, LocalDate createdTo) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * @return a filter that matches every bug
     */
    public static BugFilter all() {
        return new BugFilter(null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * @param status the status to keep
     * @return a filter that matches bugs with the given status
     */
    public static BugFilter byStatus(BugStatus status) {
        return new BugFilter(null, null, null, status, null, null, null, null, null, null);
    }

    /**
     * @param userId the developer's id
     * @return a filter that matches bugs assigned to the given user
     */
    public static BugFilter assignedTo(long userId) {
        return new BugFilter(null, null, null, null, null, null, userId, null, null, null);
    }

    /**
     * @param userId the reporter's id
     * @return a filter that matches bugs reported by the given user
     */
    public static BugFilter reportedBy(long userId) {
        return new BugFilter(null, null, null, null, null, null, null, userId, null, null);
    }

    /**
     * Checks a bug against this filter. Used by the fake backend; the real backend filters in SQL.
     *
     * @param bug the bug to check
     * @return {@code true} if the bug matches every criterion that is set
     */
    public boolean matches(BugView bug) {
        if (text != null && !text.isBlank()) {
            String needle = text.toLowerCase();
            String haystack = (bug.getTitle() + " " + bug.getDescription()).toLowerCase();
            if (!haystack.contains(needle) && !("#" + bug.getId()).equals(needle.trim())) {
                return false;
            }
        }
        if (projectId != null && bug.getProjectId() != projectId) {
            return false;
        }
        if (module != null && !module.equals(bug.getModule())) {
            return false;
        }
        if (status != null && bug.getStatus() != status) {
            return false;
        }
        if (priority != null && bug.getPriority() != priority) {
            return false;
        }
        if (severity != null && bug.getSeverity() != severity) {
            return false;
        }
        if (assigneeId != null && (bug.getAssignee() == null || bug.getAssignee().getId() != assigneeId)) {
            return false;
        }
        if (reporterId != null && bug.getReporter().getId() != reporterId) {
            return false;
        }
        LocalDate reported = bug.getCreatedAt().toLocalDate();
        if (createdFrom != null && reported.isBefore(createdFrom)) {
            return false;
        }
        return createdTo == null || !reported.isAfter(createdTo);
    }
}
