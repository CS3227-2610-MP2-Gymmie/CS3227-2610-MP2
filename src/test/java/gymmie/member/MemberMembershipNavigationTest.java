package gymmie.member;

import static gymmie.testutil.JavaFxTestSupport.awaitUi;
import static gymmie.testutil.JavaFxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;

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
import gymmie.model.Membership;
import gymmie.model.MembershipPlan;
import gymmie.model.MembershipStatus;
import gymmie.model.Role;
import gymmie.service.PasswordHasher;
import gymmie.testutil.JavaFxTestSupport;
import gymmie.ui.DisplayFormatters;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/** Opt-in JavaFX integration checks for Member membership navigation and purchases. */
@EnabledIfSystemProperty(named = "gymmie.uiTests", matches = "true")
class MemberMembershipNavigationTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    void cardShowsArchivedPlanAndRefreshesThroughMouseAndKeyboard() throws Exception {
        LocalDate today = LocalDate.now();
        AppContext context = contextWithMember(temporaryDirectory.resolve("active-membership.db"));
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(1, "Archived monthly", 30, 4990, true));
            context.getPersistence().memberships().insert(connection,
                    new Membership(1, 2, 1, today.minusDays(29), today, MembershipStatus.ACTIVE, 4990, 30));
            return null;
        });
        context.getAuthService().login("member", "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<String> state = onFxThread(() -> {
                openMemberMembershipScreen(stage, context);
                return ((Label) stage.getScene().lookup("#membershipStatus")).textProperty();
            });
            awaitUi(state, "Active"::equals);
            onFxThread(() -> {
                String planText = ((Label) stage.getScene().lookup("#membershipPlan")).getText();
                String expiryText = ((Label) stage.getScene().lookup("#membershipExpiry")).getText();
                assertEquals("Plan: Archived monthly", planText);
                assertEquals("Expiry date: " + DisplayFormatters.date(today), expiryText);
                Button refresh = (Button) stage.getScene().lookup("#refreshMembership");
                assertTrue(refresh.isFocusTraversable());
                context.getPersistence().unitOfWork().inTransaction(connection -> {
                    context.getPersistence().memberships().update(connection,
                            new Membership(1, 2, 1, today.minusDays(29), today, MembershipStatus.CANCELLED,
                                    4990, 30));
                    return null;
                });
                refresh.fire();
                return null;
            });
            awaitUi(state, text -> text.startsWith("Inactive"));
            onFxThread(() -> {
                assertEquals("Plan: —", ((Label) stage.getScene().lookup("#membershipPlan")).getText());
                assertEquals("Expiry date: —", ((Label) stage.getScene().lookup("#membershipExpiry")).getText());
                context.getPersistence().unitOfWork().inTransaction(connection -> {
                    context.getPersistence().memberships().update(connection,
                            new Membership(1, 2, 1, today.minusDays(29), today, MembershipStatus.ACTIVE,
                                    4990, 30));
                    return null;
                });
                pressKey((Button) stage.getScene().lookup("#refreshMembership"), KeyCode.SPACE);
                return null;
            });
            awaitUi(state, "Active"::equals);
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void emptyAndExpiredHistoryShowInactiveWithoutStaleDetails(boolean expiredHistory) throws Exception {
        LocalDate today = LocalDate.now();
        AppContext context = contextWithMember(temporaryDirectory.resolve("inactive-" + expiredHistory + ".db"));
        if (expiredHistory) {
            context.getPersistence().unitOfWork().inTransaction(connection -> {
                context.getPersistence().plans().insert(connection,
                        new MembershipPlan(1, "Past plan", 30, 4990, false));
                context.getPersistence().memberships().insert(connection,
                        new Membership(1, 2, 1, today.minusDays(40), today.minusDays(1),
                                MembershipStatus.ACTIVE, 4990, 30));
                return null;
            });
        }
        context.getAuthService().login("member", "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<String> state = onFxThread(() -> {
                openMemberMembershipScreen(stage, context);
                return ((Label) stage.getScene().lookup("#membershipStatus")).textProperty();
            });
            awaitUi(state, text -> text.startsWith("Inactive"));
            onFxThread(() -> {
                assertEquals("Plan: —", ((Label) stage.getScene().lookup("#membershipPlan")).getText());
                assertEquals("Expiry date: —", ((Label) stage.getScene().lookup("#membershipExpiry")).getText());
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
    void membershipEntryPointIsVisibleOnlyToMembers() throws Exception {
        AppContext context = contextWithMember(temporaryDirectory.resolve("role-visibility.db"));
        var hash = new PasswordHasher().hash("password123");
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection,
                    new Account(3, "trainer", hash, "Trainer", Role.TRAINER, true));
            return null;
        });
        Stage stage = onFxThread(Stage::new);
        try {
            for (String username : new String[]{"manager", "trainer", "member"}) {
                context.getAuthService().login(username,
                        username.equals("manager") ? "manager123" : "password123");
                onFxThread(() -> {
                    new Router(stage, context, new ViewLoader()).showDashboard();
                    stage.show();
                    stage.getScene().getRoot().applyCss();
                    boolean member = context.getUserSession().requireUser().role() == Role.MEMBER;
                    assertEquals(member, stage.getScene().lookup("#memberMembershipButton").isVisible());
                    assertEquals(member, stage.getScene().lookup("#memberMembershipButton").isManaged());
                    return null;
                });
                context.getAuthService().logout();
            }
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test
    void memberCanPurchaseVisiblePlanAndArchivedPlansAreHidden() throws Exception {
        AppContext context = contextWithMember(temporaryDirectory.resolve("purchase-membership.db"));
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(1, "Quarterly", 90, 7499, false));
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(2, "Annual", 365, 24990, false));
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(3, "Archived", 30, 2999, true));
            return null;
        });
        context.getAuthService().login("member", "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<String> purchaseState = onFxThread(() -> {
                openMemberMembershipScreen(stage, context);
                return ((Label) stage.getScene().lookup("#purchaseStatus")).textProperty();
            });
            awaitUi(purchaseState, text -> !text.equals("Loading available plans…"));
            ObservableValue<Boolean> purchaseDisabled = onFxThread(() -> {
                Button purchase = (Button) stage.getScene().lookup("#purchaseMembership");
                return purchase.disableProperty();
            });
            awaitUi(purchaseDisabled, disabled -> !disabled);
            onFxThread(() -> {
                assertEquals("Choose a plan to purchase.", purchaseState.getValue());
                ComboBox<?> choices = (ComboBox<?>) stage.getScene().lookup("#availablePlans");
                assertEquals(2, choices.getItems().size());
                assertEquals("Quarterly", ((MembershipPlan) choices.getItems().getFirst()).name());
                Button purchase = (Button) stage.getScene().lookup("#purchaseMembership");
                assertFalse(purchase.isDisabled());
                purchase.fire();
                choices.getSelectionModel().select(1);
                assertTrue(purchase.isDisabled());
                return null;
            });
            awaitUi(purchaseState, "Success: Membership purchased."::equals);
            onFxThread(() -> {
                Button purchase = (Button) stage.getScene().lookup("#purchaseMembership");
                assertTrue(purchase.isDisabled());
                return null;
            });
            Membership saved = context.getPersistence().unitOfWork().inTransaction(connection ->
                    context.getPersistence().memberships().findByMemberId(connection, 2).getFirst());
            assertEquals(1, saved.planId());
            assertEquals(7499, saved.snapshotPriceCents());
            assertEquals(90, saved.snapshotDurationDays());
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    private AppContext contextWithMember(Path database) throws Exception {
        AppContext context = new AppContext(database);
        var hash = new PasswordHasher().hash("password123");
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().accounts().insert(connection,
                    new Account(2, "member", hash, "Member", Role.MEMBER, true));
            return null;
        });
        return context;
    }

    private static void openMemberMembershipScreen(Stage stage, AppContext context) throws Exception {
        new Router(stage, context, new ViewLoader()).showDashboard();
        stage.show();
        stage.getScene().getRoot().applyCss();
        Button membershipButton = (Button) stage.getScene().lookup("#memberMembershipButton");
        membershipButton.fire();
        stage.getScene().getRoot().applyCss();
    }

    private static void pressKey(Button button, KeyCode code) {
        button.applyCss();
        button.requestFocus();
        button.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
        button.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, false, false, false, false));
    }
}
