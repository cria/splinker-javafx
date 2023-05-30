package br.org.cria.splinkerapp.controllers;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

import com.github.fracpete.processoutput4j.output.CollectingProcessOutput;
import com.github.fracpete.rsync4j.RSync;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.Pane;

public class HomeController extends AbstractController{

    @FXML
    Pane pane;

    @FXML
    MenuBar menuBar;

    @FXML
    Button syncServerBtn;
    
    @FXML
    Button syncMetaDataBtn;

    @FXML
    void onSyncServerBtnClicked() throws Exception
    {
      try {
        var username = "bruno";
        var hostname = "35.224.172.146";
        var port = 22;
        var password = "cria@1234";
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, hostname, port);
        session.setPassword(password);
  
        // Disable strict host key checking
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
  
        session.connect();
 
        RSync rsync = new RSync()
    .source("/Users/brunobemfica/Downloads/place/dwca-amnh_birds-v3.0.zip")
    .destination("rsync://35.224.172.146:22/home/bruno/")
    .recursive(true);

    CollectingProcessOutput output = rsync.execute(); //new ConsoleOutputProcessOutput();
    //output.monitor(rsync.builder());

      } catch (Exception e) {
        e.printStackTrace();
      }
     

    }
    
    @FXML
    void onSyncMetadataBtnClicked(){ }

    @Override
    protected Pane getPane() {
        return this.pane;
    }
    
}
