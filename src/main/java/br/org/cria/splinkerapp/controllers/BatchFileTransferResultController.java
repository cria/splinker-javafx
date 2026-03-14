package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.models.TransferResult;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class BatchFileTransferResultController extends AbstractController {

    private static final List<TransferResult> RESULTS = new ArrayList<>();

    @FXML
    private TableView<TransferResult> tblResultados;

    @FXML
    private TableColumn<TransferResult, String> colToken;

    @FXML
    private TableColumn<TransferResult, String> colStatus;

    @FXML
    private TableColumn<TransferResult, String> colMensagem;

    @FXML
    private Button btnFechar;

    /**
     * Recebe os resultados do batch antes da navegação para a tela
     */
    public static void setResults(List<TransferResult> results) {
        RESULTS.clear();
        RESULTS.addAll(results);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        colToken.setCellValueFactory(new PropertyValueFactory<>("token"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colMensagem.setCellValueFactory(new PropertyValueFactory<>("message"));

        tblResultados.setItems(FXCollections.observableArrayList(RESULTS));

        configurarCoresDasLinhas();

        btnFechar.setOnAction(e -> navigateTo("home"));
    }

    /**
     * Colore linhas de sucesso e erro
     */
    private void configurarCoresDasLinhas() {

        tblResultados.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(TransferResult item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setStyle("");
                    return;
                }

                if (item.isSuccess()) {
                    setStyle("-fx-background-color: #e8f5e9;");
                } else {
                    setStyle("-fx-background-color: #fdecea;");
                }
            }
        });
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_RECTANGULAR_SCREEN_HEIGHT);
    }
}