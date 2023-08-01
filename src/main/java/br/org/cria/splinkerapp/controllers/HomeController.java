package br.org.cria.splinkerapp.controllers;


import com.github.perlundq.yajsync.ui.YajsyncClient;

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
   
        var command = new String[]{"--port=10000", "-r", "~/Downloads/debian-12.0.0-arm64-netinst.iso", "bruno@35.224.172.146::meu_modulo"};
        var session = new YajsyncClient().start(command);
        System.out.println("yajsync result is "+ session);
        // ConnectionSetup.firstConnection();
        // var sshKey = "%s -i /Users/brunobemfica/.ssh/splinker.pub".formatted(Binaries.sshBinary());
        // RSync rsync = new RSync()
        // .source("/Users/brunobemfica/Downloads/dwca-tropicosspecimens-v1.124.zip")
        // .destination("user_copy@172.10.0.2")
        // .port(2222)
        // .recursive(true).progress(true)
        // .rsh(sshKey);
        // var out = new ConsoleOutputProcessOutput();
        // var executor = Executors.newSingleThreadExecutor();
        // StopWatch watch = new StopWatch();

        // executor.submit(() -> {
        //   try 
          
        //   {
        //           watch.start();
        //           out.monitor(rsync.builder());
        //           var output = rsync.execute();
        //           watch.stop();
        //           var timedOut = output.hasTimedOut() ;//new ConsoleOutputProcessOutput();
        //           var time = watch.getTime(TimeUnit.SECONDS);
        //           System.out.printf("Time is %s seconds", time);
        //           while(!output.hasSucceeded())
        //           {
        //             var result = output.getStdOut();
        //             var err = output.getStdErr();
        //             var code = output.getExitCode();
        //             System.out.printf("Stdout is %s%n", result);
        //             System.out.printf("StdErr is %s%n", err);
        //             System.out.printf("ExitCode is %s%n", code);
        //           }
        //   } catch (Exception e) {
        //     e.printStackTrace();
        //   }
        // });
          //rsync -Pav -e "ssh -f -p 2222 -i docker_splinker" dwca-tropicosspecimens-v1.124.zip.old.zip user_copy@172.10.0.2:/home/user_copy/zip_files


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
