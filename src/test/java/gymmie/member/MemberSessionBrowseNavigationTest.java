package gymmie.member;

import static gymmie.testutil.JavaFxTestSupport.awaitUi;
import static gymmie.testutil.JavaFxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import gymmie.AppContext;
import gymmie.Router;
import gymmie.ViewLoader;
import gymmie.model.Account;
import gymmie.model.CancellationReason;
import gymmie.model.MembershipPlan;
import gymmie.model.Role;
import gymmie.service.PasswordHasher;
import gymmie.testutil.AccountBuilder;
import gymmie.testutil.BookingBuilder;
import gymmie.testutil.JavaFxTestSupport;
import gymmie.testutil.MembershipBuilder;
import gymmie.testutil.TrainingSessionBuilder;
import gymmie.ui.DisplayFormatters;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Opt-in JavaFX integration checks for Member session browsing. */
@EnabledIfSystemProperty(named = "gymmie.uiTests", matches = "true")
class MemberSessionBrowseNavigationTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    void filtersSessionCardsByTrainer() throws Exception {
        AppContext context = contextWithSessions(temporaryDirectory.resolve("filter-sessions.db"));
        context.getAuthService().login("member", "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<String> status = onFxThread(() -> {
                openSessionBrowser(stage, context);
                return ((Label) stage.getScene().lookup("#sessionStatus")).textProperty();
            });
            awaitUi(status, "2 upcoming sessions"::equals);
            LocalDateTime zedStart = context.getPersistence().unitOfWork().inTransaction(connection ->
                    context.getPersistence().sessions().findById(connection, 2).orElseThrow().startsAt());
            onFxThread(() -> {
                ComboBox<?> trainerFilter = (ComboBox<?>) stage.getScene().lookup("#trainerFilter");
                assertEquals(3, trainerFilter.getItems().size());
                int zedIndex = java.util.stream.IntStream.range(0, trainerFilter.getItems().size())
                        .filter(index -> trainerFilter.getItems().get(index).toString().contains("Trainer #4"))
                        .findFirst().orElseThrow();
                trainerFilter.getSelectionModel().select(zedIndex);
                VBox cards = (VBox) stage.getScene().lookup("#sessionCards");
                assertEquals(1, cards.getChildren().size());
                VBox card = (VBox) cards.getChildren().getFirst();
                assertEquals("Zed Coach", ((Label) card.getChildren().get(0)).getText());
                assertEquals(DisplayFormatters.dateTime(zedStart), ((Label) card.getChildren().get(1)).getText());
                assertEquals("60 minutes · 0 of 10 Members booked", ((Label) card.getChildren().get(2)).getText());
                assertEquals("Strength basics", ((Label) card.getChildren().get(3)).getText());
                assertEquals("1 upcoming session", status.getValue());
                return null;
            });
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test
    void refreshReloadsChangedBookingCountsAndSessionAvailability() throws Exception {
        AppContext context = contextWithSessions(temporaryDirectory.resolve("refresh-sessions.db"));
        context.getAuthService().login("member", "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<String> status = onFxThread(() -> {
                openSessionBrowser(stage, context);
                return ((Label) stage.getScene().lookup("#sessionStatus")).textProperty();
            });
            awaitUi(status, "2 upcoming sessions"::equals);
            context.getPersistence().unitOfWork().inTransaction(connection -> {
                var session = context.getPersistence().sessions().findById(connection, 2).orElseThrow();
                context.getPersistence().sessions().update(connection,
                        new gymmie.model.TrainingSession(session.id(), session.trainerId(), session.startsAt(),
                                session.durationMinutes(), session.capacity(), session.description(), true));
                context.getPersistence().bookings().insert(connection,
                        new BookingBuilder().withSessionId(1).withMemberId(2).build());
                return null;
            });
            onFxThread(() -> {
                Button refresh = (Button) stage.getScene().lookup("#refreshButton");
                refresh.fire();
                return null;
            });
            awaitUi(status, "1 upcoming session"::equals);
            onFxThread(() -> {
                VBox cards = (VBox) stage.getScene().lookup("#sessionCards");
                assertEquals(1, cards.getChildren().size());
                VBox card = (VBox) cards.getChildren().getFirst();
                assertEquals("Amy Coach", ((Label) card.getChildren().get(0)).getText());
                assertEquals("60 minutes · 1 of 10 Members booked", ((Label) card.getChildren().get(2)).getText());
                return null;
            });
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test
    void bookingPersistsAndMarksTheSessionCardAsAlreadyBooked() throws Exception {
        AppContext context = contextWithSessions(temporaryDirectory.resolve("book-session.db"));
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(1, "Monthly", 30, 4990, false));
            context.getPersistence().memberships().insert(connection, new MembershipBuilder().withId(1)
                    .withMemberId(2).withPlanId(1).withExpiryDate(LocalDate.now().plusDays(3)).build());
            return null;
        });
        context.getAuthService().login("member", "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<String> status = onFxThread(() -> {
                openSessionBrowser(stage, context);
                return ((Label) stage.getScene().lookup("#sessionStatus")).textProperty();
            });
            awaitUi(status, "2 upcoming sessions"::equals);
            Button book = onFxThread(() -> (Button) ((VBox) ((VBox) stage.getScene().lookup("#sessionCards"))
                    .getChildren().getFirst()).getChildren().get(4));
            onFxThread(() -> {
                book.fire();
                return null;
            });
            awaitUi(status, "Success: Session booked. See My membership → Booking history."::equals);
            onFxThread(() -> {
                VBox card = (VBox) ((VBox) stage.getScene().lookup("#sessionCards")).getChildren().getFirst();
                Button booked = (Button) card.getChildren().get(4);
                assertEquals("Already booked", booked.getText());
                assertTrue(booked.isDisabled());
                return null;
            });
            int persistedBookingCount = context.getPersistence().unitOfWork().inTransaction(connection ->
                    context.getPersistence().bookings().countBookedBySessionId(connection, 1));
            assertEquals(1, persistedBookingCount);
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test
    void cancelledBookingLeavesBookSessionAvailable() throws Exception {
        AppContext context = contextWithSessions(temporaryDirectory.resolve("rebook-session.db"));
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().bookings().insert(connection, new BookingBuilder().withId(1)
                    .withSessionId(1).withMemberId(2).withStatus(gymmie.model.BookingStatus.CANCELLED)
                    .withCancellationReason(CancellationReason.MEMBER_CANCELLED_BOOKING).build());
            return null;
        });
        context.getAuthService().login("member", "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<String> status = onFxThread(() -> {
                openSessionBrowser(stage, context);
                return ((Label) stage.getScene().lookup("#sessionStatus")).textProperty();
            });
            awaitUi(status, "2 upcoming sessions"::equals);
            onFxThread(() -> {
                VBox card = (VBox) ((VBox) stage.getScene().lookup("#sessionCards")).getChildren().getFirst();
                Button book = (Button) card.getChildren().get(4);
                assertEquals("Book session", book.getText());
                assertFalse(book.isDisabled());
                return null;
            });
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test
    void failedRefreshShowsAnErrorAndDoesNotClaimNoSessionsAreAvailable() throws Exception {
        AppContext context = contextWithSessions(temporaryDirectory.resolve("failed-refresh-sessions.db"));
        context.getAuthService().login("member", "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<String> status = onFxThread(() -> {
                openSessionBrowser(stage, context);
                return ((Label) stage.getScene().lookup("#sessionStatus")).textProperty();
            });
            awaitUi(status, "2 upcoming sessions"::equals);
            context.getPersistence().unitOfWork().inTransaction(connection -> {
                try (var statement = connection.createStatement()) {
                    statement.execute("DROP TABLE booking");
                }
                return null;
            });
            onFxThread(() -> {
                Button refresh = (Button) stage.getScene().lookup("#refreshButton");
                refresh.fire();
                return null;
            });
            awaitUi(status, value -> value.startsWith("Error:"));
            onFxThread(() -> {
                assertFalse(status.getValue().contains("No upcoming sessions are available."));
                assertTrue(((ComboBox<?>) stage.getScene().lookup("#trainerFilter")).isDisabled());
                return null;
            });
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    private AppContext contextWithSessions(Path database) throws Exception {
        AppContext context = new AppContext(database);
        var password = new PasswordHasher().hash("password123");
        Account member = account(2, "member", "Member", Role.MEMBER, password);
        Account trainer = account(3, "trainer_one", "Amy Coach", Role.TRAINER, password);
        Account secondTrainer = account(4, "trainer_two", "Zed Coach", Role.TRAINER, password);
        LocalDateTime firstStart = LocalDateTime.now().plusDays(1);
        LocalDateTime secondStart = LocalDateTime.now().plusDays(2);
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            for (Account account : List.of(member, trainer, secondTrainer)) {
                context.getPersistence().accounts().insert(connection, account);
            }
            context.getPersistence().sessions().insert(connection, new TrainingSessionBuilder()
                    .withId(1).withTrainerId(trainer.id()).withStartsAt(firstStart)
                    .withDescription("Mobility basics").build());
            context.getPersistence().sessions().insert(connection, new TrainingSessionBuilder()
                    .withId(2).withTrainerId(secondTrainer.id()).withStartsAt(secondStart)
                    .withDescription("Strength basics").build());
            return null;
        });
        return context;
    }

    private static Account account(long id, String username, String name, Role role,
            gymmie.model.PasswordHash password) {
        return new AccountBuilder().withId(id).withUsername(username).withDisplayName(name)
                .withRole(role).withPassword(password).build();
    }

    private static void openSessionBrowser(Stage stage, AppContext context) throws Exception {
        Router router = new Router(stage, context, new ViewLoader());
        router.showDashboard();
        stage.show();
        stage.getScene().getRoot().applyCss();
        Button browseSessions = (Button) stage.getScene().lookup("#browseSessionsButton");
        browseSessions.fire();
        stage.getScene().getRoot().applyCss();
    }
}
