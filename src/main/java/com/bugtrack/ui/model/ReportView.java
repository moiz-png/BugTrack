package com.bugtrack.ui.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * A generated report: a list of labelled numbers plus when and for whom it was made.
 * <p>Serializable so it can be saved as a {@code .ser} snapshot and reopened later
 * exactly as it was, even after the live data has changed.</p>
 *
 * @param type         which report this is
 * @param title        heading shown above the chart
 * @param values       label to number, in display order
 * @param generatedAt  when the report was generated
 * @param generatedFor who the report was generated for, e.g. "Ayesha Khan (Admin / PM)"
 */
public record ReportView(ReportType type, String title, Map<String, Long> values,
                         LocalDateTime generatedAt, String generatedFor) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
