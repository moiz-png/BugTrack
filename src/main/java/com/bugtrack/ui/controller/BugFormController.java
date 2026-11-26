package com.bugtrack.ui.controller;

import com.bugtrack.ui.AppContext;
import com.bugtrack.ui.Navigator;
import com.bugtrack.ui.Session;
import com.bugtrack.ui.View;
import com.bugtrack.ui.backend.UiBackend;
import com.bugtrack.ui.model.BugDraft;
import com.bugtrack.ui.model.BugView;
import com.bugtrack.ui.model.Priority;
import com.bugtrack.ui.model.ProjectView;
import com.bugtrack.ui.model.Severity;
import com.bugtrack.ui.model.UserView;
import com.bugtrack.ui.util.Async;
import com.bugtrack.ui.util.Combos;
import com.bugtrack.ui.util.Dialogs;
import com.bugtrack.ui.util.Permissions;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the report-a-bug form, also used to edit an existing bug.
 * <p>While the reporter types a title, similar open bugs in the same project are shown
 * so duplicates can be caught before submitting (proposal section 9.3).</p>
 */
public class BugFormController {

    @FXML
    private Label formTitle;
    @FXML
    private ComboBox<ProjectView> projectCombo;
    @FXML
    private ComboBox<String> moduleCombo;
    @FXML
    private TextField versionField;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextArea stepsArea;
    @FXML
    private TextArea expectedArea;
    @FXML
    private TextArea actualArea;
    @FXML
    private ComboBox<Severity> severityCombo;
    @FXML
    private Label priorityLabel;
    @FXML
    private ComboBox<Priority> priorityCombo;
    @FXML
    private TextField environmentField;
    @FXML
    private javafx.scene.layout.VBox duplicateBox;
    @FXML
    private ListView<BugView> duplicateList;
    @FXML
    private ListView<String> attachmentList;
    @FXML
    private Label errorLabel;
    @FXML
    private Button submitButton;

    private final PauseTransition duplicateDelay = new PauseTransition(Duration.millis(500));
    private Long editingBugId;

    @FXML
    private void initialize() {
        UserView user = Session.user();
        severityCombo.getItems().setAll(Severity.values());
        priorityCombo.getItems().setAll(Priority.values());
        priorityLabel.setText(Permissions.canSetPriority(user) ? "Priority" : "Suggested priority");
        Combos.showPromptWhenEmpty(projectCombo, moduleCombo);

        projectCombo.valueProperty().addListener((obs, old, project) -> {
            moduleCombo.getItems().setAll(project == null ? List.of() : project.getModules());
            moduleCombo.setValue(null);
            if (project != null && (versionField.getText() == null || versionField.getText().isBlank())) {
                versionField.setText(project.getVersion());
            }
            duplicateDelay.playFromStart();
        });
        titleField.textProperty().addListener((obs, old, title) -> duplicateDelay.playFromStart());
        duplicateDelay.setOnFinished(event -> checkDuplicates());

        duplicateList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(BugView bug, boolean empty) {
                super.updateItem(bug, empty);
                setText(empty || bug == null ? null
                        : "#" + bug.getId() + "   " + bug.getTitle() + "   (" + bug.getStatus().label() + ")");
            }
        });
        duplicateList.setOnMouseClicked(event -> {
            BugView selected = duplicateList.getSelectionModel().getSelectedItem();
            if (event.getClickCount() == 2 && selected != null
                    && Dialogs.confirm("Open bug #" + selected.getId(),
                    "Leave this form and open bug #" + selected.getId() + "? What you typed will be lost.")) {
                Navigator.showBug(selected.getId());
            }
        });
        attachmentList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                setText(empty || path == null ? null : new File(path).getName());
            }
        });
        attachmentList.setPlaceholder(new Label("Screenshots, logs or screen recordings"));
        setVisible(duplicateBox, false);
    }

    /** Prepares an empty form for a new bug, with the user's default project selected. */
    public void newBug() {
        editingBugId = null;
        formTitle.setText("Report a Bug");
        submitButton.setText("Submit bug");
        severityCombo.setValue(Severity.MINOR);
        priorityCombo.setValue(Priority.MEDIUM);
        Long defaultProject = Session.settings().getDefaultProjectId();
        Async.run(() -> AppContext.backend().listProjects(), projects -> {
            projectCombo.getItems().setAll(projects);
            if (defaultProject != null) {
                projects.stream().filter(p -> p.getId() == defaultProject).findFirst()
                        .ifPresent(projectCombo::setValue);
            }
        });
    }

    /**
     * Fills the form with an existing bug for editing.
     *
     * @param bugId the bug to edit
     */
    public void editBug(long bugId) {
        editingBugId = bugId;
        formTitle.setText("Edit Bug #" + bugId);
        submitButton.setText("Save changes");
        priorityCombo.setDisable(!Permissions.canSetPriority(Session.user()));
        UiBackend backend = AppContext.backend();
        Async.run(() -> new EditData(backend.listProjects(), backend.getBug(bugId)), this::fill);
    }

    private void fill(EditData data) {
        BugView bug = data.bug();
        projectCombo.getItems().setAll(data.projects());
        projectCombo.setValue(data.projects().stream()
                .filter(p -> p.getId() == bug.getProjectId()).findFirst().orElse(null));
        moduleCombo.setValue(bug.getModule());
        versionField.setText(bug.getVersion());
        titleField.setText(bug.getTitle());
        descriptionArea.setText(bug.getDescription());
        stepsArea.setText(bug.getSteps());
        expectedArea.setText(bug.getExpected());
        actualArea.setText(bug.getActual());
        severityCombo.setValue(bug.getSeverity());
        priorityCombo.setValue(bug.getPriority());
        environmentField.setText(bug.getEnvironment());
        attachmentList.getItems().setAll(bug.getAttachments());
        setVisible(duplicateBox, false);
    }

    private void checkDuplicates() {
        ProjectView project = projectCombo.getValue();
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (editingBugId != null || project == null || title.length() < 4) {
            setVisible(duplicateBox, false);
            return;
        }
        long projectId = project.getId();
        Async.run(() -> AppContext.backend().findSimilarBugs(projectId, title), similar -> {
            duplicateList.getItems().setAll(similar);
            setVisible(duplicateBox, !similar.isEmpty());
        }, error -> setVisible(duplicateBox, false));
    }

    @FXML
    private void onAddAttachment() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Attach files");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All files", "*.*"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Logs and text", "*.log", "*.txt"),
                new FileChooser.ExtensionFilter("Screen recordings", "*.mp4", "*.mov", "*.webm"));
        List<File> files = chooser.showOpenMultipleDialog(titleField.getScene().getWindow());
        if (files != null) {
            files.stream()
                    .map(File::getAbsolutePath)
                    .filter(path -> !attachmentList.getItems().contains(path))
                    .forEach(attachmentList.getItems()::add);
        }
    }

    @FXML
    private void onRemoveAttachment() {
        String selected = attachmentList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            attachmentList.getItems().remove(selected);
        }
    }

    @FXML
    private void onSubmit() {
        if (!validate()) {
            return;
        }
        ProjectView project = projectCombo.getValue();
        BugDraft draft = new BugDraft(project.getId(), moduleCombo.getValue(), text(versionField),
                text(titleField), text(descriptionArea), text(stepsArea), text(expectedArea), text(actualArea),
                text(environmentField), severityCombo.getValue(), priorityCombo.getValue(),
                new ArrayList<>(attachmentList.getItems()));

        submitButton.setDisable(true);
        UserView user = Session.user();
        Long editing = editingBugId;
        UiBackend backend = AppContext.backend();
        Async.run(
                () -> editing == null ? backend.reportBug(draft, user) : backend.updateBug(editing, draft, user),
                bug -> {
                    submitButton.setDisable(false);
                    Navigator.refreshUnread();
                    Navigator.showBug(bug.getId());
                },
                error -> {
                    submitButton.setDisable(false);
                    Dialogs.error(error);
                });
    }

    @FXML
    private void onCancel() {
        if (editingBugId != null) {
            Navigator.showBug(editingBugId);
        } else {
            Navigator.open(View.DASHBOARD);
        }
    }

    /** Checks the required fields, highlights the wrong ones and lists the problems. */
    private boolean validate() {
        List<Control> fields = List.of(projectCombo, titleField, descriptionArea, stepsArea, severityCombo);
        fields.forEach(field -> field.getStyleClass().remove("invalid"));
        List<String> problems = new ArrayList<>();
        if (projectCombo.getValue() == null) {
            invalid(projectCombo, problems, "Choose a project.");
        }
        if (text(titleField).length() < 5) {
            invalid(titleField, problems, "The title must be at least 5 characters.");
        }
        if (text(descriptionArea).isEmpty()) {
            invalid(descriptionArea, problems, "Describe the bug.");
        }
        if (text(stepsArea).isEmpty()) {
            invalid(stepsArea, problems, "Add the steps to reproduce the bug.");
        }
        if (severityCombo.getValue() == null) {
            invalid(severityCombo, problems, "Choose a severity.");
        }
        errorLabel.setText(String.join("\n", problems));
        setVisible(errorLabel, !problems.isEmpty());
        return problems.isEmpty();
    }

    private static void invalid(Control field, List<String> problems, String message) {
        field.getStyleClass().add("invalid");
        problems.add(message);
    }

    private static String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private static String text(TextArea area) {
        return area.getText() == null ? "" : area.getText().trim();
    }

    private static void setVisible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private record EditData(List<ProjectView> projects, BugView bug) {
    }
}
