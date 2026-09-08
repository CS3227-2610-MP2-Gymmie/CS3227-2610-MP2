package gymmie;

import javafx.application.Application;

/**
 * Entry point that launches the JavaFX application.
 */
public final class Launcher {
    private Launcher() {
    }

    /**
     * Starts the Gymmie JavaFX application.
     *
     * @param args command-line arguments passed to JavaFX.
     */
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
