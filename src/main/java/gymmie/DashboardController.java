package gymmie;

import java.io.IOException;

import gymmie.model.Role;
import gymmie.trainer.service.TrainerProfileService.TrainerView;
import gymmie.ui.StatusLabel;
import gymmie.ui.UiFeedback;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
    private Label profileDetails;
    @FXML
    private TextField ownDisplayName;
    @FXML
    private VBox displayNameSection;
    @FXML
    private StatusLabel displayNameStatus;
    @FXML
    private VBox actions;
    @FXML
    private VBox trainerDetails;
    @FXML
    private Label trainerSynopsis;
    @FXML
    private Label trainerSpecializations;
    @FXML
    private StatusLabel trainerDetailsStatus;
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
        displayNameSection.setVisible(!trainer);
        displayNameSection.setManaged(!trainer);
        profileButton.setVisible(trainer);
        profileButton.setManaged(trainer);
        refreshIdentity();
        displayNameStatus.managedProperty().bind(displayNameStatus.textProperty().isNotEmpty());
        displayNameStatus.visibleProperty().bind(displayNameStatus.managedProperty());
        trainerDetails.setVisible(trainer);
        trainerDetails.setManaged(trainer);
        trainerDetailsStatus.managedProperty().bind(trainerDetailsStatus.textProperty().isNotEmpty());
        trainerDetailsStatus.visibleProperty().bind(trainerDetailsStatus.managedProperty());
        if (trainer) {
            loadTrainerDetails();
        }
        title.setText(dashboardTitle);
    }

    private void refreshIdentity() {
        var user = context.getUserSession().requireUser();
        profileDetails.setText(user.displayName() + "\n@" + user.username() + "\n" + user.role());
        welcome.setText("Welcome, " + user.displayName());
        ownDisplayName.setText(user.displayName());
    }

    @FXML
    private void changeDisplayName() {
        if (actions.isDisabled()) {
            return;
        }
        String replacement = ownDisplayName.getText();
        actions.setDisable(true);
        displayNameStatus.info("Updating display name…");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                context.getProfileService().changeOwnDisplayName(replacement);
                return null;
            }
        };
        task.setOnSucceeded(_ -> {
            actions.setDisable(false);
            refreshIdentity();
            displayNameStatus.success("Display name changed.");
            ownDisplayName.requestFocus();
        });
        task.setOnFailed(_ -> {
            actions.setDisable(false);
            if (!context.getUserSession().isAuthenticated()) {
                logout();
                return;
            }
            displayNameStatus.error(task.getException(), "Unable to change your display name. Please try again.");
            ownDisplayName.requestFocus();
        });
        Thread.ofPlatform().daemon().name("gymmie-display-name-change").start(task);
    }

    private void loadTrainerDetails() {
        trainerDetailsStatus.info("Loading trainer details…");
        Task<TrainerView> task = new Task<>() {
            @Override
            protected TrainerView call() throws Exception {
                return context.getTrainerProfileService().getOwnTrainerProfile();
            }
        };
        task.setOnSucceeded(_ -> {
            TrainerView profile = task.getValue();
            trainerSynopsis.setText(profile.synopsis().isBlank() ? "No synopsis added." : profile.synopsis());
            trainerSpecializations.setText(profile.specializations().isEmpty()
                    ? "No specializations added." : String.join("\n", profile.specializations()));
            trainerDetailsStatus.info("");
        });
        task.setOnFailed(_ -> trainerDetailsStatus.error(task.getException(),
                "Unable to load trainer details. Reopen Home to try again."));
        Thread.ofPlatform().daemon().name("gymmie-trainer-summary").start(task);
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
