package br.org.cria.splinkerapp.controllers;

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
    Alert dialog = new Alert(AlertType.INFORMATION);
    String basePath = "/br/org/cria/splinkerapp/%s.fxml";

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
            loader = new FXMLLoader(getClass().getResource(template));
            Node childNode = loader.load();

            var children = pane.getChildren();
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
            if (ex.getLocalizedMessage().contains("no such column")) {
                String[] split = ex.getLocalizedMessage().split("no such column:");
                if (split.length > 1) {
                    DataSet dataSet = DataSetService.getDataSet(TokenRepository.getCurrentToken());
                    if (dataSet.isFile()) {
                        msg = dataSet.getDataSetName() + " (" + dataSet.getToken() + ") - não foi encontrada a coluna (" + split[1].replace(")", "") + " ) na planilha";
                    } else {
                        msg = dataSet.getDataSetName() + " (" + dataSet.getToken() + ") - não foi encontrada a coluna (" + split[1].replace(")", "") + " ) no banco de dados";
                    }
                    showErrorModal("Entre em contado com o time de suporte do CRIA e informe o erro:\n\n" + msg, ex.getLocalizedMessage());
                }
                Sentry.captureMessage(msg,SentryLevel.ERROR);
            } else if (ex.getLocalizedMessage().contains("no such table")) {
                String[] split = ex.getLocalizedMessage().split("no such table:");
                if (split.length > 1) {
                    DataSet dataSet = DataSetService.getDataSet(TokenRepository.getCurrentToken());
                    if (dataSet.isFile()) {
                        msg = dataSet.getDataSetName() + " (" + dataSet.getToken() + ") - não foi encontrada a aba (" + split[1].replace(")", "") + " ) na planilha";
                    } else {
                        msg = dataSet.getDataSetName() + " (" + dataSet.getToken() + ") - não foi encontrada a tabela (" + split[1].replace(")", "") + " ) no banco de dados";
                    }
                    showErrorModal("Entre em contado com o time de suporte do CRIA e informe o erro:\n\n" + msg, ex.getLocalizedMessage());
                }
                Sentry.captureMessage(msg, SentryLevel.ERROR);
            } else if (ex.getLocalizedMessage().contains("Can't open the specified file input stream from file")) {
                NoSuchFileException cause = (NoSuchFileException) ex.getCause();
                msg = "Não foi possivel abrir o arquivo em: " + cause.getFile() + ".  Certifique se o arquivo está no local correto e ajuste na configuração. \n " +
                        "Para ajustar, acesse a área de configuração para escolher corretamente o arquivo válido. Qualquer dúvida, entre em contato com o time de suporte CRIA";
                showErrorModal(msg, "Para ajustar, acesse a área de configuração para escolher corretamente o arquivo válido");
            } else if (ex.getLocalizedMessage().contains("br.org.cria.splinkerapp.models.DataSet.getDataSetFilePath()") && ex.getLocalizedMessage().contains("null")) {
                msg = "Deve ser selecionado um arquivo para extrair os dados.";
                showErrorModal(msg);
            } else if (isConnectionError(ex)) {
                msg = "Ausência de conexão com a Internet. Este software precisa de uma conexão para funcionar. Verifique sua conexão e tente novamente.";
                showErrorModal(msg);
            } else {
                Sentry.captureException(ex);
                String stackTrace = ExceptionUtils.getStackTrace(ex);
                ApplicationLog.error(stackTrace);
                showErrorModal("Erro no spLinker. Entre em contado com o time de suporte do CRIA relatando o problema.", stackTrace);
            }
        } catch (Exception e) {
            Sentry.captureException(ex);
            String stackTrace = ExceptionUtils.getStackTrace(ex);
            ApplicationLog.error(stackTrace);
            showErrorModal("Erro no spLinker. Entre em contado com o time de suporte do CRIA relatando o problema.", stackTrace);
        }

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
