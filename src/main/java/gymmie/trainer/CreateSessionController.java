package gymmie.trainer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

import gymmie.AppContext;
import gymmie.Router;
import gymmie.model.TrainingSession;
import gymmie.ui.StatusLabel;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

/** Captures session details with mouse and keyboard accessible controls. */
public final class CreateSessionController {
    private static final DateTimeFormatter START_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    private final AppContext context;
    private final Router router;
    @FXML
    private VBox form;
    @FXML
    private DatePicker startDate;
    @FXML
    private TextField startTime;
    @FXML
    private TextField duration;
    @FXML
    private TextField capacity;
    @FXML
    private TextArea description;
    @FXML
    private Button saveButton;
    @FXML
    private Button backButton;
    @FXML
    private StatusLabel status;

    /** Creates a form backed by the protected session service. */
    public CreateSessionController(AppContext context, Router router) {
        this.context = context;
        this.router = router;
    }

    @FXML
    private void initialize() {
        description.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB && !event.isControlDown()
                    && !event.isAltDown() && !event.isMetaDown()) {
                if (event.isShiftDown()) {
                    capacity.requestFocus();
                } else {
                    saveButton.requestFocus();
                }
                event.consume();
            }
        });
        Platform.runLater(startDate::requestFocus);
    }

    @FXML
    private void save() {
        if (form.isDisabled()) {
            return;
        }
        LocalDateTime start;
        int minutes;
        int places;
        if (startDate.getValue() == null) {
            status.error("Choose a start date from the calendar.");
            startDate.requestFocus();
            return;
        }
        try {
            start = LocalDateTime.of(startDate.getValue(), LocalTime.parse(startTime.getText().strip(), TIME_FORMAT));
        } catch (DateTimeParseException exception) {
            status.error("Enter a valid start time as HH:mm, using 24-hour local time.");
            startTime.requestFocus();
            return;
        }
        try {
            minutes = Integer.parseInt(duration.getText().strip());
            places = Integer.parseInt(capacity.getText().strip());
        } catch (NumberFormatException exception) {
            status.error("Duration and capacity must be whole numbers.");
            return;
        }
        String details = description.getText();
        form.setDisable(true);
        backButton.setDisable(true);
        status.info("Creating session…");
        Task<TrainingSession> task = new Task<>() {
            @Override
            protected TrainingSession call() throws Exception {
                return context.getTrainingSessionService().create(start, minutes, places, details);
            }
        };
        task.setOnSucceeded(_ -> {
            finish();
            startDate.setValue(null);
            startTime.clear();
            duration.clear();
            capacity.clear();
            description.clear();
            status.success("Session created for " + START_FORMAT.format(task.getValue().startsAt()) + ".");
            startDate.requestFocus();
        });
        task.setOnFailed(_ -> {
            finish();
            status.error(task.getException(), "Unable to create the session. Please try again.");
            form.setDisable(!context.getUserSession().isAuthenticated());
        });
        Thread.ofPlatform().daemon().name("gymmie-create-session").start(task);
    }

    private void finish() {
        form.setDisable(false);
        backButton.setDisable(false);
    }

    @FXML
    private void back() {
        try {
            router.showDashboard();
        } catch (IOException exception) {
            status.error("Unable to open the dashboard. Please try again.");
        }
    }
}
