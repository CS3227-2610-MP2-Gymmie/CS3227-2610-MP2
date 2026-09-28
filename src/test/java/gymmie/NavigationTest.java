package gymmie;

import static gymmie.testutil.JavaFxTestSupport.awaitUi;
import static gymmie.testutil.JavaFxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import gymmie.model.Account;
import gymmie.model.Role;
import gymmie.model.exception.ValidationException;
import gymmie.service.PasswordHasher;
import gymmie.testutil.JavaFxTestSupport;
import gymmie.ui.SharedStyles;
import gymmie.ui.StatusLabel;
import gymmie.ui.UiFeedback;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
        JavaFxTestSupport.startToolkit();
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
                    assertEquals(1, stage.getScene().getRoot().getStylesheets().size());
                    assertTrue(stage.getScene().getRoot().getStylesheets().getFirst()
                            .endsWith("/gymmie/css/gymmie.css"));
                    assertInstanceOf(StatusLabel.class, stage.getScene().lookup("#status"));
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

    @Test
    void alertsAndInlineFeedbackShareServiceMessagesAndTheme() throws Exception {
        onFxThread(() -> {
            StatusLabel status = new StatusLabel();
            Scene scene = new Scene(status);
            SharedStyles.apply(scene);
            SharedStyles.apply(scene);
            Stage owner = new Stage();
            owner.setScene(scene);
            ValidationException failure = new ValidationException("Choose a future date");
            status.error(failure, "Unable to save.");
            Alert alert = UiFeedback.errorAlert(owner, "Unable to save", failure, "Unable to save.");
            assertEquals("Error: " + alert.getContentText(), status.getText());
            assertEquals(owner, alert.getOwner());
            assertEquals(scene.getStylesheets(), alert.getDialogPane().getStylesheets());
            assertEquals(1, scene.getStylesheets().size());
            status.success("Saved.");
            assertTrue(status.getPseudoClassStates().contains(javafx.css.PseudoClass.getPseudoClass("success")));
            assertFalse(status.getPseudoClassStates().contains(javafx.css.PseudoClass.getPseudoClass("error")));
            owner.close();
            return null;
        });
    }

    @Test
    void sideTabsKeepRoleNavigationAvailableAndPagesFitSmallAndLargeWindows() throws Exception {
        AppContext context = new AppContext(temporaryDirectory.resolve("responsive.db"));
        var hash = new PasswordHasher().hash("password123");
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection,
                    new Account(2, "trainer", hash, "Trainer", Role.TRAINER, true));
            context.getPersistence().accounts().insert(connection,
                    new Account(3, "member", hash, "Member", Role.MEMBER, true));
            return null;
        });
        Stage stage = onFxThread(Stage::new);
        try {
            for (String username : new String[]{"manager", "trainer", "member"}) {
                context.getAuthService().login(username, username.equals("manager") ? "manager123" : "password123");
                onFxThread(() -> {
                    Router router = new Router(stage, context, new ViewLoader());
                    router.showDashboard();
                    stage.show();
                    var root = stage.getScene().getRoot();
                    root.applyCss();
                    Label profile = (Label) root.lookup("#profileDetails");
                    assertTrue(profile.getText().contains("@" + username));
                    assertNotNull(root.lookup("#currentPassword"));
                    var tabs = (javafx.scene.layout.VBox) root.lookup("#sideTabs");
                    var destinations = tabs.getChildren().stream().filter(Button.class::isInstance)
                            .map(javafx.scene.Node::getId).filter(id -> !id.equals("logoutButton")).toList();
                    assertEquals(username.equals("member") ? 4 : 3, destinations.size());
                    for (String destination : destinations) {
                        Button navigate = (Button) stage.getScene().lookup("#" + destination);
                        navigate.fire();
                        root = stage.getScene().getRoot();
                        for (int width : new int[]{520, 840, 1440}) {
                            root.resize(width, 760);
                            root.applyCss();
                            root.layout();
                            var scroll = (javafx.scene.control.ScrollPane) root.lookup("#pageScroll");
                            assertTrue(scroll.getViewportBounds().getWidth() > 200);
                            assertTrue(scroll.getContent().getLayoutBounds().getWidth()
                                    <= scroll.getViewportBounds().getWidth() + 1,
                                    username + " " + destination + " overflows at " + width);
                            Button active = (Button) root.lookup("#" + destination);
                            assertTrue(active.getPseudoClassStates().contains(
                                    javafx.css.PseudoClass.getPseudoClass("selected")));
                            assertTrue(active.localToScene(active.getBoundsInLocal()).getMaxX()
                                    <= scroll.localToScene(scroll.getBoundsInLocal()).getMinX());
                        }
                    }
                    Button home = (Button) root.lookup("#homeButton");
                    home.fire();
                    root = stage.getScene().getRoot();
                    root.applyCss();
                    Label helper = (Label) root.lookup(".helper");
                    Label heading = (Label) root.lookup(".heading");
                    assertNotEquals(heading.getTextFill(), helper.getTextFill());
                    return null;
                });
            }
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test
    void dropdownSelectionsUseNeutralBackgroundsWithReadableText() throws Exception {
        onFxThread(() -> {
            var roles = new javafx.scene.control.ComboBox<Role>();
            roles.getItems().setAll(Role.TRAINER, Role.MEMBER);
            Stage stage = new Stage();
            stage.setScene(new Scene(new javafx.scene.layout.VBox(roles), 400, 200));
            SharedStyles.apply(stage.getScene());
            try {
                stage.show();
                for (Role role : roles.getItems()) {
                    roles.getSelectionModel().select(role);
                    stage.getScene().getRoot().applyCss();
                    stage.getScene().getRoot().layout();
                    var skin = (javafx.scene.control.skin.ComboBoxListViewSkin<?>) roles.getSkin();
                    var closedCell = (javafx.scene.control.ListCell<?>) skin.getDisplayNode();
                    assertEquals(role.name(), closedCell.getText());
                    assertEquals(javafx.scene.paint.Color.TRANSPARENT,
                            closedCell.getBackground().getFills().getFirst().getFill());
                    assertEquals(javafx.scene.paint.Color.web("#243b2f"), closedCell.getTextFill());
                    roles.show();
                    var popup = javafx.stage.Window.getWindows().stream()
                            .filter(window -> window != stage && window.isShowing()).findFirst().orElseThrow();
                    popup.getScene().getRoot().applyCss();
                    popup.getScene().getRoot().layout();
                    var selected = popup.getScene().getRoot().lookupAll(".list-cell").stream()
                            .filter(javafx.scene.control.ListCell.class::isInstance)
                            .map(node -> (javafx.scene.control.ListCell<?>) node)
                            .filter(javafx.scene.control.ListCell::isSelected).findFirst().orElseThrow();
                    assertEquals(javafx.scene.paint.Color.web("#e2e7ed"),
                            selected.getBackground().getFills().getFirst().getFill());
                    assertEquals(javafx.scene.paint.Color.web("#243b2f"), selected.getTextFill());
                    roles.hide();
                }
            } finally {
                roles.hide();
                stage.close();
            }
            return null;
        });
    }

    private static void enterCredentials(Stage stage, String username, String password) {
        stage.getScene().getRoot().applyCss();
        TextField usernameField = (TextField) stage.getScene().lookup("#username");
        usernameField.setText(username);
        PasswordField field = (PasswordField) stage.getScene().lookup("#password");
        field.setText(password);
        field.fireEvent(new javafx.event.ActionEvent());
    }

}
