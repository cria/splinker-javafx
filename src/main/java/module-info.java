module br.org.cria.splinkerapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.apache.commons.lang3;
    requires yajsync;
    requires calcite.core;
    requires calcite.linq4j;
    requires proj4j;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires org.apache.commons.compress;
    requires org.checkerframework.checker.qual;

    opens br.org.cria.splinkerapp.controllers to javafx.fxml;
    exports br.org.cria.splinkerapp;
}