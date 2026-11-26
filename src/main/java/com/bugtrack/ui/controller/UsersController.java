package com.bugtrack.ui.controller;

import com.bugtrack.ui.AppContext;
import com.bugtrack.ui.Session;
import com.bugtrack.ui.model.Role;
import com.bugtrack.ui.model.UserView;
import com.bugtrack.ui.util.Async;
import com.bugtrack.ui.util.Dialogs;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for user management (Admin / PM only): create, edit, deactivate users,
 * assign roles and reset passwords.
 */
public class UsersController {

    @FXML
    private TableView<UserView> usersTable;
    @FXML
    private TableColumn<UserView, String> nameColumn;
    @FXML
    private TableColumn<UserView, String> emailColumn;
    @FXML
    private TableColumn<UserView, String> roleColumn;
    @FXML
    private TableColumn<UserView, String> statusColumn;
    @FXML
    private Label formTitle;
    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private ComboBox<Role> roleCombo;
    @FXML
    private VBox passwordBox;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button toggleActiveButton;
    @FXML
    private Button resetPasswordButton;

    private UserView editing;

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getName()));
        emailColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getEmail()));
        roleColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getRole().label()));
        statusColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().isActive() ? "Active" : "Deactivated"));
        roleCombo.getItems().setAll(Role.values());
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, user) -> {
            if (user != null) {
                edit(user);
            }
        });
        startNew();
        reload(null);
    }

    private void reload(Long selectId) {
        Async.run(() -> AppContext.backend().listUsers(), users -> {
            usersTable.getItems().setAll(users);
            if (selectId != null) {
                users.stream().filter(u -> u.getId() == selectId).findFirst()
                        .ifPresent(u -> usersTable.getSelectionModel().select(u));
            }
        });
    }

    @FXML
    private void onNew() {
        usersTable.getSelectionModel().clearSelection();
        startNew();
    }

    private void startNew() {
        editing = null;
        formTitle.setText("New user");
        nameField.clear();
        emailField.clear();
        roleCombo.setValue(Role.DEVELOPER);
        passwordField.clear();
        show(passwordBox, true);
        toggleActiveButton.setDisable(true);
        toggleActiveButton.setText("Deactivate");
        resetPasswordButton.setDisable(true);
        show(errorLabel, false);
    }

    private void edit(UserView user) {
        editing = user;
        formTitle.setText("Edit " + user.getName());
        nameField.setText(user.getName());
        emailField.setText(user.getEmail());
        roleCombo.setValue(user.getRole());
        show(passwordBox, false);
        toggleActiveButton.setDisable(false);
        toggleActiveButton.setText(user.isActive() ? "Deactivate" : "Activate");
        resetPasswordButton.setDisable(false);
        show(errorLabel, false);
    }

    @FXML
    private void onSave() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        Role role = roleCombo.getValue();
        List<String> problems = new ArrayList<>();
        if (name.isEmpty()) {
            problems.add("Enter the user's full name.");
        }
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            problems.add("Enter a valid email address.");
        }
        if (role == null) {
            problems.add("Choose a role.");
        }
        if (editing == null && passwordField.getText().length() < 6) {
            problems.add("The initial password must be at least 6 characters.");
        }
        if (editing != null && editing.getId() == Session.user().getId() && role != Role.ADMIN) {
            problems.add("You cannot remove your own Admin / PM role.");
        }
        if (!problems.isEmpty()) {
            errorLabel.setText(String.join("\n", problems));
            show(errorLabel, true);
            return;
        }
        show(errorLabel, false);

        UserView current = editing;
        String password = passwordField.getText();
        Async.run(() -> current == null
                        ? AppContext.backend().createUser(name, email, role, password)
                        : AppContext.backend().updateUser(new UserView(current.getId(), name, email, role, current.isActive())),
                saved -> {
                    Dialogs.info("User saved", saved.getName() + " has been saved.");
                    reload(saved.getId());
                });
    }

    @FXML
    private void onToggleActive() {
        if (editing == null) {
            return;
        }
        if (editing.getId() == Session.user().getId()) {
            Dialogs.error("You cannot deactivate your own account.");
            return;
        }
        boolean activate = !editing.isActive();
        String action = activate ? "Activate" : "Deactivate";
        if (!Dialogs.confirm(action + " user", action + " " + editing.getName() + "?"
                + (activate ? "" : " They will no longer be able to log in."))) {
            return;
        }
        long userId = editing.getId();
        Async.run(() -> {
            AppContext.backend().setUserActive(userId, activate);
            return userId;
        }, id -> reload(id));
    }

    @FXML
    private void onResetPassword() {
        if (editing == null) {
            return;
        }
        long userId = editing.getId();
        String name = editing.getName();
        Dialogs.password("Reset password", "New password for " + name).ifPresent(password ->
                Async.run(() -> {
                    AppContext.backend().resetPassword(userId, password);
                    return Boolean.TRUE;
                }, done -> Dialogs.info("Password reset", "The password for " + name + " has been changed.")));
    }

    private static void show(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
