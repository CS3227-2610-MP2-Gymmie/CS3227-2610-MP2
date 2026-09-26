package gymmie;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import gymmie.model.Membership;
import gymmie.model.MembershipPlan;
import gymmie.model.Role;
import gymmie.service.MembershipStatusService.CurrentMembership;
import gymmie.ui.DisplayFormatters;
import gymmie.ui.StatusLabel;
import gymmie.ui.UiFeedback;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
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
    private ComboBox<MembershipPlan> availablePlans;
    @FXML
    private Button purchaseMembership;
    @FXML
    private StatusLabel purchaseStatus;
    @FXML
    private Button refreshPlans;
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
    private boolean hasActiveMembership;
    private boolean membershipStatusLoaded;

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
            configurePlanChoices();
            refreshMembership();
            loadAvailablePlans();
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
                hasActiveMembership = true;
                membershipPlan.setText("Plan: " + current.planName());
                membershipExpiry.setText("Expiry date: " + DisplayFormatters.date(current.expiryDate()));
                membershipStatus.info("Active");
            } else {
                hasActiveMembership = false;
                membershipStatus.info("Inactive — no current membership.");
            }
            membershipStatusLoaded = true;
            if (hasActiveMembership) {
                purchaseStatus.info("You already have an active membership.");
            } else if (!availablePlans.getItems().isEmpty()) {
                purchaseStatus.info("Choose a plan to purchase.");
            }
            updatePurchaseAvailability();
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

    private void configurePlanChoices() {
        availablePlans.setButtonCell(new MembershipPlanCell());
        availablePlans.setCellFactory(_ -> new MembershipPlanCell());
        availablePlans.setOnAction(_ -> updatePurchaseAvailability());
    }

    private void loadAvailablePlans() {
        refreshPlans.setDisable(true);
        availablePlans.setDisable(true);
        availablePlans.getItems().clear();
        purchaseMembership.setDisable(true);
        purchaseStatus.info("Loading available plans…");
        Task<List<MembershipPlan>> task = new Task<>() {
            @Override
            protected List<MembershipPlan> call() throws Exception {
                return context.getMembershipPurchaseService().availablePlans();
            }
        };
        task.setOnSucceeded(_ -> {
            availablePlans.getItems().setAll(task.getValue());
            refreshPlans.setDisable(false);
            availablePlans.setDisable(false);
            if (!task.getValue().isEmpty()) {
                availablePlans.getSelectionModel().selectFirst();
                purchaseStatus.info(membershipStatusLoaded && hasActiveMembership
                        ? "You already have an active membership." : "Choose a plan to purchase.");
            } else {
                purchaseStatus.info(hasActiveMembership ? "You already have an active membership."
                        : "No plans are currently available.");
            }
            updatePurchaseAvailability();
        });
        task.setOnFailed(_ -> {
            refreshPlans.setDisable(false);
            availablePlans.setDisable(false);
            purchaseStatus.error(task.getException(), "Unable to load available plans. Please restart the screen.");
            if (!context.getUserSession().isAuthenticated()) {
                logout();
            }
        });
        Thread.ofPlatform().daemon().name("gymmie-membership-plans").start(task);
    }

    @FXML
    private void refreshAvailablePlans() {
        if (!refreshPlans.isDisabled()) {
            loadAvailablePlans();
        }
    }

    @FXML
    private void purchaseMembership() {
        MembershipPlan selectedPlan = availablePlans.getValue();
        if (purchaseMembership.isDisabled() || selectedPlan == null) {
            return;
        }
        purchaseMembership.setDisable(true);
        purchaseStatus.info("Purchasing membership…");
        Task<Membership> task = new Task<>() {
            @Override
            protected Membership call() throws Exception {
                return context.getMembershipPurchaseService().purchase(selectedPlan.id());
            }
        };
        task.setOnSucceeded(_ -> {
            Membership purchased = task.getValue();
            hasActiveMembership = true;
            membershipStatusLoaded = true;
            purchaseMembership.setDisable(true);
            purchaseStatus.success("Membership purchased.");
            membershipPlan.setText("Plan: " + selectedPlan.name());
            membershipExpiry.setText("Expiry date: " + DisplayFormatters.date(purchased.expiryDate()));
            membershipStatus.info("Active");
        });
        task.setOnFailed(_ -> {
            purchaseStatus.error(task.getException(), "Unable to purchase this plan. Please try again.");
            updatePurchaseAvailability();
            if (!context.getUserSession().isAuthenticated()) {
                logout();
            }
        });
        Thread.ofPlatform().daemon().name("gymmie-membership-purchase").start(task);
    }

    private void updatePurchaseAvailability() {
        if (purchaseMembership == null) {
            return;
        }
        purchaseMembership.setDisable(!membershipStatusLoaded || hasActiveMembership
                || availablePlans.getValue() == null);
    }

    private static final class MembershipPlanCell extends ListCell<MembershipPlan> {
        @Override
        protected void updateItem(MembershipPlan plan, boolean empty) {
            super.updateItem(plan, empty);
            setText(empty || plan == null ? null : plan.name() + " · " + plan.durationDays() + " days · "
                    + DisplayFormatters.price(plan.priceCents()));
        }
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
