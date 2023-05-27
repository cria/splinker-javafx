package br.org.cria.splinkerapp.controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
/*
 * Classe responsável pelo formulário de configuração de banco de dados
 */
public class CollectionLocalDatabaseController extends AbstractController{

    @FXML
    AnchorPane pane;
    @FXML
    TextField usernameField;
    @FXML
    TextField passwordField;
    @FXML
    TextField tablenameField;
    @FXML
    TextField portField;
    @FXML
    Button saveBtn;

    boolean testConnection(){
        var url = "jdbc:mysql://localhost:3306/bruno_testdb";
        var username = usernameField.getText();
        var password = passwordField.getText();
        var tableName = tablenameField.getText();
        var isConnectionValid = true;
        try(Connection connection = DriverManager.getConnection(url, username, password)) {
            Statement statement = connection.createStatement();

            String sql = "SELECT * FROM %s LIMIT 1;".formatted(tableName);

            statement.executeQuery(sql);

        } catch (Exception e) {
            e.printStackTrace();
            isConnectionValid = false;
        }
        return isConnectionValid;
    }


    @FXML
    void onSaveButtonClicked() throws ClassNotFoundException{
        
        String url = "jdbc:mysql://localhost:3306/bruno_testdb";
        String username = usernameField.getText();
        String password = passwordField.getText();

        try(Connection connection = DriverManager.getConnection(url, username, password)) {
            Statement statement = connection.createStatement();

            String sql = "SELECT * FROM %s LIMIT 1;".formatted(tablenameField.getText());

            var result = statement.executeQuery(sql);
            while(result.next()){
                var name = result.getString("name");
                var address = result.getString("address");
                var phone = result.getString("phone");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    @Override
    protected Pane getPane() { return this.pane; }
    
}
