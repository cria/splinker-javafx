package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.i18n.I18n;
import br.org.cria.splinkerapp.i18n.LocalizedViews;
import br.org.cria.splinkerapp.utils.SupportBrowser;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class HomeController extends AbstractController {

    private SupportBrowser supportBrowser;

    @FXML
    Pane content;

    @FXML
    ScrollPane pageScroll;

    @FXML
    Button btnAbout;

    @FXML
    Button btnSupport;

    @FXML
    Button btnReport;

    @FXML
    Button btnEmail;

    @FXML
    Button btnSettings;

    @FXML
    Button btnPrincipal;

    @FXML
    Label pageTitle;

    String basePath = "/br/org/cria/splinkerapp/%s.fxml";

    @FXML
    void onSettingsItemClick() {
        loadPage("configuration");
    }

    @FXML
    void onPrincipalItemClick() {
        loadPage("principal");
    }

    @FXML
    void onAboutItemClick() {
        loadPage("help");
    }

    @FXML
    void onSupportItemClick() {
        if (supportBrowser == null) loadPage("support");
    }

    @FXML
    void onEmailItemClick() {
        loadPage("email");
    }

    @FXML
    void onReportItemClick() {
        loadPage("report");
    }

    protected void loadPage(String pageName) {
        try {
            Node childNode;
            if (pageName.equals("support")) {
                childNode = new SupportBrowser();
            } else {
                var template = basePath.formatted(pageName);
                loader = LocalizedViews.loader(getClass().getResource(template));
                childNode = LocalizedViews.load(loader);
            }
            if (supportBrowser != null) {
                supportBrowser.close();
                supportBrowser = null;
            }
            var children = content.getChildren();
            children.forEach(LocalizedViews::dispose);
            children.clear();
            children.add(childNode);
            pageScroll.setFitToHeight(pageName.equals("support"));
            selectPage(pageName);
            if (childNode instanceof SupportBrowser browser) {
                supportBrowser = browser;
                browser.openDonationPage();
            }
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    void configureButtons() {
        configureVectorButton(btnPrincipal, "home",
                "M2 10 L11 2 L20 10 M4 9 V20 H8 V13 H14 V20 H18 V9");
        configureVectorButton(btnSettings, "settings",
                "M2 5 H5 M9 5 H20 M2 11 H13 M17 11 H20 M2 17 H5 M9 17 H20 "
                + "M9 5 A2 2 0 1 0 5 5 A2 2 0 1 0 9 5 "
                + "M17 11 A2 2 0 1 0 13 11 A2 2 0 1 0 17 11 "
                + "M9 17 A2 2 0 1 0 5 17 A2 2 0 1 0 9 17");
        configureVectorButton(btnReport, "report",
                "M13 2 H4 V20 H18 V7 Z M13 2 V7 H18 "
                + "M7 16 V13 M11 16 V10 M15 16 V12");
        configureVectorButton(btnEmail, "email",
                "M4 4 H18 Q20 4 20 6 V16 Q20 18 18 18 H4 Q2 18 2 16 V6 Q2 4 4 4 Z "
                + "M2 6 L11 12 L20 6");
        configureVectorButton(btnAbout, "about",
                "M20 11 A9 9 0 1 0 2 11 A9 9 0 1 0 20 11 Z "
                + "M11 10 V16 M11 6 V6.2");
        configureVectorButton(btnSupport, "support",
                "M11 20 L3.5 12.5 C-2 7 5 0 11 6 "
                + "C17 0 24 7 18.5 12.5 Z");
    }

    private void configureVectorButton(Button button, String key, String path) {
        var icon = new SVGPath();
        icon.setContent(path);
        icon.getStyleClass().add("nav-icon");
        var graphic = new StackPane(icon);
        graphic.setMinSize(22, 22);
        graphic.setPrefSize(22, 22);
        graphic.setMaxSize(22, 22);
        graphic.setMouseTransparent(true);
        button.setGraphic(graphic);
        button.setTooltip(new Tooltip(I18n.text(key)));
    }

    private void selectPage(String pageName) {
        var buttons = java.util.Map.of(
                "principal", btnPrincipal, "configuration", btnSettings,
                "report", btnReport, "email", btnEmail, "help", btnAbout,
                "support", btnSupport);
        buttons.forEach((page, button) -> button.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("selected"), page.equals(pageName)));
        pageTitle.setText(buttons.get(pageName).getText());
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            // FXML initialization runs before Router attaches the scene to the stage.
            pane.sceneProperty().addListener((observable, previous, scene) -> {
                if (scene == null) return;
                scene.windowProperty().addListener((property, oldWindow, newWindow) -> {
                    if (newWindow instanceof Stage stage) configureWindow(stage);
                });
                if (scene.getWindow() instanceof Stage stage) configureWindow(stage);
            });
            configureButtons();
            loadPage("principal");
            super.initialize(location, resources);
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    private void configureWindow(Stage stage) {
        stage.setResizable(false);
        // Apply dimensions after the scene switch and the resizable-state change.
        Platform.runLater(() -> {
            if (stage.getScene() != null && stage.getScene().getRoot() == pane) {
                setScreensize();
            }
        });
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        var bounds = Screen.getScreensForRectangle(stage.getX(), stage.getY(), 1, 1)
                .stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
        stage.setMinWidth(0);
        stage.setMinHeight(0);
        stage.setWidth(Math.min(940, bounds.getWidth()));
        stage.setHeight(Math.min(660, bounds.getHeight()));
        stage.setX(Math.max(bounds.getMinX(), Math.min(stage.getX(), bounds.getMaxX() - stage.getWidth())));
        stage.setY(Math.max(bounds.getMinY(), Math.min(stage.getY(), bounds.getMaxY() - stage.getHeight())));
    }
}
