package br.org.cria.splinkerapp.controllers;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.Currency;
import java.util.Iterator;
import java.util.List;

import com.github.fracpete.processoutput4j.output.CollectingProcessOutput;
import com.github.fracpete.processoutput4j.output.ConsoleOutputProcessOutput;
import com.github.fracpete.rsync4j.RSync;
import com.github.fracpete.rsync4j.Ssh;
import com.github.fracpete.rsync4j.SshPass;
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
    void onSyncMetadataBtnClicked(){

      try {
        System.setProperty("java.net.useSystemProxies","true");
        List<Proxy> l = ProxySelector.getDefault().select(
                    new URI("http://www.yahoo.com/"));

        for (Iterator<Proxy> iter = l.iterator(); iter.hasNext(); ) {

          // proxy hostname : HTTP
          // proxy host string: 127.0.0.1
          // proxy address : null
          // proxy hostname : 127.0.0.1
          // proxy port : 8866
          

            Proxy proxy = iter.next();

            System.out.println("proxy hostname : " + proxy.type());

            InetSocketAddress addr = (InetSocketAddress)proxy.address();

            if(addr == null) {

                System.out.println("No Proxy");

            } else {
              var user = System.getProperty("http.proxyUser", System.getProperty("https.proxyUser"));
              var pwd = System.getProperty("http.proxyPassword", System.getProperty("https.proxyPassword"));
              var port = System.getProperty("http.proxyPassword", System.getProperty("https.proxyPort"));
              System.out.println("proxy host string: " +addr.getHostString());
                System.out.println("proxy address : " + addr.getAddress());
                System.out.println("proxy hostname : " + addr.getHostName());
                System.out.println("proxy port : " + addr.getPort());
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

\    }

    @Override
    protected Pane getPane() {
        return this.pane;
    }
    
}
