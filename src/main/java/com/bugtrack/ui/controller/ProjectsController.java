package com.bugtrack.ui.controller;

import com.bugtrack.ui.AppContext;
import com.bugtrack.ui.backend.UiBackend;
import com.bugtrack.ui.model.ProjectView;
import com.bugtrack.ui.model.UserView;
import com.bugtrack.ui.util.Async;
import com.bugtrack.ui.util.Combos;
import com.bugtrack.ui.util.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * Controller for project management (Admin / PM only): projects with their version,
 * modules and team members.
 */
public class ProjectsController {

    @FXML
    private ListView<ProjectView> projectList;
    @FXML
    private Label formTitle;
    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField versionField;
    @FXML
    private ListView<String> moduleList;
    @FXML
    private TextField moduleField;
    @FXML
    private ListView<UserView> memberList;
    @FXML
    private ComboBox<UserView> memberCombo;
    @FXML
    private Label errorLabel;

    private ProjectView editing;
    private List<UserView> allUsers = List.of();

    @FXML
    private void initialize() {
        projectList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ProjectView project, boolean empty) {
                super.updateItem(project, empty);
                setText(empty || project == null ? null
                        : project.getName() + "   v" + project.getVersion()
                        + "   ·   " + project.getModules().size() + " modules");
            }
        });
        projectList.setPlaceholder(new Label("No projects yet."));
        memberList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(UserView user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.getName() + "  (" + user.getRole().label() + ")");
            }
        });
        Combos.showPromptWhenEmpty(memberCombo);
        projectList.getSelectionModel().selectedItemProperty().addListener((obs, old, project) -> {
            if (project != null) {
                edit(project);
            }
        });

        startNew();
        UiBackend backend = AppContext.backend();
        Async.run(backend::listUsers, users -> {
            allUsers = users;
            memberCombo.getItems().setAll(users.stream().filter(UserView::isActive).toList());
            reload(null);
        });
    }

    private void reload(Long selectId) {
        Async.run(() -> AppContext.backend().listProjects(), projects -> {
            projectList.getItems().setAll(projects);
            if (selectId != null) {
                projects.stream().filter(p -> p.getId() == selectId).findFirst()
                        .ifPresent(p -> projectList.getSelectionModel().select(p));
            }
        });
    }

    @FXML
    private void onNew() {
        projectList.getSelectionModel().clearSelection();
        startNew();
    }

    private void startNew() {
        editing = null;
        formTitle.setText("New project");
        nameField.clear();
        descriptionArea.clear();
        versionField.setText("1.0.0");
        moduleList.getItems().clear();
        memberList.getItems().clear();
        hideError();
    }

    private void edit(ProjectView project) {
        editing = project;
        formTitle.setText("Edit " + project.getName());
        nameField.setText(project.getName());
        descriptionArea.setText(project.getDescription());
        versionField.setText(project.getVersion());
        moduleList.getItems().setAll(project.getModules());
        memberList.getItems().setAll(allUsers.stream()
                .filter(u -> project.getMemberIds().contains(u.getId()))
                .toList());
        hideError();
    }

    @FXML
    private void onAddModule() {
        String module = moduleField.getText() == null ? "" : moduleField.getText().trim();
        if (module.isEmpty()) {
            return;
        }
        if (moduleList.getItems().stream().anyMatch(m -> m.equalsIgnoreCase(module))) {
            showError("The module \"" + module + "\" already exists.");
            return;
        }
        moduleList.getItems().add(module);
        moduleField.clear();
        hideError();
    }

    @FXML
    private void onRemoveModule() {
        String selected = moduleList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            moduleList.getItems().remove(selected);
        }
    }

    @FXML
    private void onAddMember() {
        UserView user = memberCombo.getValue();
        if (user != null && !memberList.getItems().contains(user)) {
            memberList.getItems().add(user);
        }
        memberCombo.setValue(null);
    }

    @FXML
    private void onRemoveMember() {
        UserView selected = memberList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            memberList.getItems().remove(selected);
        }
    }

    @FXML
    private void onSave() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            showError("A project needs a name.");
            return;
        }
        ProjectView project = new ProjectView(editing == null ? 0 : editing.getId(), name,
                descriptionArea.getText() == null ? "" : descriptionArea.getText().trim(),
                versionField.getText() == null ? "" : versionField.getText().trim());
        project.getModules().addAll(moduleList.getItems());
        project.getMemberIds().addAll(memberList.getItems().stream().map(UserView::getId).toList());

        Async.run(() -> AppContext.backend().saveProject(project), saved -> {
            Dialogs.info("Project saved", saved.getName() + " has been saved.");
            reload(saved.getId());
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
}
