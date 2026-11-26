package com.bugtrack.ui;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * The JavaFX application: opens the main window on the login screen.
 */
public class BugTrackApp extends Application {

    @Override
    public void start(Stage stage) {
        AppContext.setHostServices(getHostServices());
        Navigator.init(stage);
        stage.setMinWidth(1024);
        stage.setMinHeight(660);
        Navigator.showLogin();
        stage.show();
    }

    /**
     * Starts the application.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
