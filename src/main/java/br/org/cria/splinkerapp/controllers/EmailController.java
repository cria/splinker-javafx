package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.enums.WindowSizes;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

import java.net.URL;
import java.util.ResourceBundle;

public class EmailController extends AbstractController {

    @FXML
    ComboBox<String> cmbAssunto;

    @Override
    protected void setScreensize() {
        var stage = getStage();
        stage.setWidth(WindowSizes.LARGE_RECTANGULAR_SCREEN_WIDTH);
        stage.setHeight(WindowSizes.LARGE_RECTANGULAR_SCREEN_HEIGHT);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbAssunto.getItems().add("Sugestões");
        cmbAssunto.getItems().add("Erro na transmissão");
        cmbAssunto.getItems().add("Outros");
    }
}
