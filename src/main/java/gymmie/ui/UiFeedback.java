package gymmie.ui;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import gymmie.model.exception.DomainException;
import gymmie.service.exception.AccountDeactivatedException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Consistent user-facing error messages and owned, themed dialogs for every role. */
public final class UiFeedback {
    private UiFeedback() {
    }

    /**
     * Preserves expected service messages and hides unexpected infrastructure details.
     *
     * @param failure service failure, possibly wrapped by asynchronous execution.
     * @param fallback safe explanation of an unexpected failure.
     * @return message suitable for inline feedback or an alert.
     */
    public static String errorMessage(Throwable failure, String fallback) {
        Throwable cause = failure;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof AccountDeactivatedException) {
            return "This account is deactivated. Please contact a Manager.";
        }
        if (cause instanceof DomainException && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            String message = cause.getMessage();
            return message.endsWith(".") || message.endsWith("!") || message.endsWith("?") ? message : message + ".";
        }
        return fallback;
    }

    /** Creates a themed, owned error alert; call showAndWait on the JavaFX thread to display it. */
    public static Alert errorAlert(Window owner, String title, Throwable failure, String fallback) {
        return alert(owner, Alert.AlertType.ERROR, title, errorMessage(failure, fallback));
    }

    /** Creates a themed information alert for completion messages on the JavaFX thread. */
    public static Alert informationAlert(Window owner, String title, String message) {
        return alert(owner, Alert.AlertType.INFORMATION, title, message);
    }

    /** Returns true only when the user explicitly confirms; closing or Escape cancels. */
    public static boolean confirm(Window owner, String title, String message) {
        Alert alert = alert(owner, Alert.AlertType.CONFIRMATION, title, message);
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private static Alert alert(Window owner, Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.setResizable(true);
        SharedStyles.apply(alert.getDialogPane());
        return alert;
    }
}
