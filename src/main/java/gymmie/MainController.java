package gymmie;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * JavaFX controller for the Gymmie welcome window.
 */
public class MainController {
    @FXML
    private Label welcomeLabel;

    @FXML
    private void initialize() {
        welcomeLabel.setText("Welcome to " + App.getAppTitle());
    }
}
