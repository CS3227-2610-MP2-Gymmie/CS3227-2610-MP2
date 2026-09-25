package gymmie.ui;

import javafx.css.PseudoClass;
import javafx.scene.control.Label;

/** Shared wrapping inline feedback control; all mutations occur on the JavaFX thread. */
public final class StatusLabel extends Label {
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    private static final PseudoClass SUCCESS = PseudoClass.getPseudoClass("success");

    /** Creates an empty feedback area that retains its space when messages change. */
    public StatusLabel() {
        getStyleClass().add("status");
        setWrapText(true);
        setMinHeight(48);
    }

    /** Displays neutral progress or informational feedback. */
    public void info(String message) {
        display(message, false, false);
    }

    /** Displays successful completion feedback with a text prefix as well as color. */
    public void success(String message) {
        display("Success: " + message, false, true);
    }

    /** Displays a validation error with a text prefix as well as color. */
    public void error(String message) {
        display("Error: " + message, true, false);
    }

    /** Displays an expected service error or a safe fallback for an unexpected failure. */
    public void error(Throwable failure, String fallback) {
        error(UiFeedback.errorMessage(failure, fallback));
    }

    private void display(String message, boolean error, boolean success) {
        pseudoClassStateChanged(ERROR, error);
        pseudoClassStateChanged(SUCCESS, success);
        setText(message);
        setAccessibleText(message);
    }
}
