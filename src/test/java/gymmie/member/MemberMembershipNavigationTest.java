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
    void emptyHistoryIsInactiveAndExpiredHistoryCanBeRenewed(boolean expiredHistory) throws Exception {
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
            awaitUi(state, text -> expiredHistory ? text.equals("Expired") : text.startsWith("Inactive"));
            onFxThread(() -> {
                if (expiredHistory) {
                    Label plan = (Label) stage.getScene().lookup("#membershipPlan");
                    Label expiry = (Label) stage.getScene().lookup("#membershipExpiry");
                    assertEquals("Plan: Past plan", plan.getText());
                    assertEquals("Expiry date: " + DisplayFormatters.date(today.minusDays(1)), expiry.getText());
                    assertFalse(((Button) stage.getScene().lookup("#renewMembership")).isDisabled());
                } else {
                    assertEquals("Plan: —", ((Label) stage.getScene().lookup("#membershipPlan")).getText());
                    assertEquals("Expiry date: —", ((Label) stage.getScene().lookup("#membershipExpiry")).getText());
                    assertTrue(((Button) stage.getScene().lookup("#renewMembership")).isDisabled());
                }
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

    @Test
    void memberCanRenewCurrentMembershipFromItsArchivedPlan() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate expiry = today.minusDays(1);
        AppContext context = contextWithMember(temporaryDirectory.resolve("renew-membership.db"));
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(1, "Archived monthly", 90, 9900, true));
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(2, "Available monthly", 30, 4990, false));
            context.getPersistence().memberships().insert(connection,
                    new Membership(1, 2, 1, today.minusDays(31), expiry, MembershipStatus.ACTIVE, 4990, 30));
            return null;
        });
        context.getAuthService().login("member", "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<Boolean> renewalDisabled = onFxThread(() -> {
                openMemberMembershipScreen(stage, context);
                return ((Button) stage.getScene().lookup("#renewMembership")).disableProperty();
            });
            awaitUi(renewalDisabled, disabled -> !disabled);
            ObservableValue<Boolean> purchaseDisabled = onFxThread(() -> {
                Button purchase = (Button) stage.getScene().lookup("#purchaseMembership");
                return purchase.disableProperty();
            });
            awaitUi(purchaseDisabled, disabled -> !disabled);
            ObservableValue<String> renewalStatus = onFxThread(() -> ((Label) stage.getScene()
                    .lookup("#renewalStatus")).textProperty());
            onFxThread(() -> {
                Label plan = (Label) stage.getScene().lookup("#membershipPlan");
                Button renew = (Button) stage.getScene().lookup("#renewMembership");
                Button purchase = (Button) stage.getScene().lookup("#purchaseMembership");
                Button refresh = (Button) stage.getScene().lookup("#refreshMembership");
                assertEquals("Plan: Archived monthly", plan.getText());
                renew.fire();
                assertTrue(purchase.isDisabled());
                assertTrue(refresh.isDisabled());
                return null;
            });
            awaitUi(renewalStatus, "Success: Membership renewed."::equals);
            ObservableValue<String> purchaseStatus = onFxThread(() -> ((Label) stage.getScene()
                    .lookup("#purchaseStatus")).textProperty());
            awaitUi(purchaseStatus, "You already have an active membership."::equals);

            Membership renewed = context.getPersistence().unitOfWork().inTransaction(connection ->
                    context.getPersistence().memberships().findById(connection, 1).orElseThrow());
            assertEquals(today.plusDays(30), renewed.expiryDate());
            assertEquals(1, renewed.planId());
            assertEquals(4990, renewed.snapshotPriceCents());
            assertEquals(30, renewed.snapshotDurationDays());
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test
    void membershipScreenDoesNotDuplicateTheMemberBookingsLists() throws Exception {
        AppContext context = contextWithMember(temporaryDirectory.resolve("membership-without-bookings.db"));
        context.getAuthService().login("member", "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            onFxThread(() -> {
                openMemberMembershipScreen(stage, context);
                assertEquals(null, stage.getScene().lookup("#bookingHistory"));
                assertEquals(null, stage.getScene().lookup("#bookingHistoryStatus"));
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
    void renewalFailureShowsFeedbackAndRestoresActions() throws Exception {
        LocalDate today = LocalDate.now();
        AppContext context = contextWithMember(temporaryDirectory.resolve("renewal-failure.db"));
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(1, "Past plan", 30, 4990, false));
            context.getPersistence().memberships().insert(connection,
                    new Membership(1, 2, 1, today.minusDays(31), today.minusDays(1),
                            MembershipStatus.ACTIVE, 4990, 30));
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TRIGGER fail_membership_renewal BEFORE UPDATE ON membership "
                        + "BEGIN SELECT RAISE(ABORT, 'forced renewal failure'); END");
            }
            return null;
        });
        context.getAuthService().login("member", "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<Boolean> renewalDisabled = onFxThread(() -> {
                openMemberMembershipScreen(stage, context);
                return ((Button) stage.getScene().lookup("#renewMembership")).disableProperty();
            });
            awaitUi(renewalDisabled, disabled -> !disabled);
            ObservableValue<String> renewalStatus = onFxThread(() -> ((Label) stage.getScene()
                    .lookup("#renewalStatus")).textProperty());
            onFxThread(() -> {
                Button renew = (Button) stage.getScene().lookup("#renewMembership");
                Button refresh = (Button) stage.getScene().lookup("#refreshMembership");
                renew.fire();
                assertTrue(refresh.isDisabled());
                return null;
            });
            awaitUi(renewalStatus, text -> text.startsWith("Error:"));
            awaitUi(renewalDisabled, disabled -> !disabled);
            onFxThread(() -> {
                assertFalse(((Button) stage.getScene().lookup("#refreshMembership")).isDisabled());
                assertFalse(((Button) stage.getScene().lookup("#purchaseMembership")).isDisabled());
                return null;
            });
            Membership unchanged = context.getPersistence().unitOfWork().inTransaction(connection ->
                    context.getPersistence().memberships().findById(connection, 1).orElseThrow());
            assertEquals(today.minusDays(1), unchanged.expiryDate());
        } finally {
            onFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test
    void failedRefreshClearsPreviouslyLoadedMembershipAndDisablesRenewal() throws Exception {
        LocalDate today = LocalDate.now();
        AppContext context = contextWithMember(temporaryDirectory.resolve("refresh-failure.db"));
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(1, "Past plan", 30, 4990, false));
            context.getPersistence().memberships().insert(connection,
                    new Membership(1, 2, 1, today.minusDays(31), today.minusDays(1),
                            MembershipStatus.ACTIVE, 4990, 30));
            return null;
        });
        context.getAuthService().login("member", "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<Boolean> renewalDisabled = onFxThread(() -> {
                openMemberMembershipScreen(stage, context);
                return ((Button) stage.getScene().lookup("#renewMembership")).disableProperty();
            });
            awaitUi(renewalDisabled, disabled -> !disabled);
            ObservableValue<String> membershipStatus = onFxThread(() -> ((Label) stage.getScene()
                    .lookup("#membershipStatus")).textProperty());
            context.getPersistence().unitOfWork().inTransaction(connection -> {
                try (var statement = connection.createStatement()) {
                    statement.execute("DROP TABLE membership");
                }
                return null;
            });
            onFxThread(() -> {
                Button refresh = (Button) stage.getScene().lookup("#refreshMembership");
                Button renew = (Button) stage.getScene().lookup("#renewMembership");
                refresh.fire();
                assertTrue(renew.isDisabled());
                return null;
            });
            awaitUi(membershipStatus, text -> text.startsWith("Error:"));
            onFxThread(() -> {
                assertEquals("Plan: —", ((Label) stage.getScene().lookup("#membershipPlan")).getText());
                assertEquals("Expiry date: —", ((Label) stage.getScene().lookup("#membershipExpiry")).getText());
                assertTrue(((Button) stage.getScene().lookup("#renewMembership")).isDisabled());
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
    void purchaseInProgressDisablesRenewalAction() throws Exception {
        LocalDate today = LocalDate.now();
        AppContext context = contextWithMember(temporaryDirectory.resolve("purchase-disables-renewal.db"));
        context.getPersistence().unitOfWork().inTransaction(connection -> {
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(1, "Current plan", 30, 4990, false));
            context.getPersistence().plans().insert(connection,
                    new MembershipPlan(2, "Other plan", 90, 9900, false));
            context.getPersistence().memberships().insert(connection,
                    new Membership(1, 2, 1, today.minusDays(31), today.minusDays(1), MembershipStatus.ACTIVE,
                            4990, 30));
            return null;
        });
        context.getAuthService().login("member", "password123");
        Stage stage = onFxThread(Stage::new);
        try {
            ObservableValue<String> purchaseStatus = onFxThread(() -> {
                openMemberMembershipScreen(stage, context);
                return ((Label) stage.getScene().lookup("#purchaseStatus")).textProperty();
            });
            awaitUi(purchaseStatus, "Choose a plan to purchase."::equals);
            ObservableValue<Boolean> purchaseDisabled = onFxThread(() -> {
                Button purchase = (Button) stage.getScene().lookup("#purchaseMembership");
                return purchase.disableProperty();
            });
            awaitUi(purchaseDisabled, disabled -> !disabled);
            onFxThread(() -> {
                Button purchase = (Button) stage.getScene().lookup("#purchaseMembership");
                Button renew = (Button) stage.getScene().lookup("#renewMembership");
                Button refresh = (Button) stage.getScene().lookup("#refreshMembership");
                purchase.fire();
                assertTrue(renew.isDisabled());
                assertTrue(refresh.isDisabled());
                return null;
            });
            awaitUi(purchaseStatus, "Success: Membership purchased."::equals);
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
