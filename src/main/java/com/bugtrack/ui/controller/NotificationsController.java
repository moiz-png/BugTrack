package com.bugtrack.ui.controller;

import com.bugtrack.ui.AppContext;
import com.bugtrack.ui.Navigator;
import com.bugtrack.ui.Session;
import com.bugtrack.ui.model.NotificationView;
import com.bugtrack.ui.util.Async;
import com.bugtrack.ui.util.Formats;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

/**
 * Controller for the notifications screen: assignments, status changes, comments,
 * mentions and reopened bugs. Unread notifications are shown in bold.
 */
public class NotificationsController {

    @FXML
    private ListView<NotificationView> notificationList;

    @FXML
    private void initialize() {
        notificationList.setPlaceholder(new Label("You have no notifications."));
        notificationList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(NotificationView notification, boolean empty) {
                super.updateItem(notification, empty);
                getStyleClass().remove("unread");
                if (empty || notification == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label message = new Label(notification.message());
                message.getStyleClass().add("notification-message");
                message.setWrapText(true);
                Label time = new Label(Formats.dateTime(notification.createdAt()));
                time.getStyleClass().add("comment-meta");
                setGraphic(new VBox(2, message, time));
                setText(null);
                if (!notification.read()) {
                    getStyleClass().add("unread");
                }
            }
        });
        notificationList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                openSelected();
            }
        });
        notificationList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                openSelected();
            }
        });
        reload();
    }

    private void reload() {
        long userId = Session.user().getId();
        Async.run(() -> AppContext.backend().listNotifications(userId),
                notifications -> notificationList.getItems().setAll(notifications));
    }

    private void openSelected() {
        NotificationView selected = notificationList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Async.run(() -> {
            if (!selected.read()) {
                AppContext.backend().markNotificationRead(selected.id());
            }
            return selected.bugId();
        }, bugId -> {
            Navigator.refreshUnread();
            Navigator.showBug(bugId);
        });
    }

    @FXML
    private void onMarkAllRead() {
        long userId = Session.user().getId();
        Async.run(() -> {
            AppContext.backend().markAllRead(userId);
            return Boolean.TRUE;
        }, done -> {
            Navigator.refreshUnread();
            reload();
        });
    }

    @FXML
    private void onRefresh() {
        Navigator.refreshUnread();
        reload();
    }
}
