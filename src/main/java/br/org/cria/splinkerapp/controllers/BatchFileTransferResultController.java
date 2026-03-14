package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.enums.WindowSizes;
import br.org.cria.splinkerapp.models.TransferResult;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class BatchFileTransferResultController extends AbstractController {

    @FXML
    private TableView<TransferResult> tblResultados;

    @FXML
    private TableColumn<TransferResult, String> colStatusIcon;

    @FXML
    private TableColumn<TransferResult, String> colToken;

    @FXML
    private TableColumn<TransferResult, String> colStatus;

    @FXML
    private TableColumn<TransferResult, String> colMensagem;

    @FXML
    private TableColumn<TransferResult, Void> colAcao;

    @FXML
    private Button btnFechar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colStatusIcon.setCellValueFactory(new PropertyValueFactory<>("statusIcon"));
        colToken.setCellValueFactory(new PropertyValueFactory<>("token"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colMensagem.setCellValueFactory(new PropertyValueFactory<>("message"));

        tblResultados.setItems(FXCollections.observableArrayList(BatchTransferContext.getResults()));

        configurarCoresDasLinhas();
        configurarColunaAcao();

        btnFechar.setOnAction(e -> {
            BatchTransferContext.clear();
            navigateTo("home");
        });
    }

    private void configurarCoresDasLinhas() {
        tblResultados.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(TransferResult item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setStyle("");
                } else if (item.isPendent()) {
                    setStyle("-fx-background-color: #fff8e1;");
                } else if (item.isSuccess()) {
                    setStyle("-fx-background-color: #e8f5e9;");
                } else {
                    setStyle("-fx-background-color: #fdecea;");
                }
            }
        });
    }

    private void configurarColunaAcao() {
        colAcao.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Logs");

            {
                btn.setOnAction(event -> {
                    TransferResult result = getTableView().getItems().get(getIndex());
                    if (result != null && result.hasErrorLog()) {
                        exibirLogErro(result);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                TransferResult result = getTableView().getItems().get(getIndex());

                if (result != null && !result.isPendent() && !result.isSuccess() && result.hasErrorLog()) {
                    setGraphic(btn);
                } else {
                    setGraphic(null);
                }
            }
        });
    }

    private void exibirLogErro(TransferResult result) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Log do erro");
        alert.setHeaderText("Coleção: " + result.getToken());

        TextArea textArea = new TextArea(result.getErrorLog());
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setPrefWidth(550);
        textArea.setPrefHeight(250);

        alert.getDialogPane().setContent(textArea);
        alert.setResizable(true);
        alert.showAndWait();
    }

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_RECTANGULAR_SCREEN_HEIGHT);
    }
}