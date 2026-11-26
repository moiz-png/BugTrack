package com.bugtrack.ui.controller;

import com.bugtrack.ui.AppContext;
import com.bugtrack.ui.Navigator;
import com.bugtrack.ui.Session;
import com.bugtrack.ui.View;
import com.bugtrack.ui.backend.UiBackend;
import com.bugtrack.ui.model.ActivityView;
import com.bugtrack.ui.model.BugView;
import com.bugtrack.ui.model.CommentView;
import com.bugtrack.ui.model.ResolutionView;
import com.bugtrack.ui.model.Role;
import com.bugtrack.ui.model.UserView;
import com.bugtrack.ui.util.Async;
import com.bugtrack.ui.util.Dialogs;
import com.bugtrack.ui.util.Formats;
import com.bugtrack.ui.util.Permissions;
import com.bugtrack.ui.util.WorkflowDialogs;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Controller for the bug details page: all fields, the lifecycle action buttons for the
 * user's role, threaded comments with mentions, attachments and the activity history.
 */
public class BugDetailController {

    @FXML
    private Label bugIdLabel;
    @FXML
    private Label bugTitle;
    @FXML
    private Label statusBadge;
    @FXML
    private HBox actionBar;
    @FXML
    private Label projectValue;
    @FXML
    private Label moduleValue;
    @FXML
    private Label versionValue;
    @FXML
    private Label severityValue;
    @FXML
    private Label priorityValue;
    @FXML
    private Label dueValue;
    @FXML
    private Label reporterValue;
    @FXML
    private Label assigneeValue;
    @FXML
    private Label reopenValue;
    @FXML
    private Label environmentValue;
    @FXML
    private Label createdValue;
    @FXML
    private Label updatedValue;
    @FXML
    private Label descriptionValue;
    @FXML
    private Label stepsValue;
    @FXML
    private Label expectedValue;
    @FXML
    private Label actualValue;
    @FXML
    private Label rootCauseValue;
    @FXML
    private Label fixValue;
    @FXML
    private Label commitValue;
    @FXML
    private Label resolvedByValue;
    @FXML
    private Tab commentsTab;
    @FXML
    private VBox commentsBox;
    @FXML
    private HBox replyBar;
    @FXML
    private Label replyingToLabel;
    @FXML
    private TextArea commentArea;
    @FXML
    private ListView<String> attachmentList;
    @FXML
    private TableView<ActivityView> historyTable;
    @FXML
    private TableColumn<ActivityView, String> historyWhen;
    @FXML
    private TableColumn<ActivityView, String> historyWho;
    @FXML
    private TableColumn<ActivityView, String> historyAction;
    @FXML
    private TableColumn<ActivityView, String> historyChange;

    private BugView bug;
    private Long replyToId;

    @FXML
    private void initialize() {
        historyWhen.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formats.dateTime(c.getValue().timestamp())));
        historyWho.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().userName()));
        historyAction.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().action()));
        historyChange.setCellValueFactory(c -> new ReadOnlyStringWrapper(describeChange(c.getValue())));
        historyTable.setPlaceholder(new Label("No history yet."));

        attachmentList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                setText(empty || path == null ? null : new File(path).getName());
                setTooltip(empty || path == null ? null : new Tooltip(path));
            }
        });
        attachmentList.setPlaceholder(new Label("No attachments."));
        attachmentList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOpenAttachment();
            }
        });
        cancelReply();
    }

    /**
     * Loads and shows a bug with its comments and history.
     *
     * @param bugId the bug to show
     */
    public void load(long bugId) {
        UiBackend backend = AppContext.backend();
        Async.run(() -> new Details(backend.getBug(bugId), backend.listComments(bugId), backend.listActivity(bugId)),
                this::show);
    }

    private void show(Details details) {
        bug = details.bug();
        commentsTab.setText("Comments (" + details.comments().size() + ")");

        bugIdLabel.setText("#" + bug.getId());
        bugTitle.setText(bug.getTitle());
        statusBadge.setText(bug.getStatus().label());
        statusBadge.getStyleClass().removeIf(style -> style.startsWith("status-"));
        statusBadge.getStyleClass().add(bug.getStatus().styleClass());

        projectValue.setText(bug.getProjectName());
        moduleValue.setText(Formats.orDash(bug.getModule()));
        versionValue.setText(Formats.orDash(bug.getVersion()));
        severityValue.setText(bug.getSeverity().label());
        priorityValue.setText(bug.getPriority().label());
        dueValue.setText(Formats.date(bug.getDueDate()));
        reporterValue.setText(Formats.user(bug.getReporter()));
        assigneeValue.setText(Formats.user(bug.getAssignee()));
        reopenValue.setText(bug.getReopenCount() == 1 ? "1 time" : bug.getReopenCount() + " times");
        environmentValue.setText(Formats.orDash(bug.getEnvironment()));
        createdValue.setText(Formats.dateTime(bug.getCreatedAt()));
        updatedValue.setText(Formats.dateTime(bug.getUpdatedAt()));

        descriptionValue.setText(Formats.orDash(bug.getDescription()));
        stepsValue.setText(Formats.orDash(bug.getSteps()));
        expectedValue.setText(Formats.orDash(bug.getExpected()));
        actualValue.setText(Formats.orDash(bug.getActual()));

        ResolutionView resolution = bug.getResolution();
        rootCauseValue.setText(resolution == null ? "Not resolved yet." : resolution.rootCause());
        fixValue.setText(resolution == null ? "—" : resolution.fixDescription());
        commitValue.setText(resolution == null ? "—" : Formats.orDash(resolution.commitRef()));
        resolvedByValue.setText(resolution == null ? "—"
                : resolution.resolvedBy() + " on " + Formats.dateTime(resolution.resolvedAt()));

        attachmentList.getItems().setAll(bug.getAttachments());
        historyTable.getItems().setAll(details.activity());
        renderComments(details.comments());
        buildActions();
    }

    // ----- Lifecycle actions -----

    /** Shows only the actions the user's role may take at the bug's current stage. */
    private void buildActions() {
        actionBar.getChildren().clear();
        UserView user = Session.user();
        long bugId = bug.getId();
        UiBackend backend = AppContext.backend();

        if (Permissions.canEdit(user, bug)) {
            addAction("Edit", false, () -> Navigator.editBug(bugId));
        }
        if (Permissions.canAssign(user, bug)) {
            addAction(bug.getAssignee() == null ? "Assign" : "Reassign", true, this::onAssign);
        }
        if (Permissions.canStartWork(user, bug)) {
            addAction("Start work", true, () -> perform(() -> backend.startWork(bugId, user)));
        }
        if (Permissions.canResolve(user, bug)) {
            addAction("Submit fix for QA", true, () -> WorkflowDialogs.resolve(bugId).ifPresent(input ->
                    perform(() -> backend.resolveBug(bugId, input.rootCause(), input.fixDescription(),
                            input.commitRef(), user))));
        }
        if (Permissions.canVerify(user, bug)) {
            addAction("Verify & close", true, () -> {
                if (Dialogs.confirm("Verify fix", "Confirm that the fix for bug #" + bugId
                        + " works and close the bug?")) {
                    perform(() -> backend.verifyFix(bugId, user));
                }
            });
        }
        if (Permissions.canReopen(user, bug)) {
            addAction("Reopen", false, () -> WorkflowDialogs
                    .reason("Reopen bug #" + bugId, "Why is the bug still present?")
                    .ifPresent(reason -> perform(() -> backend.reopenBug(bugId, reason, user))));
        }
        if (Permissions.canReject(user, bug)) {
            addAction("Reject", false, () -> WorkflowDialogs
                    .reason("Reject bug #" + bugId,
                            "Why is this not a valid bug? For a duplicate, write e.g. \"Duplicate of #12\".")
                    .ifPresent(reason -> perform(() -> backend.rejectBug(bugId, reason, user))));
        }
        if (actionBar.getChildren().isEmpty()) {
            Label none = new Label("No actions for your role at this stage.");
            none.getStyleClass().add("muted");
            actionBar.getChildren().add(none);
        }
    }

    private void addAction(String text, boolean primary, Runnable handler) {
        Button button = new Button(text);
        button.getStyleClass().add(primary ? "primary" : "ghost");
        button.setOnAction(event -> handler.run());
        actionBar.getChildren().add(button);
    }

    private void onAssign() {
        long bugId = bug.getId();
        BugView current = bug;
        UserView actor = Session.user();
        UiBackend backend = AppContext.backend();
        Async.run(() -> backend.listUsersByRole(Role.DEVELOPER), developers ->
                WorkflowDialogs.assign(developers.stream().filter(UserView::isActive).toList(), current)
                        .ifPresent(input -> perform(() -> backend.assignBug(bugId, input.developer().getId(),
                                input.priority(), input.dueDate(), actor))));
    }

    /** Runs a lifecycle action in the background, then reloads the page. */
    private void perform(Callable<BugView> action) {
        actionBar.setDisable(true);
        Async.run(action, updated -> {
            actionBar.setDisable(false);
            Navigator.refreshUnread();
            load(updated.getId());
        }, error -> {
            actionBar.setDisable(false);
            Dialogs.error(error);
        });
    }

    // ----- Comments -----

    private void renderComments(List<CommentView> comments) {
        commentsBox.getChildren().clear();
        if (comments.isEmpty()) {
            Label empty = new Label("No comments yet.");
            empty.getStyleClass().add("muted");
            commentsBox.getChildren().add(empty);
            return;
        }
        Map<Long, List<CommentView>> replies = comments.stream()
                .filter(CommentView::isReply)
                .collect(Collectors.groupingBy(CommentView::parentId));
        for (CommentView comment : comments) {
            if (!comment.isReply()) {
                commentsBox.getChildren().add(commentNode(comment, false));
                for (CommentView reply : replies.getOrDefault(comment.id(), List.of())) {
                    commentsBox.getChildren().add(commentNode(reply, true));
                }
            }
        }
    }

    private Node commentNode(CommentView comment, boolean reply) {
        Label meta = new Label(comment.authorName() + "  ·  " + Formats.dateTime(comment.createdAt()));
        meta.getStyleClass().add("comment-meta");

        TextFlow body = new TextFlow();
        List<Node> parts = new ArrayList<>();
        for (String part : comment.body().split("(?<=\\s)|(?=\\s)")) {
            Text text = new Text(part);
            text.getStyleClass().add(part.startsWith("@") && part.length() > 1 ? "mention" : "comment-text");
            parts.add(text);
        }
        body.getChildren().setAll(parts);

        VBox box = new VBox(4, meta, body);
        box.getStyleClass().add("comment");
        if (reply) {
            VBox.setMargin(box, new Insets(0, 0, 0, 36));
        } else {
            Hyperlink replyLink = new Hyperlink("Reply");
            replyLink.setOnAction(event -> startReply(comment));
            box.getChildren().add(replyLink);
        }
        return box;
    }

    private void startReply(CommentView comment) {
        replyToId = comment.id();
        replyingToLabel.setText("Replying to " + comment.authorName());
        replyBar.setVisible(true);
        replyBar.setManaged(true);
        commentArea.requestFocus();
    }

    @FXML
    private void onCancelReply() {
        cancelReply();
    }

    private void cancelReply() {
        replyToId = null;
        replyBar.setVisible(false);
        replyBar.setManaged(false);
    }

    @FXML
    private void onPostComment() {
        String body = commentArea.getText() == null ? "" : commentArea.getText().trim();
        if (body.isEmpty() || bug == null) {
            return;
        }
        long bugId = bug.getId();
        Long parentId = replyToId;
        UserView author = Session.user();
        Async.run(() -> AppContext.backend().addComment(bugId, parentId, body, author), comment -> {
            commentArea.clear();
            cancelReply();
            Navigator.refreshUnread();
            load(bugId);
        });
    }

    // ----- Attachments -----

    @FXML
    private void onAddAttachment() {
        if (bug == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Attach files to bug #" + bug.getId());
        List<File> files = chooser.showOpenMultipleDialog(attachmentList.getScene().getWindow());
        if (files == null || files.isEmpty()) {
            return;
        }
        long bugId = bug.getId();
        UserView actor = Session.user();
        Async.run(() -> {
            BugView updated = null;
            for (File file : files) {
                updated = AppContext.backend().addAttachment(bugId, file.getAbsolutePath(), actor);
            }
            return updated;
        }, updated -> load(bugId));
    }

    @FXML
    private void onOpenAttachment() {
        String selected = attachmentList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (!new File(selected).exists()) {
            Dialogs.error("The file " + selected + " cannot be found on this computer.");
            return;
        }
        AppContext.openFile(selected);
    }

    @FXML
    private void onBack() {
        Navigator.open(View.BUG_LIST);
    }

    private static String describeChange(ActivityView entry) {
        if (entry.oldValue() == null && entry.newValue() == null) {
            return "";
        }
        if (entry.oldValue() == null) {
            return entry.newValue();
        }
        return entry.oldValue() + "  →  " + entry.newValue();
    }

    private record Details(BugView bug, List<CommentView> comments, List<ActivityView> activity) {
    }
}
