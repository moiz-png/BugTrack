package com.bugtrack.ui;

import com.bugtrack.ui.controller.BugDetailController;
import com.bugtrack.ui.controller.BugFormController;
import com.bugtrack.ui.controller.BugListController;
import com.bugtrack.ui.controller.MainController;
import com.bugtrack.ui.model.BugFilter;
import com.bugtrack.ui.model.BugStatus;
import com.bugtrack.ui.model.Role;
import com.bugtrack.ui.model.UserView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;

/**
 * Switches between the login screen, the main window and the screens inside it.
 */
public final class Navigator {

    private static final String VIEW_FOLDER = "/com/bugtrack/ui/view/";

    private static Stage stage;
    private static MainController main;

    private Navigator() {
    }

    /**
     * @param primaryStage the application's main window
     */
    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    /** Shows the login screen. */
    public static void showLogin() {
        main = null;
        Loaded<Object> loaded = load("login.fxml");
        setScene(loaded.root());
        stage.setTitle("BugTrack - Sign in");
    }

    /** Shows the main window (sidebar and top bar) for the logged-in user, starting on the dashboard. */
    public static void showMain() {
        Loaded<MainController> loaded = load("main.fxml");
        main = loaded.controller();
        Scene scene = setScene(loaded.root());
        main.installShortcuts(scene);
        stage.setTitle("BugTrack - " + Session.user().getName());
        open(View.DASHBOARD);
    }

    /**
     * Opens a screen from the sidebar, with the right starting filter for list screens.
     *
     * @param view the screen to open
     */
    public static void open(View view) {
        switch (view) {
            case BUG_LIST -> {
                BugListController list = show(view);
                UserView user = Session.user();
                list.applyFilter(user.getRole() == Role.DEVELOPER
                        ? BugFilter.assignedTo(user.getId())
                        : BugFilter.all());
            }
            case QA_QUEUE -> {
                BugListController list = show(view);
                list.applyFilter(BugFilter.byStatus(BugStatus.QA_VERIFICATION));
            }
            case BUG_FORM -> {
                BugFormController form = show(view);
                form.newBug();
            }
            case BUG_DETAIL -> throw new IllegalArgumentException("Use showBug(id) to open a bug.");
            default -> show(view);
        }
    }

    /**
     * Opens the details page of a bug.
     *
     * @param bugId the bug to show
     */
    public static void showBug(long bugId) {
        BugDetailController details = show(View.BUG_DETAIL);
        details.load(bugId);
    }

    /**
     * Opens the bug form filled in with an existing bug, for editing.
     *
     * @param bugId the bug to edit
     */
    public static void editBug(long bugId) {
        BugFormController form = show(View.BUG_FORM);
        form.editBug(bugId);
    }

    /** Updates the unread-notifications badge in the top bar. */
    public static void refreshUnread() {
        if (main != null) {
            main.refreshUnread();
        }
    }

    /** Re-applies the theme after the user changes it in Settings. */
    public static void applyTheme() {
        if (stage.getScene() != null) {
            ThemeManager.apply(stage.getScene());
        }
    }

    /**
     * @return the main window, used as the owner of dialogs
     */
    public static Window owner() {
        return stage;
    }

    /**
     * @return the stylesheets of the current scene, so dialogs can use the same theme
     */
    public static List<String> stylesheets() {
        return stage != null && stage.getScene() != null ? stage.getScene().getStylesheets() : List.of();
    }

    private static <T> T show(View view) {
        Loaded<T> loaded = load(view.fxml());
        main.setContent(loaded.root(), view);
        return loaded.controller();
    }

    private static Scene setScene(Parent root) {
        double width = stage.getScene() != null ? stage.getScene().getWidth() : 1240;
        double height = stage.getScene() != null ? stage.getScene().getHeight() : 780;
        Scene scene = new Scene(root, width, height);
        ThemeManager.apply(scene);
        stage.setScene(scene);
        return scene;
    }

    private static <T> Loaded<T> load(String fxml) {
        URL url = Navigator.class.getResource(VIEW_FOLDER + fxml);
        if (url == null) {
            throw new IllegalStateException("Missing view file: " + fxml);
        }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            return new Loaded<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load view " + fxml, e);
        }
    }

    private record Loaded<T>(Parent root, T controller) {
    }
}
