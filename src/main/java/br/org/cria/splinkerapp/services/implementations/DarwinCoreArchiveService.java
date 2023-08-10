package br.org.cria.splinkerapp.services.implementations;
import br.org.cria.splinkerapp.models.DataSource;
import br.org.cria.splinkerapp.services.interfaces.IDarwinCoreArchiveService;
import com.github.perlundq.yajsync.ui.YajsyncClient;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import java.io.PrintStream;

public class DarwinCoreArchiveService implements IDarwinCoreArchiveService{

    @Override
    public DarwinCoreArchiveService generateTXTFile() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generateTXTFile'");
    }

    @Override
    public DarwinCoreArchiveService generateZIPFile() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generateZIPFile'");
    }

    @Override
    public DarwinCoreArchiveService readDataFromSource(DataSource source) {
        throw new UnsupportedOperationException("Unimplemented method 'generateZIPFile'");
    }

    @Override
    public DarwinCoreArchiveService transferData() {
        try
        {
            var port = 10_000;
            var source = ConfigurationData.getDarwinCoreFileSourcePath();
            var destination = ConfigurationData.getTransferDataDestination();
            var command = new String[]{"--port=%s".formatted(port), "-r", source, destination};
            var client = new YajsyncClient();
            var stream = new PrintStream("/Users/brunobemfica/Downloads/splinker_teste.txt");
            client.setStandardOut(stream);
            var service = new Service<Void>() {

                @Override
                protected Task<Void> createTask() {
                    return new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            var session = client.start(command);
                            stream.flush();
                            stream.close();
                            return null;
                        }
                    };
                }
            };
            service.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

}
