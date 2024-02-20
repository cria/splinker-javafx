package br.org.cria.splinkerapp.tasks;

import java.time.Instant;
import java.time.ZoneId;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import javafx.concurrent.Task;

public class TransferFileTask extends Task<Void> {

    DarwinCoreArchiveService service;
    public TransferFileTask(DarwinCoreArchiveService service)
    {
        this.service = service;
    }

    @Override
    protected Void call() throws Exception {
        service.transferData();
        return null;
    }
    
}
