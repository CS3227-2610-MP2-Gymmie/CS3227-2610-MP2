package gymmie;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

/** Adds a read-only password preview while its button is held with the mouse or Space key. */
public final class PasswordReveal {
    private PasswordReveal() {
    }

    /**
     * Connects a password field and its adjacent reveal button in an HBox.
     *
     * @param password original editable password field.
     * @param button press-and-hold reveal control.
     */
    public static void install(PasswordField password, Button button) {
        HBox row = (HBox) password.getParent();
        int index = row.getChildren().indexOf(password);
        row.getChildren().remove(password);
        TextField preview = new TextField();
        preview.setEditable(false);
        preview.setFocusTraversable(false);
        preview.setMouseTransparent(true);
        preview.visibleProperty().bind(button.armedProperty().and(button.disabledProperty().not()));
        preview.textProperty().bind(Bindings.when(preview.visibleProperty())
                .then(password.textProperty()).otherwise(""));
        StackPane fields = new StackPane(password, preview);
        fields.setMinWidth(0);
        HBox.setHgrow(fields, Priority.ALWAYS);
        row.getChildren().add(index, fields);
        button.setMinWidth(Button.USE_PREF_SIZE);
        button.setAccessibleText("Hold to show password");
        button.setAccessibleHelp("Hold the mouse button or Space key to reveal. Release to hide.");
        button.setTooltip(new Tooltip("Hold to show password (mouse or Space)"));
        button.focusedProperty().addListener((_, _, focused) -> {
            if (!focused) {
                button.disarm();
            }
        });
    }
}
