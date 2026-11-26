package com.bugtrack.ui.model;

import java.time.LocalDateTime;

/**
 * What the developer recorded when submitting a fix.
 *
 * @param rootCause      why the bug happened
 * @param fixDescription what was changed
 * @param commitRef      commit hash or build number of the fix
 * @param resolvedBy     name of the developer
 * @param resolvedAt     when the fix was submitted
 */
public record ResolutionView(String rootCause, String fixDescription, String commitRef,
                             String resolvedBy, LocalDateTime resolvedAt) {
}
