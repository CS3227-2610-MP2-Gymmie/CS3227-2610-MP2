package gymmie;

import java.io.IOException;

import gymmie.service.exception.AccountDeactivatedException;
import gymmie.service.exception.AuthenticationException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    private Label status;

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
            status.setText("Enter your username and password.");
            return;
        }
        password.clear();
        form.setDisable(true);
        status.setText("Logging in…");
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
                status.setText("Unable to open your dashboard. Please try again.");
            }
        });
        task.setOnFailed(_ -> {
            form.setDisable(false);
            status.setText(failureMessage(task.getException()));
            password.requestFocus();
        });
        Thread.ofPlatform().daemon().name("gymmie-login").start(task);
    }

    static String failureMessage(Throwable failure) {
        if (failure instanceof AccountDeactivatedException) {
            return "This account is deactivated. Please contact a Manager.";
        }
        if (failure instanceof AuthenticationException) {
            return "Invalid username or password.";
        }
        return "Unable to log in. Please try again. If this continues, contact a Manager.";
    }
}
