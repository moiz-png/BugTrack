package com.bugtrack.ui;

import com.bugtrack.ui.backend.FakeBackend;
import com.bugtrack.ui.backend.UiBackend;
import javafx.application.HostServices;

import java.io.File;
import java.util.Objects;

/**
 * Holds the objects the whole UI shares: the backend and the platform's host services.
 * <p>To switch from sample data to the real backend, call {@link #useBackend(UiBackend)}
 * once at startup with an adapter over the real services.</p>
 */
public final class AppContext {

    private static UiBackend backend = new FakeBackend();
    private static HostServices hostServices;

    private AppContext() {
    }

    /**
     * @return the backend every screen talks to
     */
    public static UiBackend backend() {
        return backend;
    }

    /**
     * Replaces the backend, e.g. with the adapter over the real services.
     *
     * @param newBackend the backend to use from now on
     */
    public static void useBackend(UiBackend newBackend) {
        backend = Objects.requireNonNull(newBackend);
    }

    static void setHostServices(HostServices services) {
        hostServices = services;
    }

    /**
     * Opens a file (such as an attachment) in the operating system's default program.
     *
     * @param path the file to open
     */
    public static void openFile(String path) {
        if (hostServices != null) {
            hostServices.showDocument(new File(path).toURI().toString());
        }
    }
}
