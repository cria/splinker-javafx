module br.org.cria.splinkerapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.google.gson;
    requires org.apache.poi.ooxml.schemas;
    requires com.google.common;
    requires org.apache.commons.lang3;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires com.github.albfernandez.javadbf;
    requires com.opencsv;
    requires yajsync;
    requires  com.github.miachm.sods;
    opens br.org.cria.splinkerapp.controllers to javafx.fxml;

    exports br.org.cria.splinkerapp;
}