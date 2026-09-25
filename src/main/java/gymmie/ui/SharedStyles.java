package gymmie.ui;

import java.util.Objects;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

/** Applies the same bundled stylesheet to application scenes and native JavaFX dialogs. */
public final class SharedStyles {
    private static final String RESOURCE = "/gymmie/css/gymmie.css";

    private SharedStyles() {
    }

    /** Applies the shared theme once to a scene. */
    public static void apply(Scene scene) {
        apply(scene.getStylesheets());
    }

    /** Applies the shared theme once to a dialog. */
    public static void apply(DialogPane pane) {
        apply(pane.getStylesheets());
    }

    private static void apply(ObservableList<String> stylesheets) {
        String url = Objects.requireNonNull(SharedStyles.class.getResource(RESOURCE),
                "Missing shared stylesheet: " + RESOURCE).toExternalForm();
        if (!stylesheets.contains(url)) {
            stylesheets.add(url);
        }
    }
}
