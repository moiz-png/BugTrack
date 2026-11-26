package com.bugtrack.ui.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A bug as the screens see it: every field the bug list and the bug details page need.
 */
public class BugView {

    private long id;
    private long projectId;
    private String projectName;
    private String module;
    private String version;
    private String title;
    private String description;
    private String steps;
    private String expected;
    private String actual;
    private String environment;
    private Severity severity;
    private Priority priority;
    private BugStatus status;
    private UserView reporter;
    private UserView assignee;
    private LocalDate dueDate;
    private int reopenCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ResolutionView resolution;
    private List<String> attachments = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    public String getActual() {
        return actual;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public BugStatus getStatus() {
        return status;
    }

    public void setStatus(BugStatus status) {
        this.status = status;
    }

    public UserView getReporter() {
        return reporter;
    }

    public void setReporter(UserView reporter) {
        this.reporter = reporter;
    }

    /**
     * @return the assigned developer, or {@code null} if the bug is unassigned
     */
    public UserView getAssignee() {
        return assignee;
    }

    public void setAssignee(UserView assignee) {
        this.assignee = assignee;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public int getReopenCount() {
        return reopenCount;
    }

    public void setReopenCount(int reopenCount) {
        this.reopenCount = reopenCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * @return the developer's resolution notes, or {@code null} if the bug has not been resolved
     */
    public ResolutionView getResolution() {
        return resolution;
    }

    public void setResolution(ResolutionView resolution) {
        this.resolution = resolution;
    }

    /**
     * @return paths of the attached files
     */
    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = new ArrayList<>(attachments);
    }
}
