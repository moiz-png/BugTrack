package com.bugtrack.ui;

import com.bugtrack.ui.model.SettingsView;
import javafx.scene.Scene;

import java.util.Objects;

/**
 * Applies the light or dark CSS theme to a scene.
 * <p>{@code base.css} defines the colours and component styles; {@code dark.css} only
 * overrides the colours, so every screen supports both themes automatically.</p>
 */
public final class ThemeManager {

    private static final String BASE = "/com/bugtrack/ui/css/base.css";
    private static final String DARK = "/com/bugtrack/ui/css/dark.css";

    private ThemeManager() {
    }

    /**
     * Applies the logged-in user's theme (light on the login screen).
     *
     * @param scene the scene to style
     */
    public static void apply(Scene scene) {
        scene.getStylesheets().setAll(url(BASE));
        SettingsView settings = Session.settings();
        if (settings != null && SettingsView.DARK.equals(settings.getTheme())) {
            scene.getStylesheets().add(url(DARK));
        }
    }

    private static String url(String path) {
        return Objects.requireNonNull(ThemeManager.class.getResource(path), "Missing stylesheet " + path)
                .toExternalForm();
    }
}
