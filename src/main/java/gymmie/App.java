package gymmie;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

import gymmie.persistence.Persistence;
import gymmie.service.AuthService;
import gymmie.service.PasswordHasher;
import gymmie.service.Permissions;
import gymmie.service.UserSession;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application for Gymmie.
 */
public class App extends Application {
    private static final String APP_TITLE = "Gymmie";

    private final Persistence persistence = new Persistence();
    private final UserSession userSession = new UserSession();
    private final Permissions permissions = new Permissions(persistence.accounts(), userSession);
    private final AuthService authService = new AuthService(persistence.accounts(), persistence.unitOfWork(),
            userSession, new PasswordHasher());

    /**
     * Initializes the local database before the welcome window is created.
     *
     * @throws SQLException if local storage cannot be initialized.
     */
    @Override
    public void init() throws SQLException {
        persistence.initialize();
    }

    /** Returns the shared persistence boundary for top-level application services. */
    public Persistence getPersistence() {
        return persistence;
    }

    /** Returns the application's authentication operations. */
    public AuthService getAuthService() {
        return authService;
    }

    /** Returns the credential-free session snapshot for display and dashboard routing. */
    public UserSession getUserSession() {
        return userSession;
    }

    /** Returns the service-layer authorization boundary for protected operations. */
    public Permissions getPermissions() {
        return permissions;
    }

    /**
     * Loads the root view and stylesheet, then displays the primary stage.
     *
     * @param stage primary stage supplied by JavaFX.
     * @throws IOException if the main FXML view cannot be loaded or the stylesheet resource is missing.
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/gymmie/view/MainWindow.fxml"));
        Scene scene = new Scene(loader.load());
        URL stylesheet = App.class.getResource("/gymmie/css/gymmie.css");
        if (stylesheet == null) {
            throw new IOException("Missing stylesheet resource: /gymmie/css/gymmie.css");
        }
        scene.getStylesheets().add(stylesheet.toExternalForm());

        stage.setTitle(APP_TITLE);
        stage.setMinWidth(420);
        stage.setMinHeight(280);
        stage.setScene(scene);
        stage.show();
    }

    static String getAppTitle() {
        return APP_TITLE;
    }
}
