package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.models.ProxyConfiguration;
import br.org.cria.splinkerapp.repositories.ProxyConfigRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.apache.poi.util.StringUtil;

public class ProxyConfigController extends AbstractController {

    @FXML
    TextField proxyUsername;
    @FXML
    PasswordField proxyPassword;
    @FXML
    TextField proxyAddress;
    @FXML
    TextField proxyPort;
    @FXML
    Button saveBtn;

    @FXML
    void onButtonSaveClicked() {
        try {
            /*if (!validateData()) {
                return;
            }*/

            String address = proxyAddress.getText().trim();
            String port = proxyPort.getText().trim();
            String username = proxyUsername.getText().trim();
            String password = proxyPassword.getText().trim();

            /*if (!isValidProxyHost(address) || !isValidProxyPort(port) ||
                    !isValidProxyUsername(username) || !isValidProxyPassword(password)) {
                return;
            }*/

            var hasConfig = DataSetService.hasConfiguration();
            var routeName = hasConfig ? "home" : "central-service";
            var config = new ProxyConfiguration(address, password, port, username);

            ProxyConfigRepository.saveProxyConfig(config);
            navigateTo(getStage(), routeName);

        } catch (Exception e) {
            handleErrors(e);
        }
    }

    boolean validateData() {
        var hasAddress = StringUtil.isNotBlank(proxyAddress.getText());
        var hasPassword = StringUtil.isNotBlank(proxyPassword.getText());
        var hasUsername = StringUtil.isNotBlank(proxyUsername.getText());
        var hasPort = StringUtil.isNotBlank(proxyPort.getText());

        if (!hasAddress || !hasPassword || !hasUsername || !hasPort) {
            showErrorModal("Todos os campos são obrigatórios");
            return false;
        }
        return true;
    }

    private boolean isValidProxyHost(String host) {
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

        showErrorModal("Endereço de proxy inválido. Verifique se o valor informado é um hostname, endereço IP (IPv4 ou IPv6) ou 'localhost'.");
        return false;
    }

    private boolean isValidProxyPort(String portStr) {
        try {
            int port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                showErrorModal("Porta do proxy inválida. Informe um número inteiro entre 1 e 65535.");
                return false;
            }
        } catch (NumberFormatException e) {
            showErrorModal("Porta do proxy inválida. Informe um número inteiro entre 1 e 65535.");
            return false;
        }
        return true;
    }

    private boolean isValidProxyUsername(String username) {
        if (username.length() < 3) {
            showErrorModal("Nome de usuário do proxy deve ter pelo menos 3 caracteres.");
            return false;
        }

        String usernamePattern = "^[a-zA-Z0-9._-]+$";
        if (!username.matches(usernamePattern)) {
            showErrorModal("Nome de usuário do proxy contém caracteres inválidos. Use apenas letras, números, pontos, hífens e sublinhados.");
            return false;
        }

        return true;
    }

    private boolean isValidProxyPassword(String password) {
        if (password.length() < 6) {
            showErrorModal("A senha do proxy deve ter pelo menos 6 caracteres.");
            return false;
        }
        return true;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            super.initialize(location, resources);
            var config = ProxyConfigRepository.getConfiguration();
            if (config != null) {
                proxyUsername.setText(config.getUsername());
                proxyAddress.setText(config.getAddress());
                proxyPort.setText(config.getPort());
                proxyPassword.setText(config.getPassword());
            }
        } catch (Exception e) {
            handleErrors(e);
        }

    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_SQUARE_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_SQUARE_SCREEN_HEIGHT);
    }
}
