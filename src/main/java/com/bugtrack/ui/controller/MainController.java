package com.bugtrack.ui.controller;

import com.bugtrack.ui.AppContext;
import com.bugtrack.ui.Navigator;
import com.bugtrack.ui.Session;
import com.bugtrack.ui.View;
import com.bugtrack.ui.model.Role;
import com.bugtrack.ui.model.UserView;
import com.bugtrack.ui.util.Async;
import com.bugtrack.ui.util.Dialogs;
import com.bugtrack.ui.util.Permissions;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Controller for the main window: the role-based sidebar, the top bar with the
 * notification badge, and the content area where the screens are shown.
 */
public class MainController {

    @FXML
    private VBox navBox;
    @FXML
    private Label pageTitle;
    @FXML
    private Label userLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Label unreadBadge;
    @FXML
    private StackPane contentPane;

    private final Map<View, Button> navButtons = new EnumMap<>(View.class);
    private Timeline unreadPoller;

    @FXML
    private void initialize() {
        UserView user = Session.user();
        userLabel.setText(user.getName());
        roleLabel.setText(user.getRole().label());
        buildNavigation(user);

        refreshUnread();
        unreadPoller = new Timeline(new KeyFrame(Duration.seconds(30), event -> refreshUnread()));
        unreadPoller.setCycleCount(Animation.INDEFINITE);
        unreadPoller.play();
    }

    /** Shows only the sidebar entries the user's role may use. */
    private void buildNavigation(UserView user) {
        addNav(View.DASHBOARD, "Dashboard");
        addNav(View.BUG_LIST, user.getRole() == Role.DEVELOPER ? "My Bugs" : "Bugs");
        if (Permissions.canReportBug(user)) {
            addNav(View.BUG_FORM, "Report Bug");
        }
        if (Permissions.canUseQaQueue(user)) {
            addNav(View.QA_QUEUE, "QA Queue");
        }
        if (Permissions.canManageProjects(user)) {
            addNav(View.PROJECTS, "Projects");
        }
        if (Permissions.canManageUsers(user)) {
            addNav(View.USERS, "Users");
        }
        addNav(View.REPORTS, "Reports");
        addNav(View.NOTIFICATIONS, "Notifications");
        addNav(View.SETTINGS, "Settings");
    }

    private void addNav(View view, String text) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> Navigator.open(view));
        navButtons.put(view, button);
        navBox.getChildren().add(button);
    }

    /**
     * Puts a screen in the content area and highlights its sidebar entry.
     *
     * @param root the screen's root node
     * @param view which screen it is
     */
    public void setContent(Parent root, View view) {
        contentPane.getChildren().setAll(root);
        pageTitle.setText(view.title());
        navButtons.values().forEach(button -> button.getStyleClass().remove("active"));
        Button active = navButtons.get(view);
        if (active != null) {
            active.getStyleClass().add("active");
        }
    }

    /** Reloads the unread-notifications count shown on the badge. */
    public void refreshUnread() {
        UserView user = Session.user();
        if (user == null) {
            return;
        }
        Async.run(() -> AppContext.backend().countUnread(user.getId()),
                count -> {
                    unreadBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    unreadBadge.setVisible(count > 0);
                },
                error -> unreadBadge.setVisible(false));
    }

    /**
     * Adds the keyboard shortcuts: Ctrl+D dashboard, Ctrl+B bugs, Ctrl+N report a bug.
     *
     * @param scene the main window's scene
     */
    public void installShortcuts(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN),
                () -> Navigator.open(View.DASHBOARD));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN),
                () -> Navigator.open(View.BUG_LIST));
        if (Permissions.canReportBug(Session.user())) {
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                    () -> Navigator.open(View.BUG_FORM));
        }
    }

    @FXML
    private void onNotifications() {
        Navigator.open(View.NOTIFICATIONS);
    }

    @FXML
    private void onLogout() {
        if (!Dialogs.confirm("Log out", "Do you want to log out of BugTrack?")) {
            return;
        }
        unreadPoller.stop();
        UserView user = Session.user();
        Async.run(() -> {
            AppContext.backend().logout(user);
            return Boolean.TRUE;
        }, done -> { }, error -> { });
        Session.end();
        Navigator.showLogin();
    }
}
