package gymmie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import gymmie.model.Account;
import gymmie.model.Role;
import gymmie.service.PasswordHasher;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/** Opt-in JavaFX integration checks; requires a graphical desktop. */
@EnabledIfSystemProperty(named = "gymmie.uiTests", matches = "true")
class NavigationTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void startToolkit() throws Exception {
        Platform.startup(() -> Platform.setImplicitExit(false));
        onFxThread(() -> null);
    }

    @Test
    void loginRoutesEveryRoleAndLogoutReturnsToAnEmptyForm() throws Exception {
        AppContext context = new AppContext(temporaryDirectory.resolve("navigation.db"));
        var hash = new PasswordHasher().hash("password123");
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection,
                    new Account(2, "trainer", hash, "Trainer", Role.TRAINER, true));
            context.getPersistence().accounts().insert(connection,
                    new Account(3, "member", hash, "Member", Role.MEMBER, true));
            context.getPersistence().accounts().insert(connection,
                    new Account(4, "inactive", hash, "Inactive", Role.MEMBER, false));
            return null;
        });
        Stage stage = onFxThread(Stage::new);
        Router router = onFxThread(() -> new Router(stage, context, new ViewLoader()));
        try {
            for (String username : new String[]{"manager", "trainer", "member"}) {
                onFxThread(() -> {
                    router.showLogin();
                    stage.show();
                    enterCredentials(stage, username, username.equals("manager") ? "manager123" : "password123");
                    return null;
                });
                awaitUi(stage.titleProperty(), title -> title.endsWith("dashboard"));
                onFxThread(() -> {
                    stage.getScene().getRoot().applyCss();
                    Label title = (Label) stage.getScene().lookup("#title");
                    assertEquals(Router.dashboardTitle(context.getUserSession().requireUser().role()), title.getText());
                    assertNotNull(stage.getScene().lookup("#currentPassword"));
                    Button logout = (Button) stage.getScene().lookup("#logoutButton");
                    logout.fire();
                    stage.getScene().getRoot().applyCss();
                    assertTrue(context.getUserSession().currentUser().isEmpty());
                    assertEquals("", ((PasswordField) stage.getScene().lookup("#password")).getText());
                    return null;
                });
            }
            onFxThread(() -> {
                enterCredentials(stage, "inactive", "password123");
                return null;
            });
            ObservableValue<String> status = onFxThread(() -> {
                Label label = (Label) stage.getScene().lookup("#status");
                return label.textProperty();
            });
            awaitUi(status, text -> text.contains("deactivated"));
            assertTrue(context.getUserSession().currentUser().isEmpty());
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    private static void enterCredentials(Stage stage, String username, String password) {
        stage.getScene().getRoot().applyCss();
        TextField usernameField = (TextField) stage.getScene().lookup("#username");
        usernameField.setText(username);
        PasswordField field = (PasswordField) stage.getScene().lookup("#password");
        field.setText(password);
        field.fireEvent(new javafx.event.ActionEvent());
    }

    private static <T> void awaitUi(ObservableValue<T> observable, Predicate<T> condition) throws Exception {
        CompletableFuture<Void> completed = new CompletableFuture<>();
        ChangeListener<T> listener = (_, _, value) -> {
            if (condition.test(value)) {
                completed.complete(null);
            }
        };
        try {
            onFxThread(() -> {
                observable.addListener(listener);
                if (condition.test(observable.getValue())) {
                    completed.complete(null);
                }
                return null;
            });
            completed.get(15, TimeUnit.SECONDS);
        } finally {
            onFxThread(() -> {
                observable.removeListener(listener);
                return null;
            });
        }
    }

    private static <T> T onFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}
