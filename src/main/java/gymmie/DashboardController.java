package gymmie;

import java.io.IOException;
import java.util.Optional;

import gymmie.model.Role;
import gymmie.service.MembershipStatusService.CurrentMembership;
import gymmie.ui.DisplayFormatters;
import gymmie.ui.StatusLabel;
import gymmie.ui.UiFeedback;
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
    private VBox membershipCard;
    @FXML
    private Label membershipPlan;
    @FXML
    private Label membershipExpiry;
    @FXML
    private StatusLabel membershipStatus;
    @FXML
    private Button refreshMembership;
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
    private StatusLabel status;
    @FXML
    private Button logoutButton;
    @FXML
    private Button profileButton;

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

        Role role = context.getUserSession().requireUser().role();
        boolean trainer = role == Role.TRAINER;
        boolean member = role == Role.MEMBER;
        profileButton.setVisible(trainer);
        profileButton.setManaged(trainer);
        membershipCard.setVisible(member);
        membershipCard.setManaged(member);
        title.setText(dashboardTitle);
        welcome.setText("Welcome, " + context.getUserSession().requireUser().displayName());
        if (member) {
            refreshMembership();
        }
        Platform.runLater(logoutButton::requestFocus);
    }

    @FXML
    private void refreshMembership() {
        if (refreshMembership.isDisabled()) {
            return;
        }
        refreshMembership.setDisable(true);
        membershipPlan.setText("Plan: —");
        membershipExpiry.setText("Expiry date: —");
        membershipStatus.info("Loading membership…");
        Task<Optional<CurrentMembership>> task = new Task<>() {
            @Override
            protected Optional<CurrentMembership> call() throws Exception {
                return context.getMembershipStatusService().currentMembership();
            }
        };
        task.setOnSucceeded(_ -> {
            refreshMembership.setDisable(false);
            if (task.getValue().isPresent()) {
                CurrentMembership current = task.getValue().orElseThrow();
                membershipPlan.setText("Plan: " + current.planName());
                membershipExpiry.setText("Expiry date: " + DisplayFormatters.date(current.expiryDate()));
                membershipStatus.info("Active");
            } else {
                membershipStatus.info("Inactive — no current membership.");
            }
        });
        task.setOnFailed(_ -> {
            refreshMembership.setDisable(false);
            membershipStatus.error(task.getException(), "Unable to load membership. Please try Refresh.");
            if (!context.getUserSession().isAuthenticated()) {
                logout();
            }
        });
        Thread.ofPlatform().daemon().name("gymmie-membership-status").start(task);
    }

    @FXML
    private void editProfile() {
        try {
            router.showTrainerProfile();
        } catch (IOException exception) {
            status.error("Unable to open your profile. Please try again.");
        }
    }

    @FXML
    private void logout() {
        try {
            router.showLogin();
        } catch (IOException exception) {
            context.getAuthService().logout();
            actions.setDisable(true);
            status.error("You are logged out. Please restart Gymmie to log in again.");
        }
    }

    @FXML
    private void changePassword() {
        if (actions.isDisabled()) {
            return;
        }
        if (currentPassword.getText().isEmpty() || newPassword.getText().isEmpty()) {
            status.error("Enter your current and new passwords.");
            return;
        }
        if (!newPassword.getText().equals(confirmPassword.getText())) {
            status.error("The new passwords do not match.");
            confirmPassword.requestFocus();
            return;
        }
        String current = currentPassword.getText();
        String replacement = newPassword.getText();
        currentPassword.clear();
        newPassword.clear();
        confirmPassword.clear();
        actions.setDisable(true);
        status.info("Updating password…");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                context.getAuthService().changeOwnPassword(current, replacement);
                return null;
            }
        };
        task.setOnSucceeded(_ -> {
            actions.setDisable(false);
            status.success("Password changed.");
            currentPassword.requestFocus();
        });
        task.setOnFailed(_ -> {
            actions.setDisable(false);
            if (!context.getUserSession().isAuthenticated()) {
                UiFeedback.errorAlert(status.getScene().getWindow(), "Session ended", task.getException(),
                        "Please log in again.").showAndWait();
                logout();
                return;
            }
            status.error(task.getException(), "Unable to change your password. Please try again.");
            currentPassword.requestFocus();
        });
        Thread.ofPlatform().daemon().name("gymmie-password-change").start(task);
    }
}
