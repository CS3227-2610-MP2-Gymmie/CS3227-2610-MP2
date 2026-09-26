package gymmie.member;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import gymmie.AppContext;
import gymmie.Router;
import gymmie.member.service.MemberBookingHistoryService.MemberBooking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.ui.DisplayFormatters;
import gymmie.ui.StatusLabel;
import gymmie.ui.UiFeedback;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

/** Shows the authenticated Member's upcoming and past bookings. */
public final class MemberBookingsController {
    private final AppContext context;
    private final Router router;

    @FXML
    private StatusLabel bookingsStatus;
    @FXML
    private StatusLabel upcomingStatus;
    @FXML
    private StatusLabel pastStatus;
    @FXML
    private ListView<MemberBooking> upcomingBookings;
    @FXML
    private ListView<MemberBooking> pastBookings;
    @FXML
    private Button refreshButton;

    /** Creates the Member bookings screen with shared application services and navigation. */
    public MemberBookingsController(AppContext context, Router router) {
        this.context = context;
        this.router = router;
    }

    @FXML
    private void initialize() {
        upcomingBookings.setCellFactory(_ -> new MemberBookingCell(true));
        pastBookings.setCellFactory(_ -> new MemberBookingCell(false));
        loadBookings();
    }

    @FXML
    private void refreshBookings() {
        loadBookings();
    }

    private void loadBookings() {
        refreshButton.setDisable(true);
        bookingsStatus.info("Loading bookings…");
        upcomingStatus.setText("");
        pastStatus.setText("");
        Task<List<MemberBooking>> task = new Task<>() {
            @Override
            protected List<MemberBooking> call() throws Exception {
                return context.getMemberBookingHistoryService().bookingHistory();
            }
        };
        task.setOnSucceeded(_ -> {
            LocalDateTime now = LocalDateTime.now();
            List<MemberBooking> allBookings = task.getValue();
            List<MemberBooking> upcoming = upcomingBookings(allBookings, now);
            List<MemberBooking> past = pastBookings(allBookings, now);
            upcomingBookings.getItems().setAll(upcoming);
            pastBookings.getItems().setAll(past);
            refreshButton.setDisable(false);
            bookingsStatus.info(allBookings.isEmpty() ? "No bookings yet."
                    : allBookings.size() + " booking" + (allBookings.size() == 1 ? "" : "s"));
            upcomingStatus.info(countText(upcoming.size(), "upcoming booking"));
            pastStatus.info(countText(past.size(), "past booking"));
        });
        task.setOnFailed(_ -> {
            upcomingBookings.getItems().clear();
            pastBookings.getItems().clear();
            refreshButton.setDisable(false);
            bookingsStatus.error(task.getException(), "Unable to load bookings. Please try again.");
            returnToLoginIfSessionEnded();
        });
        Thread.ofPlatform().daemon().name("gymmie-member-bookings").start(task);
    }

    private void cancelBooking(MemberBooking booking, Button cancelButton) {
        if (!UiFeedback.confirm(upcomingBookings.getScene().getWindow(), "Cancel booking",
                "Cancel your booking for " + DisplayFormatters.dateTime(booking.startsAt())
                        + "? The released space will be available to another Member.")) {
            return;
        }
        cancelButton.setDisable(true);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                context.getMemberBookingCancellationService().cancel(booking.bookingId());
                return null;
            }
        };
        task.setOnSucceeded(_ -> loadBookings());
        task.setOnFailed(_ -> {
            cancelButton.setDisable(false);
            bookingsStatus.error(task.getException(), "Unable to cancel this booking. Please refresh and try again.");
            returnToLoginIfSessionEnded();
        });
        Thread.ofPlatform().daemon().name("gymmie-member-booking-cancel").start(task);
    }

    private static String countText(int count, String label) {
        return count + " " + label + (count == 1 ? "" : "s");
    }

    static List<MemberBooking> upcomingBookings(List<MemberBooking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(booking -> booking.startsAt().isAfter(now))
                .sorted(Comparator.comparing(MemberBooking::startsAt))
                .toList();
    }

    static List<MemberBooking> pastBookings(List<MemberBooking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(booking -> !booking.startsAt().isAfter(now))
                .sorted(Comparator.comparing(MemberBooking::startsAt).reversed())
                .toList();
    }

    private void returnToLoginIfSessionEnded() {
        if (!context.getUserSession().isAuthenticated()) {
            try {
                router.showLogin();
            } catch (IOException exception) {
                bookingsStatus.error("Your session has ended. Restart Gymmie to sign in again.");
            }
        }
    }

    @FXML
    private void back() {
        try {
            router.showDashboard();
        } catch (IOException exception) {
            bookingsStatus.error("Unable to return to the dashboard. Please restart Gymmie.");
        }
    }

    private final class MemberBookingCell extends ListCell<MemberBooking> {
        private final boolean upcoming;

        private MemberBookingCell(boolean upcoming) {
            this.upcoming = upcoming;
        }

        @Override
        protected void updateItem(MemberBooking booking, boolean empty) {
            super.updateItem(booking, empty);
            if (empty || booking == null) {
                setText(null);
                setGraphic(null);
            } else if (booking.status() == BookingStatus.CANCELLED) {
                setBookingGraphic(booking,
                        "Cancelled · Reason: " + cancellationReasonText(booking.cancellationReason()), false);
            } else {
                setBookingGraphic(booking, "Booked", upcoming);
            }
        }

        private void setBookingGraphic(MemberBooking booking, String bookingStatus, boolean canCancel) {
            setText(null);
            Label details = new Label("Booking #" + booking.bookingId() + " · "
                    + DisplayFormatters.dateTime(booking.startsAt()) + " · " + bookingStatus
                    + "\nTrainer: " + booking.trainerName() + " · Duration: " + booking.durationMinutes()
                    + " minutes\nDescription: " + (booking.description() == null || booking.description().isBlank()
                            ? "No description provided" : booking.description()));
            details.setWrapText(true);
            VBox content = new VBox(6, details);
            if (canCancel) {
                Button cancelButton = new Button("Cancel booking");
                cancelButton.setAccessibleText("Cancel booking #" + booking.bookingId());
                cancelButton.setOnAction(_ -> cancelBooking(booking, cancelButton));
                content.getChildren().add(cancelButton);
            }
            setGraphic(content);
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
