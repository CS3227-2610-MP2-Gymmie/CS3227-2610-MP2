package gymmie.trainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import gymmie.AppContext;
import gymmie.Router;
import gymmie.trainer.service.TrainerProfileService.TrainerView;
import gymmie.ui.StatusLabel;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/** Edits the current Trainer's profile with keyboard-accessible tag controls. */
public final class TrainerProfileController {
    private final AppContext context;
    private final Router router;
    private final List<String> specializations = new ArrayList<>();
    private boolean loaded;
    private boolean busy;
    @FXML
    private VBox form;
    @FXML
    private TextField username;
    @FXML
    private TextField displayName;
    @FXML
    private TextArea synopsis;
    @FXML
    private TextField tagInput;
    @FXML
    private FlowPane tags;
    @FXML
    private Button backButton;
    @FXML
    private Button reloadButton;
    @FXML
    private StatusLabel profileStatus;

    /** Creates an editor backed by the shared profile service. */
    public TrainerProfileController(AppContext context, Router router) {
        this.context = context;
        this.router = router;
    }

    @FXML
    private void initialize() {
        synopsis.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB && !event.isControlDown()
                    && !event.isAltDown() && !event.isMetaDown()) {
                if (event.isShiftDown()) {
                    displayName.requestFocus();
                } else if (tags.getChildren().isEmpty()) {
                    tagInput.requestFocus();
                } else {
                    tags.getChildren().getFirst().requestFocus();
                }
                event.consume();
            }
        });
        load();
    }

    @FXML
    private void load() {
        if (busy) {
            return;
        }
        run("Loading profile…", () -> context.getTrainerProfileService().getOwnTrainerProfile(), profile -> {
            show(profile);
            loaded = true;
            profileStatus.info("Profile loaded.");
            displayName.requestFocus();
        });
    }

    @FXML
    private void save() {
        if (busy || !loaded) {
            return;
        }
        if (!tagInput.getText().isBlank()) {
            addTag();
        }
        String name = displayName.getText();
        String biography = synopsis.getText();
        List<String> selectedTags = List.copyOf(specializations);
        run("Saving profile…", () -> context.getTrainerProfileService()
                .updateOwnTrainerProfile(name, biography, selectedTags), profile -> {
                    show(profile);
                    profileStatus.success("Profile saved.");
                });
    }

    @FXML
    private void addTag() {
        String value = tagInput.getText().strip();
        if (value.isEmpty()) {
            profileStatus.error("Enter a specialization tag.");
            tagInput.requestFocus();
            return;
        }
        if (specializations.stream().noneMatch(tag -> tag.equalsIgnoreCase(value))) {
            specializations.add(value);
            renderTags();
        }
        tagInput.clear();
        tagInput.requestFocus();
    }

    private void show(TrainerView profile) {
        username.setText(profile.username());
        displayName.setText(profile.displayName());
        synopsis.setText(profile.synopsis());
        specializations.clear();
        specializations.addAll(profile.specializations());
        tagInput.clear();
        renderTags();
    }

    private void renderTags() {
        tags.getChildren().clear();
        for (String tag : specializations) {
            Button remove = new Button(tag + " ×");
            remove.setMnemonicParsing(false);
            remove.setWrapText(true);
            remove.maxWidthProperty().bind(tags.widthProperty());
            remove.getStyleClass().add("tag-button");
            remove.setAccessibleText("Remove specialization " + tag);
            remove.setOnAction(_ -> {
                specializations.remove(tag);
                renderTags();
                tagInput.requestFocus();
            });
            tags.getChildren().add(remove);
        }
    }

    private void run(String message, Callable<TrainerView> operation, Consumer<TrainerView> success) {
        busy = true;
        form.setDisable(true);
        backButton.setDisable(true);
        reloadButton.setDisable(true);
        profileStatus.info(message);
        Task<TrainerView> task = new Task<>() {
            @Override
            protected TrainerView call() throws Exception {
                return operation.call();
            }
        };
        task.setOnSucceeded(_ -> {
            finish();
            success.accept(task.getValue());
            form.setDisable(!loaded);
            displayName.requestFocus();
        });
        task.setOnFailed(_ -> {
            finish();
            profileStatus.error(task.getException(), "Unable to save or load your profile. Please try again.");
            if (!context.getUserSession().isAuthenticated()) {
                form.setDisable(true);
            }
        });
        Thread.ofPlatform().daemon().name("gymmie-trainer-profile").start(task);
    }

    private void finish() {
        busy = false;
        form.setDisable(!loaded);
        backButton.setDisable(false);
        reloadButton.setDisable(false);
    }

    @FXML
    private void back() {
        try {
            router.showDashboard();
        } catch (IOException exception) {
            profileStatus.error("Unable to open the dashboard. Please try again.");
        }
    }
}
