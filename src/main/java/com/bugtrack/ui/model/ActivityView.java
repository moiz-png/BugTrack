package com.bugtrack.ui.model;

import java.time.LocalDateTime;

/**
 * One entry in a bug's activity history.
 *
 * @param userName  who made the change
 * @param action    what they did, e.g. "Changed status"
 * @param oldValue  the value before the change, or {@code null}
 * @param newValue  the value after the change, or {@code null}
 * @param timestamp when it happened
 */
public record ActivityView(String userName, String action, String oldValue, String newValue,
                           LocalDateTime timestamp) {
}
