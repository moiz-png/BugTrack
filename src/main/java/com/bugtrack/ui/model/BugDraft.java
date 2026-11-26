package com.bugtrack.ui.model;

import java.util.List;

/**
 * What the report-bug form sends when a bug is created or edited.
 *
 * @param projectId   project the bug belongs to
 * @param module      affected module, or {@code null}
 * @param version     affected version, or {@code null}
 * @param title       short summary
 * @param description full description
 * @param steps       steps to reproduce
 * @param expected    expected result
 * @param actual      actual result
 * @param environment OS, Java version, screen size and so on
 * @param severity    how bad the bug is
 * @param priority    suggested priority (only the Admin / PM's choice is final)
 * @param attachments paths of files to attach
 */
public record BugDraft(long projectId, String module, String version, String title, String description,
                       String steps, String expected, String actual, String environment,
                       Severity severity, Priority priority, List<String> attachments) {
}
