package com.bugtrack.ui;

import com.bugtrack.ui.model.Role;
import com.bugtrack.ui.model.SettingsView;
import com.bugtrack.ui.model.UserView;

/**
 * The logged-in user and their settings, kept in memory and cleared on logout.
 */
public final class Session {

    private static UserView user;
    private static SettingsView settings;

    private Session() {
    }

    /**
     * Starts a session after a successful login.
     *
     * @param loggedIn     the user who logged in
     * @param userSettings their saved settings
     */
    public static void start(UserView loggedIn, SettingsView userSettings) {
        user = loggedIn;
        settings = userSettings;
    }

    /** Clears the session on logout. */
    public static void end() {
        user = null;
        settings = null;
    }

    /**
     * @return the logged-in user, or {@code null} on the login screen
     */
    public static UserView user() {
        return user;
    }

    /**
     * @return the logged-in user's settings, or {@code null} on the login screen
     */
    public static SettingsView settings() {
        return settings;
    }

    /**
     * @param role the role to check
     * @return {@code true} if someone is logged in with that role
     */
    public static boolean hasRole(Role role) {
        return user != null && user.getRole() == role;
    }
}
