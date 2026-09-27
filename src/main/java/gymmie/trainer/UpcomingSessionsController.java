package gymmie.trainer;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import gymmie.AppContext;
import gymmie.Router;
import gymmie.model.TrainingSession;
import gymmie.trainer.service.SessionCancellationService.Preview;
import gymmie.ui.DisplayFormatters;
import gymmie.ui.StatusLabel;
import gymmie.ui.UiFeedback;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Displays the authenticated Trainer's upcoming sessions with refreshable local-time filtering. */
public final class UpcomingSessionsController {
    private static final DateTimeFormatter START_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");
    private final AppContext context;
    private final Router router;
    @FXML
    private VBox sessions;
    @FXML
    private Button refreshButton;
    @FXML
    private Button backButton;
    @FXML
    private StatusLabel status;

    /** Creates a view backed by the protected session service. */
    public UpcomingSessionsController(AppContext context, Router router) {
        this.context = context;
        this.router = router;
    }

    @FXML
    private void initialize() {
        refresh();
        Platform.runLater(backButton::requestFocus);
    }

    @FXML
    private void refresh() {
        if (refreshButton.isDisabled()) {
            return;
        }
        sessions.getChildren().clear();
        refreshButton.setDisable(true);
        status.info("Loading upcoming sessions…");
        Task<List<TrainingSession>> task = new Task<>() {
            @Override
            protected List<TrainingSession> call() throws Exception {
                return context.getTrainingSessionService().getOwnUpcomingSessions();
            }
        };
        task.setOnSucceeded(_ -> {
            refreshButton.setDisable(false);
            for (TrainingSession session : task.getValue()) {
                sessions.getChildren().add(sessionCard(session));
            }
            status.info(task.getValue().isEmpty() ? "No upcoming sessions." : "Upcoming sessions loaded.");
        });
        task.setOnFailed(_ -> {
            refreshButton.setDisable(false);
            status.error(task.getException(), "Unable to load upcoming sessions. Please try Refresh again.");
        });
        Thread.ofPlatform().daemon().name("gymmie-upcoming-sessions").start(task);
    }

    private VBox sessionCard(TrainingSession session) {
        Label start = label(START_FORMAT.format(session.startsAt()));
        start.getStyleClass().add("heading");
        Label details = label("Session #" + session.id() + " · " + session.durationMinutes()
                + " minutes · Capacity: " + session.capacity() + " Members");
        String description = session.description();
        VBox card = new VBox(10, start, details,
                label(description == null || description.isBlank() ? "No description." : description));
        Button rosterButton = new Button("View roster");
        rosterButton.setId("rosterButton-" + session.id());
        rosterButton.setAccessibleText("View roster for session " + session.id());
        VBox roster = new VBox(8);
        roster.setId("roster-" + session.id());
        rosterButton.setOnAction(_ -> loadRoster(session.id(), rosterButton, roster));
        Button editButton = new Button("Edit session");
        editButton.setId("editButton-" + session.id());
        editButton.setAccessibleText("Edit session " + session.id());
        editButton.setOnAction(_ -> {
            try {
                router.showEditSession(session);
            } catch (IOException exception) {
                status.error("Unable to open the session editor. Please try again.");
            }
        });
        Button deleteButton = new Button("Delete session");
        deleteButton.setId("deleteButton-" + session.id());
        deleteButton.setAccessibleText("Delete session " + session.id());
        deleteButton.setOnAction(_ -> deleteSession(session, card));
        Button cancelButton = new Button("Cancel session");
        cancelButton.setId("cancelButton-" + session.id());
        cancelButton.setAccessibleText("Cancel session " + session.id());
        cancelButton.setOnAction(_ -> reviewCancellation(session.id(), card));
        card.getChildren().addAll(rosterButton, roster, editButton, deleteButton, cancelButton);
        card.getStyleClass().add("card");
        return card;
    }

    private void setCancellationBusy(boolean busy) {
        sessions.setDisable(busy);
        refreshButton.setDisable(busy);
        backButton.setDisable(busy);
    }

    private void reviewCancellation(long sessionId, VBox card) {
        setCancellationBusy(true);
        status.info("Loading cancellation details…");
        Task<Preview> task = new Task<>() {
            @Override
            protected Preview call() throws Exception {
                return context.getSessionCancellationService().preview(sessionId);
            }
        };
        task.setOnSucceeded(_ -> {
            var preview = task.getValue();
            var reason = SessionCancellationDialog.confirm(backButton.getScene().getWindow(), preview);
            if (reason.isEmpty()) {
                setCancellationBusy(false);
                status.info("Cancellation declined. No changes made.");
                return;
            }
            cancelSession(preview, reason.orElseThrow(), card);
        });
        task.setOnFailed(_ -> {
            setCancellationBusy(false);
            status.error(task.getException(), "Unable to load cancellation details. Please try again.");
        });
        Thread.ofPlatform().daemon().name("gymmie-review-session-cancellation").start(task);
    }

    private void cancelSession(Preview preview,
            String reason, VBox card) {
        status.info("Cancelling session…");
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return context.getSessionCancellationService().cancel(preview, reason);
            }
        };
        task.setOnSucceeded(_ -> {
            sessions.getChildren().remove(card);
            setCancellationBusy(false);
            refreshButton.requestFocus();
            status.success("Session cancelled. Bookings cancelled: " + task.getValue() + ".");
        });
        task.setOnFailed(_ -> {
            setCancellationBusy(false);
            status.error(task.getException(), "Unable to save cancellation. No changes were saved. Please try again.");
        });
        Thread.ofPlatform().daemon().name("gymmie-cancel-session").start(task);
    }

    private void deleteSession(TrainingSession session, VBox card) {
        if (!UiFeedback.confirm(backButton.getScene().getWindow(), "Delete session",
                "Delete session #" + session.id() + " on " + DisplayFormatters.dateTime(session.startsAt())
                        + "? This cannot be undone. Only sessions with no booking history can be deleted.")) {
            return;
        }
        sessions.setDisable(true);
        refreshButton.setDisable(true);
        backButton.setDisable(true);
        status.info("Deleting session…");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                context.getTrainingSessionService().delete(session.id());
                return null;
            }
        };
        task.setOnSucceeded(_ -> {
            sessions.getChildren().remove(card);
            sessions.setDisable(false);
            refreshButton.setDisable(false);
            backButton.setDisable(false);
            refreshButton.requestFocus();
            status.success("Session deleted.");
        });
        task.setOnFailed(_ -> {
            sessions.setDisable(false);
            refreshButton.setDisable(false);
            backButton.setDisable(false);
            status.error(task.getException(), "Unable to delete the session. Please try again.");
        });
        Thread.ofPlatform().daemon().name("gymmie-delete-session").start(task);
    }

    private void loadRoster(long sessionId, Button button, VBox roster) {
        roster.getChildren().clear();
        StatusLabel feedback = new StatusLabel();
        feedback.setId("rosterStatus-" + sessionId);
        feedback.info("Loading roster…");
        roster.getChildren().add(feedback);
        button.setDisable(true);
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                return context.getSessionRosterService().getRoster(sessionId);
            }
        };
        task.setOnSucceeded(_ -> {
            button.setDisable(false);
            button.setText("Refresh roster");
            button.setAccessibleText("Refresh roster for session " + sessionId);
            feedback.info(task.getValue().isEmpty()
                    ? "No Members booked." : "Members booked: " + task.getValue().size());
            for (String name : task.getValue()) {
                roster.getChildren().add(label(name));
            }
        });
        task.setOnFailed(_ -> {
            button.setDisable(false);
            feedback.error(task.getException(), "Unable to load the roster. Please try again.");
        });
        Thread.ofPlatform().daemon().name("gymmie-session-roster").start(task);
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    @FXML
    private void back() {
        try {
            router.showDashboard();
        } catch (IOException exception) {
            status.error("Unable to return to the dashboard. Please try again.");
        }
    }
}
