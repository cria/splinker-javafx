module br.org.cria.splinkerapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens br.org.cria.splinkerapp.controllers to javafx.fxml;
    
    exports br.org.cria.splinkerapp;
}