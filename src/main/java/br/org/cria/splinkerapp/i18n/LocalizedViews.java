package br.org.cria.splinkerapp.i18n;

import java.io.IOException;
import java.net.URL;
import br.org.cria.splinkerapp.managers.EventBusManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;

/** Shared FXML entry point for windows and nested pages. */
public final class LocalizedViews {
    private static final String CONTROLLER = LocalizedViews.class.getName() + ".controller";

    private LocalizedViews() { }

    public static FXMLLoader loader(URL location) {
        return new FXMLLoader(location, I18n.bundle());
    }

    public static <T extends Node> T load(FXMLLoader loader) throws IOException {
        T root = loader.load();
        if (loader.getController() != null) root.getProperties().put(CONTROLLER, loader.getController());
        return root;
    }

    public static void dispose(Node node) {
        Object controller = node.getProperties().remove(CONTROLLER);
        if (controller != null) EventBusManager.unregister(controller);
        if (node instanceof Parent parent) parent.getChildrenUnmodifiable().forEach(LocalizedViews::dispose);
    }
}
