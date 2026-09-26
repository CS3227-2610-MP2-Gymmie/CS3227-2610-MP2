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
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

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
        upcomingBookings.setCellFactory(_ -> new MemberBookingCell());
        pastBookings.setCellFactory(_ -> new MemberBookingCell());
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
