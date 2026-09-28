package gymmie;

import java.io.IOException;

import gymmie.model.Role;
import gymmie.ui.StatusLabel;
import javafx.css.PseudoClass;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/** Provides persistent role-specific side tabs around authenticated pages. */
final class DashboardNavigation {
    private final Router router;
    private final String current;
    private final VBox tabs = new VBox(8);
    private final StatusLabel status = new StatusLabel();

    DashboardNavigation(Router router, String current) {
        this.router = router;
        this.current = current;
    }

    Parent wrap(Parent page, Role role) {
        page.setId("pageScroll");
        BorderPane shell = new BorderPane(page);
        shell.setId("dashboardShell");
        shell.getStylesheets().setAll(page.getStylesheets());
        tabs.setId("sideTabs");
        var content = page instanceof ScrollPane scroll ? scroll.getContent() : page;
        var navigationLock = content.lookup("#actions");
        if (navigationLock == null) {
            navigationLock = content.lookup("#backButton");
        }
        if (navigationLock != null) {
            tabs.disableProperty().bind(navigationLock.disabledProperty());
        }
        tabs.getStyleClass().add("side-tabs");
        Label brand = new Label("GYMMIE");
        brand.getStyleClass().add("sidebar-brand");
        tabs.getChildren().add(brand);
        add("homeButton", "Home", Router.dashboardTitle(role), router::showDashboard);
        switch (role) {
            case MANAGER:
                add("managePlansButton", "Manage membership plans", "Membership plans", router::showManagerPlans);
                add("manageAccountsButton", "Manage accounts", "Manage accounts", router::showManagerAccounts);
                break;
            case TRAINER:
                add("upcomingSessionsButton", "My upcoming sessions", "My upcoming sessions",
                        router::showUpcomingSessions);
                add("createSessionButton", "Create session", "Create session", router::showCreateSession);
                break;
            case MEMBER:
                add("memberMembershipButton", "My membership", "My membership", router::showMemberMembership);
                add("browseSessionsButton", "Browse sessions", "Browse sessions", router::showMemberSessions);
                add("memberBookingsButton", "My bookings", "My bookings", router::showMemberBookings);
                break;
            default:
                throw new IllegalStateException("Unsupported role");
        }
        add("logoutButton", "Log out", "Log in", router::showLogin);
        tabs.getChildren().add(status);
        ScrollPane rail = new ScrollPane(tabs);
        rail.setFitToWidth(true);
        rail.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rail.getStyleClass().add("navigation-rail");
        rail.setMinWidth(140);
        rail.prefWidthProperty().bind(shell.widthProperty().multiply(0.23).map(width ->
                Math.max(140, Math.min(230, width.doubleValue()))));
        rail.setMaxWidth(230);
        shell.setLeft(rail);
        return shell;
    }

    private void add(String id, String text, String destination, NavigationAction action) {
        Button tab = new Button(text);
        tab.setId(id);
        tab.setWrapText(true);
        tab.setMinWidth(0);
        tab.setMaxWidth(Double.MAX_VALUE);
        tab.getStyleClass().add("navigation-tab");
        boolean selected = current.equals(destination)
                || id.equals("homeButton") && current.equals("My Trainer profile")
                || id.equals("upcomingSessionsButton") && current.equals("Edit session");
        tab.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), selected);
        tab.setAccessibleHelp(selected ? "Current page" : "Open " + text);
        tab.setOnAction(_ -> {
            try {
                action.open();
            } catch (IOException exception) {
                status.error("Unable to open " + text + ". Please try again.");
            }
        });
        tabs.getChildren().add(tab);
    }

    @FunctionalInterface
    private interface NavigationAction {
        void open() throws IOException;
    }
}
