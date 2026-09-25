package gymmie;

import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/** Loads bundled FXML with explicitly supplied, dependency-injected controllers. */
public final class ViewLoader {
    /**
     * Loads a view without reflective controller construction.
     *
     * @param name bundled FXML filename without its extension.
     * @param controller controller wired by the router.
     * @return loaded view root.
     * @throws IOException if the resource is missing or malformed.
     */
    public Parent load(String name, Object controller) throws IOException {
        return loadResource("/gymmie/view/" + name + ".fxml", controller);
    }

    /**
     * Loads a bundled view at an absolute resource path, including role-specific folders.
     *
     * @param path absolute FXML resource path.
     * @param controller controller wired by the router.
     * @return loaded view root.
     * @throws IOException if the resource is missing or malformed.
     */
    public Parent loadResource(String path, Object controller) throws IOException {
        FXMLLoader loader = new FXMLLoader(resource(path));
        loader.setControllerFactory(type -> {
            if (!type.isInstance(controller)) {
                throw new IllegalArgumentException("Controller does not match the FXML declaration: " + type.getName());
            }
            return controller;
        });
        return loader.load();
    }

    /**
     * Resolves a required application resource.
     *
     * @param path absolute classpath resource path.
     * @return resource location.
     * @throws IOException if the resource is missing.
     */
    public URL resource(String path) throws IOException {
        URL resource = ViewLoader.class.getResource(path);
        if (resource == null) {
            throw new IOException("Missing application resource: " + path);
        }
        return resource;
    }
}
