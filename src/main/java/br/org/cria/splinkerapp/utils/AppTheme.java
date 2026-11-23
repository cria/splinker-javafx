package br.org.cria.splinkerapp.utils;

import javafx.scene.control.Dialog;

/** Applies the same visual theme to dialogs created outside the FXML router. */
public final class AppTheme {
    private AppTheme() { }

    public static <T extends Dialog<?>> T apply(T dialog) {
        String stylesheet = AppTheme.class.getResource(
                "/br/org/cria/splinkerapp/styles.css").toExternalForm();
        var stylesheets = dialog.getDialogPane().getStylesheets();
        if (!stylesheets.contains(stylesheet)) stylesheets.add(stylesheet);
        return dialog;
    }
}
