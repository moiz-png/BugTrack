package com.bugtrack.ui.controller;

import com.bugtrack.ui.AppContext;
import com.bugtrack.ui.Session;
import com.bugtrack.ui.model.BugFilter;
import com.bugtrack.ui.model.BugStatus;
import com.bugtrack.ui.model.BugView;
import com.bugtrack.ui.model.Priority;
import com.bugtrack.ui.model.UserView;
import com.bugtrack.ui.util.Async;
import com.bugtrack.ui.util.BugTables;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Controller for the role-specific dashboard: four counters, two charts and a "needs your
 * attention" list. Admin / PM sees everything, a developer their own work, and a QA tester
 * the bugs waiting for verification.
 */
public class DashboardController {

    @FXML
    private Label openCount;
    @FXML
    private Label mineCaption;
    @FXML
    private Label mineCount;
    @FXML
    private Label verifyCount;
    @FXML
    private Label closedCount;
    @FXML
    private PieChart statusChart;
    @FXML
    private BarChart<String, Number> priorityChart;
    @FXML
    private Label listTitle;
    @FXML
    private TableView<BugView> listTable;

    @FXML
    private void initialize() {
        BugTables.configure(listTable);
        UserView user = Session.user();
        mineCaption.setText(switch (user.getRole()) {
            case ADMIN -> "New, waiting for triage";
            case DEVELOPER -> "Assigned to me";
            case QA_TESTER -> "Reported by me";
        });
        BugFilter scope = switch (user.getRole()) {
            case DEVELOPER -> BugFilter.assignedTo(user.getId());
            case ADMIN, QA_TESTER -> BugFilter.all();
        };
        Async.run(() -> AppContext.backend().findBugs(scope), bugs -> show(user, bugs));
    }

    private void show(UserView user, List<BugView> bugs) {
        openCount.setText(String.valueOf(count(bugs, b -> b.getStatus().isOpen())));
        verifyCount.setText(String.valueOf(count(bugs, b -> b.getStatus() == BugStatus.QA_VERIFICATION)));
        closedCount.setText(String.valueOf(count(bugs, b -> b.getStatus() == BugStatus.CLOSED)));
        mineCount.setText(String.valueOf(switch (user.getRole()) {
            case ADMIN -> count(bugs, b -> b.getStatus() == BugStatus.NEW);
            case DEVELOPER -> count(bugs, b -> b.getStatus().isOpen());
            case QA_TESTER -> count(bugs, b -> b.getReporter().getId() == user.getId());
        }));

        Map<BugStatus, Long> byStatus = bugs.stream().collect(Collectors.groupingBy(
                BugView::getStatus, () -> new EnumMap<>(BugStatus.class), Collectors.counting()));
        statusChart.setData(byStatus.entrySet().stream()
                .map(e -> new PieChart.Data(e.getKey().label() + " (" + e.getValue() + ")", e.getValue()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Priority priority : Priority.values()) {
            series.getData().add(new XYChart.Data<>(priority.label(),
                    count(bugs, b -> b.getStatus().isOpen() && b.getPriority() == priority)));
        }
        priorityChart.getData().clear();
        priorityChart.getData().add(series);

        Focus focus = switch (user.getRole()) {
            case ADMIN -> new Focus("New bugs waiting for triage", b -> b.getStatus() == BugStatus.NEW);
            case DEVELOPER -> new Focus("My open bugs", b -> b.getStatus().isOpen());
            case QA_TESTER -> new Focus("Waiting for your verification",
                    b -> b.getStatus() == BugStatus.QA_VERIFICATION);
        };
        listTitle.setText(focus.title());
        listTable.setItems(FXCollections.observableArrayList(bugs.stream().filter(focus.filter()).toList()));
    }

    private static long count(List<BugView> bugs, Predicate<BugView> condition) {
        return bugs.stream().filter(condition).count();
    }

    private record Focus(String title, Predicate<BugView> filter) {
    }
}
