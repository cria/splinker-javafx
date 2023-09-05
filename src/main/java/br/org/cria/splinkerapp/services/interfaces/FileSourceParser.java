package br.org.cria.splinkerapp.services.interfaces;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;

public abstract class FileSourceParser {
    abstract protected String buildCreateTableCommand();
    abstract protected String normalizeString(String str) ;
    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:splinker.db");
    }
    abstract protected void createTableBasedOnSheet() throws SQLException ;
    abstract protected List<String> getRowAsStringList(Row row, int numberOfCells);
    abstract protected void insertDataIntoTable() throws SQLException;
}
