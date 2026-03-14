package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import org.controlsfx.control.CheckComboBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SelectFileTransferController extends AbstractController {

    @FXML
    private Button btnEnviar;

    @FXML
    private CheckComboBox<String> cmbToken;
    

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_RECTANGULAR_SCREEN_HEIGHT);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            cmbToken.getItems().addAll(TokenRepository.getAcronyms());
            cmbToken.setTitle("Selecione as coleções");
            cmbToken.setShowCheckedCount(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        btnEnviar.setOnAction(e -> enviar());
    }

    private void enviar() {
        List<String> colecoesSelecionadas = new ArrayList<>(cmbToken.getCheckModel().getCheckedItems());

        if (colecoesSelecionadas.isEmpty()) {
            showAlert("Erro", "Por favor, selecione pelo menos uma coleção.");
            return;
        }

        BatchTransferContext.setSelectedCollections(colecoesSelecionadas);
        loadPage("batch-file-transfer-progress");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}