package br.org.cria.splinkerapp.events;

import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import br.org.cria.splinkerapp.tasks.TransferFileTask;

public class TransferFileEvent implements IBaseEvent{
    DarwinCoreArchiveService service;
    
    public TransferFileEvent(DarwinCoreArchiveService service)
    { 
        this.service = service;
    }

    public TransferFileTask getTask()
    {
        return new TransferFileTask(service);
    }
}
