package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class ConfigurationController extends AbstractController {

    @FXML
    Pane content;

    @FXML
    Label serverLabel;

    @FXML
    Label proxyLabel;

    @FXML
    Label tokenLabel;

    @FXML
    Label dataLabel;

    String basePath = "/br/org/cria/splinkerapp/%s.fxml";

    @Override
    public void initialize(URL location, ResourceBundle bundle) {
        try {
            loadPage("central-service");
            token = TokenRepository.getCurrentToken();
            pane.sceneProperty().addListener((observable, oldScene, newScene) -> {
                if (newScene != null) {
                    paintItBlue("serverLabel");
                }
            });
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_RECTANGULAR_SCREEN_HEIGHT);
    }

    protected void loadPage(String pageName) {
        try {
            var template = basePath.formatted(pageName);
            loader = new FXMLLoader(getClass().getResource(template));
            Node childNode = loader.load();

            var children = content.getChildren();
            children.clear();
            children.add(childNode);
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    void paintItBlue(String lblName) {
        var lbls = pane.getScene().getRoot().lookupAll(".label");
        lbls.forEach((lbl) -> {
            var id = lbl.getId();
            var isSelected = lblName.equals(id);
            Label label = (Label) lbl;
            if (isSelected) {
                label.setTextFill(Color.rgb(14, 85, 145));
                label.setStyle(
                        "-fx-font-weight: bold;" +
                                "-fx-border-color: transparent transparent #0E5591 transparent;" +
                                "-fx-border-width: 0 0 1.7px 0;"
                );
            } else {
                label.setTextFill(Color.BLACK);
                label.setStyle("");
            }
        });
    }

    @FXML
    void showCentralServiceConfiguration() {
        loadPage("central-service");
        paintItBlue("serverLabel");
    }

    @FXML
    void showProxyConfiguration() {
        loadPage("proxy-config");
        paintItBlue("proxyLabel");
    }

    @FXML
    void showTokenConfiguration() {
        loadPage("token-login");
        paintItBlue("tokenLabel");
    }

    @FXML
    void showDataSourceConfiguration() {
        try {
            var ds = DataSetService.getDataSet(token);
            var pageName = "collection-database";

            if (ds.isAccessDb()) {
                pageName = "access-db-modal";
            }

            if (ds.isFile()) {
                pageName = "file-selection";
            }

            loadPage(pageName);
            paintItBlue("dataLabel");
        } catch (Exception e) {
            handleErrors(e);
        }
    }
}
