package com.bugtrack.ui.util;

import com.bugtrack.ui.Navigator;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

/**
 * Ready-made message, confirmation and input dialogs that follow the current theme.
 */
public final class Dialogs {

    private Dialogs() {
    }

    /**
     * Shows the message of a failed backend call.
     *
     * @param error what went wrong
     */
    public static void error(Throwable error) {
        Throwable cause = error;
        while (cause.getMessage() == null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        error(cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName());
    }

    /**
     * @param message the error text
     */
    public static void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Something went wrong");
        alert.setHeaderText(null);
        prepare(alert);
        alert.showAndWait();
    }

    /**
     * @param title   window title
     * @param message the text
     */
    public static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        prepare(alert);
        alert.showAndWait();
    }

    /**
     * @param title   window title
     * @param message the question
     * @return {@code true} if the user clicked OK
     */
    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        prepare(alert);
        return alert.showAndWait().filter(button -> button == ButtonType.OK).isPresent();
    }

    /**
     * Asks for one line of text.
     *
     * @param title   window title
     * @param prompt  label next to the field
     * @param initial starting text
     * @return the text, or empty if cancelled
     */
    public static Optional<String> text(String title, String prompt, String initial) {
        TextInputDialog dialog = new TextInputDialog(initial);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(prompt);
        prepare(dialog);
        return dialog.showAndWait();
    }

    /**
     * Asks for a new password of at least 6 characters.
     *
     * @param title  window title
     * @param header text above the field
     * @return the password, or empty if cancelled
     */
    public static Optional<String> password(String title, String header) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        PasswordField field = new PasswordField();
        field.setPromptText("At least 6 characters");
        dialog.getDialogPane().setContent(field);
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(
                Bindings.createBooleanBinding(() -> field.getText().length() < 6, field.textProperty()));
        dialog.setResultConverter(button -> button == ButtonType.OK ? field.getText() : null);
        prepare(dialog);
        Platform.runLater(field::requestFocus);
        return dialog.showAndWait();
    }

    /**
     * Gives a dialog the main window as owner and the current theme's stylesheets.
     *
     * @param dialog the dialog to prepare
     */
    public static void prepare(Dialog<?> dialog) {
        if (Navigator.owner() != null && dialog.getOwner() == null) {
            dialog.initOwner(Navigator.owner());
        }
        dialog.getDialogPane().getStylesheets().setAll(Navigator.stylesheets());
    }
}
