package gymmie.member;

import static gymmie.testutil.JavaFxTestSupport.awaitUi;
import static gymmie.testutil.JavaFxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import javafx.stage.Window;

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
            awaitUi(status, "5 bookings"::equals);
            CompletableFuture<Void> bookingListUpdated = new CompletableFuture<>();
            ListChangeListener<MemberBooking> bookingChangeListener = _ -> bookingListUpdated.complete(null);
            Booking originalBooking = bookingById(context, 5);
            onFxThread(() -> {
                @SuppressWarnings("unchecked")
                ListView<MemberBooking> upcoming = (ListView<MemberBooking>) stage.getScene()
                        .lookup("#upcomingBookings");
                @SuppressWarnings("unchecked")
                ListView<MemberBooking> past = (ListView<MemberBooking>) stage.getScene().lookup("#pastBookings");
                assertEquals(3, upcoming.getItems().size());
                assertEquals(2, past.getItems().size());
                assertEquals("3 upcoming bookings", ((Label) stage.getScene().lookup("#upcomingStatus")).getText());
                assertEquals("2 past bookings", ((Label) stage.getScene().lookup("#pastStatus")).getText());
                assertTrue(upcoming.getItems().stream().allMatch(item -> startsAt(item).isAfter(LocalDateTime.now())));
                assertTrue(past.getItems().stream().allMatch(item -> startsAt(item).isBefore(LocalDateTime.now())));

                ScrollPane screen = (ScrollPane) stage.getScene().getRoot();
                screen.setVvalue(1);
                Parent content = (Parent) screen.getContent();
                content.applyCss();
                content.layout();
                List<String> upcomingText = rowLabels(upcoming);
                List<String> pastText = rowLabels(past);
                assertTrue(upcomingText.stream().anyMatch(MemberBookingsNavigationTest::hasSessionDetails));
                assertTrue(pastText.stream().anyMatch(MemberBookingsNavigationTest::hasSessionDetails));
                assertTrue(pastText.stream().anyMatch(text -> text.contains("Cancelled")
                        && text.contains("Reason: Membership cancelled")));
                assertTrue(pastText.stream().anyMatch(text -> text.contains("Cancelled")
                        && text.contains("Reason: Member cancelled booking")));
                assertTrue(upcomingText.stream().anyMatch(text -> text.contains("Reason: Trainer cancelled session")));
                assertTrue(upcomingText.stream().anyMatch(text -> text.contains("Reason: Account deactivated")));

                Button cancel = (Button) stage.getScene().lookup("#upcomingBookings .list-cell .button");
                assertTrue(cancel.getText().equals("Cancel booking"));
                Platform.runLater(() -> clickConfirmationButton("Cancel booking", ButtonType.CANCEL));
                cancel.fire();
                return null;
            });
            assertEquals(originalBooking, bookingById(context, 5));

            onFxThread(() -> {
                @SuppressWarnings("unchecked")
                ListView<MemberBooking> upcoming = (ListView<MemberBooking>) stage.getScene()
                        .lookup("#upcomingBookings");
                upcoming.getItems().addListener(bookingChangeListener);
                Button cancel = (Button) stage.getScene().lookup("#upcomingBookings .list-cell .button");
                Platform.runLater(() -> clickConfirmationButton("Cancel booking", ButtonType.OK));
                cancel.fire();
                return null;
            });
            bookingListUpdated.get(15, TimeUnit.SECONDS);
            onFxThread(() -> {
                @SuppressWarnings("unchecked")
                ListView<MemberBooking> upcoming = (ListView<MemberBooking>) stage.getScene()
                        .lookup("#upcomingBookings");
                upcoming.getItems().removeListener(bookingChangeListener);
                MemberBooking cancelled = upcoming.getItems().stream().filter(item -> item.bookingId() == 5)
                        .findFirst().orElseThrow();
                assertEquals(BookingStatus.CANCELLED, cancelled.status());
                assertEquals(CancellationReason.MEMBER_CANCELLED_BOOKING, cancelled.cancellationReason());
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
            addBooking(context, connection, 5, now.plusDays(4), BookingStatus.BOOKED, null);
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

    private static List<String> rowLabels(ListView<?> list) {
        return list.lookupAll(".list-cell").stream()
                .flatMap(node -> ((ListCell<?>) node).lookupAll(".label").stream())
                .map(node -> ((Label) node).getText())
                .toList();
    }

    private static boolean hasSessionDetails(String text) {
        return text.contains("Trainer: Trainer") && text.contains("Duration: 60 minutes")
                && text.contains("Description: Workout");
    }

    private static void clickConfirmationButton(String title, ButtonType type) {
        Window dialog = Window.getWindows().stream()
                .filter(Window::isShowing)
                .filter(window -> window instanceof Stage stage && title.equals(stage.getTitle()))
                .findFirst().orElseThrow();
        DialogPane dialogPane = (DialogPane) dialog.getScene().getRoot();
        Button confirmation = (Button) dialogPane.lookupButton(type);
        confirmation.fire();
    }

    private static Booking bookingById(AppContext context, long bookingId) throws Exception {
        return context.getPersistence().unitOfWork().inTransaction(connection ->
                context.getPersistence().bookings().findById(connection, bookingId).orElseThrow());
    }
}
