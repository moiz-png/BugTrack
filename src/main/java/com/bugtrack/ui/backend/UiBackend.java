package com.bugtrack.ui.backend;

import com.bugtrack.ui.model.ActivityView;
import com.bugtrack.ui.model.BugDraft;
import com.bugtrack.ui.model.BugFilter;
import com.bugtrack.ui.model.BugView;
import com.bugtrack.ui.model.CommentView;
import com.bugtrack.ui.model.ExportFormat;
import com.bugtrack.ui.model.NotificationView;
import com.bugtrack.ui.model.Priority;
import com.bugtrack.ui.model.ProjectView;
import com.bugtrack.ui.model.ReportType;
import com.bugtrack.ui.model.ReportView;
import com.bugtrack.ui.model.Role;
import com.bugtrack.ui.model.SettingsView;
import com.bugtrack.ui.model.UserView;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Everything the frontend needs from the backend, in one place.
 * <p>The screens only ever talk to this interface. Today it is implemented by {@link FakeBackend};
 * once the service layer is ready, an adapter that calls the real services
 * ({@code AuthService}, {@code BugService}, {@code WorkflowService}, ...) replaces it,
 * and no screen has to change.</p>
 * <p>Every method may be slow (it talks to the database), so the screens always call it
 * on a background thread. Methods throw {@link IllegalStateException} when an action is not
 * allowed (wrong role or wrong lifecycle stage) and {@link IllegalArgumentException} when
 * something does not exist; the message is shown to the user as is.</p>
 */
public interface UiBackend {

    // ----- Authentication -----

    /**
     * Checks a user's email and password.
     *
     * @param email    login email
     * @param password plain-text password as typed
     * @return the user, or empty if the details are wrong or the account is deactivated
     */
    Optional<UserView> login(String email, String password);

    /**
     * Ends the user's session.
     *
     * @param user the user logging out
     */
    void logout(UserView user);

    // ----- Users (Admin / PM) -----

    /**
     * @return every user, active or not
     */
    List<UserView> listUsers();

    /**
     * @param role the role to keep
     * @return users with the given role
     */
    List<UserView> listUsersByRole(Role role);

    /**
     * Creates a user account.
     *
     * @param name     full name
     * @param email    login email; must be unique
     * @param role     role of the new user
     * @param password initial password
     * @return the created user
     */
    UserView createUser(String name, String email, Role role, String password);

    /**
     * Saves changes to a user's name, email and role.
     *
     * @param user the user with the new values
     * @return the updated user
     */
    UserView updateUser(UserView user);

    /**
     * Activates or deactivates an account. Deactivated users cannot log in.
     *
     * @param userId the user
     * @param active the new state
     */
    void setUserActive(long userId, boolean active);

    /**
     * Sets a new password for a user (administrator-initiated reset).
     *
     * @param userId      the user
     * @param newPassword the new password
     */
    void resetPassword(long userId, String newPassword);

    // ----- Projects -----

    /**
     * @return every project
     */
    List<ProjectView> listProjects();

    /**
     * Creates a project (id {@code 0}) or saves changes to an existing one, including its modules and members.
     *
     * @param project the project
     * @return the saved project, with its id set
     */
    ProjectView saveProject(ProjectView project);

    // ----- Bugs -----

    /**
     * @param filter search and filter criteria
     * @return matching bugs, most recently updated first
     */
    List<BugView> findBugs(BugFilter filter);

    /**
     * @param bugId the bug
     * @return the bug
     */
    BugView getBug(long bugId);

    /**
     * Finds open bugs in the same project whose titles share keywords with the given title,
     * so the reporter can spot a duplicate before submitting.
     *
     * @param projectId the project
     * @param title     the title being typed
     * @return up to five similar bugs
     */
    List<BugView> findSimilarBugs(long projectId, String title);

    /**
     * Reports a new bug. It starts in the New status.
     *
     * @param draft    the form contents
     * @param reporter who is reporting it
     * @return the created bug
     */
    BugView reportBug(BugDraft draft, UserView reporter);

    /**
     * Saves edits to an open bug's details.
     *
     * @param bugId the bug
     * @param draft the new form contents
     * @param actor who is editing
     * @return the updated bug
     */
    BugView updateBug(long bugId, BugDraft draft, UserView actor);

    /**
     * Adds an attachment to a bug.
     *
     * @param bugId the bug
     * @param path  path of the file
     * @param actor who is attaching it
     * @return the updated bug
     */
    BugView addAttachment(long bugId, String path, UserView actor);

    // ----- Workflow (State pattern on the backend) -----

    /**
     * Assigns a bug to a developer and sets its priority and due date (Admin / PM).
     * The assignment, its activity-log entry and the notification are saved in one transaction.
     *
     * @param bugId      the bug
     * @param assigneeId the developer
     * @param priority   the priority
     * @param dueDate    the due date, or {@code null}
     * @param actor      who is assigning
     * @return the updated bug
     */
    BugView assignBug(long bugId, long assigneeId, Priority priority, LocalDate dueDate, UserView actor);

    /**
     * Moves an assigned or reopened bug to In Progress.
     *
     * @param bugId the bug
     * @param actor the assigned developer (or Admin / PM)
     * @return the updated bug
     */
    BugView startWork(long bugId, UserView actor);

    /**
     * Records the developer's fix and sends the bug to QA verification.
     *
     * @param bugId          the bug
     * @param rootCause      why it happened
     * @param fixDescription what was changed
     * @param commitRef      commit hash or build number, or {@code null}
     * @param actor          the assigned developer (or Admin / PM)
     * @return the updated bug
     */
    BugView resolveBug(long bugId, String rootCause, String fixDescription, String commitRef, UserView actor);

    /**
     * Confirms that a fix works and closes the bug (QA Tester or Admin / PM).
     *
     * @param bugId the bug
     * @param actor who verified it
     * @return the updated bug
     */
    BugView verifyFix(long bugId, UserView actor);

    /**
     * Sends a bug back to the developer because the fix did not work.
     *
     * @param bugId  the bug
     * @param reason why it is reopened
     * @param actor  who reopened it
     * @return the updated bug
     */
    BugView reopenBug(long bugId, String reason, UserView actor);

    /**
     * Marks a new bug as invalid or a duplicate (Admin / PM).
     *
     * @param bugId  the bug
     * @param reason why it is rejected
     * @param actor  who rejected it
     * @return the updated bug
     */
    BugView rejectBug(long bugId, String reason, UserView actor);

    /**
     * Assigns several bugs to one developer in one batch (Admin / PM).
     *
     * @param bugIds     the bugs
     * @param assigneeId the developer
     * @param actor      who is assigning
     * @return how many bugs were assigned
     */
    int bulkAssign(List<Long> bugIds, long assigneeId, UserView actor);

    /**
     * Closes several bugs that are waiting in QA verification, in one batch (Admin / PM).
     *
     * @param bugIds the bugs
     * @param actor  who is closing them
     * @return how many bugs were closed
     */
    int bulkClose(List<Long> bugIds, UserView actor);

    // ----- Comments, history, notifications -----

    /**
     * @param bugId the bug
     * @return its comments, oldest first
     */
    List<CommentView> listComments(long bugId);

    /**
     * Posts a comment or a reply. Mentioned users ({@code @name}) are notified.
     *
     * @param bugId    the bug
     * @param parentId the comment being replied to, or {@code null}
     * @param body     the text
     * @param author   who is writing it
     * @return the created comment
     */
    CommentView addComment(long bugId, Long parentId, String body, UserView author);

    /**
     * @param bugId the bug
     * @return its activity history, newest first
     */
    List<ActivityView> listActivity(long bugId);

    /**
     * @param userId the user
     * @return the user's notifications, newest first
     */
    List<NotificationView> listNotifications(long userId);

    /**
     * @param userId the user
     * @return how many of the user's notifications are unread
     */
    long countUnread(long userId);

    /**
     * @param notificationId the notification to mark as read
     */
    void markNotificationRead(long notificationId);

    /**
     * @param userId the user whose notifications should all be marked as read
     */
    void markAllRead(long userId);

    // ----- Reports and snapshots -----

    /**
     * Builds a report. Admin / PM sees everything, a developer their own work,
     * and a QA tester the bugs they reported.
     *
     * @param type   which report
     * @param viewer who is asking
     * @return the report
     */
    ReportView buildReport(ReportType type, UserView viewer);

    /**
     * Writes a report to a file (Strategy pattern on the backend).
     *
     * @param report the report
     * @param format CSV or PDF
     * @param target the file to write
     * @throws IOException if the file cannot be written
     */
    void exportReport(ReportView report, ExportFormat format, File target) throws IOException;

    /**
     * Saves a report as a serialized snapshot ({@code .ser}).
     *
     * @param report the report
     * @param target the file to write
     * @throws IOException if the file cannot be written
     */
    void saveSnapshot(ReportView report, File target) throws IOException;

    /**
     * Loads a snapshot saved with {@link #saveSnapshot}. Only report classes are accepted.
     *
     * @param source the {@code .ser} file
     * @return the report exactly as it was saved
     * @throws IOException if the file cannot be read or is not a BugTrack snapshot
     */
    ReportView loadSnapshot(File source) throws IOException;

    // ----- Settings -----

    /**
     * @param userId the user
     * @return the user's saved settings, or defaults
     */
    SettingsView loadSettings(long userId);

    /**
     * @param userId   the user
     * @param settings the settings to save
     */
    void saveSettings(long userId, SettingsView settings);
}
