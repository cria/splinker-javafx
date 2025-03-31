package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.apache.poi.util.StringUtil;

/*
 * Classe responsável pelo formulário de configuração de banco de dados
 */
public class CollectionLocalDatabaseController extends AbstractController {

    @FXML
    TextField usernameField;
    @FXML
    PasswordField passwordField;

    @FXML
    TextField portField;
    @FXML
    TextField hostAddressField;
    @FXML
    TextField dbNameField;

    @FXML
    Button saveBtn;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            super.initialize(location, resources);
            token = TokenRepository.getCurrentToken();
            var ds = DataSetService.getDataSet(token);
            if (ds != null) {
                usernameField.setText(ds.getDbUser());
                passwordField.setText(ds.getDbPassword());
                hostAddressField.setText(ds.getDbHost());
                dbNameField.setText(ds.getDbName());
                portField.setText(ds.getDbPort());
            }

        } catch (Exception e) {
            handleErrors(e);
        }
    }


    @FXML
    void onSaveButtonClicked() {
        try {
            if (!validateFields()) {
                return;
            }
            var hasPassword = StringUtil.isNotBlank(passwordField.getText());
            var username = usernameField.getText();
            var password = hasPassword ? passwordField.getText() : "";
            var hostName = hostAddressField.getText();
            var databaseName = dbNameField.getText();
            var port = portField.getText();

            if (!isValidHost(hostName) || !isValidPort(port)) {
                return;
            }
            DataSetService.saveSQLDataSource(token, hostName, port, databaseName, username, password);
            navigateTo(getStage(), "home");

        } catch (Exception e) {
            handleErrors(e);
        }
    }

    private boolean isValidPort(String portStr) {
        try {
            int port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                showErrorModal("Porta inválida. Informe um número inteiro entre 1 e 65535.");
                return false;
            }
        } catch (NumberFormatException e) {
            showErrorModal("Porta inválida. Informe um número inteiro entre 1 e 65535.");
            return false;
        }
        return true;
    }

    private boolean isValidHost(String host) {
        if (host.equalsIgnoreCase("localhost")) {
            return true;
        }
        String ipv4Pattern = "^(?:(?:\\d{1,3}\\.){3}\\d{1,3})$";
        if (host.matches(ipv4Pattern)) {
            String[] parts = host.split("\\.");
            for (String part : parts) {
                try {
                    int num = Integer.parseInt(part);
                    if (num < 0 || num > 255) {
                        showErrorModal("Endereço IPv4 inválido: cada octeto deve estar entre 0 e 255.");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    showErrorModal("Endereço IPv4 inválido: formato incorreto.");
                    return false;
                }
            }
            return true;
        }
        String ipv6Pattern = "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$";
        if (host.matches(ipv6Pattern)) {
            return true;
        }
        String hostnamePattern = "^(?!-)[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(?<!-)$";
        if (host.matches(hostnamePattern)) {
            return true;
        }
        showErrorModal("Endereço de host inválido. Verifique se o valor informado é um hostname, endereço IP (IPv4 ou IPv6) ou 'localhost'.");
        return false;
    }

    boolean validateFields() {
        var hasUserName = StringUtil.isNotBlank(usernameField.getText());
        var hasHostName = StringUtil.isNotBlank(hostAddressField.getText());
        var hasDBName = StringUtil.isNotBlank(dbNameField.getText());
        var hasPort = StringUtil.isNotBlank(portField.getText());

        if (!hasUserName || !hasHostName || !hasDBName || !hasPort) {
            showErrorModal("Todos os campos são obrigatórios");
            return false;
        }
        return true;
    }


    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
}
