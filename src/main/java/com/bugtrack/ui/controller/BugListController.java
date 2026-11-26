package com.bugtrack.ui.controller;

import com.bugtrack.ui.AppContext;
import com.bugtrack.ui.Navigator;
import com.bugtrack.ui.Session;
import com.bugtrack.ui.backend.UiBackend;
import com.bugtrack.ui.model.BugFilter;
import com.bugtrack.ui.model.BugStatus;
import com.bugtrack.ui.model.BugView;
import com.bugtrack.ui.model.Priority;
import com.bugtrack.ui.model.ProjectView;
import com.bugtrack.ui.model.Role;
import com.bugtrack.ui.model.Severity;
import com.bugtrack.ui.model.SettingsView;
import com.bugtrack.ui.model.UserView;
import com.bugtrack.ui.util.Async;
import com.bugtrack.ui.util.BugTables;
import com.bugtrack.ui.util.Combos;
import com.bugtrack.ui.util.Dialogs;
import com.bugtrack.ui.util.Permissions;
import com.bugtrack.ui.util.WorkflowDialogs;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Predicate;

/**
 * Controller for the bug list: search, filters, saved searches, pagination and, for the
 * Admin / PM, bulk assign and bulk close. Also used for the QA verification queue.
 */
public class BugListController {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<ProjectView> projectCombo;
    @FXML
    private ComboBox<String> moduleCombo;
    @FXML
    private ComboBox<BugStatus> statusCombo;
    @FXML
    private ComboBox<Priority> priorityCombo;
    @FXML
    private ComboBox<Severity> severityCombo;
    @FXML
    private ComboBox<UserView> assigneeCombo;
    @FXML
    private DatePicker fromDate;
    @FXML
    private DatePicker toDate;
    @FXML
    private ComboBox<String> savedSearchCombo;
    @FXML
    private HBox bulkBox;
    @FXML
    private Label resultLabel;
    @FXML
    private TableView<BugView> bugTable;
    @FXML
    private Pagination pagination;

    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(300));
    private BugFilter current = BugFilter.all();
    private List<BugView> results = List.of();
    /** True while the code (not the user) is changing the filter controls, so they don't trigger reloads. */
    private boolean updatingControls;

    @FXML
    private void initialize() {
        UserView user = Session.user();
        BugTables.configure(bugTable);
        statusCombo.getItems().setAll(BugStatus.values());
        priorityCombo.getItems().setAll(Priority.values());
        severityCombo.getItems().setAll(Severity.values());
        Combos.showPromptWhenEmpty(projectCombo, moduleCombo, statusCombo, priorityCombo, severityCombo,
                assigneeCombo, savedSearchCombo);

        boolean bulk = Permissions.canBulkEdit(user);
        bulkBox.setVisible(bulk);
        bulkBox.setManaged(bulk);
        bugTable.getSelectionModel().setSelectionMode(bulk ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);

        searchDelay.setOnFinished(event -> onFiltersChanged());
        searchField.textProperty().addListener((obs, old, text) -> {
            if (!updatingControls) {
                searchDelay.playFromStart();
            }
        });
        projectCombo.valueProperty().addListener((obs, old, project) -> {
            boolean wasUpdating = updatingControls;
            updatingControls = true;
            moduleCombo.getItems().setAll(project == null ? List.of() : project.getModules());
            moduleCombo.setValue(null);
            updatingControls = wasUpdating;
            if (!updatingControls) {
                onFiltersChanged();
            }
        });
        for (ComboBox<?> combo : List.<ComboBox<?>>of(moduleCombo, statusCombo, priorityCombo, severityCombo,
                assigneeCombo)) {
            combo.valueProperty().addListener((obs, old, value) -> {
                if (!updatingControls) {
                    onFiltersChanged();
                }
            });
        }
        for (DatePicker picker : List.of(fromDate, toDate)) {
            picker.valueProperty().addListener((obs, old, value) -> {
                if (!updatingControls) {
                    onFiltersChanged();
                }
            });
        }

        pagination.setPageFactory(index -> new Region());
        pagination.currentPageIndexProperty().addListener((obs, old, index) -> showPage(index.intValue()));

        searchField.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
                        searchField::requestFocus);
            }
        });

        refreshSavedSearches();
        UiBackend backend = AppContext.backend();
        Async.run(() -> new Lookups(backend.listProjects(), backend.listUsersByRole(Role.DEVELOPER)), lookups -> {
            updatingControls = true;
            projectCombo.getItems().setAll(lookups.projects());
            assigneeCombo.getItems().setAll(lookups.developers());
            updatingControls = false;
            syncControls();
        });
    }

    /**
     * Shows the bugs that match a filter and sets the filter controls to match it.
     *
     * @param filter the filter to apply
     */
    public void applyFilter(BugFilter filter) {
        current = filter;
        syncControls();
        reload();
    }

    private void syncControls() {
        updatingControls = true;
        try {
            searchField.setText(current.text() == null ? "" : current.text());
            projectCombo.setValue(find(projectCombo.getItems(),
                    p -> current.projectId() != null && p.getId() == current.projectId()));
            moduleCombo.setValue(current.module());
            statusCombo.setValue(current.status());
            priorityCombo.setValue(current.priority());
            severityCombo.setValue(current.severity());
            assigneeCombo.setValue(find(assigneeCombo.getItems(),
                    u -> current.assigneeId() != null && u.getId() == current.assigneeId()));
            fromDate.setValue(current.createdFrom());
            toDate.setValue(current.createdTo());
        } finally {
            updatingControls = false;
        }
    }

    private void onFiltersChanged() {
        ProjectView project = projectCombo.getValue();
        UserView assignee = assigneeCombo.getValue();
        String text = searchField.getText().trim();
        current = new BugFilter(text.isEmpty() ? null : text,
                project == null ? null : project.getId(),
                moduleCombo.getValue(), statusCombo.getValue(), priorityCombo.getValue(), severityCombo.getValue(),
                assignee == null ? null : assignee.getId(),
                current.reporterId(), fromDate.getValue(), toDate.getValue());
        reload();
    }

    private void reload() {
        BugFilter filter = current;
        Async.run(() -> AppContext.backend().findBugs(filter), bugs -> {
            results = bugs;
            resultLabel.setText(bugs.size() == 1 ? "1 bug" : bugs.size() + " bugs");
            pagination.setPageCount(Math.max(1, (int) Math.ceil(bugs.size() / (double) pageSize())));
            if (pagination.getCurrentPageIndex() == 0) {
                showPage(0);
            } else {
                pagination.setCurrentPageIndex(0);
            }
        });
    }

    private void showPage(int index) {
        int size = pageSize();
        int from = Math.min(index * size, results.size());
        int to = Math.min(from + size, results.size());
        bugTable.getItems().setAll(results.subList(from, to));
    }

    private int pageSize() {
        return Session.settings().getPageSize();
    }

    @FXML
    private void onClearFilters() {
        applyFilter(BugFilter.all());
    }

    // ----- Saved searches (stored in the user's serialized settings) -----

    private void refreshSavedSearches() {
        updatingControls = true;
        savedSearchCombo.getItems().setAll(Session.settings().getSavedSearches().keySet());
        savedSearchCombo.setValue(null);
        updatingControls = false;
    }

    @FXML
    private void onSavedSearchChosen() {
        String name = savedSearchCombo.getValue();
        if (updatingControls || name == null) {
            return;
        }
        BugFilter saved = Session.settings().getSavedSearches().get(name);
        if (saved != null) {
            applyFilter(saved);
            Platform.runLater(() -> {
                updatingControls = true;
                savedSearchCombo.setValue(name);
                updatingControls = false;
            });
        }
    }

    @FXML
    private void onSaveSearch() {
        Dialogs.text("Save search", "Name for this search:", "")
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .ifPresent(name -> {
                    SettingsView settings = Session.settings();
                    settings.getSavedSearches().put(name, current);
                    saveSettings(settings);
                });
    }

    @FXML
    private void onDeleteSearch() {
        String name = savedSearchCombo.getValue();
        if (name == null) {
            Dialogs.info("Delete saved search", "Choose a saved search first.");
            return;
        }
        if (Dialogs.confirm("Delete saved search", "Delete the saved search \"" + name + "\"?")) {
            SettingsView settings = Session.settings();
            settings.getSavedSearches().remove(name);
            saveSettings(settings);
        }
    }

    private void saveSettings(SettingsView settings) {
        long userId = Session.user().getId();
        Async.run(() -> {
            AppContext.backend().saveSettings(userId, settings);
            return Boolean.TRUE;
        }, done -> refreshSavedSearches());
    }

    // ----- Bulk operations (Admin / PM) -----

    @FXML
    private void onBulkAssign() {
        List<Long> ids = selectedIds();
        if (ids.isEmpty()) {
            Dialogs.info("Assign selected bugs", "Select one or more bugs first (Ctrl+click or Shift+click).");
            return;
        }
        List<UserView> developers = assigneeCombo.getItems().stream().filter(UserView::isActive).toList();
        WorkflowDialogs.chooseDeveloper(developers, ids.size()).ifPresent(developer -> {
            UserView actor = Session.user();
            Async.run(() -> AppContext.backend().bulkAssign(ids, developer.getId(), actor), assigned -> {
                Dialogs.info("Assign selected bugs",
                        assigned + " of " + ids.size() + " bugs assigned to " + developer.getName() + "."
                                + (assigned < ids.size() ? " Closed or rejected bugs were skipped." : ""));
                Navigator.refreshUnread();
                reload();
            });
        });
    }

    @FXML
    private void onBulkClose() {
        List<Long> ids = selectedIds();
        if (ids.isEmpty()) {
            Dialogs.info("Close verified bugs", "Select one or more bugs first (Ctrl+click or Shift+click).");
            return;
        }
        if (!Dialogs.confirm("Close verified bugs",
                "Close the selected bugs? Only bugs waiting in QA Verification will be closed.")) {
            return;
        }
        UserView actor = Session.user();
        Async.run(() -> AppContext.backend().bulkClose(ids, actor), closed -> {
            Dialogs.info("Close verified bugs", closed + " of " + ids.size() + " bugs closed.");
            Navigator.refreshUnread();
            reload();
        });
    }

    private List<Long> selectedIds() {
        return bugTable.getSelectionModel().getSelectedItems().stream().map(BugView::getId).toList();
    }

    private static <T> T find(List<T> items, Predicate<T> condition) {
        return items.stream().filter(condition).findFirst().orElse(null);
    }

    private record Lookups(List<ProjectView> projects, List<UserView> developers) {
    }
}
