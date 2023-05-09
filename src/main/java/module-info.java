module br.org.cria.splinkerapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens br.org.cria.splinkerapp to javafx.fxml;
    exports br.org.cria.splinkerapp;
}