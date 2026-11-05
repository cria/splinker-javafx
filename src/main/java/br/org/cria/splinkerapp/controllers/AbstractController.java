package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.i18n.I18n;
import br.org.cria.splinkerapp.i18n.Language;
import br.org.cria.splinkerapp.i18n.LocalizedViews;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.Router;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.utils.ModalAlertUtil;
import com.google.common.eventbus.EventBus;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.apache.commons.lang3.exception.ExceptionUtils;

public abstract class AbstractController implements Initializable {

    protected EventBus bus;
    protected String token;
    protected FXMLLoader loader;
    protected Stage modalStage = new Stage();
    @FXML
    Pane pane;
    @FXML
    private MenuButton languageSelector;
    Alert dialog = new Alert(AlertType.INFORMATION);
    String basePath = "/br/org/cria/splinkerapp/%s.fxml";

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        if (languageSelector != null) {
            languageSelector.setText(I18n.getLanguage().code().toUpperCase(java.util.Locale.ROOT));
            languageSelector.setAccessibleText(I18n.text("language") + ": " + I18n.getLanguage());
            languageSelector.setTooltip(new Tooltip(I18n.text("language") + ": " + I18n.getLanguage()));
            for (Language selected : Language.values()) {
                MenuItem item = new MenuItem(selected.toString());
                item.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
                item.setOnAction(event -> {
                if (selected == I18n.getLanguage()) return;
                Language previous = I18n.getLanguage();
                try {
                    I18n.setLanguage(selected);
                    Router.navigateTo(getStage(), this instanceof HomeController ? "home" : "first-config-dialog");
                } catch (Exception e) {
                    I18n.setLanguage(previous);
                    handleErrors(e);
                }
                });
                languageSelector.getItems().add(item);
            }
        }
    }

    protected abstract void setScreensize();

    void navigateTo(String routeName) {
        try {
            Router.navigateTo(getStage(), routeName);
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    void navigateTo(Stage stage, String routeName) {
        try {
            Router.navigateTo(stage, routeName);
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    protected void showAlert(AlertType type, String title, String message) {
        if (type != null) {
            dialog = new Alert(type);
        }

        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        dialog.show();
    }

    protected Stage getStage() {
        try {
            var scene = pane.getScene();
            var stage = (Stage) scene.getWindow();
            stage.setResizable(false);
            return stage;
        } catch (Exception ex) {
            Sentry.captureException(ex);
            ex.printStackTrace();
            return null;
        }
    }

    @FXML
    void showDataSourceConfiguration(String token) {
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

    protected void loadPage(String pageName) {
        try {
            var template = basePath.formatted(pageName);
            loader = LocalizedViews.loader(getClass().getResource(template));
            Node childNode = LocalizedViews.load(loader);

            var children = pane.getChildren();
            children.forEach(LocalizedViews::dispose);
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
                label.setTextFill(Color.rgb(14, 85, 220));
                label.setStyle(
                        "-fx-font-weight: bold;" +
                                "-fx-border-color: transparent;" +
                                "-fx-border-width: 0 0 1.7px 0;"
                );
            } else {
                label.setTextFill(Color.BLACK);
                label.setStyle("");
            }
        });
    }

    protected void showErrorModal(String errorMessage, String detalhe) {
        ModalAlertUtil.show(errorMessage, detalhe);
    }

    protected void showErrorModal(String errorMessage) {
        ModalAlertUtil.show(errorMessage, null);
    }

    protected void handleErrors(Throwable ex) {
        String msg = "";
        try {
            String localizedMessage = ex != null && ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : "";

            if (isUserFacingValidationError(ex, localizedMessage)) {
                showErrorModal(localizedMessage);
            } else if (localizedMessage.contains("no such column")) {
                String[] split = localizedMessage.split("no such column:");
                if (split.length > 1) {
                    DataSet dataSet = DataSetService.getDataSet(TokenRepository.getCurrentToken());
                    if (dataSet.isFile()) {
                        msg = dataSet.getDataSetName() + " (" + dataSet.getToken() + I18n.text("error.column") + split[1].replace(")", "") + I18n.text("error.in.spreadsheet");
                    } else {
                        msg = dataSet.getDataSetName() + " (" + dataSet.getToken() + I18n.text("error.column") + split[1].replace(")", "") + I18n.text("error.in.database");
                    }
                    showErrorModal(I18n.text("error.support") + msg, localizedMessage);
                }
                Sentry.captureMessage(msg, SentryLevel.ERROR);
            } else if (localizedMessage.contains("no such table")) {
                String[] split = localizedMessage.split("no such table:");
                if (split.length > 1) {
                    DataSet dataSet = DataSetService.getDataSet(TokenRepository.getCurrentToken());
                    if (dataSet.isFile()) {
                        msg = dataSet.getDataSetName() + " (" + dataSet.getToken() + I18n.text("error.sheet") + split[1].replace(")", "") + I18n.text("error.in.spreadsheet");
                    } else {
                        msg = dataSet.getDataSetName() + " (" + dataSet.getToken() + I18n.text("error.table") + split[1].replace(")", "") + I18n.text("error.in.database");
                    }
                    showErrorModal(I18n.text("error.support") + msg, localizedMessage);
                }
                Sentry.captureMessage(msg, SentryLevel.ERROR);
            } else if (localizedMessage.contains("Can't open the specified file input stream from file")) {
                NoSuchFileException cause = (NoSuchFileException) ex.getCause();
                msg = I18n.text("error.file.open") + cause.getFile() + I18n.text("error.file.location") +
                        I18n.text("error.file.help");
                showErrorModal(msg, I18n.text("error.file.settings"));
            } else if (localizedMessage.contains("br.org.cria.splinkerapp.models.DataSet.getDataSetFilePath()") && localizedMessage.contains("null")) {
                msg = I18n.text("error.file.select");
                showErrorModal(msg);
            } else if (isConnectionError(ex)) {
                msg = I18n.text("error.connection");
                showErrorModal(msg);
            } else {
                Sentry.captureException(ex);
                String stackTrace = ExceptionUtils.getStackTrace(ex);
                ApplicationLog.error(stackTrace);
                showErrorModal(I18n.text("error.application"), stackTrace);
            }
        } catch (Exception e) {
            Sentry.captureException(ex);
            String stackTrace = ExceptionUtils.getStackTrace(ex);
            ApplicationLog.error(stackTrace);
            showErrorModal(I18n.text("error.application"), stackTrace);
        }

    }

    private boolean isUserFacingValidationError(Throwable ex, String localizedMessage) {
        return ex instanceof IllegalArgumentException &&
                localizedMessage != null &&
                !localizedMessage.isBlank();
    }

    private boolean isConnectionError(Throwable ex) {
        if (ex instanceof java.net.SocketException ||
                ex instanceof java.net.UnknownHostException ||
                ex instanceof java.net.ConnectException) {
            return true;
        }

        return false;
    }
}
