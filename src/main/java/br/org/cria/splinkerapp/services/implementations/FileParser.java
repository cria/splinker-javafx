package br.org.cria.splinkerapp.services.implementations;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public abstract class FileParser {
    protected Connection getConnection() throws SQLException { return DriverManager.getConnection("jdbc:sqlite:splinker.db"); }

    public void extractColumnNames(){}
    public void readRows(){};
    public void createTable(){};
    public void insertData(){};
    public abstract List<String> getRowAsStringList();
    protected String normalizeString(String str) 
    {
        return StringUtils.stripAccents(str.toLowerCase()).replace(" ", "_")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "_").trim();
    }

    
}
