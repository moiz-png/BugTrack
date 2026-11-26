package com.bugtrack.ui.model;

import java.time.LocalDateTime;

/**
 * An in-app notification for one user about one bug.
 *
 * @param id        notification id
 * @param userId    who receives it
 * @param bugId     the bug it is about
 * @param message   the text shown to the user
 * @param read      {@code true} once the user has opened it
 * @param createdAt when it was created
 */
public record NotificationView(long id, long userId, long bugId, String message, boolean read,
                               LocalDateTime createdAt) {
}
