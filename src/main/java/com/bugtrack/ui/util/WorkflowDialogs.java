package com.bugtrack.ui.util;

import com.bugtrack.ui.model.BugView;
import com.bugtrack.ui.model.Priority;
import com.bugtrack.ui.model.UserView;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Dialogs for the bug lifecycle actions: assign, submit a fix, reopen and reject.
 */
public final class WorkflowDialogs {

    /**
     * What the Admin / PM chose in the assign dialog.
     *
     * @param developer the developer to assign
     * @param priority  the priority
     * @param dueDate   the due date, or {@code null}
     */
    public record AssignInput(UserView developer, Priority priority, LocalDate dueDate) {
    }

    /**
     * What the developer entered when submitting a fix.
     *
     * @param rootCause      why the bug happened
     * @param fixDescription what was changed
     * @param commitRef      commit hash or build number, may be blank
     */
    public record ResolveInput(String rootCause, String fixDescription, String commitRef) {
    }

    private WorkflowDialogs() {
    }

    /**
     * Asks for a developer, priority and due date.
     *
     * @param developers the active developers to choose from
     * @param bug        the bug being assigned (its current values are pre-filled)
     * @return the choice, or empty if cancelled
     */
    public static Optional<AssignInput> assign(List<UserView> developers, BugView bug) {
        Dialog<AssignInput> dialog = new Dialog<>();
        dialog.setTitle((bug.getAssignee() == null ? "Assign" : "Reassign") + " bug #" + bug.getId());
        dialog.setHeaderText(bug.getTitle());
        ButtonType assign = new ButtonType("Assign", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(assign, ButtonType.CANCEL);

        ComboBox<UserView> developer = new ComboBox<>(FXCollections.observableArrayList(developers));
        developer.setPromptText("Choose a developer");
        developer.setValue(bug.getAssignee());
        developer.setMaxWidth(Double.MAX_VALUE);
        ComboBox<Priority> priority = new ComboBox<>(FXCollections.observableArrayList(Priority.values()));
        priority.setValue(bug.getPriority());
        priority.setMaxWidth(Double.MAX_VALUE);
        DatePicker dueDate = new DatePicker(bug.getDueDate());
        dueDate.setPromptText("Optional");

        GridPane grid = form();
        grid.addRow(0, new Label("Developer"), developer);
        grid.addRow(1, new Label("Priority"), priority);
        grid.addRow(2, new Label("Due date"), dueDate);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(assign).disableProperty()
                .bind(developer.valueProperty().isNull().or(priority.valueProperty().isNull()));
        dialog.setResultConverter(button -> button == assign
                ? new AssignInput(developer.getValue(), priority.getValue(), dueDate.getValue())
                : null);
        Dialogs.prepare(dialog);
        return dialog.showAndWait();
    }

    /**
     * Asks which developer should get several selected bugs.
     *
     * @param developers the active developers to choose from
     * @param bugCount   how many bugs are selected
     * @return the developer, or empty if cancelled
     */
    public static Optional<UserView> chooseDeveloper(List<UserView> developers, int bugCount) {
        if (developers.isEmpty()) {
            Dialogs.info("No developers", "There are no active developers to assign bugs to.");
            return Optional.empty();
        }
        ChoiceDialog<UserView> dialog = new ChoiceDialog<>(developers.get(0), developers);
        dialog.setTitle("Assign selected bugs");
        dialog.setHeaderText(null);
        dialog.setContentText("Assign " + bugCount + (bugCount == 1 ? " bug" : " bugs") + " to:");
        Dialogs.prepare(dialog);
        return dialog.showAndWait();
    }

    /**
     * Asks the developer for the root cause, the fix and the commit reference.
     *
     * @param bugId the bug being resolved
     * @return what the developer entered, or empty if cancelled
     */
    public static Optional<ResolveInput> resolve(long bugId) {
        Dialog<ResolveInput> dialog = new Dialog<>();
        dialog.setTitle("Submit fix for bug #" + bugId);
        dialog.setHeaderText("The bug goes to QA for verification after you submit.");
        ButtonType submit = new ButtonType("Submit for QA", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submit, ButtonType.CANCEL);

        TextArea rootCause = area("What caused the bug?");
        TextArea fix = area("What did you change to fix it?");
        TextField commit = new TextField();
        commit.setPromptText("Commit hash or build number (optional)");

        VBox box = new VBox(6, new Label("Root cause *"), rootCause, new Label("Fix description *"), fix,
                new Label("Commit / build reference"), commit);
        box.setPrefWidth(460);
        dialog.getDialogPane().setContent(box);

        dialog.getDialogPane().lookupButton(submit).disableProperty().bind(Bindings.createBooleanBinding(
                () -> rootCause.getText().isBlank() || fix.getText().isBlank(),
                rootCause.textProperty(), fix.textProperty()));
        dialog.setResultConverter(button -> button == submit
                ? new ResolveInput(rootCause.getText(), fix.getText(), commit.getText())
                : null);
        Dialogs.prepare(dialog);
        return dialog.showAndWait();
    }

    /**
     * Asks for a required reason, used when reopening or rejecting a bug.
     *
     * @param title  window title
     * @param header the question
     * @return the reason, or empty if cancelled
     */
    public static Optional<String> reason(String title, String header) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextArea reason = area("Reason");
        reason.setPrefWidth(420);
        dialog.getDialogPane().setContent(reason);
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(
                Bindings.createBooleanBinding(() -> reason.getText().isBlank(), reason.textProperty()));
        dialog.setResultConverter(button -> button == ButtonType.OK ? reason.getText().trim() : null);
        Dialogs.prepare(dialog);
        return dialog.showAndWait();
    }

    private static GridPane form() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPrefWidth(400);
        return grid;
    }

    private static TextArea area(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setWrapText(true);
        area.setPrefRowCount(3);
        return area;
    }
}
