package br.org.cria.splinkerapp.controllers;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.facade.ConfigFacade;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.models.DataSourceType;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.utils.ModalAlertUtil;
import com.google.common.eventbus.EventBus;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

public class TokenLoginController extends AbstractController {

    @FXML
    Button btnAddToken;
    @FXML
    Button btnDeleteToken;

    @FXML
    TextField tokenField;

    @FXML
    private VBox tokenBox;

    private EventBus bus;

    @FXML
    void onButtonDeleteTokenClicked() {
        try {
            var tokenToBeDeleted = tokenField.getText().trim();
            var tokenIsNotEmpty = (tokenToBeDeleted != null) && (!tokenToBeDeleted.isEmpty());
            var tokenExists = DataSetService.getDataSet(tokenToBeDeleted) != null;
            var currentToken = TokenRepository.getCurrentToken();

            if (tokenIsNotEmpty && tokenExists) {
                if (tokenToBeDeleted.equals(currentToken)) {
                    ModalAlertUtil.show("Você não pode excluir o token que está logado!");
                    return;
                }
                DataSetService.deleteDataSet(tokenToBeDeleted);
                var datasets = DataSetService.getAllDataSets();
                token = datasets.getFirst().getToken();
                TokenRepository.setCurrentToken(token);
                navigateTo(getStage(), "home");
            } else {
                ModalAlertUtil.show("Token inválido ou inexistente!");
            }
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    @FXML
    void onButtonAddTokenClicked() {
        try {

            var newToken = tokenField.getText().trim();
            if (newToken == null || newToken.trim().isEmpty()) {
                showErrorModal("Token não pode ser vazio!");
                return;
            }
            var tokenExists = DataSetService.getDataSet(newToken) != null;
            var hasConfig = DataSetService.hasConfiguration();
            if (tokenExists && hasConfig) {
                showErrorModal("Token já existente!");
                return;
            }
            var apiConfig = DataSetService.getConfigurationDataFromAPI(newToken);
            if (apiConfig != null) {
                var routeName = "collection-database";
                var collName = apiConfig.get("dataset_name").toString();
                var datasetAcronym = apiConfig.get("dataset_acronym").toString();
                var id = (int) Double.parseDouble(apiConfig.get("dataset_id").toString());
                TokenRepository.setCurrentToken(newToken);
                if (apiConfig.get("data_source_type") == null) {
                    showErrorModal("Esta coleção ainda não foi configurada no servidor. Favor entrar em contato com o CRIA.");
                    return;
                }
                String datasouceType = apiConfig.get("data_source_type").toString();
                var dsType = DataSourceType.valueOf(datasouceType);
                if (!tokenExists) {
                    DataSetService.saveDataSet(newToken, dsType, datasetAcronym, collName, id);
                }
                ConfigFacade.HandleBackendData(newToken, apiConfig);
                bus.post(newToken);
                switch (dsType) {
                    case Access:
                        routeName = "access-db-modal";
                        break;
                    case dBase:
                    case Excel:
                    case LibreOfficeCalc:
                    case CSV:
                    case Numbers:
                        routeName = "file-selection";
                        break;
                    default:
                        break;
                }

                if (!hasConfig) {
                    navigateTo(getStage(), routeName);
                } else {
                    navigateTo(getStage(), "home");
                }
            }
        } catch (Exception e) {
            handleErrors(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        bus = new EventBus();

        Tooltip tooltip = new Tooltip("Token é um código único fornecido pelo CRIA para sua coleção poder enviar dados. Entre em contato com o CRIA se não tiver ainda recebido seu token.");

        tokenField.setOnMouseEntered(event -> {
            tooltip.show(tokenField, event.getScreenX(), event.getScreenY() + 15);
        });

        tokenField.setOnMouseExited(event -> {
            tooltip.hide();
        });

        tokenBox.setOnMouseEntered(event -> {
            tooltip.show(tokenBox, event.getScreenX(), event.getScreenY() + 15);
        });

        tokenBox.setOnMouseExited(event -> {
            tooltip.hide();
        });

        List<DataSet> tokens = null;
        try {
            tokens = DataSetService.getAllDataSets();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (tokens.isEmpty()) {
            btnDeleteToken.setVisible(false);
            btnAddToken.setLayoutX(194);
        } else {
            btnDeleteToken.setVisible(true);
        }


    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.SMALL_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.SMALL_RECTANGULAR_SCREEN_HEIGHT);
    }
}
