package com.bugtrack.ui.util;

import com.bugtrack.ui.model.BugStatus;
import com.bugtrack.ui.model.BugView;
import com.bugtrack.ui.model.Role;
import com.bugtrack.ui.model.UserView;

import java.util.EnumSet;

/**
 * Decides which screens and buttons each role sees (permission matrix, proposal section 8.1).
 * <p>This only hides what a user cannot do. The service layer checks the same rules again,
 * so nothing can be bypassed from the UI.</p>
 */
public final class Permissions {

    private Permissions() {
    }

    /**
     * @param user the user
     * @return {@code true} for the Admin / PM
     */
    public static boolean isAdmin(UserView user) {
        return user.getRole() == Role.ADMIN;
    }

    /**
     * @param user the user
     * @return whether the user can manage users and roles
     */
    public static boolean canManageUsers(UserView user) {
        return isAdmin(user);
    }

    /**
     * @param user the user
     * @return whether the user can create and manage projects
     */
    public static boolean canManageProjects(UserView user) {
        return isAdmin(user);
    }

    /**
     * @param user the user
     * @return whether the user can report new bugs (Admin / PM and QA Tester)
     */
    public static boolean canReportBug(UserView user) {
        return user.getRole() == Role.ADMIN || user.getRole() == Role.QA_TESTER;
    }

    /**
     * @param user the user
     * @return whether the user's priority choice is final (QA Testers can only suggest one)
     */
    public static boolean canSetPriority(UserView user) {
        return isAdmin(user);
    }

    /**
     * @param user the user
     * @return whether the user can assign or close several bugs at once
     */
    public static boolean canBulkEdit(UserView user) {
        return isAdmin(user);
    }

    /**
     * @param user the user
     * @return whether the user works the QA verification queue
     */
    public static boolean canUseQaQueue(UserView user) {
        return user.getRole() == Role.ADMIN || user.getRole() == Role.QA_TESTER;
    }

    /**
     * @param user the user
     * @param bug  the bug
     * @return whether the user can edit the bug's details
     */
    public static boolean canEdit(UserView user, BugView bug) {
        return bug.getStatus().isOpen() && (isAdmin(user) || bug.getReporter().getId() == user.getId());
    }

    /**
     * @param user the user
     * @param bug  the bug
     * @return whether the user can assign or reassign the bug
     */
    public static boolean canAssign(UserView user, BugView bug) {
        return isAdmin(user) && EnumSet.of(BugStatus.NEW, BugStatus.ASSIGNED, BugStatus.IN_PROGRESS,
                BugStatus.REOPENED).contains(bug.getStatus());
    }

    /**
     * @param user the user
     * @param bug  the bug
     * @return whether the user can move the bug to In Progress
     */
    public static boolean canStartWork(UserView user, BugView bug) {
        return (isAdmin(user) || isAssignee(user, bug))
                && (bug.getStatus() == BugStatus.ASSIGNED || bug.getStatus() == BugStatus.REOPENED);
    }

    /**
     * @param user the user
     * @param bug  the bug
     * @return whether the user can submit a fix for QA
     */
    public static boolean canResolve(UserView user, BugView bug) {
        return (isAdmin(user) || isAssignee(user, bug)) && bug.getStatus() == BugStatus.IN_PROGRESS;
    }

    /**
     * @param user the user
     * @param bug  the bug
     * @return whether the user can verify the fix and close the bug
     */
    public static boolean canVerify(UserView user, BugView bug) {
        return canUseQaQueue(user) && bug.getStatus() == BugStatus.QA_VERIFICATION;
    }

    /**
     * @param user the user
     * @param bug  the bug
     * @return whether the user can reopen the bug
     */
    public static boolean canReopen(UserView user, BugView bug) {
        return canUseQaQueue(user) && bug.getStatus() == BugStatus.QA_VERIFICATION;
    }

    /**
     * @param user the user
     * @param bug  the bug
     * @return whether the user can reject the bug as invalid or a duplicate
     */
    public static boolean canReject(UserView user, BugView bug) {
        return isAdmin(user) && bug.getStatus() == BugStatus.NEW;
    }

    private static boolean isAssignee(UserView user, BugView bug) {
        return bug.getAssignee() != null && bug.getAssignee().getId() == user.getId();
    }
}
