package gymmie;

import static gymmie.testutil.JavaFxTestSupport.awaitUi;
import static gymmie.testutil.JavaFxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import gymmie.model.Role;
import gymmie.testutil.AccountBuilder;
import gymmie.testutil.JavaFxTestSupport;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/** Exercises shared Home display-name editing and its interaction with Trainer profiles. */
@EnabledIfSystemProperty(named = "gymmie.uiTests", matches = "true")
class DashboardDisplayNameTest {
    @TempDir
    Path directory;

    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void everyRoleValidatesAndSavesByButtonAndEnterThenReloads(Role role) throws Exception {
        AppContext context = fixture(role);
        if (role == Role.TRAINER) {
            context.getTrainerProfileService().updateOwnTrainerProfile("Original Trainer", "Saved biography",
                    List.of("Mobility"));
        }
        String original = context.getUserSession().requireUser().displayName();
        Stage stage = onFxThread(Stage::new);
        try {
            var feedback = open(stage, context);
            for (String invalid : new String[]{"", "x".repeat(101)}) {
                onFxThread(() -> {
                    TextField input = (TextField) stage.getScene().lookup("#ownDisplayName");
                    input.setText(invalid);
                    Button save = (Button) stage.getScene().lookup("#saveDisplayNameButton");
                    save.fire();
                    assertTrue(stage.getScene().lookup("#actions").isDisabled());
                    assertTrue(stage.getScene().lookup("#sideTabs").isDisabled());
                    return null;
                });
                awaitUi(feedback, text -> text.startsWith("Error:"));
                onFxThread(() -> {
                    assertEquals(invalid, ((TextField) stage.getScene().lookup("#ownDisplayName")).getText());
                    assertEquals("Welcome, " + original, ((Label) stage.getScene().lookup("#welcome")).getText());
                    assertFalse(stage.getScene().lookup("#actions").isDisabled());
                    return null;
                });
            }
            for (boolean enter : new boolean[]{false, true}) {
                String replacement = enter ? "Updated by Enter" : "Updated by button";
                onFxThread(() -> {
                    TextField input = (TextField) stage.getScene().lookup("#ownDisplayName");
                    input.setText(replacement);
                    if (enter) {
                        input.fireEvent(new ActionEvent());
                    } else {
                        Button save = (Button) stage.getScene().lookup("#saveDisplayNameButton");
                        save.fire();
                    }
                    return null;
                });
                awaitUi(feedback, "Success: Display name changed."::equals);
                onFxThread(() -> {
                    assertEquals("Welcome, " + replacement, ((Label) stage.getScene().lookup("#welcome")).getText());
                    assertTrue(((Label) stage.getScene().lookup("#profileDetails")).getText().startsWith(replacement));
                    assertFalse(stage.getScene().lookup("#sideTabs").isDisabled());
                    return null;
                });
                assertEquals(replacement, context.getUserSession().requireUser().displayName());
            }
            if (role == Role.TRAINER) {
                var profile = context.getTrainerProfileService().getOwnTrainerProfile();
                assertEquals("Updated by Enter", profile.displayName());
                assertEquals("Saved biography", profile.synopsis());
                assertEquals(List.of("Mobility"), profile.specializations());
                var profileFeedback = onFxThread(() -> {
                    Button edit = (Button) stage.getScene().lookup("#profileButton");
                    edit.fire();
                    stage.getScene().getRoot().applyCss();
                    return ((Label) stage.getScene().lookup("#profileStatus")).textProperty();
                });
                awaitUi(profileFeedback, "Profile loaded."::equals);
                onFxThread(() -> {
                    TextField name = (TextField) stage.getScene().lookup("#displayName");
                    assertEquals("Updated by Enter", name.getText());
                    name.setText("Trainer editor name");
                    Button save = (Button) stage.getScene().lookup("#saveProfileButton");
                    save.fire();
                    return null;
                });
                awaitUi(profileFeedback, "Success: Profile saved."::equals);
                onFxThread(() -> {
                    Button back = (Button) stage.getScene().lookup("#backButton");
                    back.fire();
                    stage.getScene().getRoot().applyCss();
                    TextField name = (TextField) stage.getScene().lookup("#ownDisplayName");
                    Label welcome = (Label) stage.getScene().lookup("#welcome");
                    assertEquals("Trainer editor name", name.getText());
                    assertEquals("Welcome, Trainer editor name", welcome.getText());
                    return null;
                });
            }
            AppContext restarted = new AppContext(directory.resolve(role + ".db"));
            login(restarted, role);
            open(stage, restarted);
            onFxThread(() -> {
                TextField name = (TextField) stage.getScene().lookup("#ownDisplayName");
                assertEquals(role == Role.TRAINER ? "Trainer editor name" : "Updated by Enter", name.getText());
                return null;
            });
        } finally {
            close(stage);
        }
    }

    @Test
    void failedSaveKeepsInputAndIdentityThenAllowsRetry() throws Exception {
        AppContext context = fixture(Role.MEMBER);
        String original = context.getUserSession().requireUser().displayName();
        Stage stage = onFxThread(Stage::new);
        try {
            var feedback = open(stage, context);
            execute(context, "CREATE TRIGGER reject_name BEFORE UPDATE ON account "
                    + "BEGIN SELECT RAISE(ABORT, 'private storage failure'); END");
            onFxThread(() -> {
                TextField input = (TextField) stage.getScene().lookup("#ownDisplayName");
                input.setText("Retry this name");
                input.fireEvent(new ActionEvent());
                return null;
            });
            awaitUi(feedback, text -> text.startsWith("Error:"));
            onFxThread(() -> {
                assertEquals("Retry this name", ((TextField) stage.getScene().lookup("#ownDisplayName")).getText());
                assertEquals("Welcome, " + original, ((Label) stage.getScene().lookup("#welcome")).getText());
                assertFalse(feedback.getValue().contains("private storage failure"));
                assertFalse(stage.getScene().lookup("#actions").isDisabled());
                return null;
            });
            execute(context, "DROP TRIGGER reject_name");
            onFxThread(() -> {
                Button save = (Button) stage.getScene().lookup("#saveDisplayNameButton");
                save.fire();
                return null;
            });
            awaitUi(feedback, "Success: Display name changed."::equals);
            assertEquals("Retry this name", context.getUserSession().requireUser().displayName());
        } finally {
            close(stage);
        }
    }

    @Test
    void deactivatedAccountReturnsToLoginInsteadOfSavingName() throws Exception {
        AppContext context = fixture(Role.MEMBER);
        Stage stage = onFxThread(Stage::new);
        try {
            open(stage, context);
            execute(context, "UPDATE account SET active = 0 WHERE id = 2");
            onFxThread(() -> {
                TextField name = (TextField) stage.getScene().lookup("#ownDisplayName");
                name.setText("Cannot save");
                name.fireEvent(new ActionEvent());
                return null;
            });
            awaitUi(stage.titleProperty(), title -> title.endsWith("Log in"));
            assertFalse(context.getUserSession().isAuthenticated());
        } finally {
            close(stage);
        }
    }

    private AppContext fixture(Role role) throws Exception {
        AppContext context = new AppContext(directory.resolve(role + ".db"));
        if (role != Role.MANAGER) {
            context.getPersistence().unitOfWork().inTransaction(connection -> {
                context.getPersistence().accounts().insert(connection, new AccountBuilder().withId(2)
                        .withUsername(role.name().toLowerCase(Locale.ROOT)).withRole(role).build());
                return null;
            });
        }
        login(context, role);
        return context;
    }

    private void login(AppContext context, Role role) throws Exception {
        context.getAuthService().login(role.name().toLowerCase(Locale.ROOT),
                role == Role.MANAGER ? "manager123" : AccountBuilder.DEFAULT_PASSWORD);
    }

    private ObservableValue<String> open(Stage stage, AppContext context) throws Exception {
        return onFxThread(() -> {
            new Router(stage, context, new ViewLoader()).showDashboard();
            stage.show();
            stage.getScene().getRoot().applyCss();
            TextField name = (TextField) stage.getScene().lookup("#ownDisplayName");
            assertEquals(context.getUserSession().requireUser().displayName(), name.getText());
            return ((Label) stage.getScene().lookup("#displayNameStatus")).textProperty();
        });
    }

    private void execute(AppContext context, String sql) throws Exception {
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute(sql);
            }
            return null;
        });
    }

    private void close(Stage stage) throws Exception {
        onFxThread(() -> {
            stage.close();
            return null;
        });
    }
}
