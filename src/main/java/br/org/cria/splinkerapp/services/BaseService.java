package br.org.cria.splinkerapp.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class BaseService {
    
    protected static Connection getConnection() throws SQLException 
    {
        String url = "jdbc:sqlite:splinker.db";
        return DriverManager.getConnection(url);
    }
}
