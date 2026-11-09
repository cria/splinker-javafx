package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.i18n.I18n;
import br.org.cria.splinkerapp.i18n.LocalizedViews;
import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

public class HomeController extends AbstractController {

    @FXML
    Pane content;

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
        try {
            Desktop.getDesktop().browse(URI.create("https://doacao.cria.org.br/?utm_source=sitecria&ref=cria.org.br"));
        } catch (Exception e) {
            handleErrors(e);
        }
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
            var template = basePath.formatted(pageName);
            loader = LocalizedViews.loader(getClass().getResource(template));
            Node childNode = LocalizedViews.load(loader);
            var children = content.getChildren();
            children.forEach(LocalizedViews::dispose);
            children.clear();
            children.add(childNode);
            selectPage(pageName);
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    void configureButtons() {
        configureButton(btnPrincipal, "home", "⌂");
        configureButton(btnSettings, "settings", "⚙");
        configureButton(btnReport, "report", "▤");
        configureButton(btnEmail, "email", "✉");
        configureButton(btnAbout, "about", "ⓘ");
        configureButton(btnSupport, "support", "♥");
    }

    private void configureButton(Button button, String key, String symbol) {
        var icon = new Label(symbol);
        icon.setMinWidth(22);
        icon.setStyle("-fx-font-size: 19px; -fx-text-fill: #326ba3;");
        button.setGraphic(icon);
        button.setTooltip(new Tooltip(I18n.text(key)));
    }

    private void selectPage(String pageName) {
        var buttons = java.util.Map.of(
                "principal", btnPrincipal, "configuration", btnSettings,
                "report", btnReport, "email", btnEmail, "help", btnAbout);
        buttons.forEach((page, button) -> button.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("selected"), page.equals(pageName)));
        pageTitle.setText(buttons.get(pageName).getText());
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            configureButtons();
            loadPage("principal");
            super.initialize(location, resources);
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(940);
        stage.setHeight(660);
    }
}
