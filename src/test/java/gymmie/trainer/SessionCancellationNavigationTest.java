package gymmie.trainer;

import static gymmie.testutil.JavaFxTestSupport.awaitUi;
import static gymmie.testutil.JavaFxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import gymmie.AppContext;
import gymmie.Router;
import gymmie.ViewLoader;
import gymmie.model.BookingStatus;
import gymmie.model.Role;
import gymmie.testutil.AccountBuilder;
import gymmie.testutil.BookingBuilder;
import gymmie.testutil.JavaFxTestSupport;
import gymmie.testutil.TrainingSessionBuilder;
import gymmie.ui.DisplayFormatters;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Opt-in GUI checks for cancellation confirmation and retained Member history. */
@EnabledIfSystemProperty(named = "gymmie.uiTests", matches = "true")
class SessionCancellationNavigationTest {
    @TempDir
    Path directory;

    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void confirmsOrDeclinesWithPointerEventsAndKeyboardThenShowsMemberReason(boolean keyboard) throws Exception {
        AppContext context = fixture();
        Stage stage = onFxThread(Stage::new);
        try {
            var feedback = showSessions(stage, context);
            var original = context.getTrainingSessionService().getOwnUpcomingSessions().getFirst();
            DialogPane declined = openDialog(stage, keyboard);
            onFxThread(() -> {
                var reason = (TextArea) declined.lookup("#sessionCancellationReason");
                var confirm = (Button) declined.lookup("#confirmSessionCancellation");
                assertTrue(confirm.isDisabled());
                reason.setText("  \n ");
                assertTrue(confirm.isDisabled());
                reason.setText("Trainer is unwell");
                assertFalse(confirm.isDisabled());
                assertTrue(labels(declined).contains("Member name"));
                assertTrue(labels(declined).contains(DisplayFormatters.dateTime(original.startsAt())));
                assertTrue(labels(declined).contains("Current bookings: 1"));
                if (keyboard) {
                    key(reason, KeyCode.ESCAPE);
                } else {
                    click((Button) declined.lookupButton(ButtonType.CANCEL));
                }
                return null;
            });
            awaitUi(feedback, text -> text.equals("Cancellation declined. No changes made."));
            assertEquals(List.of(original), context.getTrainingSessionService().getOwnUpcomingSessions());
            assertEquals(BookingStatus.BOOKED, bookingStatus(context));
            DialogPane accepted = openDialog(stage, keyboard);
            onFxThread(() -> {
                TextArea reason = (TextArea) accepted.lookup("#sessionCancellationReason");
                reason.setText("Trainer is unwell");
                Button confirm = (Button) accepted.lookup("#confirmSessionCancellation");
                if (keyboard) {
                    reason.requestFocus();
                    key(reason, KeyCode.TAB);
                    Control cancel = (Control) accepted.lookupButton(ButtonType.CANCEL);
                    assertEquals(cancel, accepted.getScene().getFocusOwner());
                    key(cancel, KeyCode.TAB);
                    assertEquals(confirm, accepted.getScene().getFocusOwner());
                    key(confirm, KeyCode.SPACE);
                } else {
                    click(confirm);
                }
                return null;
            });
            awaitUi(feedback, text -> text.equals("Success: Session cancelled. Bookings cancelled: 1."));
            assertTrue(context.getTrainingSessionService().getOwnUpcomingSessions().isEmpty());
            AppContext restarted = new AppContext(directory.resolve("gui-cancel.db"));
            restarted.getAuthService().login("member", AccountBuilder.DEFAULT_PASSWORD);
            var memberFeedback = onFxThread(() -> {
                new Router(stage, restarted, new ViewLoader()).showMemberBookings();
                stage.getScene().getRoot().applyCss();
                return ((Label) stage.getScene().lookup("#bookingsStatus")).textProperty();
            });
            awaitUi(memberFeedback, "1 booking"::equals);
            onFxThread(() -> {
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                String text = labels(stage.getScene().getRoot());
                assertTrue(text.contains("Cancelled · Reason: Trainer cancelled session — Trainer is unwell"));
                return null;
            });
        } finally {
            closeWindows();
        }
    }

    @Test
    void failedWriteReportsFailureAndKeepsSessionAndBookings() throws Exception {
        AppContext context = fixture();
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TRIGGER fail_booking BEFORE UPDATE ON booking "
                        + "BEGIN SELECT RAISE(ABORT, 'private persistence detail'); END");
            }
            return null;
        });
        Stage stage = onFxThread(Stage::new);
        try {
            var feedback = showSessions(stage, context);
            var dialog = openDialog(stage, false);
            onFxThread(() -> {
                TextArea reason = (TextArea) dialog.lookup("#sessionCancellationReason");
                reason.setText("Trainer is unwell");
                click((Button) dialog.lookup("#confirmSessionCancellation"));
                return null;
            });
            awaitUi(feedback, text -> text.startsWith("Error: Unable to save cancellation."));
            assertEquals(1, context.getTrainingSessionService().getOwnUpcomingSessions().size());
            assertEquals(BookingStatus.BOOKED, bookingStatus(context));
            onFxThread(() -> {
                assertFalse(stage.getScene().lookup("#cancelButton-1").isDisabled());
                assertFalse(feedback.getValue().contains("private persistence detail"));
                return null;
            });
        } finally {
            closeWindows();
        }
    }

    private AppContext fixture() throws Exception {
        AppContext context = new AppContext(directory.resolve("gui-cancel.db"));
        var persistence = context.getPersistence();
        persistence.unitOfWork().inTransaction(connection -> {
            persistence.accounts().insert(connection, new AccountBuilder().withId(2).withUsername("trainer")
                    .withRole(Role.TRAINER).build());
            persistence.accounts().insert(connection, new AccountBuilder().withId(3).withUsername("member")
                    .withDisplayName("Member name").withRole(Role.MEMBER).build());
            persistence.sessions().insert(connection, new TrainingSessionBuilder()
                    .withStartsAt(LocalDateTime.now().plusDays(2)).build());
            persistence.bookings().insert(connection, new BookingBuilder().withMemberId(3).build());
            return null;
        });
        context.getAuthService().login("trainer", AccountBuilder.DEFAULT_PASSWORD);
        return context;
    }

    private ObservableValue<String> showSessions(Stage stage, AppContext context) throws Exception {
        var feedback = onFxThread(() -> {
            new Router(stage, context, new ViewLoader()).showUpcomingSessions();
            stage.show();
            stage.getScene().getRoot().applyCss();
            return ((Label) stage.getScene().lookup("#status")).textProperty();
        });
        awaitUi(feedback, "Upcoming sessions loaded."::equals);
        return feedback;
    }

    private DialogPane openDialog(Stage stage, boolean keyboard) throws Exception {
        CompletableFuture<DialogPane> shown = new CompletableFuture<>();
        ListChangeListener<Window> listener = _ -> Platform.runLater(() -> {
            for (Window window : List.copyOf(Window.getWindows())) {
                if (window.getScene().getRoot() instanceof DialogPane pane
                        && pane.lookup("#sessionCancellationReason") != null) {
                    shown.complete(pane);
                }
            }
        });
        try {
            onFxThread(() -> {
                Window.getWindows().addListener(listener);
                Button cancel = (Button) stage.getScene().lookup("#cancelButton-1");
                assertTrue(cancel.isFocusTraversable());
                if (keyboard) {
                    Button delete = (Button) stage.getScene().lookup("#deleteButton-1");
                    delete.requestFocus();
                    key(delete, KeyCode.TAB);
                    assertEquals(cancel, stage.getScene().getFocusOwner());
                    key(cancel, KeyCode.SPACE);
                } else {
                    click(cancel);
                }
                return null;
            });
            return shown.get(15, TimeUnit.SECONDS);
        } finally {
            onFxThread(() -> {
                Window.getWindows().removeListener(listener);
                return null;
            });
        }
    }

    private BookingStatus bookingStatus(AppContext context) throws Exception {
        return context.getPersistence().unitOfWork().inTransaction(connection ->
                context.getPersistence().bookings().findById(connection, 1).orElseThrow().status());
    }

    private static String labels(javafx.scene.Parent parent) {
        return parent.lookupAll(".label").stream().filter(Label.class::isInstance).map(Label.class::cast)
                .map(Label::getText).collect(java.util.stream.Collectors.joining("\n"));
    }

    private static void key(Control target, KeyCode code) {
        target.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
        target.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, false, false, false, false));
    }

    private static void click(Button button) {
        button.applyCss();
        for (var type : List.of(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED)) {
            button.fireEvent(new MouseEvent(type, 1, 1, 1, 1, MouseButton.PRIMARY, 1,
                    false, false, false, false, type == MouseEvent.MOUSE_PRESSED,
                    false, false, false, false, true, new PickResult(button, 1, 1)));
        }
    }

    private static void closeWindows() throws Exception {
        onFxThread(() -> {
            List.copyOf(Window.getWindows()).forEach(Window::hide);
            return null;
        });
    }
}
