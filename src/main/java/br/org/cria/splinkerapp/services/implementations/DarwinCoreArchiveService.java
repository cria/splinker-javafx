package br.org.cria.splinkerapp.services.implementations;
import br.org.cria.splinkerapp.models.DataSource;
import br.org.cria.splinkerapp.services.interfaces.IDarwinCoreArchiveService;
import com.github.perlundq.yajsync.ui.YajsyncClient;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DarwinCoreArchiveService implements IDarwinCoreArchiveService{
    static String zipFile = "%s/spLinker_dwca.zip".formatted(System.getProperty("user.dir"));
    static String textFile = "%s/spLinker_dwca.txt".formatted(System.getProperty("user.dir"));
    @Override
    public DarwinCoreArchiveService generateTXTFile() {
        var file = new File(textFile);
        if(!file.exists())
        {
            try {
                file.createNewFile();
                return this;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        throw new UnsupportedOperationException("Unimplemented method 'generateTXTFile'");
    }

    @Override
    public DarwinCoreArchiveService generateZIPFile() {

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile)))
        {
            File fileToZip = new File(textFile);
            zipOut.putNextEntry(new ZipEntry(fileToZip.getName()));
            Files.copy(fileToZip.toPath(), zipOut);
            return this;
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public DarwinCoreArchiveService readDataFromSource(DataSource source) {
        throw new UnsupportedOperationException("Unimplemented method 'generateZIPFile'");
    }

    @Override
    public DarwinCoreArchiveService transferData() {
        try
        {
            //TODO: Ler map.dat e db.dat das coleções.
            //Esses arquivos são combinados para montar o select dos dados
            // nas fontes de dados das coleções (DB, planilhas, Brahms, etc)
            // O que não for planilha, é banco sempre
            var port = ConfigurationData.getRSyncPort();
            var source = "%s/splinker_dwca.zip".formatted(System.getProperty("user.dir"));
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
                            stream.println();
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
