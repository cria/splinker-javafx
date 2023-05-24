package br.org.cria.splinkerapp.controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

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



    @FXML
    void onSaveButtonClicked() throws ClassNotFoundException{
        
        Class.forName("com.mysql.cj.jdbc.Driver");  
        String url = "jdbc:mysql://localhost:3306/bruno_testdb";
        String username = usernameField.getText();
        String password = passwordField.getText();

        try(Connection connection = DriverManager.getConnection(url, username, password)) {
            Statement statement = connection.createStatement();

            String sql = "SELECT * FROM %s;".formatted(tablenameField.getText());

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
