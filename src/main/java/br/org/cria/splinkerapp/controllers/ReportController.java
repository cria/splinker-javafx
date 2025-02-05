package br.org.cria.splinkerapp.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;

public class ReportController {


    @FXML private TableView<ObservableList<String>> tableView;
    @FXML private TableColumn<ObservableList<String>, String> colData;
    @FXML private TableColumn<ObservableList<String>, String> colRegistros;

    private final ObservableList<ObservableList<String>> dados = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Configurar as colunas para buscar os valores pelo índice
        colData.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(0)));
        colRegistros.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(1)));

        // Permitir edição (opcional)
        colData.setCellFactory(TextFieldTableCell.forTableColumn());
        colRegistros.setCellFactory(TextFieldTableCell.forTableColumn());

        // Adicionar alguns dados de exemplo

        adicionarRegistro("05/02/2025 07:56:12", "317171");
        adicionarRegistro("04/02/2025 10:35:21", "311456");
        adicionarRegistro("04/02/2025 09:45:59", "279912");
        adicionarRegistro("03/02/2025 20:00:21", "277129");
        adicionarRegistro("03/02/2025 16:33:45", "227311");
        adicionarRegistro("03/02/2025 12:15:18", "226712");

        // Popular a tabela com os dados
        tableView.setItems(dados);
    }

    private void adicionarRegistro(String data, String registros) {
        ObservableList<String> linha = FXCollections.observableArrayList(data, registros);
        dados.add(linha);
    }
}
