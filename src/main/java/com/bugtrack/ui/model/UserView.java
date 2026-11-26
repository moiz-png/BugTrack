package com.bugtrack.ui.model;

import java.util.Objects;

/**
 * A user account as the screens see it. Never carries the password hash.
 */
public class UserView {

    private long id;
    private String name;
    private String email;
    private Role role;
    private boolean active;

    /**
     * Creates a user.
     *
     * @param id     the user id
     * @param name   full name
     * @param email  login email
     * @param role   the user's role
     * @param active {@code false} if the account is deactivated
     */
    public UserView(long id, String name, String email, Role role, boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.active = active;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** Two users are equal when they have the same id, so combo boxes can match them. */
    @Override
    public boolean equals(Object o) {
        return o instanceof UserView other && other.id == id;
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
