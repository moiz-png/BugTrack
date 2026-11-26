package com.bugtrack.ui.util;

import com.bugtrack.ui.Navigator;
import com.bugtrack.ui.model.BugStatus;
import com.bugtrack.ui.model.BugView;
import com.bugtrack.ui.model.Priority;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;

import java.util.List;
import java.util.function.Function;

/**
 * Sets up the bug table used by the dashboard and the bug list, so both look and behave the same.
 * Double-click a row (or press Enter) to open the bug.
 */
public final class BugTables {

    private BugTables() {
    }

    /**
     * Adds the standard columns and the open-on-double-click behaviour.
     *
     * @param table the table to configure
     */
    public static void configure(TableView<BugView> table) {
        TableColumn<BugView, Long> id = new TableColumn<>("ID");
        id.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        id.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Long value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : "#" + value);
            }
        });
        id.setPrefWidth(64);

        TableColumn<BugView, BugStatus> status = new TableColumn<>("Status");
        status.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStatus()));
        status.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BugStatus value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                setGraphic(empty || value == null ? null : badge(value.label(), value.styleClass()));
            }
        });
        status.setPrefWidth(140);

        TableColumn<BugView, Priority> priority = new TableColumn<>("Priority");
        priority.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getPriority()));
        priority.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Priority value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                setGraphic(empty || value == null ? null : badge(value.label(), value.styleClass()));
            }
        });
        priority.setPrefWidth(95);

        List<TableColumn<BugView, ?>> columns = List.of(
                id,
                text("Title", BugView::getTitle, 330),
                status,
                priority,
                text("Severity", b -> b.getSeverity().label(), 90),
                text("Assignee", b -> Formats.user(b.getAssignee()), 130),
                text("Project", BugView::getProjectName, 140),
                text("Updated", b -> Formats.dateTime(b.getUpdatedAt()), 145));
        table.getColumns().setAll(columns);
        table.setPlaceholder(new Label("No bugs to show."));

        table.setRowFactory(view -> {
            TableRow<BugView> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Navigator.showBug(row.getItem().getId());
                }
            });
            return row;
        });
        table.setOnKeyPressed(event -> {
            BugView selected = table.getSelectionModel().getSelectedItem();
            if (event.getCode() == KeyCode.ENTER && selected != null) {
                Navigator.showBug(selected.getId());
            }
        });
    }

    /**
     * @param text       the badge text
     * @param styleClass the colour class, e.g. {@code status-closed}
     * @return a small rounded label
     */
    public static Label badge(String text, String styleClass) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("badge", styleClass);
        return badge;
    }

    private static TableColumn<BugView, String> text(String name, Function<BugView, String> value, double width) {
        TableColumn<BugView, String> column = new TableColumn<>(name);
        column.setCellValueFactory(c -> new ReadOnlyStringWrapper(value.apply(c.getValue())));
        column.setPrefWidth(width);
        return column;
    }
}
