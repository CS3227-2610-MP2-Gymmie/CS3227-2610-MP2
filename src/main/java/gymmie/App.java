package gymmie;

import javafx.application.Application;
import javafx.stage.Stage;

/** JavaFX application for Gymmie. */
public class App extends Application {
    private AppContext context;

    @Override
    public void init() throws Exception {
        context = new AppContext();
    }

    @Override
    public void start(Stage stage) throws Exception {
        new Router(stage, context, new ViewLoader()).showLogin();
        stage.show();
    }

    static String getAppTitle() {
        return "Gymmie";
    }
}
