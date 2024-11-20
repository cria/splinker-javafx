package br.org.cria.splinkerapp.utils;

import javafx.scene.control.Alert;
import javafx.stage.Modality;

public class ModalAlertUtil {

    public static void show(String conteudo) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(conteudo);
        alert.showAndWait();
    }
}
