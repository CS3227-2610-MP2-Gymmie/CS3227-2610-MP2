package gymmie;

import java.io.IOException;

import gymmie.model.exception.ValidationException;
import gymmie.service.exception.AuthenticationException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

/** Displays the authenticated role and exposes the delivered account operations. */
public final class DashboardController {
    private final AppContext context;
    private final Router router;
    private final String dashboardTitle;
    @FXML
    private Label title;
    @FXML
    private Label welcome;
    @FXML
    private VBox actions;
    @FXML
    private PasswordField currentPassword;
    @FXML
    private Button currentPasswordReveal;
    @FXML
    private PasswordField newPassword;
    @FXML
    private Button newPasswordReveal;
    @FXML
    private PasswordField confirmPassword;
    @FXML
    private Button confirmPasswordReveal;
    @FXML
    private Label status;
    @FXML
    private Button logoutButton;

    /** Creates a dashboard using the current session and shared account services. */
    public DashboardController(AppContext context, Router router, String dashboardTitle) {
        this.context = context;
        this.router = router;
        this.dashboardTitle = dashboardTitle;
    }

    @FXML
    private void initialize() {
        PasswordReveal.install(currentPassword, currentPasswordReveal);
        PasswordReveal.install(newPassword, newPasswordReveal);
        PasswordReveal.install(confirmPassword, confirmPasswordReveal);

        title.setText(dashboardTitle);
        welcome.setText("Welcome, " + context.getUserSession().requireUser().displayName());
        Platform.runLater(logoutButton::requestFocus);
    }

    @FXML
    private void logout() {
        try {
            router.showLogin();
        } catch (IOException exception) {
            context.getAuthService().logout();
            actions.setDisable(true);
            status.setText("You are logged out. Please restart Gymmie to log in again.");
        }
    }

    @FXML
    private void changePassword() {
        if (actions.isDisabled()) {
            return;
        }
        if (currentPassword.getText().isEmpty() || newPassword.getText().isEmpty()) {
            status.setText("Enter your current and new passwords.");
            return;
        }
        if (!newPassword.getText().equals(confirmPassword.getText())) {
            status.setText("The new passwords do not match.");
            confirmPassword.requestFocus();
            return;
        }
        String current = currentPassword.getText();
        String replacement = newPassword.getText();
        currentPassword.clear();
        newPassword.clear();
        confirmPassword.clear();
        actions.setDisable(true);
        status.setText("Updating password…");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                context.getAuthService().changeOwnPassword(current, replacement);
                return null;
            }
        };
        task.setOnSucceeded(_ -> {
            actions.setDisable(false);
            status.setText("Password changed.");
            currentPassword.requestFocus();
        });
        task.setOnFailed(_ -> {
            actions.setDisable(false);
            if (!context.getUserSession().isAuthenticated()) {
                logout();
                return;
            }
            Throwable failure = task.getException();
            if (failure instanceof ValidationException) {
                status.setText("Use a new password of 8–128 characters.");
            } else if (failure instanceof AuthenticationException) {
                status.setText("Your current password is incorrect.");
            } else {
                status.setText("Unable to change your password. Please try again.");
            }
            currentPassword.requestFocus();
        });
        Thread.ofPlatform().daemon().name("gymmie-password-change").start(task);
    }
}
