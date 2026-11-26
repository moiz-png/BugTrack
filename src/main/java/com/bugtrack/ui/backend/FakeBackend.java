package com.bugtrack.ui.backend;

import com.bugtrack.ui.model.ActivityView;
import com.bugtrack.ui.model.BugDraft;
import com.bugtrack.ui.model.BugFilter;
import com.bugtrack.ui.model.BugStatus;
import com.bugtrack.ui.model.BugView;
import com.bugtrack.ui.model.CommentView;
import com.bugtrack.ui.model.ExportFormat;
import com.bugtrack.ui.model.NotificationView;
import com.bugtrack.ui.model.Priority;
import com.bugtrack.ui.model.ProjectView;
import com.bugtrack.ui.model.ReportType;
import com.bugtrack.ui.model.ReportView;
import com.bugtrack.ui.model.ResolutionView;
import com.bugtrack.ui.model.Role;
import com.bugtrack.ui.model.Severity;
import com.bugtrack.ui.model.SettingsView;
import com.bugtrack.ui.model.UserView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An in-memory stand-in for the real backend, filled with sample data.
 * <p>It lets every screen be built and demonstrated before the service and data layers exist.
 * It applies light versions of the workflow and permission rules so the UI behaves realistically;
 * the real rules live in the service layer. Nothing is saved: restarting the app resets the data.</p>
 * <p>Demo accounts (password {@code password} for all): {@code admin@bugtrack.dev},
 * {@code dev@bugtrack.dev}, {@code qa@bugtrack.dev}.</p>
 */
public class FakeBackend implements UiBackend {

    private static final String DEMO_PASSWORD = "password";
    private static final Set<String> STOP_WORDS =
            Set.of("when", "with", "that", "this", "from", "into", "does", "after", "page", "screen", "shows");
    private static final String SYSTEM = "System";

    private final Map<Long, UserView> users = new LinkedHashMap<>();
    private final Map<Long, String> passwords = new HashMap<>();
    private final Map<Long, ProjectView> projects = new LinkedHashMap<>();
    private final Map<Long, BugView> bugs = new LinkedHashMap<>();
    private final List<CommentView> comments = new ArrayList<>();
    private final Map<Long, List<ActivityView>> activity = new HashMap<>();
    private final List<NotificationView> notifications = new ArrayList<>();
    private final Map<Long, SettingsView> settings = new HashMap<>();
    private final AtomicLong bugIds = new AtomicLong(1);
    private final AtomicLong otherIds = new AtomicLong(100);

    /** Creates the fake backend and fills it with sample users, projects and bugs. */
    public FakeBackend() {
        seed();
    }

    // ===================== Authentication =====================

    @Override
    public synchronized Optional<UserView> login(String email, String password) {
        return users.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email.trim()))
                .filter(UserView::isActive)
                .filter(u -> password.equals(passwords.get(u.getId())))
                .findFirst();
    }

    @Override
    public void logout(UserView user) {
        // Nothing to clean up in memory.
    }

    // ===================== Users =====================

    @Override
    public synchronized List<UserView> listUsers() {
        return new ArrayList<>(users.values());
    }

    @Override
    public synchronized List<UserView> listUsersByRole(Role role) {
        return users.values().stream().filter(u -> u.getRole() == role).toList();
    }

    @Override
    public synchronized UserView createUser(String name, String email, Role role, String password) {
        require(emailIsFree(email, -1), "A user with the email " + email + " already exists.");
        UserView user = new UserView(otherIds.getAndIncrement(), name, email, role, true);
        users.put(user.getId(), user);
        passwords.put(user.getId(), password);
        return user;
    }

    @Override
    public synchronized UserView updateUser(UserView changes) {
        UserView user = user(changes.getId());
        require(emailIsFree(changes.getEmail(), user.getId()),
                "A user with the email " + changes.getEmail() + " already exists.");
        user.setName(changes.getName());
        user.setEmail(changes.getEmail());
        user.setRole(changes.getRole());
        return user;
    }

    @Override
    public synchronized void setUserActive(long userId, boolean active) {
        user(userId).setActive(active);
    }

    @Override
    public synchronized void resetPassword(long userId, String newPassword) {
        user(userId);
        passwords.put(userId, newPassword);
    }

    // ===================== Projects =====================

    @Override
    public synchronized List<ProjectView> listProjects() {
        return new ArrayList<>(projects.values());
    }

    @Override
    public synchronized ProjectView saveProject(ProjectView project) {
        require(project.getName() != null && !project.getName().isBlank(), "A project needs a name.");
        if (project.getId() == 0) {
            project.setId(otherIds.getAndIncrement());
        }
        projects.put(project.getId(), project);
        bugs.values().stream()
                .filter(b -> b.getProjectId() == project.getId())
                .forEach(b -> b.setProjectName(project.getName()));
        return project;
    }

    // ===================== Bugs =====================

    @Override
    public synchronized List<BugView> findBugs(BugFilter filter) {
        return bugs.values().stream()
                .filter(filter::matches)
                .sorted(Comparator.comparing(BugView::getUpdatedAt).reversed())
                .toList();
    }

    @Override
    public synchronized BugView getBug(long bugId) {
        return bug(bugId);
    }

    @Override
    public synchronized List<BugView> findSimilarBugs(long projectId, String title) {
        Set<String> words = keywords(title);
        if (words.isEmpty()) {
            return List.of();
        }
        return bugs.values().stream()
                .filter(b -> b.getProjectId() == projectId && b.getStatus().isOpen())
                .filter(b -> !Collections.disjoint(words, keywords(b.getTitle())))
                .limit(5)
                .toList();
    }

    @Override
    public synchronized BugView reportBug(BugDraft draft, UserView reporter) {
        requireRole(reporter, Role.ADMIN, Role.QA_TESTER);
        BugView bug = new BugView();
        bug.setId(bugIds.getAndIncrement());
        applyDraft(bug, draft);
        bug.setPriority(draft.priority() == null ? Priority.MEDIUM : draft.priority());
        bug.setStatus(BugStatus.NEW);
        bug.setReporter(reporter);
        bug.setCreatedAt(LocalDateTime.now());
        bug.setUpdatedAt(LocalDateTime.now());
        bugs.put(bug.getId(), bug);
        log(bug, reporter.getName(), "Reported the bug", null, BugStatus.NEW.label());
        users.values().stream()
                .filter(u -> u.getRole() == Role.ADMIN && u.getId() != reporter.getId())
                .forEach(admin -> notify(admin.getId(), bug.getId(),
                        "New bug #" + bug.getId() + " needs triage: " + bug.getTitle()));
        return bug;
    }

    @Override
    public synchronized BugView updateBug(long bugId, BugDraft draft, UserView actor) {
        BugView bug = bug(bugId);
        require(bug.getStatus().isOpen(), "Closed or rejected bugs cannot be edited.");
        require(actor.getRole() == Role.ADMIN || bug.getReporter().getId() == actor.getId(),
                "Only the reporter or an Admin / PM can edit this bug.");
        applyDraft(bug, draft);
        if (actor.getRole() == Role.ADMIN && draft.priority() != null) {
            bug.setPriority(draft.priority());
        }
        touch(bug);
        log(bug, actor.getName(), "Edited the bug details", null, null);
        return bug;
    }

    @Override
    public synchronized BugView addAttachment(long bugId, String path, UserView actor) {
        BugView bug = bug(bugId);
        bug.getAttachments().add(path);
        touch(bug);
        log(bug, actor.getName(), "Attached a file", null, new File(path).getName());
        return bug;
    }

    // ===================== Workflow =====================

    @Override
    public synchronized BugView assignBug(long bugId, long assigneeId, Priority priority, LocalDate dueDate,
                                          UserView actor) {
        BugView bug = bug(bugId);
        requireRole(actor, Role.ADMIN);
        require(EnumSet.of(BugStatus.NEW, BugStatus.ASSIGNED, BugStatus.IN_PROGRESS, BugStatus.REOPENED)
                        .contains(bug.getStatus()),
                "Bug #" + bugId + " cannot be assigned while it is " + bug.getStatus().label() + ".");
        UserView developer = user(assigneeId);
        require(developer.getRole() == Role.DEVELOPER && developer.isActive(),
                "Bugs can only be assigned to active developers.");
        String previous = nameOf(bug.getAssignee());
        bug.setAssignee(developer);
        bug.setPriority(priority == null ? bug.getPriority() : priority);
        bug.setDueDate(dueDate);
        changeStatus(bug, BugStatus.ASSIGNED, actor.getName());
        log(bug, actor.getName(), "Assigned the bug", previous, developer.getName());
        notify(developer.getId(), bugId, "You have been assigned bug #" + bugId + ": " + bug.getTitle());
        return bug;
    }

    @Override
    public synchronized BugView startWork(long bugId, UserView actor) {
        BugView bug = bug(bugId);
        require(actor.getRole() == Role.ADMIN || isAssignee(actor, bug),
                "Only the assigned developer can start work on this bug.");
        require(bug.getStatus() == BugStatus.ASSIGNED || bug.getStatus() == BugStatus.REOPENED,
                "Work can only start on an assigned or reopened bug.");
        changeStatus(bug, BugStatus.IN_PROGRESS, actor.getName());
        return bug;
    }

    @Override
    public synchronized BugView resolveBug(long bugId, String rootCause, String fixDescription, String commitRef,
                                           UserView actor) {
        BugView bug = bug(bugId);
        require(actor.getRole() == Role.ADMIN || isAssignee(actor, bug),
                "Only the assigned developer can submit a fix for this bug.");
        require(bug.getStatus() == BugStatus.IN_PROGRESS, "Only bugs that are In Progress can be resolved.");
        require(!isBlank(rootCause) && !isBlank(fixDescription), "Enter the root cause and the fix description.");
        bug.setResolution(new ResolutionView(rootCause.trim(), fixDescription.trim(),
                isBlank(commitRef) ? null : commitRef.trim(), actor.getName(), LocalDateTime.now()));
        changeStatus(bug, BugStatus.RESOLVED, actor.getName());
        changeStatus(bug, BugStatus.QA_VERIFICATION, SYSTEM);
        notify(bug.getReporter().getId(), bugId, "Bug #" + bugId + " has a fix waiting for your verification.");
        return bug;
    }

    @Override
    public synchronized BugView verifyFix(long bugId, UserView actor) {
        BugView bug = bug(bugId);
        requireRole(actor, Role.ADMIN, Role.QA_TESTER);
        require(bug.getStatus() == BugStatus.QA_VERIFICATION, "Only bugs waiting in QA Verification can be closed.");
        changeStatus(bug, BugStatus.CLOSED, actor.getName());
        notifyAll(bug, actor, "Bug #" + bugId + " was verified and closed by " + actor.getName() + ".");
        return bug;
    }

    @Override
    public synchronized BugView reopenBug(long bugId, String reason, UserView actor) {
        BugView bug = bug(bugId);
        requireRole(actor, Role.ADMIN, Role.QA_TESTER);
        require(bug.getStatus() == BugStatus.QA_VERIFICATION, "Only bugs waiting in QA Verification can be reopened.");
        require(!isBlank(reason), "Give a reason for reopening the bug.");
        bug.setReopenCount(bug.getReopenCount() + 1);
        changeStatus(bug, BugStatus.REOPENED, actor.getName());
        log(bug, actor.getName(), "Reopen reason", null, reason.trim());
        comments.add(new CommentView(otherIds.getAndIncrement(), bugId, null, actor.getName(),
                "Reopened: " + reason.trim(), LocalDateTime.now()));
        if (bug.getAssignee() != null) {
            notify(bug.getAssignee().getId(), bugId, "Bug #" + bugId + " was reopened: " + reason.trim());
        }
        return bug;
    }

    @Override
    public synchronized BugView rejectBug(long bugId, String reason, UserView actor) {
        BugView bug = bug(bugId);
        requireRole(actor, Role.ADMIN);
        require(bug.getStatus() == BugStatus.NEW, "Only new bugs can be rejected.");
        require(!isBlank(reason), "Give a reason for rejecting the bug.");
        changeStatus(bug, BugStatus.REJECTED, actor.getName());
        log(bug, actor.getName(), "Reject reason", null, reason.trim());
        notify(bug.getReporter().getId(), bugId, "Bug #" + bugId + " was rejected: " + reason.trim());
        return bug;
    }

    @Override
    public synchronized int bulkAssign(List<Long> bugIdsToAssign, long assigneeId, UserView actor) {
        int assigned = 0;
        for (long id : bugIdsToAssign) {
            try {
                BugView bug = bug(id);
                assignBug(id, assigneeId, bug.getPriority(), bug.getDueDate(), actor);
                assigned++;
            } catch (IllegalStateException | IllegalArgumentException skipped) {
                // The real backend runs this as one transaction; the fake just skips bugs it cannot assign.
            }
        }
        return assigned;
    }

    @Override
    public synchronized int bulkClose(List<Long> bugIdsToClose, UserView actor) {
        requireRole(actor, Role.ADMIN);
        int closed = 0;
        for (long id : bugIdsToClose) {
            if (bug(id).getStatus() == BugStatus.QA_VERIFICATION) {
                verifyFix(id, actor);
                closed++;
            }
        }
        return closed;
    }

    // ===================== Comments, history, notifications =====================

    @Override
    public synchronized List<CommentView> listComments(long bugId) {
        return comments.stream()
                .filter(c -> c.bugId() == bugId)
                .sorted(Comparator.comparing(CommentView::createdAt))
                .toList();
    }

    @Override
    public synchronized CommentView addComment(long bugId, Long parentId, String body, UserView author) {
        BugView bug = bug(bugId);
        require(!isBlank(body), "A comment cannot be empty.");
        if (parentId != null) {
            require(comments.stream().anyMatch(c -> c.id() == parentId), "The comment you replied to no longer exists.");
        }
        CommentView comment = new CommentView(otherIds.getAndIncrement(), bugId, parentId, author.getName(),
                body.trim(), LocalDateTime.now());
        comments.add(comment);
        touch(bug);
        notifyAll(bug, author, author.getName() + " commented on bug #" + bugId + ".");
        for (UserView mentioned : mentionedUsers(body)) {
            if (mentioned.getId() != author.getId()) {
                notify(mentioned.getId(), bugId, author.getName() + " mentioned you on bug #" + bugId + ".");
            }
        }
        return comment;
    }

    @Override
    public synchronized List<ActivityView> listActivity(long bugId) {
        List<ActivityView> history = new ArrayList<>(activity.getOrDefault(bugId, List.of()));
        history.sort(Comparator.comparing(ActivityView::timestamp).reversed());
        return history;
    }

    @Override
    public synchronized List<NotificationView> listNotifications(long userId) {
        return notifications.stream()
                .filter(n -> n.userId() == userId)
                .sorted(Comparator.comparing(NotificationView::createdAt).reversed())
                .toList();
    }

    @Override
    public synchronized long countUnread(long userId) {
        return notifications.stream().filter(n -> n.userId() == userId && !n.read()).count();
    }

    @Override
    public synchronized void markNotificationRead(long notificationId) {
        notifications.replaceAll(n -> n.id() == notificationId ? read(n) : n);
    }

    @Override
    public synchronized void markAllRead(long userId) {
        notifications.replaceAll(n -> n.userId() == userId ? read(n) : n);
    }

    // ===================== Reports and snapshots =====================

    @Override
    public synchronized ReportView buildReport(ReportType type, UserView viewer) {
        List<BugView> scope = bugs.values().stream().filter(b -> switch (viewer.getRole()) {
            case ADMIN -> true;
            case DEVELOPER -> isAssignee(viewer, b);
            case QA_TESTER -> b.getReporter().getId() == viewer.getId();
        }).toList();

        Map<String, Long> values = new LinkedHashMap<>();
        switch (type) {
            case BUGS_BY_STATUS -> {
                for (BugStatus status : BugStatus.values()) {
                    values.put(status.label(), count(scope, b -> b.getStatus() == status));
                }
            }
            case OPEN_BY_PRIORITY -> {
                for (Priority priority : Priority.values()) {
                    values.put(priority.label(),
                            count(scope, b -> b.getStatus().isOpen() && b.getPriority() == priority));
                }
            }
            case DEVELOPER_WORKLOAD -> users.values().stream()
                    .filter(u -> u.getRole() == Role.DEVELOPER)
                    .forEach(dev -> values.put(dev.getName(),
                            count(scope, b -> b.getStatus().isOpen() && isAssignee(dev, b))));
            case DEFECTS_PER_MODULE -> scope.stream()
                    .collect(Collectors.groupingBy(
                            b -> b.getProjectName() + " / " + (b.getModule() == null ? "General" : b.getModule()),
                            TreeMap::new, Collectors.counting()))
                    .forEach(values::put);
            case QUALITY_SUMMARY -> {
                long resolvedEver = count(scope, b -> b.getResolution() != null);
                long reopened = count(scope, b -> b.getReopenCount() > 0);
                double avgHours = scope.stream()
                        .filter(b -> b.getResolution() != null)
                        .mapToLong(b -> Duration.between(b.getCreatedAt(), b.getResolution().resolvedAt()).toHours())
                        .average().orElse(0);
                values.put("Total bugs", (long) scope.size());
                values.put("Open bugs", count(scope, b -> b.getStatus().isOpen()));
                values.put("Closed bugs", count(scope, b -> b.getStatus() == BugStatus.CLOSED));
                values.put("Bugs reopened at least once", reopened);
                values.put("Reopen rate (%)", resolvedEver == 0 ? 0 : Math.round(reopened * 100.0 / resolvedEver));
                values.put("Average resolution time (hours)", Math.round(avgHours));
            }
        }
        return new ReportView(type, type.label(), values, LocalDateTime.now(),
                viewer.getName() + " (" + viewer.getRole().label() + ")");
    }

    @Override
    public void exportReport(ReportView report, ExportFormat format, File target) throws IOException {
        if (format == ExportFormat.PDF) {
            throw new UnsupportedOperationException(
                    "PDF export will work once the backend's PDF exporter is connected. Use CSV for now.");
        }
        StringBuilder csv = new StringBuilder("Item,Value\n");
        report.values().forEach((item, value) -> csv.append(csvCell(item)).append(',').append(value).append('\n'));
        Files.writeString(target.toPath(), csv.toString(), StandardCharsets.UTF_8);
    }

    @Override
    public void saveSnapshot(ReportView report, File target) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(target))) {
            out.writeObject(report);
        }
    }

    @Override
    public ReportView loadSnapshot(File source) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(source))) {
            in.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                    "com.bugtrack.ui.model.*;java.util.*;java.time.*;java.lang.*;!*"));
            Object object = in.readObject();
            if (!(object instanceof ReportView report)) {
                throw new IOException("This file is not a BugTrack report snapshot.");
            }
            return report;
        } catch (ClassNotFoundException | InvalidClassException e) {
            throw new IOException("This snapshot was saved by an incompatible version of BugTrack.", e);
        }
    }

    // ===================== Settings =====================

    @Override
    public synchronized SettingsView loadSettings(long userId) {
        return settings.computeIfAbsent(userId, id -> new SettingsView());
    }

    @Override
    public synchronized void saveSettings(long userId, SettingsView newSettings) {
        settings.put(userId, newSettings);
    }

    // ===================== Helpers =====================

    private BugView bug(long id) {
        BugView bug = bugs.get(id);
        if (bug == null) {
            throw new IllegalArgumentException("Bug #" + id + " does not exist.");
        }
        return bug;
    }

    private UserView user(long id) {
        UserView user = users.get(id);
        if (user == null) {
            throw new IllegalArgumentException("User " + id + " does not exist.");
        }
        return user;
    }

    private ProjectView project(long id) {
        ProjectView project = projects.get(id);
        if (project == null) {
            throw new IllegalArgumentException("Project " + id + " does not exist.");
        }
        return project;
    }

    private void applyDraft(BugView bug, BugDraft draft) {
        ProjectView project = project(draft.projectId());
        bug.setProjectId(project.getId());
        bug.setProjectName(project.getName());
        bug.setModule(draft.module());
        bug.setVersion(draft.version());
        bug.setTitle(draft.title());
        bug.setDescription(draft.description());
        bug.setSteps(draft.steps());
        bug.setExpected(draft.expected());
        bug.setActual(draft.actual());
        bug.setEnvironment(draft.environment());
        bug.setSeverity(draft.severity());
        bug.setAttachments(draft.attachments() == null ? List.of() : draft.attachments());
    }

    private void changeStatus(BugView bug, BugStatus to, String who) {
        BugStatus from = bug.getStatus();
        bug.setStatus(to);
        touch(bug);
        if (from != to) {
            log(bug, who, "Changed status", from.label(), to.label());
        }
    }

    private void touch(BugView bug) {
        bug.setUpdatedAt(LocalDateTime.now());
    }

    private void log(BugView bug, String who, String action, String oldValue, String newValue) {
        log(bug, who, action, oldValue, newValue, LocalDateTime.now());
    }

    private void log(BugView bug, String who, String action, String oldValue, String newValue, LocalDateTime at) {
        activity.computeIfAbsent(bug.getId(), id -> new ArrayList<>())
                .add(new ActivityView(who, action, oldValue, newValue, at));
    }

    private void notify(long userId, long bugId, String message) {
        notifications.add(new NotificationView(otherIds.getAndIncrement(), userId, bugId, message, false,
                LocalDateTime.now()));
    }

    /** Notifies the reporter and the assignee, except the person who caused the event. */
    private void notifyAll(BugView bug, UserView actor, String message) {
        Set<Long> recipients = new java.util.LinkedHashSet<>();
        recipients.add(bug.getReporter().getId());
        if (bug.getAssignee() != null) {
            recipients.add(bug.getAssignee().getId());
        }
        recipients.remove(actor.getId());
        recipients.forEach(id -> notify(id, bug.getId(), message));
    }

    private List<UserView> mentionedUsers(String body) {
        Set<String> handles = Arrays.stream(body.split("\\s+"))
                .filter(word -> word.startsWith("@") && word.length() > 1)
                .map(word -> word.substring(1).replaceAll("[^A-Za-z]", "").toLowerCase())
                .collect(Collectors.toSet());
        return users.values().stream()
                .filter(u -> handles.contains(u.getName().split(" ")[0].toLowerCase())
                        || handles.contains(u.getName().replace(" ", "").toLowerCase()))
                .toList();
    }

    private boolean emailIsFree(String email, long ignoreUserId) {
        return users.values().stream()
                .noneMatch(u -> u.getId() != ignoreUserId && u.getEmail().equalsIgnoreCase(email));
    }

    private static NotificationView read(NotificationView n) {
        return new NotificationView(n.id(), n.userId(), n.bugId(), n.message(), true, n.createdAt());
    }

    private static boolean isAssignee(UserView user, BugView bug) {
        return bug.getAssignee() != null && bug.getAssignee().getId() == user.getId();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void requireRole(UserView actor, Role... allowed) {
        require(Arrays.asList(allowed).contains(actor.getRole()),
                "Your role (" + actor.getRole().label() + ") is not allowed to do this.");
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private static String nameOf(UserView user) {
        return user == null ? "Unassigned" : user.getName();
    }

    private static long count(List<BugView> list, Predicate<BugView> condition) {
        return list.stream().filter(condition).count();
    }

    private static Set<String> keywords(String text) {
        return Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(word -> word.length() >= 4 && !STOP_WORDS.contains(word))
                .collect(Collectors.toSet());
    }

    private static String csvCell(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // ===================== Sample data =====================

    private void seed() {
        UserView admin = addUser(1, "Ayesha Khan", "admin@bugtrack.dev", Role.ADMIN);
        UserView ali = addUser(2, "Ali Raza", "dev@bugtrack.dev", Role.DEVELOPER);
        UserView bilal = addUser(3, "Bilal Ahmed", "bilal@bugtrack.dev", Role.DEVELOPER);
        UserView hina = addUser(4, "Hina Tariq", "qa@bugtrack.dev", Role.QA_TESTER);
        UserView usman = addUser(5, "Usman Ali", "usman@bugtrack.dev", Role.QA_TESTER);

        ProjectView desktop = addProject(10, "BugTrack Desktop", "The bug tracker itself (dogfooding).", "0.1.0",
                List.of("Login", "Bug Board", "Reports", "Notifications"), List.of(1L, 2L, 3L, 4L, 5L));
        ProjectView portal = addProject(11, "Campus Portal", "Student enrollment and fee portal.", "2.3.1",
                List.of("Enrollment", "Fees", "Timetable"), List.of(1L, 2L, 4L));

        seedBug(desktop, "Login", "Login button stays disabled after a failed attempt",
                Severity.MAJOR, Priority.HIGH, BugStatus.NEW, hina, null, 1);
        BugView refresh = seedBug(desktop, "Bug Board", "Bug table does not refresh after assigning a bug",
                Severity.MINOR, Priority.MEDIUM, BugStatus.ASSIGNED, hina, ali, 3);
        seedBug(desktop, "Reports", "CSV export uses semicolons instead of commas",
                Severity.MINOR, Priority.LOW, BugStatus.IN_PROGRESS, usman, bilal, 6);
        seedBug(desktop, "Notifications", "Unread badge shows a negative number",
                Severity.COSMETIC, Priority.LOW, BugStatus.QA_VERIFICATION, hina, ali, 8);
        seedBug(desktop, "Login", "Password field accepts more than 128 characters",
                Severity.MINOR, Priority.MEDIUM, BugStatus.CLOSED, usman, bilal, 15);
        seedBug(desktop, "Bug Board", "Pagination skips the last page of results",
                Severity.MAJOR, Priority.HIGH, BugStatus.REOPENED, hina, ali, 10);
        seedBug(portal, "Fees", "Fee voucher shows the wrong due date",
                Severity.CRITICAL, Priority.CRITICAL, BugStatus.IN_PROGRESS, hina, ali, 4);
        seedBug(portal, "Enrollment", "Enrollment fails when a course has no section",
                Severity.BLOCKER, Priority.CRITICAL, BugStatus.ASSIGNED, usman, bilal, 2);
        seedBug(portal, "Timetable", "Timetable clash warning appears twice",
                Severity.COSMETIC, Priority.LOW, BugStatus.REJECTED, usman, null, 12);
        seedBug(portal, "Fees", "Late fee is added twice after payment",
                Severity.MAJOR, Priority.HIGH, BugStatus.QA_VERIFICATION, hina, bilal, 9);
        seedBug(portal, "Enrollment", "Course search ignores lowercase input",
                Severity.MINOR, Priority.MEDIUM, BugStatus.NEW, usman, null, 0);
        seedBug(desktop, "Reports", "Average resolution time shows NaN with no data",
                Severity.MINOR, Priority.MEDIUM, BugStatus.CLOSED, hina, ali, 20);

        long first = otherIds.getAndIncrement();
        comments.add(new CommentView(first, refresh.getId(), null, hina.getName(),
                "Assign any bug and look at the table: the row keeps showing the old assignee until you reopen the screen.",
                refresh.getCreatedAt().plusHours(1)));
        comments.add(new CommentView(otherIds.getAndIncrement(), refresh.getId(), first, ali.getName(),
                "@Hina thanks, I can reproduce it. Looking into it today.", refresh.getCreatedAt().plusHours(5)));

        notify(ali.getId(), refresh.getId(), "You have been assigned bug #" + refresh.getId() + ": " + refresh.getTitle());
        notify(ali.getId(), 6, "Bug #6 was reopened: the last page is still missing.");
        notify(hina.getId(), 4, "Bug #4 has a fix waiting for your verification.");
        notify(admin.getId(), 1, "New bug #1 needs triage: Login button stays disabled after a failed attempt");
        notify(admin.getId(), 11, "New bug #11 needs triage: Course search ignores lowercase input");
    }

    private UserView addUser(long id, String name, String email, Role role) {
        UserView user = new UserView(id, name, email, role, true);
        users.put(id, user);
        passwords.put(id, DEMO_PASSWORD);
        return user;
    }

    private ProjectView addProject(long id, String name, String description, String version,
                                   List<String> modules, List<Long> members) {
        ProjectView project = new ProjectView(id, name, description, version);
        project.getModules().addAll(modules);
        project.getMemberIds().addAll(members);
        projects.put(id, project);
        return project;
    }

    private BugView seedBug(ProjectView project, String module, String title, Severity severity, Priority priority,
                            BugStatus status, UserView reporter, UserView assignee, int daysAgo) {
        BugView bug = new BugView();
        bug.setId(bugIds.getAndIncrement());
        bug.setProjectId(project.getId());
        bug.setProjectName(project.getName());
        bug.setModule(module);
        bug.setVersion(project.getVersion());
        bug.setTitle(title);
        bug.setDescription("Found while testing the " + module + " module: " + title.toLowerCase() + ".");
        bug.setSteps("1. Open the " + module + " module.\n2. Repeat the action described in the title.\n3. Observe the result.");
        bug.setExpected("The feature works as described in the requirements.");
        bug.setActual(title + ".");
        bug.setEnvironment("Windows 11, Java 17, 1920x1080");
        bug.setSeverity(severity);
        bug.setPriority(priority);
        bug.setStatus(status);
        bug.setReporter(reporter);
        bug.setAssignee(assignee);

        LocalDateTime created = LocalDateTime.now().minusDays(daysAgo).minusHours(3);
        bug.setCreatedAt(created);
        bug.setUpdatedAt(created.plusHours(daysAgo == 0 ? 1 : 20));
        if (assignee != null) {
            bug.setDueDate(LocalDate.now().plusDays(7 - daysAgo % 5));
        }
        if (assignee != null && EnumSet.of(BugStatus.QA_VERIFICATION, BugStatus.CLOSED, BugStatus.REOPENED).contains(status)) {
            bug.setResolution(new ResolutionView("Missing validation in the " + module + " code.",
                    "Added the missing check and a unit test for it.", "a1b2c3d", assignee.getName(),
                    created.plusHours(18)));
        }
        if (status == BugStatus.REOPENED) {
            bug.setReopenCount(1);
        }
        bugs.put(bug.getId(), bug);

        log(bug, reporter.getName(), "Reported the bug", null, BugStatus.NEW.label(), created);
        if (assignee != null) {
            log(bug, "Ayesha Khan", "Assigned the bug", "Unassigned", assignee.getName(), created.plusHours(2));
        }
        if (status != BugStatus.NEW) {
            log(bug, assignee != null ? assignee.getName() : "Ayesha Khan", "Changed status",
                    BugStatus.NEW.label(), status.label(), created.plusHours(19));
        }
        return bug;
    }
}
