module br.org.cria.splinkerapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires rsync4j.all;
    requires rsync4j.core;
    requires rsync4j.windows64;
    requires processoutput4j;
    requires jsch;
    
    opens br.org.cria.splinkerapp.controllers to javafx.fxml;
    
    exports br.org.cria.splinkerapp;
}