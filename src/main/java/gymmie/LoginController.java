package gymmie;

import java.io.IOException;

import gymmie.ui.StatusLabel;
import gymmie.ui.UiFeedback;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Authenticates off the UI thread and presents distinct, credential-safe failures. */
public final class LoginController {
    private final AppContext context;
    private final Router router;
    @FXML
    private VBox form;
    @FXML
    private TextField username;
    @FXML
    private PasswordField password;
    @FXML
    private Button passwordReveal;
    @FXML
    private StatusLabel status;

    /** Creates the login controller with shared authentication and navigation. */
    public LoginController(AppContext context, Router router) {
        this.context = context;
        this.router = router;
    }

    @FXML
    private void initialize() {
        PasswordReveal.install(password, passwordReveal);

        Platform.runLater(username::requestFocus);
    }

    @FXML
    private void login() {
        if (form.isDisabled()) {
            return;
        }
        String loginName = username.getText();
        String candidate = password.getText();
        if (loginName.isEmpty() || candidate.isEmpty()) {
            status.error("Enter your username and password.");
            return;
        }
        password.clear();
        form.setDisable(true);
        status.info("Logging in…");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                context.getAuthService().login(loginName, candidate);
                return null;
            }
        };
        task.setOnSucceeded(_ -> {
            form.setDisable(false);
            try {
                router.showDashboard();
            } catch (IOException exception) {
                context.getAuthService().logout();
                status.error("Unable to open your dashboard. Please try again.");
            }
        });
        task.setOnFailed(_ -> {
            form.setDisable(false);
            status.error(failureMessage(task.getException()));
            password.requestFocus();
        });
        Thread.ofPlatform().daemon().name("gymmie-login").start(task);
    }

    static String failureMessage(Throwable failure) {
        return UiFeedback.errorMessage(failure,
                "Unable to log in. Please try again. If this continues, contact a Manager.");
    }
}
