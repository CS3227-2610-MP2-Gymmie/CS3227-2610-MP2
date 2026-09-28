package gymmie.manager;

import static gymmie.testutil.JavaFxTestSupport.awaitUi;
import static gymmie.testutil.JavaFxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import gymmie.AppContext;
import gymmie.Router;
import gymmie.ViewLoader;
import gymmie.model.Membership;
import gymmie.model.MembershipPlan;
import gymmie.model.MembershipStatus;
import gymmie.testutil.JavaFxTestSupport;
import gymmie.ui.StatusLabel;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Opt-in JavaFX integration checks for Manager membership plan management
 * navigation and interactions.
 */
@EnabledIfSystemProperty(named = "gymmie.uiTests", matches = "true")
class ManagerPlansNavigationTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    void managerDashboardDisplaysManagePlansButtonAndRoutesToPlansView() throws Exception {
        AppContext context = createContext(temporaryDirectory.resolve("manager-nav.db"));
        context.getAuthService().login("manager", "manager123");
        Stage stage = onFxThread(Stage::new);
        Router router = onFxThread(() -> new Router(stage, context, new ViewLoader()));
        try {
            onFxThread(() -> {
                router.showDashboard();
                stage.show();
                stage.getScene().getRoot().applyCss();
                Button managePlans = (Button) stage.getScene().lookup("#managePlansButton");
                assertNotNull(managePlans);
                assertTrue(managePlans.isVisible());
                assertTrue(managePlans.isManaged());
                managePlans.fire();
                return null;
            });
            awaitUi(stage.titleProperty(), title -> title.contains("Membership plans"));
            onFxThread(() -> {
                assertNotNull(stage.getScene().lookup("#backButton"));
                assertNotNull(stage.getScene().lookup("#refreshButton"));
                assertNotNull(stage.getScene().lookup("#planName"));
                assertNotNull(stage.getScene().lookup("#planDuration"));
                assertNotNull(stage.getScene().lookup("#planPrice"));
                assertNotNull(stage.getScene().lookup("#savePlanButton"));
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
    void plansScreenListsPlansWithActiveAndArchivedDistinction() throws Exception {
        AppContext context = createContext(temporaryDirectory.resolve("plans-list.db"));
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(1, "Bronze Monthly membership with a long name for checking the catalogue",
                            30, 4990, false));
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(2, "Silver Annual", 365, 49900, true));
            return null;
        });
        context.getAuthService().login("manager", "manager123");
        Stage stage = onFxThread(Stage::new);
        Router router = onFxThread(() -> new Router(stage, context, new ViewLoader()));
        try {
            ObservableValue<String> statusText = onFxThread(() -> {
                router.showManagerPlans();
                stage.show();
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(statusText, text -> text.contains("Plans loaded."));

            onFxThread(() -> {
                VBox planList = (VBox) stage.getScene().lookup("#planList");
                assertEquals(2, planList.getChildren().size());

                VBox activeCard = (VBox) planList.getChildren().getFirst();
                assertFalse(activeCard.getStyleClass().contains("plan-card-archived"));
                for (int width : new int[]{520, 840, 1440}) {
                    var root = stage.getScene().getRoot();
                    root.resize(width, 760);
                    root.applyCss();
                    root.layout();
                    FlowPane header = (FlowPane) activeCard.getChildren().getFirst();
                    for (var item : header.getChildren()) {
                        assertTrue(item.getBoundsInParent().getMaxX() <= header.getWidth() + 1);
                    }
                    FlowPane actions = actionsBox(activeCard);
                    for (var action : actions.getChildren()) {
                        assertTrue(action.getBoundsInParent().getMaxX() <= actions.getWidth() + 1);
                    }
                }

                VBox archivedCard = (VBox) planList.getChildren().get(1);
                assertTrue(archivedCard.getStyleClass().contains("plan-card-archived"));
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
    void createPlanAddsNewPlanAndRefreshesList() throws Exception {
        AppContext context = createContext(temporaryDirectory.resolve("create-plan.db"));
        context.getAuthService().login("manager", "manager123");
        Stage stage = onFxThread(Stage::new);
        Router router = onFxThread(() -> new Router(stage, context, new ViewLoader()));
        try {
            ObservableValue<String> statusText = onFxThread(() -> {
                router.showManagerPlans();
                stage.show();
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(statusText, text -> text.contains("No membership plans found.") || text.contains("Plans loaded."));

            onFxThread(() -> {
                TextField nameField = (TextField) stage.getScene().lookup("#planName");
                TextField durationField = (TextField) stage.getScene().lookup("#planDuration");
                TextField priceField = (TextField) stage.getScene().lookup("#planPrice");
                assertEquals("e.g. 50 or 49.90", priceField.getPromptText());
                Button saveButton = (Button) stage.getScene().lookup("#savePlanButton");

                nameField.setText("Platinum Pass");
                durationField.setText("90");
                priceField.setText("129.90");
                saveButton.fire();
                return null;
            });
            awaitUi(statusText, text -> text.contains("Plan created."));

            onFxThread(() -> {
                VBox planList = (VBox) stage.getScene().lookup("#planList");
                assertEquals(1, planList.getChildren().size());
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
    void editPlanLoadsExistingValuesAndSavesUpdates() throws Exception {
        AppContext context = createContext(temporaryDirectory.resolve("edit-plan.db"));
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(1, "Initial Plan", 30, 3000, false));
            return null;
        });
        context.getAuthService().login("manager", "manager123");
        Stage stage = onFxThread(Stage::new);
        Router router = onFxThread(() -> new Router(stage, context, new ViewLoader()));
        try {
            ObservableValue<String> statusText = onFxThread(() -> {
                router.showManagerPlans();
                stage.show();
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(statusText, text -> text.contains("Plans loaded."));

            onFxThread(() -> {
                VBox planList = (VBox) stage.getScene().lookup("#planList");
                VBox card = (VBox) planList.getChildren().getFirst();
                Button editBtn = (Button) actionsBox(card).getChildren().getFirst();
                assertEquals("Edit", editBtn.getText());
                editBtn.fire();

                TextField nameField = (TextField) stage.getScene().lookup("#planName");
                TextField durationField = (TextField) stage.getScene().lookup("#planDuration");
                TextField priceField = (TextField) stage.getScene().lookup("#planPrice");
                assertEquals("Initial Plan", nameField.getText());
                assertEquals("30", durationField.getText());
                assertEquals("30.00", priceField.getText());

                nameField.setText("Updated Plan");
                durationField.setText("60");
                priceField.setText("60.00");

                Button saveButton = (Button) stage.getScene().lookup("#savePlanButton");
                saveButton.fire();
                return null;
            });
            awaitUi(statusText, text -> text.contains("Plan updated."));

            onFxThread(() -> {
                List<MembershipPlan> all = context.getPersistence().unitOfWork().inTransaction(
                        connection -> context.getPersistence().plans().findAll(connection));
                assertEquals("Updated Plan", all.getFirst().name());
                assertEquals(60, all.getFirst().durationDays());
                assertEquals(6000, all.getFirst().priceCents());
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
    void archiveRestoreAndDeleteActionsExecuteCorrectly() throws Exception {
        AppContext context = createContext(temporaryDirectory.resolve("actions-plan.db"));
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(1, "Active To Archive", 30, 3000, false));
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(2, "Purchased Plan", 30, 3000, false));
            context.getPersistence().memberships().insert(connection,
                    new Membership(1, 1, 2, java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(30),
                            MembershipStatus.ACTIVE, 3000, 30));
            return null;
        });
        context.getAuthService().login("manager", "manager123");
        Stage stage = onFxThread(Stage::new);
        Router router = onFxThread(() -> new Router(stage, context, new ViewLoader()));
        try {
            ObservableValue<String> statusText = onFxThread(() -> {
                router.showManagerPlans();
                stage.show();
                return ((StatusLabel) stage.getScene().lookup("#status")).textProperty();
            });
            awaitUi(statusText, text -> text.contains("Plans loaded."));

            // Archive plan 1
            onFxThread(() -> {
                VBox planList = (VBox) stage.getScene().lookup("#planList");
                VBox card1 = (VBox) planList.getChildren().getFirst();
                Button archiveBtn = (Button) actionsBox(card1).getChildren().get(1);
                archiveBtn.fire();
                return null;
            });
            awaitUi(statusText, text -> text.contains("archived."));

            // Restore plan 1
            onFxThread(() -> {
                VBox planList = (VBox) stage.getScene().lookup("#planList");
                VBox card1 = (VBox) planList.getChildren().getFirst();
                Button restoreBtn = (Button) actionsBox(card1).getChildren().getFirst();
                assertEquals("Restore", restoreBtn.getText());
                restoreBtn.fire();
                return null;
            });
            awaitUi(statusText, text -> text.contains("restored."));

            // Delete purchased plan 2 -> should be archived instead
            onFxThread(() -> {
                VBox planList = (VBox) stage.getScene().lookup("#planList");
                VBox card2 = (VBox) planList.getChildren().get(1);
                Button deleteBtn = (Button) actionsBox(card2).getChildren().get(2);
                assertEquals("Delete", deleteBtn.getText());
                deleteBtn.fire();
                return null;
            });
            awaitUi(statusText, text -> text.contains("had purchase history and was archived instead."));
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    private AppContext createContext(Path databasePath) throws Exception {
        return new AppContext(databasePath);
    }

    private static FlowPane actionsBox(VBox card) {
        return (FlowPane) card.getChildren().get(2);
    }
}
