package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.enums.WindowSizes;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class HomeController extends AbstractController {

    @FXML
    Pane content;

    @FXML
    Button btnHelp;

    @FXML
    Button btnReport;

    @FXML
    Button btnEmail;

    @FXML
    Button btnSettings;

    @FXML
    Button btnPrincipal;

    ImageView principalImg;
    ImageView settingsImg;
    ImageView helpImg;
    ImageView emailImg;
    ImageView reportImg;

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
    void onHelpItemClick() {
        loadPage("help");
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
            loader = new FXMLLoader(getClass().getResource(template));
            Node childNode = loader.load();
            var children = content.getChildren();
            children.clear();
            children.add(childNode);
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    void configureButtons() {
        principalImg = new ImageView(new Image("images/home.png"));
        principalImg.setFitHeight(32);
        principalImg.setFitWidth(32);
        principalImg.setSmooth(true);
        principalImg.setPreserveRatio(true);

        settingsImg = new ImageView(new Image("images/settings.png"));
        settingsImg.setFitHeight(32);
        settingsImg.setFitWidth(32);
        settingsImg.setSmooth(true);
        settingsImg.setPreserveRatio(true);

        helpImg = new ImageView(new Image("images/help.png"));
        helpImg.setFitHeight(32);
        helpImg.setFitWidth(32);
        helpImg.setSmooth(true);
        helpImg.setPreserveRatio(true);

        emailImg = new ImageView(new Image("images/email.png"));
        emailImg.setFitHeight(32);
        emailImg.setFitWidth(32);
        emailImg.setSmooth(true);
        emailImg.setPreserveRatio(true);

        reportImg = new ImageView(new Image("images/report.png"));
        reportImg.setFitHeight(32);
        reportImg.setFitWidth(32);
        reportImg.setSmooth(true);
        reportImg.setPreserveRatio(true);

        btnPrincipal.setStyle("-fx-background-color: #f0f0f0; -fx-background-insets: 0;");
        btnPrincipal.setGraphic(principalImg);
        btnPrincipal.setTooltip(new Tooltip("Principal"));

        btnSettings.setStyle("-fx-background-color: #f0f0f0; -fx-background-insets: 0;");
        btnSettings.setGraphic(settingsImg);
        btnSettings.setTooltip(new Tooltip("Configurações"));

        btnHelp.setStyle("-fx-background-color: #f0f0f0; -fx-background-insets: 0;");
        btnHelp.setGraphic(helpImg);
        btnHelp.setTooltip(new Tooltip("Ajuda"));

        btnEmail.setStyle("-fx-background-color: #f0f0f0; -fx-background-insets: 0;");
        btnEmail.setGraphic(emailImg);
        btnEmail.setTooltip(new Tooltip("Email"));

        btnReport.setStyle("-fx-background-color: #f0f0f0; -fx-background-insets: 0;");
        btnReport.setGraphic(reportImg);
        btnReport.setTooltip(new Tooltip("Relatório"));
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
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT + 150);
    }
}
