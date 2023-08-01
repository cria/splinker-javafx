module br.org.cria.splinkerapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires processoutput4j;
    requires rsync4j.all;
    requires rsync4j.core;
    requires rsync4j.windows64;
    requires org.apache.commons.lang3;
    requires yajsync.app;

    opens br.org.cria.splinkerapp.controllers to javafx.fxml;
    
    exports br.org.cria.splinkerapp;
}