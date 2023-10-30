module br.org.cria.splinkerapp {
    requires javafx.controlsEmpty;
    requires javafx.fxmlEmpty;
    requires java.sql;
    requires org.apache.commons.lang3;
    requires yajsync;

    opens br.org.cria.splinkerapp.controllers to javafx.fxml;
    
    exports br.org.cria.splinkerapp;
}