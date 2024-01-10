package br.org.cria.splinkerapp.tasks;

import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import javafx.concurrent.Task;

public class GenerateDarwinCoreArchiveTask extends Task<DarwinCoreArchiveService> {
    DarwinCoreArchiveService service;

    public GenerateDarwinCoreArchiveTask(DarwinCoreArchiveService service)
    {
        this.service = service;
    }

    @Override
    protected DarwinCoreArchiveService call() throws Exception {
        return service.readDataFromSource()
                      .generateTXTFile()
                      .generateZIPFile();
    }
    
}
