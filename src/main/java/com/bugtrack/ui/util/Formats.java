package com.bugtrack.ui.util;

import com.bugtrack.ui.model.UserView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Formats dates, names and empty values the same way on every screen.
 */
public final class Formats {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final String EMPTY = "—";

    private Formats() {
    }

    /**
     * @param time the time, may be {@code null}
     * @return e.g. "26 Nov 2026, 14:05", or a dash
     */
    public static String dateTime(LocalDateTime time) {
        return time == null ? EMPTY : DATE_TIME.format(time);
    }

    /**
     * @param date the date, may be {@code null}
     * @return e.g. "26 Nov 2026", or a dash
     */
    public static String date(LocalDate date) {
        return date == null ? EMPTY : DATE.format(date);
    }

    /**
     * @param text the text, may be {@code null} or blank
     * @return the text, or a dash if there is none
     */
    public static String orDash(String text) {
        return text == null || text.isBlank() ? EMPTY : text;
    }

    /**
     * @param user the user, may be {@code null}
     * @return the user's name, or "Unassigned"
     */
    public static String user(UserView user) {
        return user == null ? "Unassigned" : user.getName();
    }
}
