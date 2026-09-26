package gymmie.member;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import gymmie.AppContext;
import gymmie.Router;
import gymmie.member.service.MemberBookingHistoryService.MemberBooking;
import gymmie.member.service.MembershipStatusService.RenewableMembership;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Membership;
import gymmie.model.MembershipPlan;
import gymmie.ui.DisplayFormatters;
import gymmie.ui.StatusLabel;
import gymmie.ui.UiFeedback;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

/** Shows the authenticated Member's current coverage and available membership plans. */
public final class MemberMembershipController {
    private final AppContext context;
    private final Router router;
    private boolean hasActiveMembership;
    private boolean hasRenewableMembership;
    private boolean membershipStatusLoaded;
    private boolean purchaseInProgress;
    private boolean renewalInProgress;
    private boolean cancellationInProgress;
    private long renewableMembershipId;
    private String currentPlanName;
    @FXML
    private VBox membershipScreen;
    @FXML
    private Label membershipPlan;
    @FXML
    private Label membershipExpiry;
    @FXML
    private StatusLabel membershipStatus;
    @FXML
    private Button refreshMembership;
    @FXML
    private Button renewMembership;
    @FXML
    private StatusLabel renewalStatus;
    @FXML
    private Button cancelMembership;
    @FXML
    private StatusLabel cancellationStatus;
    @FXML
    private StatusLabel bookingHistoryStatus;
    @FXML
    private ListView<MemberBooking> bookingHistory;
    @FXML
    private ComboBox<MembershipPlan> availablePlans;
    @FXML
    private Button purchaseMembership;
    @FXML
    private StatusLabel purchaseStatus;
    @FXML
    private Button refreshPlans;
    @FXML
    private Button backButton;

    /** Creates the Member membership screen with shared application services and navigation. */
    public MemberMembershipController(AppContext context, Router router) {
        this.context = context;
        this.router = router;
    }

    @FXML
    private void initialize() {
        configurePlanChoices();
        refreshMembership();
        loadBookingHistory();
        loadAvailablePlans();
    }

    private void loadBookingHistory() {
        bookingHistoryStatus.info("Loading booking history…");
        Task<List<MemberBooking>> task = new Task<>() {
            @Override
            protected List<MemberBooking> call() throws Exception {
                return context.getMemberBookingHistoryService().bookingHistory();
            }
        };
        task.setOnSucceeded(_ -> {
            bookingHistory.getItems().setAll(task.getValue());
            int bookingCount = task.getValue().size();
            bookingHistoryStatus.info(bookingCount == 0 ? "No bookings yet."
                    : bookingCount + " booking" + (bookingCount == 1 ? "" : "s"));
        });
        task.setOnFailed(_ -> {
            bookingHistory.getItems().clear();
            bookingHistoryStatus.error(task.getException(),
                    "Unable to load booking history. Please reopen this screen.");
            returnToLoginIfSessionEnded();
        });
        Thread.ofPlatform().daemon().name("gymmie-member-booking-history").start(task);
    }

    private void configurePlanChoices() {
        availablePlans.setButtonCell(new MembershipPlanCell());
        availablePlans.setCellFactory(_ -> new MembershipPlanCell());
        availablePlans.setOnAction(_ -> updatePurchaseAvailability());
        bookingHistory.setCellFactory(_ -> new MemberBookingCell());
    }

    @FXML
    private void refreshMembership() {
        if (refreshMembership.isDisabled()) {
            return;
        }
        refreshMembership.setDisable(true);
        membershipStatusLoaded = false;
        renewableMembershipId = 0;
        updateRenewalAvailability();
        updateCancellationAvailability();
        updatePurchaseAvailability();
        membershipPlan.setText("Plan: —");
        membershipExpiry.setText("Expiry date: —");
        membershipStatus.info("Loading membership…");
        Task<Optional<RenewableMembership>> task = new Task<>() {
            @Override
            protected Optional<RenewableMembership> call() throws Exception {
                return context.getMembershipStatusService().renewableMembership();
            }
        };
        task.setOnSucceeded(_ -> {
            refreshMembership.setDisable(false);
            hasRenewableMembership = task.getValue().isPresent();
            if (hasRenewableMembership) {
                RenewableMembership current = task.getValue().orElseThrow();
                hasActiveMembership = current.activeToday();
                renewableMembershipId = current.membershipId();
                currentPlanName = current.planName();
                membershipPlan.setText("Plan: " + currentPlanName);
                membershipExpiry.setText("Expiry date: " + DisplayFormatters.date(current.expiryDate()));
                membershipStatus.info(current.activeToday() ? "Active" : "Expired");
            } else {
                hasActiveMembership = false;
                renewableMembershipId = 0;
                membershipPlan.setText("Plan: —");
                membershipExpiry.setText("Expiry date: —");
                currentPlanName = null;
                membershipStatus.info("Inactive — no current membership.");
            }
            membershipStatusLoaded = true;
            if (hasActiveMembership) {
                purchaseStatus.info("You already have an active membership.");
            } else if (!availablePlans.getItems().isEmpty()) {
                purchaseStatus.info("Choose a plan to purchase.");
            }
            updateRenewalAvailability();
            updateCancellationAvailability();
            updatePurchaseAvailability();
        });
        task.setOnFailed(_ -> {
            refreshMembership.setDisable(false);
            updateRenewalAvailability();
            updateCancellationAvailability();
            updatePurchaseAvailability();
            membershipStatus.error(task.getException(), "Unable to load membership. Please try Refresh.");
            returnToLoginIfSessionEnded();
        });
        Thread.ofPlatform().daemon().name("gymmie-membership-status").start(task);
    }

    @FXML
    private void renewMembership() {
        if (renewalInProgress || purchaseInProgress || cancellationInProgress || renewMembership.isDisabled()
                || !hasRenewableMembership) {
            return;
        }
        renewalInProgress = true;
        refreshMembership.setDisable(true);
        updateRenewalAvailability();
        updateCancellationAvailability();
        updatePurchaseAvailability();
        renewalStatus.info("Renewing membership…");
        Task<Membership> task = new Task<>() {
            @Override
            protected Membership call() throws Exception {
                return context.getMembershipRenewalService().renew(renewableMembershipId);
            }
        };
        task.setOnSucceeded(_ -> {
            Membership renewed = task.getValue();
            hasActiveMembership = true;
            hasRenewableMembership = true;
            renewalInProgress = false;
            refreshMembership.setDisable(false);
            membershipPlan.setText("Plan: " + currentPlanName);
            membershipExpiry.setText("Expiry date: " + DisplayFormatters.date(renewed.expiryDate()));
            membershipStatus.info("Active");
            renewalStatus.success("Membership renewed.");
            purchaseStatus.info("You already have an active membership.");
            updateRenewalAvailability();
            updateCancellationAvailability();
            updatePurchaseAvailability();
        });
        task.setOnFailed(_ -> {
            renewalInProgress = false;
            refreshMembership.setDisable(false);
            renewalStatus.error(task.getException(), "Unable to renew this membership. Please try again.");
            updateRenewalAvailability();
            updateCancellationAvailability();
            updatePurchaseAvailability();
            returnToLoginIfSessionEnded();
        });
        Thread.ofPlatform().daemon().name("gymmie-membership-renewal").start(task);
    }

    private void updateRenewalAvailability() {
        if (renewMembership != null) {
            renewMembership.setDisable(renewalInProgress || purchaseInProgress || cancellationInProgress
                    || !membershipStatusLoaded
                    || !hasRenewableMembership);
        }
    }

    private void updateCancellationAvailability() {
        if (cancelMembership != null) {
            cancelMembership.setDisable(cancellationInProgress || purchaseInProgress || renewalInProgress
                    || !membershipStatusLoaded || !hasActiveMembership);
        }
    }

    @FXML
    private void cancelMembership() {
        if (cancellationInProgress || purchaseInProgress || renewalInProgress || cancelMembership.isDisabled()
                || !hasActiveMembership) {
            return;
        }
        if (!UiFeedback.confirm(membershipScreen.getScene().getWindow(), "Cancel membership",
                "Cancel your current membership immediately? There is no refund. "
                        + "All future session bookings will also be cancelled.")) {
            return;
        }
        cancellationInProgress = true;
        refreshMembership.setDisable(true);
        updateRenewalAvailability();
        updateCancellationAvailability();
        updatePurchaseAvailability();
        cancellationStatus.info("Cancelling membership…");
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return context.getMembershipCancellationService().cancel(renewableMembershipId);
            }
        };
        task.setOnSucceeded(_ -> {
            int count = task.getValue();
            cancellationInProgress = false;
            refreshMembership.setDisable(false);
            hasActiveMembership = false;
            hasRenewableMembership = false;
            membershipStatus.info("Cancelled");
            renewalStatus.info("Membership was cancelled and cannot be renewed.");
            cancellationStatus.success("Membership cancelled immediately. No refund was issued. "
                    + count + " future booking" + (count == 1 ? " was" : "s were") + " cancelled.");
            loadBookingHistory();
            purchaseStatus.info("Choose a plan to purchase.");
            updateRenewalAvailability();
            updateCancellationAvailability();
            updatePurchaseAvailability();
        });
        task.setOnFailed(_ -> {
            cancellationInProgress = false;
            refreshMembership.setDisable(false);
            cancellationStatus.error(task.getException(), "Unable to cancel this membership. Please try again.");
            updateRenewalAvailability();
            updateCancellationAvailability();
            updatePurchaseAvailability();
            returnToLoginIfSessionEnded();
        });
        Thread.ofPlatform().daemon().name("gymmie-membership-cancellation").start(task);
    }

    @FXML
    private void refreshAvailablePlans() {
        if (!refreshPlans.isDisabled()) {
            loadAvailablePlans();
        }
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
            purchaseStatus.error(task.getException(), "Unable to load available plans. Please try again.");
            returnToLoginIfSessionEnded();
        });
        Thread.ofPlatform().daemon().name("gymmie-membership-plans").start(task);
    }

    @FXML
    private void purchaseMembership() {
        MembershipPlan selectedPlan = availablePlans.getValue();
        if (purchaseInProgress || renewalInProgress || cancellationInProgress || purchaseMembership.isDisabled()
                || selectedPlan == null) {
            return;
        }
        purchaseInProgress = true;
        refreshMembership.setDisable(true);
        updatePurchaseAvailability();
        updateRenewalAvailability();
        updateCancellationAvailability();
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
            hasRenewableMembership = true;
            membershipStatusLoaded = true;
            renewableMembershipId = purchased.id();
            currentPlanName = selectedPlan.name();
            purchaseInProgress = false;
            refreshMembership.setDisable(false);
            membershipPlan.setText("Plan: " + currentPlanName);
            membershipExpiry.setText("Expiry date: " + DisplayFormatters.date(purchased.expiryDate()));
            membershipStatus.info("Active");
            updatePurchaseAvailability();
            updateRenewalAvailability();
            updateCancellationAvailability();
            purchaseStatus.success("Membership purchased.");
        });
        task.setOnFailed(_ -> {
            purchaseInProgress = false;
            refreshMembership.setDisable(false);
            purchaseStatus.error(task.getException(), "Unable to purchase this plan. Please try again.");
            updatePurchaseAvailability();
            updateRenewalAvailability();
            updateCancellationAvailability();
            returnToLoginIfSessionEnded();
        });
        Thread.ofPlatform().daemon().name("gymmie-membership-purchase").start(task);
    }

    private void updatePurchaseAvailability() {
        if (purchaseMembership == null) {
            return;
        }
        purchaseMembership.setDisable(purchaseInProgress || renewalInProgress || cancellationInProgress
                || !membershipStatusLoaded
                || hasActiveMembership || availablePlans.getValue() == null);
    }

    private void returnToLoginIfSessionEnded() {
        if (!context.getUserSession().isAuthenticated()) {
            try {
                router.showLogin();
            } catch (IOException exception) {
                context.getAuthService().logout();
                membershipScreen.setDisable(true);
            }
        }
    }

    @FXML
    private void back() {
        try {
            router.showDashboard();
        } catch (IOException exception) {
            membershipStatus.error("Unable to open the dashboard. Please try again.");
        }
    }

    private static final class MembershipPlanCell extends ListCell<MembershipPlan> {
        @Override
        protected void updateItem(MembershipPlan plan, boolean empty) {
            super.updateItem(plan, empty);
            setText(empty || plan == null ? null : plan.name() + " · " + plan.durationDays() + " days · "
                    + DisplayFormatters.price(plan.priceCents()));
        }
    }

    private static final class MemberBookingCell extends ListCell<MemberBooking> {
        @Override
        protected void updateItem(MemberBooking booking, boolean empty) {
            super.updateItem(booking, empty);
            if (empty || booking == null) {
                setText(null);
            } else if (booking.status() == BookingStatus.CANCELLED) {
                setText("Booking #" + booking.bookingId() + " · " + DisplayFormatters.dateTime(booking.startsAt())
                        + " · Cancelled · Reason: " + cancellationReasonText(booking.cancellationReason()));
            } else {
                setText("Booking #" + booking.bookingId() + " · " + DisplayFormatters.dateTime(booking.startsAt())
                        + " · Booked");
            }
        }

        private static String cancellationReasonText(CancellationReason reason) {
            return switch (reason) {
                case MEMBERSHIP_CANCELLED -> "Membership cancelled";
                case MEMBER_CANCELLED_BOOKING -> "Member cancelled booking";
                case TRAINER_CANCELLED_SESSION -> "Trainer cancelled session";
                case ACCOUNT_DEACTIVATED -> "Account deactivated";
            };
        }
    }
}
