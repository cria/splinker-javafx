package br.org.cria.splinkerapp.utils;

import br.org.cria.splinkerapp.i18n.I18n;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.web.WebView;

/** Donation page embedded in the main application's content area. */
public final class SupportBrowser extends BorderPane implements AutoCloseable {
    public static final String DONATION_URL = "https://doacao.cria.org.br/?utm_source=sitecria&ref=cria.org.br";

    private final WebView webView = new WebView();

    public SupportBrowser() {
        setMinSize(0, 0);
        webView.setMinSize(0, 0);
        webView.setPrefSize(0, 0);

        var engine = webView.getEngine();
        webView.setId("supportWebView");
        var reloadIcon = new SVGPath();
        reloadIcon.setContent("M 18 7 A 8 8 0 1 0 20 15 M 18 2 L 18 7 L 13 7");
        reloadIcon.getStyleClass().add("reload-icon");
        var reload = new Button();
        reload.setGraphic(reloadIcon);
        reload.getStyleClass().add("reload-button");
        reload.setAccessibleText(I18n.text("support.reload"));
        reload.setTooltip(new Tooltip(I18n.text("support.reload")));
        reload.setOnAction(event -> engine.reload());
        var toolbar = new HBox(12, reload);
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        toolbar.setPadding(new Insets(12));

        var progress = new ProgressBar();
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.progressProperty().bind(engine.getLoadWorker().progressProperty());
        progress.visibleProperty().bind(engine.getLoadWorker().runningProperty());
        progress.managedProperty().bind(progress.visibleProperty());
        var error = new Label(I18n.text("support.load.failed"));
        error.setId("supportLoadError");
        error.setWrapText(true);
        error.getStyleClass().add("info-note");
        error.visibleProperty().bind(engine.getLoadWorker().stateProperty().isEqualTo(Worker.State.FAILED));
        error.managedProperty().bind(error.visibleProperty());

        setCenter(webView);
        setTop(new VBox(toolbar, progress, error));
        getStylesheets().add(SupportBrowser.class.getResource(
                "/br/org/cria/splinkerapp/styles.css").toExternalForm());

        // Keep links requesting a new window inside the same content area.
        engine.setCreatePopupHandler(features -> engine);
        engine.setOnAlert(event -> {
            var alert = AppTheme.apply(new Alert(Alert.AlertType.INFORMATION, event.getData(), ButtonType.OK));
            alert.initOwner(getScene().getWindow());
            alert.showAndWait();
        });
        engine.setConfirmHandler(message -> {
            var alert = AppTheme.apply(new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL));
            alert.initOwner(getScene().getWindow());
            return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
        });
        sceneProperty().addListener((observable, previous, current) -> {
            if (previous != null && current == null) close();
        });
    }

    public void openDonationPage() {
        webView.getEngine().load(DONATION_URL);
    }

    @Override
    public void close() {
        var engine = webView.getEngine();
        engine.getLoadWorker().cancel();
        engine.load(null);
    }
}
