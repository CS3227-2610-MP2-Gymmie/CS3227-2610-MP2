package gymmie.trainer;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import gymmie.AppContext;
import gymmie.Router;
import gymmie.model.TrainingSession;
import gymmie.ui.StatusLabel;
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
        card.getChildren().addAll(rosterButton, roster);
        card.getStyleClass().add("card");
        return card;
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
