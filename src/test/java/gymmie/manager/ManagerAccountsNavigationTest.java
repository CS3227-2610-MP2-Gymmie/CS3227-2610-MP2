package gymmie.manager;

import static gymmie.testutil.JavaFxTestSupport.awaitUi;
import static gymmie.testutil.JavaFxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import gymmie.AppContext;
import gymmie.Router;
import gymmie.ViewLoader;
import gymmie.model.Account;
import gymmie.model.PasswordHash;
import gymmie.model.Role;
import gymmie.testutil.JavaFxTestSupport;
import gymmie.ui.StatusLabel;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Opt-in JavaFX integration checks for Manager account provisioning and
 * lifecycle management navigation.
 */
@EnabledIfSystemProperty(named = "gymmie.uiTests", matches = "true")
class ManagerAccountsNavigationTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    void managerDashboardDisplaysManageAccountsButtonAndRoutesToAccountsView() throws Exception {
        AppContext context = createContext(temporaryDirectory.resolve("manager-nav.db"));
        context.getAuthService().login("manager", "manager123");
        Stage stage = onFxThread(Stage::new);
        Router router = onFxThread(() -> new Router(stage, context, new ViewLoader()));
        try {
            onFxThread(() -> {
                router.showDashboard();
                stage.show();
                stage.getScene().getRoot().applyCss();
                Button manageAccounts = (Button) stage.getScene().lookup("#manageAccountsButton");
                assertNotNull(manageAccounts);
                assertTrue(manageAccounts.isVisible());
                assertTrue(manageAccounts.isManaged());
                manageAccounts.fire();
                return null;
            });
            awaitUi(stage.titleProperty(), title -> title.contains("Manage accounts"));
            onFxThread(() -> {
                assertNotNull(stage.getScene().lookup("#backButton"));
                assertNotNull(stage.getScene().lookup("#refreshButton"));
                assertNotNull(stage.getScene().lookup("#accountUsername"));
                assertNotNull(stage.getScene().lookup("#accountPassword"));
                assertNotNull(stage.getScene().lookup("#accountDisplayName"));
                assertNotNull(stage.getScene().lookup("#accountRole"));
                assertNotNull(stage.getScene().lookup("#saveAccountButton"));
                Button back = (Button) stage.getScene().lookup("#backButton");
                back.fire();
                return null;
            });
            awaitUi(stage.titleProperty(), title -> title.contains("Manager dashboard"));
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test
    void accountsScreenListsAccountsWithActiveAndDeactivatedDistinction() throws Exception {
        AppContext context = createContext(temporaryDirectory.resolve("accounts-list.db"));
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection,
                    new Account(2, "trainer_mike", PasswordHash.fromPassword("password123"),
                            "Mike Trainer", Role.TRAINER, true));
            context.getPersistence().accounts().insert(connection,
                    new Account(3, "member_sarah", PasswordHash.fromPassword("password123"),
                            "Sarah Member", Role.MEMBER, false));
            return null;
        });
        context.getAuthService().login("manager", "manager123");
        Stage stage = onFxThread(Stage::new);
        Router router = onFxThread(() -> new Router(stage, context, new ViewLoader()));
        try {
            ObservableValue<String> statusText = onFxThread(() -> {
                router.showManagerAccounts();
                stage.show();
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(statusText, text -> text.contains("Accounts loaded."));

            onFxThread(() -> {
                VBox accountList = (VBox) stage.getScene().lookup("#accountList");
                // Manager (id=1, seeded), trainer_mike (id=2), member_sarah (id=3)
                assertEquals(3, accountList.getChildren().size());

                VBox activeCard = (VBox) accountList.getChildren().get(1);
                assertFalse(activeCard.getStyleClass().contains("account-card-deactivated"));

                VBox deactivatedCard = (VBox) accountList.getChildren().get(2);
                assertTrue(deactivatedCard.getStyleClass().contains("account-card-deactivated"));
                return null;
            });
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test
    void provisionAccountAddsNewAccountAndRefreshesList() throws Exception {
        AppContext context = createContext(temporaryDirectory.resolve("provision-account.db"));
        context.getAuthService().login("manager", "manager123");
        Stage stage = onFxThread(Stage::new);
        Router router = onFxThread(() -> new Router(stage, context, new ViewLoader()));
        try {
            ObservableValue<String> statusText = onFxThread(() -> {
                router.showManagerAccounts();
                stage.show();
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(statusText, text -> text.contains("Accounts loaded."));

            onFxThread(() -> {
                TextField usernameField = (TextField) stage.getScene().lookup("#accountUsername");
                PasswordField passwordField = (PasswordField) stage.getScene().lookup("#accountPassword");
                TextField displayNameField = (TextField) stage.getScene().lookup("#accountDisplayName");
                @SuppressWarnings("unchecked")
                ComboBox<Role> roleBox = (ComboBox<Role>) stage.getScene().lookup("#accountRole");
                Button saveButton = (Button) stage.getScene().lookup("#saveAccountButton");

                usernameField.setText("trainer_lisa");
                passwordField.setText("password123");
                displayNameField.setText("Lisa Taylor");
                roleBox.setValue(Role.TRAINER);
                saveButton.fire();
                return null;
            });
            awaitUi(statusText, text -> text.contains("Account provisioned."));

            onFxThread(() -> {
                VBox accountList = (VBox) stage.getScene().lookup("#accountList");
                assertEquals(2, accountList.getChildren().size());
                return null;
            });
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test
    void editAccountUpdatesDisplayName() throws Exception {
        AppContext context = createContext(temporaryDirectory.resolve("edit-account.db"));
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection,
                    new Account(2, "trainer_dan", PasswordHash.fromPassword("password123"),
                            "Dan Initial", Role.TRAINER, true));
            return null;
        });
        context.getAuthService().login("manager", "manager123");
        Stage stage = onFxThread(Stage::new);
        Router router = onFxThread(() -> new Router(stage, context, new ViewLoader()));
        try {
            ObservableValue<String> statusText = onFxThread(() -> {
                router.showManagerAccounts();
                stage.show();
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(statusText, text -> text.contains("Accounts loaded."));

            onFxThread(() -> {
                VBox accountList = (VBox) stage.getScene().lookup("#accountList");
                VBox card = (VBox) accountList.getChildren().get(1);
                Button editBtn = (Button) actionsBox(card).getChildren().get(0);
                assertEquals("Edit", editBtn.getText());
                editBtn.fire();

                TextField usernameField = (TextField) stage.getScene().lookup("#accountUsername");
                TextField displayNameField = (TextField) stage.getScene().lookup("#accountDisplayName");
                assertTrue(usernameField.isDisabled());
                assertEquals("trainer_dan", usernameField.getText());
                assertEquals("Dan Initial", displayNameField.getText());

                displayNameField.setText("Dan Updated");
                Button saveButton = (Button) stage.getScene().lookup("#saveAccountButton");
                saveButton.fire();
                return null;
            });
            awaitUi(statusText, text -> text.contains("Account updated."));

            onFxThread(() -> {
                TextField usernameField = (TextField) stage.getScene().lookup("#accountUsername");
                assertFalse(usernameField.isDisabled());
                return null;
            });
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test
    void deactivateAndReactivateAccount() throws Exception {
        AppContext context = createContext(temporaryDirectory.resolve("lifecycle-account.db"));
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection,
                    new Account(2, "member_tom", PasswordHash.fromPassword("password123"),
                            "Tom Member", Role.MEMBER, true));
            return null;
        });
        context.getAuthService().login("manager", "manager123");
        Stage stage = onFxThread(Stage::new);
        Router router = onFxThread(() -> new Router(stage, context, new ViewLoader()));
        try {
            ObservableValue<String> statusText = onFxThread(() -> {
                router.showManagerAccounts();
                stage.show();
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(statusText, text -> text.contains("Accounts loaded."));

            // Deactivate
            onFxThread(() -> {
                VBox accountList = (VBox) stage.getScene().lookup("#accountList");
                VBox card = (VBox) accountList.getChildren().get(1);
                Button deactivateBtn = (Button) actionsBox(card).getChildren().get(1);
                assertEquals("Deactivate", deactivateBtn.getText());
                deactivateBtn.fire();
                return null;
            });
            awaitUi(statusText, text -> text.contains("deactivated"));

            // Card is now deactivated and has Reactivate button
            onFxThread(() -> {
                VBox accountList = (VBox) stage.getScene().lookup("#accountList");
                VBox card = (VBox) accountList.getChildren().get(1);
                assertTrue(card.getStyleClass().contains("account-card-deactivated"));
                Button reactivateBtn = (Button) actionsBox(card).getChildren().get(1);
                assertEquals("Reactivate", reactivateBtn.getText());
                reactivateBtn.fire();
                return null;
            });
            awaitUi(statusText, text -> text.contains("reactivated"));

            onFxThread(() -> {
                VBox accountList = (VBox) stage.getScene().lookup("#accountList");
                VBox card = (VBox) accountList.getChildren().get(1);
                assertFalse(card.getStyleClass().contains("account-card-deactivated"));
                return null;
            });
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    private static HBox actionsBox(VBox card) {
        return (HBox) card.getChildren().get(1);
    }

    private static AppContext createContext(Path path) throws Exception {
        return new AppContext(path);
    }
}
