package br.org.cria.splinkerapp.controllers;

import java.util.List;

import br.org.cria.splinkerapp.models.TransferHistoryDataSet;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;

import static br.org.cria.splinkerapp.services.implementations.DataSetService.getTransferHistory;

public class ReportController {


    @FXML private TableView<ObservableList<String>> tableView;
    @FXML private TableColumn<ObservableList<String>, String> colData;
    @FXML private TableColumn<ObservableList<String>, String> colRegistros;

    private final ObservableList<ObservableList<String>> dados = FXCollections.observableArrayList();

    @FXML
    public void initialize() throws Exception {
        // Configurar as colunas para buscar os valores pelo índice
        colData.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(0)));
        colRegistros.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(1)));

        // Permitir edição (opcional)
        colData.setCellFactory(TextFieldTableCell.forTableColumn());
        colRegistros.setCellFactory(TextFieldTableCell.forTableColumn());

        // Adicionar alguns dados de exemplo
        List<TransferHistoryDataSet> transferHistory = getTransferHistory();
        for (TransferHistoryDataSet transferHistoryDataSet : transferHistory) {
            adicionarRegistro(transferHistoryDataSet.getDate(),transferHistoryDataSet.getRowcount());
        }

        // Popular a tabela com os dados
        tableView.setItems(dados);
    }

    private void adicionarRegistro(String data, String registros) {
        ObservableList<String> linha = FXCollections.observableArrayList(data, registros);
        dados.add(linha);
    }
}
