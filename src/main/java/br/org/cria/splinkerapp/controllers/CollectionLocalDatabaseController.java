package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.utils.DatabaseLogUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.apache.poi.util.StringUtil;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ResourceBundle;

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

    private DataSourceType dataSourceType;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            super.initialize(location, resources);
            token = TokenRepository.getCurrentToken();
            var ds = DataSetService.getDataSet(token);
            if (ds != null) {
                if (ds.getType() == DataSourceType.PostgreSQL) {
                    ApplicationLog.info("[POSTGRES] Inicializando tela de configuracao PostgreSQL com dados locais. %s"
                            .formatted(DatabaseLogUtil.describeDataSet(ds)));
                }
                usernameField.setText(ds.getDbUser());
                passwordField.setText(ds.getDbPassword());
                hostAddressField.setText(ds.getDbHost());
                dbNameField.setText(ds.getDbName());
                portField.setText(ds.getDbPort());
                dataSourceType = ds.getType();
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
            var username = usernameField.getText().trim();
            var password = hasPassword ? passwordField.getText() : "";
            var hostName = hostAddressField.getText().trim();
            var databaseName = dbNameField.getText().trim();
            var port = portField.getText().trim();

            if (!isValidHost(hostName) || !isValidPort(port)) {
                return;
            }

            if (dataSourceType == null) {
                var ds = DataSetService.getDataSet(token);
                if (ds != null) {
                    dataSourceType = ds.getType();
                }
            }

            if (dataSourceType == null) {
                showErrorModal("Não foi possível identificar o tipo do banco de dados.");
                return;
            }

            if (dataSourceType == DataSourceType.PostgreSQL) {
                ApplicationLog.info("[POSTGRES] Iniciando fluxo de teste/salvamento PostgreSQL. token=%s, host=%s, port=%s, dbName=%s, user=%s, hasPassword=%s"
                        .formatted(token, hostName, port, databaseName, username, password != null && !password.isBlank()));
            }

            boolean connected = testarConexaoComFallbackSsl(
                    dataSourceType,
                    hostName,
                    port,
                    databaseName,
                    username,
                    password
            );

            if (!connected) {
                if (dataSourceType == DataSourceType.PostgreSQL) {
                    ApplicationLog.info("[POSTGRES] Teste inicial de conexao PostgreSQL falhou em todas as tentativas. token=%s, host=%s, port=%s, dbName=%s, user=%s"
                            .formatted(token, hostName, port, databaseName, username));
                }
                showErrorModal("Não foi possível se conectar ao banco de dados.");
                return;
            }

            if (dataSourceType == DataSourceType.PostgreSQL) {
                ApplicationLog.info("[POSTGRES] Teste inicial de conexao PostgreSQL aprovado. Salvando configuracao local. token=%s"
                        .formatted(token));
            }
            DataSetService.saveSQLDataSource(token, hostName, port, databaseName, username, password);
            if (dataSourceType == DataSourceType.PostgreSQL) {
                ApplicationLog.info("[POSTGRES] Configuracao PostgreSQL salva com sucesso. token=%s, host=%s, port=%s, dbName=%s, user=%s"
                        .formatted(token, hostName, port, databaseName, username));
            }
            navigateTo(getStage(), "home");

        } catch (Exception e) {
            handleErrors(e);
        }
    }

    private boolean testarConexaoComFallbackSsl(DataSourceType dsType,
                                                String hostName,
                                                String port,
                                                String databaseName,
                                                String username,
                                                String password) {
        if (dsType == DataSourceType.PostgreSQL) {
            ApplicationLog.info("[POSTGRES] Iniciando teste inicial com fallback SSL. Primeiro sslmode=require, depois sslmode=disable.");
        }
        if (testarConexao(dsType, hostName, port, databaseName, username, password, true)) {
            return true;
        }

        return testarConexao(dsType, hostName, port, databaseName, username, password, false);
    }

    private boolean testarConexao(DataSourceType dsType,
                                  String hostName,
                                  String port,
                                  String databaseName,
                                  String username,
                                  String password,
                                  boolean sslEnabled) {
        try {
            String jdbcUrl = montarJdbcUrl(dsType, hostName, port, databaseName, sslEnabled);
            Properties props = montarPropriedadesConexao(dsType, username, password, sslEnabled);
            if (dsType == DataSourceType.PostgreSQL) {
                ApplicationLog.info("[POSTGRES] Testando conexao inicial. sslEnabled=%s, url=%s, props=%s"
                        .formatted(sslEnabled, DatabaseLogUtil.showJdbcUrlWithCredentials(jdbcUrl),
                                DatabaseLogUtil.describeConnectionProperties(props)));
            }

            try (Connection ignored = DriverManager.getConnection(jdbcUrl, props)) {
                if (dsType == DataSourceType.PostgreSQL) {
                    ApplicationLog.info("[POSTGRES] Teste de conexao inicial bem-sucedido. sslEnabled=%s, url=%s"
                            .formatted(sslEnabled, DatabaseLogUtil.showJdbcUrlWithCredentials(jdbcUrl)));
                }
                return true;
            }
        } catch (SQLException e) {
            if (dsType == DataSourceType.PostgreSQL) {
                ApplicationLog.info("[POSTGRES] Teste de conexao inicial falhou. sslEnabled=%s, host=%s, port=%s, dbName=%s, user=%s, erro=%s"
                        .formatted(sslEnabled, hostName, port, databaseName, username,
                                DatabaseLogUtil.describeSqlException(e)));
            }
            return false;
        } catch (Exception e) {
            if (dsType == DataSourceType.PostgreSQL) {
                ApplicationLog.info("[POSTGRES] Teste de conexao inicial falhou com erro nao-SQL. sslEnabled=%s, host=%s, port=%s, dbName=%s, user=%s, erro=%s: %s"
                        .formatted(sslEnabled, hostName, port, databaseName, username,
                                e.getClass().getName(), e.getMessage()));
            }
            return false;
        }
    }

    private String montarJdbcUrl(DataSourceType dsType,
                                 String hostName,
                                 String port,
                                 String databaseName,
                                 boolean sslEnabled) {
        switch (dsType) {
            case PostgreSQL:
                return "jdbc:postgresql://" + hostName + ":" + port + "/" + databaseName
                        + "?sslmode=" + (sslEnabled ? "require" : "disable");

            case MySQL:
                return "jdbc:mysql://" + hostName + ":" + port + "/" + databaseName
                        + "?useSSL=" + sslEnabled
                        + "&requireSSL=" + sslEnabled
                        + "&verifyServerCertificate=false";

            case MariaDB:
                return "jdbc:mariadb://" + hostName + ":" + port + "/" + databaseName
                        + "?sslMode=" + (sslEnabled ? "trust" : "disable");

            case SQLServer:
                return "jdbc:sqlserver://" + hostName + ":" + port
                        + ";databaseName=" + databaseName
                        + ";encrypt=" + sslEnabled
                        + ";trustServerCertificate=true";

            case Oracle:
                return "jdbc:oracle:thin:@" + hostName + ":" + port + ":" + databaseName;

            default:
                throw new IllegalArgumentException("Tipo de banco não suportado para teste de conexão: " + dsType);
        }
    }

    private Properties montarPropriedadesConexao(DataSourceType dsType,
                                                 String username,
                                                 String password,
                                                 boolean sslEnabled) {
        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);

        if (dsType == DataSourceType.PostgreSQL) {
            props.setProperty("loginTimeout", "5");
            if (sslEnabled) {
                props.setProperty("ssl", "true");
            } else {
                props.setProperty("ssl", "false");
            }
        }

        if (dsType == DataSourceType.MySQL || dsType == DataSourceType.MariaDB) {
            props.setProperty("connectTimeout", "5000");
            props.setProperty("socketTimeout", "5000");
        }

        if (dsType == DataSourceType.SQLServer) {
            props.setProperty("loginTimeout", "5");
        }

        if (dsType == DataSourceType.Oracle) {
            props.setProperty("oracle.net.CONNECT_TIMEOUT", "5000");
            props.setProperty("oracle.jdbc.ReadTimeout", "5000");
        }

        return props;
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
