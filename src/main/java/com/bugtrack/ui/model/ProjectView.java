package com.bugtrack.ui.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A project with its modules and team members. An id of {@code 0} means "not saved yet".
 */
public class ProjectView {

    private long id;
    private String name;
    private String description;
    private String version;
    private final List<String> modules = new ArrayList<>();
    private final List<Long> memberIds = new ArrayList<>();

    /**
     * Creates a project.
     *
     * @param id          the project id, or {@code 0} for a new project
     * @param name        project name
     * @param description short description
     * @param version     current version number, e.g. "1.2.0"
     */
    public ProjectView(long id, String name, String description, String version) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the module names; the list can be changed directly
     */
    public List<String> getModules() {
        return modules;
    }

    /**
     * @return the ids of the project's team members; the list can be changed directly
     */
    public List<Long> getMemberIds() {
        return memberIds;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ProjectView other && other.id == id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
