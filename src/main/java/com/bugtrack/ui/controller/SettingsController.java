package com.bugtrack.ui.controller;

import com.bugtrack.ui.AppContext;
import com.bugtrack.ui.Navigator;
import com.bugtrack.ui.Session;
import com.bugtrack.ui.model.ProjectView;
import com.bugtrack.ui.model.SettingsView;
import com.bugtrack.ui.util.Async;
import com.bugtrack.ui.util.Combos;
import com.bugtrack.ui.util.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

/**
 * Controller for the user's settings: theme, default project, page size and saved searches.
 * The settings object is serialized by the backend and restored at the next login.
 */
public class SettingsController {

    private static final String LIGHT = "Light";
    private static final String DARK = "Dark";

    @FXML
    private ComboBox<String> themeCombo;
    @FXML
    private ComboBox<ProjectView> defaultProjectCombo;
    @FXML
    private Spinner<Integer> pageSizeSpinner;
    @FXML
    private ListView<String> savedSearchList;

    @FXML
    private void initialize() {
        SettingsView settings = Session.settings();
        themeCombo.getItems().setAll(LIGHT, DARK);
        themeCombo.setValue(SettingsView.DARK.equals(settings.getTheme()) ? DARK : LIGHT);
        pageSizeSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 100, settings.getPageSize(), 5));
        savedSearchList.getItems().setAll(settings.getSavedSearches().keySet());
        savedSearchList.setPlaceholder(new javafx.scene.control.Label("Save a search from the bug list."));
        Combos.showPromptWhenEmpty(defaultProjectCombo);

        Long defaultProjectId = settings.getDefaultProjectId();
        Async.run(() -> AppContext.backend().listProjects(), projects -> {
            defaultProjectCombo.getItems().setAll(projects);
            if (defaultProjectId != null) {
                projects.stream().filter(p -> p.getId() == defaultProjectId).findFirst()
                        .ifPresent(defaultProjectCombo::setValue);
            }
        });
    }

    @FXML
    private void onClearDefaultProject() {
        defaultProjectCombo.setValue(null);
    }

    @FXML
    private void onDeleteSearch() {
        String selected = savedSearchList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            savedSearchList.getItems().remove(selected);
        }
    }

    @FXML
    private void onSave() {
        SettingsView settings = Session.settings();
        settings.setTheme(DARK.equals(themeCombo.getValue()) ? SettingsView.DARK : SettingsView.LIGHT);
        ProjectView project = defaultProjectCombo.getValue();
        settings.setDefaultProjectId(project == null ? null : project.getId());
        settings.setPageSize(pageSizeSpinner.getValue());
        settings.getSavedSearches().keySet().retainAll(savedSearchList.getItems());

        long userId = Session.user().getId();
        Async.run(() -> {
            AppContext.backend().saveSettings(userId, settings);
            return Boolean.TRUE;
        }, done -> {
            Navigator.applyTheme();
            Dialogs.info("Settings saved", "Your settings have been saved.");
        });
    }
}
