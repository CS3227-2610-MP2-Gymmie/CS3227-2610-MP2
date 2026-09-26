package gymmie.member;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gymmie.AppContext;
import gymmie.Router;
import gymmie.member.service.MemberSessionBrowseService.BrowseSession;
import gymmie.ui.DisplayFormatters;
import gymmie.ui.StatusLabel;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Lets Members browse upcoming sessions and filter them by Trainer. */
public final class MemberSessionBrowseController {
    private final AppContext context;
    private final Router router;
    private List<BrowseSession> loadedSessions = List.of();
    private boolean bookingInProgress;
    private boolean showBookingConfirmation;
    @FXML
    private VBox sessionScreen;
    @FXML
    private ComboBox<TrainerChoice> trainerFilter;
    @FXML
    private VBox sessionCards;
    @FXML
    private StatusLabel sessionStatus;
    @FXML
    private Button refreshButton;

    /** Creates the Member session browser with shared application services and navigation. */
    public MemberSessionBrowseController(AppContext context, Router router) {
        this.context = context;
        this.router = router;
    }

    @FXML
    private void initialize() {
        trainerFilter.setOnAction(_ -> showSessions());
        refreshSessions();
    }

    @FXML
    private void refreshSessions() {
        if (refreshButton.isDisabled()) {
            return;
        }
        refreshButton.setDisable(true);
        trainerFilter.setDisable(true);
        loadedSessions = List.of();
        sessionCards.getChildren().clear();
        sessionStatus.info("Loading upcoming sessions…");
        Task<List<BrowseSession>> task = new Task<>() {
            @Override
            protected List<BrowseSession> call() throws Exception {
                return context.getMemberSessionBrowseService().upcomingSessions();
            }
        };
        task.setOnSucceeded(_ -> {
            loadedSessions = task.getValue();
            configureTrainerChoices();
            trainerFilter.setDisable(false);
            refreshButton.setDisable(false);
            showSessions();
            if (showBookingConfirmation) {
                showBookingConfirmation = false;
                sessionStatus.success("Session booked. See My membership → Booking history.");
            }
        });
        task.setOnFailed(_ -> {
            refreshButton.setDisable(false);
            sessionCards.getChildren().clear();
            sessionStatus.error(task.getException(), "Unable to load sessions. Choose Refresh to try again.");
            returnToLoginIfSessionEnded();
        });
        Thread.ofPlatform().daemon().name("gymmie-member-sessions").start(task);
    }

    private void configureTrainerChoices() {
        TrainerChoice selected = trainerFilter.getValue();
        List<TrainerChoice> trainers = loadedSessions.stream()
                .map(session -> new TrainerChoice(session.trainerId(), session.trainerName()))
                .distinct()
                .sorted(java.util.Comparator.comparing(TrainerChoice::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingLong(TrainerChoice::trainerId))
                .toList();
        List<TrainerChoice> choices = new ArrayList<>();
        choices.add(TrainerChoice.all());
        choices.addAll(trainers);
        trainerFilter.getItems().setAll(choices);
        trainerFilter.setButtonCell(new TrainerChoiceCell());
        trainerFilter.setCellFactory(_ -> new TrainerChoiceCell());
        if (selected != null && choices.contains(selected)) {
            trainerFilter.setValue(selected);
        } else {
            trainerFilter.setValue(TrainerChoice.all());
        }
    }

    private void showSessions() {
        sessionCards.getChildren().clear();
        TrainerChoice selected = trainerFilter.getValue();
        List<BrowseSession> visibleSessions = selected == null || selected.allTrainers()
                ? loadedSessions
                : loadedSessions.stream().filter(session -> session.trainerId() == selected.trainerId()).toList();
        if (visibleSessions.isEmpty()) {
            sessionStatus.info(loadedSessions.isEmpty() ? "No upcoming sessions are available."
                    : "No upcoming sessions for " + selected.name() + ".");
            return;
        }
        for (BrowseSession session : visibleSessions) {
            sessionCards.getChildren().add(sessionCard(session));
        }
        sessionStatus.info(visibleSessions.size() + " upcoming session"
                + (visibleSessions.size() == 1 ? "" : "s"));
    }

    private VBox sessionCard(BrowseSession session) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        Label trainer = new Label(session.trainerName());
        trainer.getStyleClass().add("heading");
        Label start = new Label(DisplayFormatters.dateTime(session.startsAt()));
        start.getStyleClass().add("session-start");
        Label details = new Label(session.durationMinutes() + " minutes · " + session.bookingCount()
                + " of " + session.capacity() + " Members booked");
        details.setWrapText(true);
        String description = session.description();
        Label descriptionLabel = new Label(description == null || description.isBlank()
                ? "No description provided." : description);
        descriptionLabel.setWrapText(true);
        card.getChildren().addAll(trainer, start, details, descriptionLabel);
        Button bookingButton = new Button(session.hasBooking() ? "Already booked" : "Book session");
        bookingButton.setDisable(session.hasBooking());
        bookingButton.setAccessibleText(session.hasBooking() ? "Session already booked" : "Book this session");
        bookingButton.setOnAction(_ -> bookSession(session, bookingButton));
        card.getChildren().add(bookingButton);
        return card;
    }

    private void bookSession(BrowseSession session, Button bookingButton) {
        if (bookingInProgress || bookingButton.isDisabled()) {
            return;
        }
        bookingInProgress = true;
        sessionCards.setDisable(true);
        refreshButton.setDisable(true);
        trainerFilter.setDisable(true);
        bookingButton.setDisable(true);
        sessionStatus.info("Booking session…");
        Task<gymmie.model.Booking> task = new Task<>() {
            @Override
            protected gymmie.model.Booking call() throws Exception {
                return context.getMemberSessionBookingService().book(session.sessionId());
            }
        };
        task.setOnSucceeded(_ -> {
            bookingInProgress = false;
            showBookingConfirmation = true;
            sessionCards.setDisable(false);
            refreshButton.setDisable(false);
            trainerFilter.setDisable(false);
            refreshSessions();
        });
        task.setOnFailed(_ -> {
            bookingInProgress = false;
            sessionCards.setDisable(false);
            refreshButton.setDisable(false);
            trainerFilter.setDisable(false);
            bookingButton.setDisable(false);
            sessionStatus.error(task.getException(), "Unable to book this session. Please try again.");
            returnToLoginIfSessionEnded();
        });
        Thread.ofPlatform().daemon().name("gymmie-member-session-booking").start(task);
    }

    private void returnToLoginIfSessionEnded() {
        if (!context.getUserSession().isAuthenticated()) {
            try {
                router.showLogin();
            } catch (IOException exception) {
                context.getAuthService().logout();
                sessionScreen.setDisable(true);
            }
        }
    }

    @FXML
    private void back() {
        try {
            router.showDashboard();
        } catch (IOException exception) {
            sessionStatus.error("Unable to open the dashboard. Please try again.");
        }
    }

    private record TrainerChoice(long trainerId, String name, boolean allTrainers) {
        private TrainerChoice(long trainerId, String name) {
            this(trainerId, name, false);
        }

        private static TrainerChoice all() {
            return new TrainerChoice(0, "All trainers", true);
        }

        @Override
        public String toString() {
            return allTrainers ? name : name + " · Trainer #" + trainerId;
        }
    }

    private static final class TrainerChoiceCell extends javafx.scene.control.ListCell<TrainerChoice> {
        @Override
        protected void updateItem(TrainerChoice choice, boolean empty) {
            super.updateItem(choice, empty);
            setText(empty || choice == null ? null : choice.toString());
        }
    }
}
