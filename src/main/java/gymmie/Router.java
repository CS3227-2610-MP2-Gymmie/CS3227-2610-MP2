package gymmie;

import java.io.IOException;

import gymmie.model.Role;
import gymmie.service.UserSession;
import gymmie.trainer.CreateSessionController;
import gymmie.trainer.TrainerProfileController;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Owns scene navigation; session roles select dashboards, while services enforce authorization. */
public final class Router {
    private final Stage stage;
    private final AppContext context;
    private final ViewLoader views;

    /** Creates navigation for the application's primary stage and shared services. */
    public Router(Stage stage, AppContext context, ViewLoader views) {
        this.stage = stage;
        this.context = context;
        this.views = views;
        stage.setMinWidth(520);
        stage.setMinHeight(580);
    }

    /**
     * Returns to a fresh login form and clears authentication.
     *
     * @throws IOException if the login resource cannot be loaded.
     */
    public void showLogin() throws IOException {
        Parent root = views.load("Login", new LoginController(context, this));
        context.getAuthService().logout();
        show(root, "Log in");
    }

    /**
     * Opens the current user's role dashboard, or login if no session exists.
     *
     * @throws IOException if a view cannot be loaded.
     */
    public void showDashboard() throws IOException {
        if (!context.getUserSession().isAuthenticated()) {
            showLogin();
            return;
        }
        UserSession.Principal user = context.getUserSession().requireUser();
        String title = dashboardTitle(user.role());
        show(views.load("Dashboard", new DashboardController(context, this, title)), title);
    }

    /**
     * Opens the Trainer profile editor; profile reads and writes are authorized by its service.
     *
     * @throws IOException if the view cannot be loaded.
     */
    public void showTrainerProfile() throws IOException {
        show(views.loadResource("/gymmie/trainer/view/TrainerProfile.fxml",
                new TrainerProfileController(context, this)), "My Trainer profile");
    }

    /**
     * Opens the session creation form; its service enforces Trainer authorization.
     *
     * @throws IOException if the view cannot be loaded.
     */
    public void showCreateSession() throws IOException {
        show(views.loadResource("/gymmie/trainer/view/CreateSession.fxml",
                new CreateSessionController(context, this)), "Create session");
    }

    /** Returns the user-facing dashboard title for each supported account role. */
    public static String dashboardTitle(Role role) {
        return switch (role) {
            case MANAGER -> "Manager dashboard";
            case TRAINER -> "Trainer dashboard";
            case MEMBER -> "Gym User dashboard";
        };
    }

    private void show(Parent root, String title) {
        if (stage.getScene() == null) {
            Scene scene = new Scene(root, 840, 680);
            stage.setScene(scene);
        } else {
            stage.getScene().setRoot(root);
        }
        stage.setTitle("Gymmie · " + title);
    }
}
