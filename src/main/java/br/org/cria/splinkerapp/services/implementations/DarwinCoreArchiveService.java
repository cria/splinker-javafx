package br.org.cria.splinkerapp.services.implementations;
import br.org.cria.splinkerapp.models.DataSource;
import br.org.cria.splinkerapp.services.interfaces.IDarwinCoreArchiveService;
import com.github.perlundq.yajsync.ui.YajsyncClient;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

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
            //TODO: Ler source e user dir do BD local
            var source = "/Users/brunobemfica/Downloads/dwca-tropicosspecimens-v1.124.zip.old.zip";
            var destination = "bruno@34.68.143.184::meu_modulo";
            var command = new String[]{"--port=%s".formatted(port), "-r", source, destination};
            var client = new YajsyncClient();
            var service = new Service<Void>() {

                @Override
                protected Task<Void> createTask() {
                    return new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            var session = client.start(command);
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
