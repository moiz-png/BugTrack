package com.bugtrack.ui;

/**
 * Entry point for the executable JAR.
 * <p>A main class that does not extend {@code Application} lets the app start from a plain
 * (non-modular) JAR, where JavaFX would otherwise refuse to launch.</p>
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * Starts BugTrack.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        BugTrackApp.main(args);
    }
}
