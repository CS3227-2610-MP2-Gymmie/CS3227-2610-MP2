package gymmie.member;

import static gymmie.testutil.JavaFxTestSupport.awaitUi;
import static gymmie.testutil.JavaFxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import gymmie.AppContext;
import gymmie.Router;
import gymmie.ViewLoader;
import gymmie.member.service.MemberBookingHistoryService.MemberBooking;
import gymmie.model.Account;
import gymmie.model.Booking;
import gymmie.model.BookingStatus;
import gymmie.model.CancellationReason;
import gymmie.model.Role;
import gymmie.model.TrainingSession;
import gymmie.service.PasswordHasher;
import gymmie.testutil.JavaFxTestSupport;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

/** Opt-in JavaFX integration checks for Member booking history. */
@EnabledIfSystemProperty(named = "gymmie.uiTests", matches = "true")
class MemberBookingsNavigationTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    void dashboardBookingsSeparateUpcomingAndPastAndKeepCancellationReasons() throws Exception {
        AppContext context = contextWithBookings(temporaryDirectory.resolve("member-bookings.db"));
        context.getAuthService().login("member", "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<String> status = onFxThread(() -> {
                new Router(stage, context, new ViewLoader()).showDashboard();
                stage.show();
                Button bookingsButton = (Button) stage.getScene().lookup("#memberBookingsButton");
                assertTrue(bookingsButton.isVisible());
                bookingsButton.fire();
                stage.getScene().getRoot().applyCss();
                return ((Label) stage.getScene().lookup("#bookingsStatus")).textProperty();
            });
            awaitUi(status, "4 bookings"::equals);
            onFxThread(() -> {
                @SuppressWarnings("unchecked")
                ListView<MemberBooking> upcoming = (ListView<MemberBooking>) stage.getScene()
                        .lookup("#upcomingBookings");
                @SuppressWarnings("unchecked")
                ListView<MemberBooking> past = (ListView<MemberBooking>) stage.getScene().lookup("#pastBookings");
                assertEquals(2, upcoming.getItems().size());
                assertEquals(2, past.getItems().size());
                assertEquals("2 upcoming bookings", ((Label) stage.getScene().lookup("#upcomingStatus")).getText());
                assertEquals("2 past bookings", ((Label) stage.getScene().lookup("#pastStatus")).getText());
                assertTrue(upcoming.getItems().stream().allMatch(item -> startsAt(item).isAfter(LocalDateTime.now())));
                assertTrue(past.getItems().stream().allMatch(item -> startsAt(item).isBefore(LocalDateTime.now())));

                ScrollPane screen = (ScrollPane) stage.getScene().getRoot();
                screen.setVvalue(1);
                Parent content = (Parent) screen.getContent();
                content.applyCss();
                content.layout();
                List<String> rowText = List.of(upcoming, past).stream()
                        .flatMap(list -> list.lookupAll(".list-cell").stream())
                        .map(node -> ((ListCell<?>) node).getText())
                        .filter(text -> text != null)
                        .toList();
                assertTrue(rowText.stream().anyMatch(text -> text.contains("Cancelled")
                        && text.contains("Reason: Membership cancelled")));
                assertTrue(rowText.stream().anyMatch(text -> text.contains("Cancelled")
                        && text.contains("Reason: Member cancelled booking")));
                assertTrue(rowText.stream().anyMatch(text -> text.contains("Reason: Trainer cancelled session")));
                assertTrue(rowText.stream().anyMatch(text -> text.contains("Reason: Account deactivated")));
                return null;
            });
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    private AppContext contextWithBookings(Path database) throws Exception {
        AppContext context = new AppContext(database);
        var hash = new PasswordHasher().hash("password123");
        LocalDateTime now = LocalDateTime.now();
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection,
                    new Account(2, "member", hash, "Member", Role.MEMBER, true));
            context.getPersistence().accounts().insert(connection,
                    new Account(3, "trainer", hash, "Trainer", Role.TRAINER, true));
            addBooking(context, connection, 1, now.minusDays(3), BookingStatus.CANCELLED,
                    CancellationReason.MEMBERSHIP_CANCELLED);
            addBooking(context, connection, 2, now.minusDays(2), BookingStatus.CANCELLED,
                    CancellationReason.MEMBER_CANCELLED_BOOKING);
            addBooking(context, connection, 3, now.plusDays(2), BookingStatus.CANCELLED,
                    CancellationReason.TRAINER_CANCELLED_SESSION);
            addBooking(context, connection, 4, now.plusDays(3), BookingStatus.CANCELLED,
                    CancellationReason.ACCOUNT_DEACTIVATED);
            return null;
        });
        return context;
    }

    private static void addBooking(AppContext context, java.sql.Connection connection, long id,
            LocalDateTime startsAt, BookingStatus status, CancellationReason reason) throws Exception {
        context.getPersistence().sessions().insert(connection,
                new TrainingSession(id, 3, startsAt, 60, 10, "Workout", false));
        context.getPersistence().bookings().insert(connection,
                new Booking(id, id, 2, startsAt.minusDays(5), status, reason));
    }

    private static LocalDateTime startsAt(Object item) {
        return ((MemberBooking) item).startsAt();
    }
}
