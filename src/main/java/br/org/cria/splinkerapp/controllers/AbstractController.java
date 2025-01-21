package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.Router;
import br.org.cria.splinkerapp.utils.ModalAlertUtil;
import com.google.common.eventbus.EventBus;
import io.sentry.Sentry;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public abstract class AbstractController implements Initializable {

    protected EventBus bus;
    protected String token;
    protected FXMLLoader loader;
    protected Stage modalStage = new Stage();
    @FXML
    Pane pane;
    Alert dialog = new Alert(AlertType.INFORMATION);

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //throw new NotImplementedException("Not implemented");
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

    protected void showErrorModal(String errorMessage) {
        ModalAlertUtil.show(errorMessage);
    }

    protected void handleErrors(Throwable ex) {
        String msg;
        if (isConnectionError(ex)) {
            msg = "Ausência de conexão com a Internet. Este software precisa de uma conexão para funcionar. Verifique sua conexão e tente novamente.";
        } else {
            var sentryId = Sentry.captureException(ex);
            var sentryMsg = (sentryId != null) ? " - Error ID %s".formatted(sentryId.toString()) : "";
            msg = "Ocorreu um erro. Contate o administrador do spLinker%s".formatted(sentryMsg);
        }
        ApplicationLog.error(ex.getLocalizedMessage());
        showErrorModal(ex.getLocalizedMessage());
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
