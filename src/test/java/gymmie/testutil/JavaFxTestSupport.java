package gymmie.testutil;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/** Shared JavaFX toolkit and event synchronization for opt-in GUI integration tests. */
public final class JavaFxTestSupport {
    private static boolean started;

    private JavaFxTestSupport() {
    }

    /**
     * Starts the toolkit once across shared and role-specific test classes.
     *
     * @throws Exception if toolkit initialization fails or times out.
     */
    public static synchronized void startToolkit() throws Exception {
        if (!started) {
            Platform.startup(() -> Platform.setImplicitExit(false));
            started = true;
        }
        onFxThread(() -> null);
    }

    /**
     * Waits for an observable UI condition without sleeping on the JavaFX thread.
     *
     * @throws Exception if the UI operation fails or the condition times out.
     */
    public static <T> void awaitUi(ObservableValue<T> observable, Predicate<T> condition) throws Exception {
        CompletableFuture<Void> completed = new CompletableFuture<>();
        ChangeListener<T> listener = (_, _, value) -> {
            if (condition.test(value)) {
                completed.complete(null);
            }
        };
        try {
            onFxThread(() -> {
                observable.addListener(listener);
                if (condition.test(observable.getValue())) {
                    completed.complete(null);
                }
                return null;
            });
            completed.get(15, TimeUnit.SECONDS);
        } finally {
            onFxThread(() -> {
                observable.removeListener(listener);
                return null;
            });
        }
    }

    /**
     * Runs an action on the JavaFX application thread and returns its result.
     *
     * @throws Exception if the action fails or times out.
     */
    public static <T> T onFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}
