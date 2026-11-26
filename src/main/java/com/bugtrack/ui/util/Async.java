package com.bugtrack.ui.util;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Runs backend calls on a background thread so the UI never freezes (proposal section 11).
 * <p>The work runs on a worker thread; the success or error callback runs back on the
 * JavaFX application thread, where it is safe to update the screen.</p>
 */
public final class Async {

    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "bugtrack-worker");
        thread.setDaemon(true);
        return thread;
    });

    private Async() {
    }

    /**
     * Runs work in the background and shows an error dialog if it fails.
     *
     * @param work      the backend call
     * @param onSuccess what to do with the result, on the UI thread
     * @param <T>       the result type
     */
    public static <T> void run(Callable<T> work, Consumer<T> onSuccess) {
        run(work, onSuccess, Dialogs::error);
    }

    /**
     * Runs work in the background.
     *
     * @param work      the backend call
     * @param onSuccess what to do with the result, on the UI thread
     * @param onError   what to do if the call fails, on the UI thread
     * @param <T>       the result type
     */
    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onError.accept(task.getException()));
        WORKERS.submit(task);
    }
}
