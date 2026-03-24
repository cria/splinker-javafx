package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.repositories.TokenRepository;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.util.StringConverter;
import org.controlsfx.control.CheckComboBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class BatchSelectFileTransferController extends AbstractController {

    @FXML
    private Button btnEnviar;

    @FXML
    private Button btnCancelar;

    @FXML
    private CheckComboBox<String> cmbToken;
    

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_RECTANGULAR_SCREEN_HEIGHT);
    }

    private static final String SELECT_ALL = "Selecionar todas";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            cmbToken.getItems().add(SELECT_ALL);
            cmbToken.getItems().addAll(TokenRepository.getAcronyms());
            cmbToken.setTitle("Selecione as coleções ("+0+"/"+(cmbToken.getItems().size()-1)+")");
            cmbToken.setShowCheckedCount(false);

            configurarSelectAll();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        btnEnviar.setOnAction(e -> enviar());
        btnCancelar.setOnAction(e ->  navigateTo("home"));
    }

    private boolean updating = false;

    private void configurarSelectAll() {
        var checkModel = cmbToken.getCheckModel();

        checkModel.getCheckedItems().addListener((ListChangeListener<String>) change -> {
            if (updating) return;

            updating = true;

            try {
                while (change.next()) {

                    if (change.getAddedSubList().contains(SELECT_ALL)) {
                        marcarTodos();
                    }

                    if (change.getRemoved().contains(SELECT_ALL)) {
                        desmarcarTodos();
                    }
                }

                long count = cmbToken.getItems().stream()
                        .filter(i -> !i.equals(SELECT_ALL))
                        .filter(checkModel::isChecked)
                        .count();

                //cmbToken.setTitle(count + " coleções selecionadas");
                cmbToken.setTitle("Selecione as coleções ("+count+"/"+(cmbToken.getItems().size()-1)+")");
                atualizarSelectAll();
            } finally {
                updating = false;
            }
        });
    }

    private void marcarTodos() {
        var checkModel = cmbToken.getCheckModel();

        for (String item : cmbToken.getItems()) {
            checkModel.check(item);
        }
    }

    private void desmarcarTodos() {
        var checkModel = cmbToken.getCheckModel();

        checkModel.clearChecks();
    }

    private void enviar() {
        List<String> colecoesSelecionadas = new ArrayList<>(cmbToken.getCheckModel().getCheckedItems());

        colecoesSelecionadas.remove(SELECT_ALL);
        if (colecoesSelecionadas.isEmpty()) {
            showAlert("Erro", "Por favor, selecione pelo menos uma coleção.");
            return;
        }

        if (colecoesSelecionadas.size() == 1) {
            String acrony = colecoesSelecionadas.getFirst();
            String token = TokenRepository.getCurrentTokenByAcronym(acrony);
            TokenRepository.setCurrentToken(token);
            navigateTo("file-transfer");
            return;
        }

        BatchTransferContext.setSelectedCollections(colecoesSelecionadas);
        navigateTo("batch-file-transfer-progress");
    }

    private void atualizarSelectAll() {
        var checkModel = cmbToken.getCheckModel();

        boolean todosSelecionados = cmbToken.getItems().stream()
                .filter(item -> !item.equals(SELECT_ALL))
                .allMatch(item -> checkModel.isChecked(item));

        if (todosSelecionados) {
            checkModel.check(SELECT_ALL);
        } else {
            checkModel.clearCheck(SELECT_ALL);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}