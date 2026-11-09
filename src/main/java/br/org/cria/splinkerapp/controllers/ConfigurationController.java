package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import br.org.cria.splinkerapp.i18n.LocalizedViews;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

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
            loader = LocalizedViews.loader(getClass().getResource(template));
            Node childNode = LocalizedViews.load(loader);

            var children = content.getChildren();
            children.forEach(LocalizedViews::dispose);
            children.clear();
            children.add(childNode);
        } catch (Exception e) {
            handleErrors(e);
        }
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
