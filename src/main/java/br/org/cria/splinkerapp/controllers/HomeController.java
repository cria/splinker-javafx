package br.org.cria.splinkerapp.controllers;

import br.org.cria.splinkerapp.config.ConnectionSetup;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import org.apache.commons.lang3.time.StopWatch;
import com.github.fracpete.processoutput4j.output.ConsoleOutputProcessOutput;
import com.github.fracpete.rsync4j.RSync;
import com.github.fracpete.rsync4j.core.Binaries;
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
      try 
      {
   
        ConnectionSetup.firstConnection();
        var sshKey = "%s -i /Users/brunobemfica/.ssh/splinker.pub".formatted(Binaries.sshBinary());
        RSync rsync = new RSync()
        .source("/Users/brunobemfica/Downloads/dwca-tropicosspecimens-v1.124.zip")
        .destination("bruno@35.224.172.146:/home/bruno/")
        .recursive(true).progress(true)
        .rsh(sshKey);
        var out = new ConsoleOutputProcessOutput();
        var executor = Executors.newSingleThreadExecutor();
        StopWatch watch = new StopWatch();

        executor.submit(() -> {
          try 
          
          {
                  watch.start();
                  out.monitor(rsync.builder());
                  var output = rsync.execute();
                  watch.stop();
                  var timedOut = output.hasTimedOut() ;//new ConsoleOutputProcessOutput();
                  var time = watch.getTime(TimeUnit.SECONDS);
                  System.out.printf("Time is %s seconds", time);
                  while(!output.hasSucceeded())
                  {
                    var result = output.getStdOut();
                    var err = output.getStdErr();
                    var code = output.getExitCode();
                    System.out.printf("Stdout is %s%n", result);
                    System.out.printf("StdErr is %s%n", err);
                    System.out.printf("ExitCode is %s%n", code);
                  }
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
          

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
