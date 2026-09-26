package gymmie.trainer;

import static gymmie.testutil.JavaFxTestSupport.awaitUi;
import static gymmie.testutil.JavaFxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import gymmie.AppContext;
import gymmie.Router;
import gymmie.ViewLoader;
import gymmie.model.Account;
import gymmie.model.Role;
import gymmie.service.PasswordHasher;
import gymmie.service.exception.AuthenticationException;
import gymmie.testutil.JavaFxTestSupport;
import gymmie.ui.StatusLabel;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Opt-in JavaFX integration checks; requires a graphical desktop. */
@EnabledIfSystemProperty(named = "gymmie.uiTests", matches = "true")
class TrainerNavigationTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void trainerChangesPasswordThroughButtonOrConfirmationAction(boolean useConfirmationAction) throws Exception {
        Path databasePath = temporaryDirectory.resolve("trainer-password.db");
        AppContext context = new AppContext(databasePath);
        var hash = new PasswordHasher().hash("password123");
        Account trainer = new Account(2, "TrainerLogin", hash, "Trainer", Role.TRAINER, true);
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection, trainer);
            return null;
        });
        context.getAuthService().login(trainer.username(), "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<String> feedback = onFxThread(() -> {
                new Router(stage, context, new ViewLoader()).showDashboard();
                stage.show();
                stage.getScene().getRoot().applyCss();
                PasswordField current = (PasswordField) stage.getScene().lookup("#currentPassword");
                PasswordField replacement = (PasswordField) stage.getScene().lookup("#newPassword");
                PasswordField confirmation = (PasswordField) stage.getScene().lookup("#confirmPassword");
                Button save = (Button) stage.getScene().lookup("#savePasswordButton");
                StatusLabel status = (StatusLabel) stage.getScene().lookup("#status");
                assertTrue(current.isFocusTraversable());
                assertTrue(replacement.isFocusTraversable());
                assertTrue(confirmation.isFocusTraversable());
                assertTrue(save.isFocusTraversable());
                save.fire();
                assertEquals("Error: Enter your current and new passwords.", status.getText());
                current.setText("password123");
                replacement.setText("replacement123");
                confirmation.setText("mismatch123");
                save.fire();
                assertEquals("Error: The new passwords do not match.", status.getText());
                return status.textProperty();
            });
            Account unchanged = context.getPersistence().unitOfWork().inTransaction(connection ->
                    context.getPersistence().accounts().findById(connection, trainer.id()).orElseThrow());
            assertEquals(trainer, unchanged);
            onFxThread(() -> {
                PasswordField confirmation = (PasswordField) stage.getScene().lookup("#confirmPassword");
                confirmation.setText("replacement123");
                if (useConfirmationAction) {
                    confirmation.fireEvent(new javafx.event.ActionEvent());
                } else {
                    Button save = (Button) stage.getScene().lookup("#savePasswordButton");
                    save.fire();
                }
                assertTrue(stage.getScene().lookup("#actions").isDisabled());
                return null;
            });
            awaitUi(feedback, text -> text.equals("Success: Password changed."));
            onFxThread(() -> {
                assertFalse(stage.getScene().lookup("#actions").isDisabled());
                for (String id : new String[]{"#currentPassword", "#newPassword", "#confirmPassword"}) {
                    assertEquals("", ((PasswordField) stage.getScene().lookup(id)).getText());
                }
                return null;
            });
            assertEquals(trainer.username(), context.getUserSession().requireUser().username());
            AppContext restarted = new AppContext(databasePath);
            assertThrows(AuthenticationException.class, () ->
                    restarted.getAuthService().login(trainer.username(), "password123"));
            assertEquals(trainer.id(), restarted.getAuthService().login(
                    trainer.username(), "replacement123").accountId());
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test
    void trainerEditsProfileThroughDashboardAndReloadsSavedDetails() throws Exception {
        AppContext context = new AppContext(temporaryDirectory.resolve("profile-ui.db"));
        var hash = new PasswordHasher().hash("password123");
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection,
                    new Account(2, "trainer", hash, "Original Trainer", Role.TRAINER, true));
            return null;
        });
        context.getAuthService().login("trainer", "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<String> feedback = onFxThread(() -> {
                Router router = new Router(stage, context, new ViewLoader());
                router.showDashboard();
                stage.show();
                stage.getScene().getRoot().applyCss();
                Button profile = (Button) stage.getScene().lookup("#profileButton");
                assertTrue(profile.isVisible());
                assertTrue(profile.isFocusTraversable());
                profile.fire();
                stage.getScene().getRoot().applyCss();
                var stylesheets = stage.getScene().getRoot().getStylesheets();
                assertEquals(2, stylesheets.size());
                assertTrue(stylesheets.getFirst().endsWith("/gymmie/css/gymmie.css"));
                assertTrue(stylesheets.getLast().endsWith("/gymmie/trainer/css/profile.css"));
                return ((StatusLabel) stage.getScene().lookup("#profileStatus")).textProperty();
            });
            awaitUi(feedback, text -> text.equals("Profile loaded."));
            onFxThread(() -> {
                TextField username = (TextField) stage.getScene().lookup("#username");
                assertEquals("trainer", username.getText());
                assertFalse(username.isEditable());
                TextField name = (TextField) stage.getScene().lookup("#displayName");
                assertEquals("Original Trainer", name.getText());
                name.setText("");
                Button save = (Button) stage.getScene().lookup("#saveProfileButton");
                assertTrue(save.isFocusTraversable());
                save.fire();
                return null;
            });
            awaitUi(feedback, text -> text.startsWith("Error:"));
            onFxThread(() -> {
                TextField name = (TextField) stage.getScene().lookup("#displayName");
                name.setText("Coach Morgan");
                TextArea synopsis = (TextArea) stage.getScene().lookup("#synopsis");
                synopsis.setText("Strength and mobility coaching for all experience levels.");
                TextField input = (TextField) stage.getScene().lookup("#tagInput");
                pressKey(synopsis, KeyCode.TAB);
                assertEquals(input, stage.getScene().getFocusOwner());
                assertEquals("Strength and mobility coaching for all experience levels.", synopsis.getText());
                input.setText("Strength training");
                pressKey(input, KeyCode.ENTER);
                input.setText("Mobility");
                Button add = (Button) stage.getScene().lookup("#addTagButton");
                add.fire();
                input.setText("Remove me");
                add.fire();
                FlowPane tags = (FlowPane) stage.getScene().lookup("#tags");
                assertEquals(3, tags.getChildren().size());
                Button remove = (Button) tags.getChildren().getLast();
                assertTrue(remove.isFocusTraversable());
                pressKey(remove, KeyCode.SPACE);
                assertEquals(2, tags.getChildren().size());
                pressKey(name, KeyCode.ENTER);
                return null;
            });
            awaitUi(feedback, text -> text.equals("Success: Profile saved."));
            var saved = context.getTrainerProfileService().getOwnTrainerProfile();
            assertEquals("Coach Morgan", saved.displayName());
            assertEquals(List.of("Strength training", "Mobility"), saved.specializations());
            onFxThread(() -> {
                stage.setWidth(700);
                stage.setHeight(820);
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                writePreview(stage, "trainer-profile.png");
                TextField name = (TextField) stage.getScene().lookup("#displayName");
                name.setText("Unsaved change");
                Button reload = (Button) stage.getScene().lookup("#reloadButton");
                reload.fire();
                return null;
            });
            awaitUi(feedback, text -> text.equals("Profile loaded."));
            onFxThread(() -> {
                assertEquals("Coach Morgan", ((TextField) stage.getScene().lookup("#displayName")).getText());
                Button back = (Button) stage.getScene().lookup("#backButton");
                back.fire();
                stage.getScene().getRoot().applyCss();
                assertEquals("Welcome, Coach Morgan", ((Label) stage.getScene().lookup("#welcome")).getText());
                return null;
            });
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void trainerCreatesSessionUsingButtonOrKeyboard(boolean keyboard) throws Exception {
        Path databasePath = temporaryDirectory.resolve("create-session.db");
        AppContext context = new AppContext(databasePath);
        var trainer = new gymmie.testutil.AccountBuilder().withId(2).withRole(Role.TRAINER).build();
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection, trainer);
            return null;
        });
        context.getAuthService().login(trainer.username(), gymmie.testutil.AccountBuilder.DEFAULT_PASSWORD);
        Stage stage = onFxThread(Stage::new);
        LocalDate date = LocalDate.now().plusDays(1);
        String start = date + " 14:30";
        try {
            ObservableValue<String> feedback = onFxThread(() -> {
                new Router(stage, context, new ViewLoader()).showDashboard();
                stage.show();
                stage.getScene().getRoot().applyCss();
                Button open = (Button) stage.getScene().lookup("#createSessionButton");
                assertTrue(open.isVisible());
                if (keyboard) {
                    pressKey(open, KeyCode.SPACE);
                } else {
                    open.fire();
                }
                stage.getScene().getRoot().applyCss();
                DatePicker startDate = (DatePicker) stage.getScene().lookup("#startDate");
                TextField startTime = (TextField) stage.getScene().lookup("#startTime");
                TextField duration = (TextField) stage.getScene().lookup("#duration");
                TextField capacity = (TextField) stage.getScene().lookup("#capacity");
                TextArea description = (TextArea) stage.getScene().lookup("#description");
                Button save = (Button) stage.getScene().lookup("#saveButton");
                StatusLabel status = (StatusLabel) stage.getScene().lookup("#status");
                save.fire();
                assertEquals("Error: Choose a start date from the calendar.", status.getText());
                selectCalendarDate(startDate, date, keyboard);
                assertEquals(date, startDate.getValue());
                for (String invalid : new String[]{"", "24:00", "12:60", "14:30:00"}) {
                    startTime.setText(invalid);
                    save.fire();
                    assertTrue(status.getText().startsWith("Error: Enter a valid start time"));
                }
                startTime.setText("14:30");
                duration.setText("sixty");
                capacity.setText("10");
                save.fire();
                assertEquals("Error: Duration and capacity must be whole numbers.", status.getText());
                duration.setText("60");
                description.setText("Strength training");
                stage.setWidth(700);
                stage.setHeight(820);
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                writePreview(stage, "create-session.png");
                if (keyboard) {
                    pressKey(startDate, KeyCode.TAB);
                    assertEquals(startTime, stage.getScene().getFocusOwner());
                    pressKey(startTime, KeyCode.TAB);
                    assertEquals(duration, stage.getScene().getFocusOwner());
                    pressKey(duration, KeyCode.TAB);
                    assertEquals(capacity, stage.getScene().getFocusOwner());
                    pressKey(capacity, KeyCode.TAB);
                    assertEquals(description, stage.getScene().getFocusOwner());
                    pressKey(description, KeyCode.TAB);
                    assertEquals(save, stage.getScene().getFocusOwner());
                    pressKey(save, KeyCode.SPACE);
                } else {
                    save.fire();
                }
                assertTrue(stage.getScene().lookup("#form").isDisabled());
                return status.textProperty();
            });
            awaitUi(feedback, text -> text.equals("Success: Session created for " + start + "."));
            AppContext restarted = new AppContext(databasePath);
            var saved = restarted.getPersistence().unitOfWork().inTransaction(connection ->
                    restarted.getPersistence().sessions().findByTrainerId(connection, trainer.id()));
            assertEquals(1, saved.size());
            assertEquals(LocalDateTime.of(date, java.time.LocalTime.of(14, 30)), saved.getFirst().startsAt());
            assertEquals(60, saved.getFirst().durationMinutes());
            assertEquals(10, saved.getFirst().capacity());
            assertEquals("Strength training", saved.getFirst().description());
            onFxThread(() -> {
                assertEquals(null, ((DatePicker) stage.getScene().lookup("#startDate")).getValue());
                assertEquals("", ((TextField) stage.getScene().lookup("#startTime")).getText());
                Button back = (Button) stage.getScene().lookup("#backButton");
                pressKey(back, KeyCode.SPACE);
                stage.getScene().getRoot().applyCss();
                assertEquals("Trainer dashboard", ((Label) stage.getScene().lookup("#title")).getText());
                return null;
            });
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void trainerOpensAndRefreshesUpcomingSessionsByMouseOrKeyboard(boolean keyboard) throws Exception {
        AppContext context = new AppContext(temporaryDirectory.resolve("upcoming.db"));
        var trainer = new gymmie.testutil.AccountBuilder().withId(2).withRole(Role.TRAINER).build();
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection, trainer);
            return null;
        });
        context.getAuthService().login(trainer.username(), gymmie.testutil.AccountBuilder.DEFAULT_PASSWORD);
        var session = context.getTrainingSessionService().create(LocalDateTime.now().plusDays(1),
                60, 10, "Strength and mobility\nBring a towel.");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<String> feedback = onFxThread(() -> {
                new Router(stage, context, new ViewLoader()).showDashboard();
                stage.show();
                stage.getScene().getRoot().applyCss();
                Button open = (Button) stage.getScene().lookup("#upcomingSessionsButton");
                assertTrue(open.isVisible());
                assertTrue(open.isFocusTraversable());
                if (keyboard) {
                    pressKey(open, KeyCode.SPACE);
                } else {
                    open.fire();
                }
                stage.getScene().getRoot().applyCss();
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(feedback, text -> text.equals("Upcoming sessions loaded."));
            onFxThread(() -> {
                var cards = (javafx.scene.layout.VBox) stage.getScene().lookup("#sessions");
                assertEquals(1, cards.getChildren().size());
                var card = (javafx.scene.layout.VBox) cards.getChildren().getFirst();
                Label description = (Label) card.getChildren().get(2);
                assertEquals("Strength and mobility\nBring a towel.", description.getText());
                stage.setWidth(520);
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                writePreview(stage, "upcoming-sessions.png");
                return null;
            });
            context.getPersistence().unitOfWork().inTransaction(connection -> {
                context.getPersistence().sessions().update(connection,
                        new gymmie.testutil.TrainingSessionBuilder(session).withCancelled(true).build());
                return null;
            });
            onFxThread(() -> {
                Button refresh = (Button) stage.getScene().lookup("#refreshButton");
                if (keyboard) {
                    Button back = (Button) stage.getScene().lookup("#backButton");
                    pressKey(back, KeyCode.TAB);
                    assertEquals(refresh, stage.getScene().getFocusOwner());
                    pressKey(refresh, KeyCode.SPACE);
                } else {
                    refresh.fire();
                }
                return null;
            });
            awaitUi(feedback, text -> text.equals("No upcoming sessions."));
            onFxThread(() -> {
                assertTrue(((javafx.scene.layout.VBox) stage.getScene().lookup("#sessions"))
                        .getChildren().isEmpty());
                pressKey((Button) stage.getScene().lookup("#backButton"), KeyCode.SPACE);
                stage.getScene().getRoot().applyCss();
                assertEquals("Trainer dashboard", ((Label) stage.getScene().lookup("#title")).getText());
                return null;
            });
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void trainerSelectsRosterAndRefreshesByMouseOrKeyboard(boolean keyboard) throws Exception {
        AppContext context = new AppContext(temporaryDirectory.resolve("roster-ui.db"));
        var trainer = new gymmie.testutil.AccountBuilder().withId(2).withRole(Role.TRAINER).build();
        var member = new gymmie.testutil.AccountBuilder().withId(3).withUsername("private_login")
                .withDisplayName("Member display name").withRole(Role.MEMBER).build();
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection, trainer);
            context.getPersistence().accounts().insert(connection, member);
            return null;
        });
        context.getAuthService().login(trainer.username(), gymmie.testutil.AccountBuilder.DEFAULT_PASSWORD);
        long sessionId = context.getTrainingSessionService()
                .create(LocalDateTime.now().plusDays(1), 60, 10, "Roster session").id();
        var booking = new gymmie.testutil.BookingBuilder().withSessionId(sessionId).withMemberId(member.id()).build();
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().bookings().insert(connection, booking);
            return null;
        });
        Stage stage = onFxThread(Stage::new);
        try {
            var feedback = onFxThread(() -> {
                new Router(stage, context, new ViewLoader()).showUpcomingSessions();
                stage.show();
                stage.getScene().getRoot().applyCss();
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(feedback, text -> text.equals("Upcoming sessions loaded."));
            var rosterFeedback = onFxThread(() -> {
                Button open = (Button) stage.getScene().lookup("#rosterButton-" + sessionId);
                assertTrue(open.isFocusTraversable());
                if (keyboard) {
                    pressKey((Button) stage.getScene().lookup("#refreshButton"), KeyCode.TAB);
                    assertEquals(open, stage.getScene().getFocusOwner());
                    pressKey(open, KeyCode.SPACE);
                } else {
                    open.fire();
                }
                return ((StatusLabel) stage.getScene().lookup("#rosterStatus-" + sessionId)).textProperty();
            });
            awaitUi(rosterFeedback, text -> text.equals("Members booked: 1"));
            onFxThread(() -> {
                var roster = (javafx.scene.layout.VBox) stage.getScene().lookup("#roster-" + sessionId);
                assertEquals(2, roster.getChildren().size());
                Label name = (Label) roster.getChildren().getLast();
                assertEquals("Member display name", name.getText());
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                writePreview(stage, "session-roster.png");
                return null;
            });
            context.getPersistence().unitOfWork().inTransaction(connection -> {
                context.getPersistence().bookings().update(connection,
                        new gymmie.testutil.BookingBuilder(booking).withStatus(gymmie.model.BookingStatus.CANCELLED)
                                .withCancellationReason(gymmie.model.CancellationReason.MEMBER_CANCELLED_BOOKING)
                                .build());
                return null;
            });
            var refreshed = onFxThread(() -> {
                Button refresh = (Button) stage.getScene().lookup("#rosterButton-" + sessionId);
                if (keyboard) {
                    pressKey(refresh, KeyCode.SPACE);
                } else {
                    refresh.fire();
                }
                return ((StatusLabel) stage.getScene().lookup("#rosterStatus-" + sessionId)).textProperty();
            });
            awaitUi(refreshed, text -> text.equals("No Members booked."));
            onFxThread(() -> {
                var roster = (javafx.scene.layout.VBox) stage.getScene().lookup("#roster-" + sessionId);
                assertEquals(1, roster.getChildren().size());
                return null;
            });
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void trainerEditsSessionByMouseOrKeyboard(boolean keyboard) throws Exception {
        Path database = temporaryDirectory.resolve("edit-session.db");
        AppContext context = new AppContext(database);
        var trainer = new gymmie.testutil.AccountBuilder().withId(2).withRole(Role.TRAINER).build();
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection, trainer);
            return null;
        });
        context.getAuthService().login(trainer.username(), gymmie.testutil.AccountBuilder.DEFAULT_PASSWORD);
        var original = context.getTrainingSessionService().create(LocalDateTime.now().plusDays(2),
                60, 10, "Original");
        Stage stage = onFxThread(Stage::new);
        try {
            var listFeedback = onFxThread(() -> {
                new Router(stage, context, new ViewLoader()).showUpcomingSessions();
                stage.show();
                stage.getScene().getRoot().applyCss();
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(listFeedback, text -> text.equals("Upcoming sessions loaded."));
            var feedback = onFxThread(() -> {
                Button edit = (Button) stage.getScene().lookup("#editButton-" + original.id());
                if (keyboard) {
                    pressKey((Button) stage.getScene().lookup("#rosterButton-" + original.id()), KeyCode.TAB);
                    assertEquals(edit, stage.getScene().getFocusOwner());
                    pressKey(edit, KeyCode.SPACE);
                } else {
                    edit.fire();
                }
                stage.getScene().getRoot().applyCss();
                DatePicker date = (DatePicker) stage.getScene().lookup("#startDate");
                assertEquals(original.startsAt().toLocalDate(), date.getValue());
                assertEquals("60", ((TextField) stage.getScene().lookup("#duration")).getText());
                assertEquals("10", ((TextField) stage.getScene().lookup("#capacity")).getText());
                assertEquals("Original", ((TextArea) stage.getScene().lookup("#description")).getText());
                TextField capacity = (TextField) stage.getScene().lookup("#capacity");
                capacity.setText("0");
                Button save = (Button) stage.getScene().lookup("#saveButton");
                save.fire();
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(feedback, text -> text.startsWith("Error:"));
            onFxThread(() -> {
                assertFalse(stage.getScene().lookup("#form").isDisabled());
                assertEquals("0", ((TextField) stage.getScene().lookup("#capacity")).getText());
                TextField capacity = (TextField) stage.getScene().lookup("#capacity");
                capacity.setText("5");
                TextField duration = (TextField) stage.getScene().lookup("#duration");
                duration.setText("90");
                TextArea description = (TextArea) stage.getScene().lookup("#description");
                description.setText("Corrected");
                Button save = (Button) stage.getScene().lookup("#saveButton");
                if (keyboard) {
                    pressKey(description, KeyCode.TAB);
                    assertEquals(save, stage.getScene().getFocusOwner());
                    pressKey(save, KeyCode.SPACE);
                } else {
                    save.fire();
                }
                assertTrue(stage.getScene().lookup("#form").isDisabled());
                return null;
            });
            awaitUi(feedback, text -> text.equals("Success: Session changes saved."));
            var restarted = new AppContext(database).getPersistence();
            var saved = restarted.unitOfWork().inTransaction(connection ->
                    restarted.sessions().findById(connection, original.id()).orElseThrow());
            assertEquals(original.startsAt(), saved.startsAt());
            assertEquals(90, saved.durationMinutes());
            assertEquals(5, saved.capacity());
            assertEquals("Corrected", saved.description());
            var refreshed = onFxThread(() -> {
                writePreview(stage, "edit-session.png");
                TextArea description = (TextArea) stage.getScene().lookup("#description");
                description.setText("Unsaved");
                pressKey((Button) stage.getScene().lookup("#backButton"), KeyCode.SPACE);
                stage.getScene().getRoot().applyCss();
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(refreshed, text -> text.equals("Upcoming sessions loaded."));
            onFxThread(() -> {
                var cards = (javafx.scene.layout.VBox) stage.getScene().lookup("#sessions");
                var card = (javafx.scene.layout.VBox) cards.getChildren().getFirst();
                assertEquals("Corrected", ((Label) card.getChildren().get(2)).getText());
                return null;
            });
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void trainerReschedulesSessionAndRetainsRosterAfterRestart(boolean keyboard) throws Exception {
        Path database = temporaryDirectory.resolve("reschedule-session.db");
        AppContext context = new AppContext(database);
        var trainer = new gymmie.testutil.AccountBuilder().withId(2).withRole(Role.TRAINER).build();
        var persistence = context.getPersistence();
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, trainer);
            for (int id = 3; id <= 5; id++) {
                persistence.accounts().insert(connection, new gymmie.testutil.AccountBuilder().withId(id)
                        .withUsername("member" + id).withDisplayName("Member " + id).withRole(Role.MEMBER).build());
            }
            return null;
        });
        context.getAuthService().login(trainer.username(), gymmie.testutil.AccountBuilder.DEFAULT_PASSWORD);
        LocalDate date = LocalDate.now().plusDays(1);
        var original = context.getTrainingSessionService().create(date.atTime(9, 0), 60, 10, "Keep these details");
        var bookings = java.util.stream.IntStream.rangeClosed(3, 5).mapToObj(id -> {
            var builder = new gymmie.testutil.BookingBuilder().withId(id).withMemberId(id)
                    .withSessionId(original.id());
            if (id == 5) {
                builder.withStatus(gymmie.model.BookingStatus.CANCELLED)
                        .withCancellationReason(gymmie.model.CancellationReason.MEMBER_CANCELLED_BOOKING);
            }
            return builder.build();
        }).toList();
        persistence.unitOfWork().inTransaction(connection -> {
            for (var booking : bookings) {
                persistence.bookings().insert(connection, booking);
            }
            return null;
        });
        LocalDateTime replacement = date.plusDays(1).atTime(14, 30);
        Stage stage = onFxThread(Stage::new);
        try {
            var listFeedback = onFxThread(() -> {
                new Router(stage, context, new ViewLoader()).showUpcomingSessions();
                stage.show();
                stage.getScene().getRoot().applyCss();
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(listFeedback, text -> text.equals("Upcoming sessions loaded."));
            var feedback = onFxThread(() -> {
                Button edit = (Button) stage.getScene().lookup("#editButton-" + original.id());
                if (keyboard) {
                    pressKey((Button) stage.getScene().lookup("#rosterButton-" + original.id()), KeyCode.TAB);
                    assertEquals(edit, stage.getScene().getFocusOwner());
                    pressKey(edit, KeyCode.SPACE);
                } else {
                    edit.fire();
                }
                stage.getScene().getRoot().applyCss();
                DatePicker picker = (DatePicker) stage.getScene().lookup("#startDate");
                assertEquals(date, picker.getValue());
                selectCalendarDate(picker, replacement.toLocalDate(), keyboard);
                assertEquals(replacement.toLocalDate(), picker.getValue());
                TextField time = (TextField) stage.getScene().lookup("#startTime");
                assertEquals("09:00", time.getText());
                if (keyboard) {
                    pressKey(picker, KeyCode.TAB);
                    assertEquals(time, stage.getScene().getFocusOwner());
                }
                time.setText("14:30");
                if (keyboard) {
                    pressKey(time, KeyCode.ENTER);
                } else {
                    Button save = (Button) stage.getScene().lookup("#saveButton");
                    save.fire();
                }
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(feedback, text -> text.equals("Success: Session changes saved."));
            AppContext restarted = new AppContext(database);
            restarted.getAuthService().login(trainer.username(), gymmie.testutil.AccountBuilder.DEFAULT_PASSWORD);
            var saved = restarted.getTrainingSessionService().getOwnUpcomingSessions();
            assertEquals(List.of(new gymmie.testutil.TrainingSessionBuilder(original)
                    .withStartsAt(replacement).build()), saved);
            assertEquals(bookings, restarted.getPersistence().unitOfWork().inTransaction(connection ->
                    restarted.getPersistence().bookings().findBySessionId(connection, original.id())));
            var reopened = onFxThread(() -> {
                new Router(stage, restarted, new ViewLoader()).showUpcomingSessions();
                stage.getScene().getRoot().applyCss();
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(reopened, text -> text.equals("Upcoming sessions loaded."));
            var rosterFeedback = onFxThread(() -> {
                var cards = (javafx.scene.layout.VBox) stage.getScene().lookup("#sessions");
                assertEquals(1, cards.getChildren().size());
                var card = (javafx.scene.layout.VBox) cards.getChildren().getFirst();
                assertEquals(replacement.toLocalDate() + " 14:30", ((Label) card.getChildren().getFirst()).getText());
                Button roster = (Button) stage.getScene().lookup("#rosterButton-" + original.id());
                roster.fire();
                return ((StatusLabel) stage.getScene().lookup("#rosterStatus-" + original.id())).textProperty();
            });
            awaitUi(rosterFeedback, text -> text.equals("Members booked: 2"));
            onFxThread(() -> {
                var roster = (javafx.scene.layout.VBox) stage.getScene().lookup("#roster-" + original.id());
                assertEquals(3, roster.getChildren().size());
                assertEquals("Member 3", ((Label) roster.getChildren().get(1)).getText());
                assertEquals("Member 4", ((Label) roster.getChildren().get(2)).getText());
                return null;
            });
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    private static void selectCalendarDate(DatePicker picker, LocalDate date, boolean keyboard) {
        if (keyboard) {
            pressKey(picker, KeyCode.F4);
        } else {
            picker.show();
        }
        assertTrue(picker.isShowing());
        DateCell cell = Window.getWindows().stream().filter(Window::isShowing)
                .flatMap(window -> window.getScene().getRoot().lookupAll(".day-cell").stream())
                .filter(DateCell.class::isInstance).map(DateCell.class::cast)
                .filter(candidate -> date.equals(candidate.getItem())).findFirst().orElseThrow();
        if (keyboard) {
            pressKey(cell, KeyCode.ENTER);
        } else {
            cell.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 1, 1, 1, 1, MouseButton.PRIMARY,
                    1, false, false, false, false, false, false, false, false, false, true, null));
        }
        assertFalse(picker.isShowing());
    }

    private static void pressKey(Control control, KeyCode code) {
        control.applyCss();
        control.requestFocus();
        control.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
        control.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, false, false, false, false));
    }

    private static void writePreview(Stage stage, String filename) throws Exception {
        ScrollPane scroll = (ScrollPane) stage.getScene().getRoot();
        WritableImage snapshot = scroll.getContent().snapshot(null, null);
        BufferedImage image = new BufferedImage((int) snapshot.getWidth(), (int) snapshot.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, snapshot.getPixelReader().getArgb(x, y));
            }
        }
        Path destination = Path.of("build", "reports", filename);
        java.nio.file.Files.createDirectories(destination.getParent());
        ImageIO.write(image, "png", destination.toFile());
    }

}
