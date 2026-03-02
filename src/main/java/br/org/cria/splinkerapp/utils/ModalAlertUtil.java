package br.org.cria.splinkerapp.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.stage.Modality;

public class ModalAlertUtil {

    public static void show(String conteudo, String detalhe) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(conteudo);

        alert.getDialogPane().setExpandableContent(null);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);


        alert.getDialogPane().getChildren().stream()
                .filter(node -> node instanceof Label)
                .map(node -> (Label) node)
                .forEach(label -> label.setWrapText(true));

        if (detalhe != null) {
            TextArea textArea = new TextArea(detalhe);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);

            alert.getDialogPane().setExpandableContent(textArea);
            alert.getDialogPane().setExpanded(false);
        }

        alert.showAndWait();
    }
}
