package gymmie.trainer;

import static gymmie.testutil.JavaFxTestSupport.awaitUi;
import static gymmie.testutil.JavaFxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
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
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

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
                writeProfilePreview(stage);
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

    private static void pressKey(Control control, KeyCode code) {
        control.applyCss();
        control.requestFocus();
        control.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
        control.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, false, false, false, false));
    }

    private static void writeProfilePreview(Stage stage) throws Exception {
        ScrollPane scroll = (ScrollPane) stage.getScene().getRoot();
        WritableImage snapshot = scroll.getContent().snapshot(null, null);
        BufferedImage image = new BufferedImage((int) snapshot.getWidth(), (int) snapshot.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, snapshot.getPixelReader().getArgb(x, y));
            }
        }
        Path destination = Path.of("build", "reports", "trainer-profile.png");
        java.nio.file.Files.createDirectories(destination.getParent());
        ImageIO.write(image, "png", destination.toFile());
    }

}
