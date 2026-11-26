package com.bugtrack.ui.util;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

/**
 * Combo box helpers.
 */
public final class Combos {

    private Combos() {
    }

    /**
     * Makes combo boxes show their prompt text again after their value is cleared to {@code null}.
     * (Without this, a cleared JavaFX combo box shows an empty box instead of e.g. "Status".)
     *
     * @param combos the combo boxes to fix
     */
    public static void showPromptWhenEmpty(ComboBox<?>... combos) {
        for (ComboBox<?> combo : combos) {
            install(combo);
        }
    }

    private static <T> void install(ComboBox<T> combo) {
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                boolean showPrompt = empty || item == null;
                setText(showPrompt ? combo.getPromptText() : item.toString());
                if (showPrompt) {
                    if (!getStyleClass().contains("prompt")) {
                        getStyleClass().add("prompt");
                    }
                } else {
                    getStyleClass().remove("prompt");
                }
            }
        });
    }
}
