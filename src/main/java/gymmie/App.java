package gymmie;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application for Gymmie.
 */
public class App extends Application {
    private static final String APP_TITLE = "Gymmie";

    /**
     * Loads the root view and stylesheet, then displays the primary stage.
     *
     * @param stage primary stage supplied by JavaFX.
     * @throws IOException if the main FXML view cannot be loaded.
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/gymmie/view/MainWindow.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(App.class.getResource("/gymmie/css/gymmie.css").toExternalForm());

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
