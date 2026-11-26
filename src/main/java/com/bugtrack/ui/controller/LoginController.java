package com.bugtrack.ui.controller;

import com.bugtrack.ui.AppContext;
import com.bugtrack.ui.Navigator;
import com.bugtrack.ui.Session;
import com.bugtrack.ui.backend.UiBackend;
import com.bugtrack.ui.model.SettingsView;
import com.bugtrack.ui.model.UserView;
import com.bugtrack.ui.util.Async;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the login screen.
 */
public class LoginController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button loginButton;

    @FXML
    private void initialize() {
        Platform.runLater(emailField::requestFocus);
    }

    @FXML
    private void onLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        if (email.isEmpty() || password.isEmpty()) {
            showError("Enter your email and password.");
            return;
        }

        loginButton.setDisable(true);
        hideError();
        UiBackend backend = AppContext.backend();
        Async.run(
                () -> backend.login(email, password)
                        .map(user -> new LoginResult(user, backend.loadSettings(user.getId())))
                        .orElse(null),
                result -> {
                    loginButton.setDisable(false);
                    if (result == null) {
                        passwordField.clear();
                        showError("Wrong email or password, or the account has been deactivated.");
                        return;
                    }
                    Session.start(result.user(), result.settings());
                    Navigator.showMain();
                },
                error -> {
                    loginButton.setDisable(false);
                    showError("Could not sign in: " + error.getMessage());
                });
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private record LoginResult(UserView user, SettingsView settings) {
    }
}
