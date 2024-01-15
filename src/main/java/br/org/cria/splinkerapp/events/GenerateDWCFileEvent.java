package br.org.cria.splinkerapp.events;

import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import br.org.cria.splinkerapp.tasks.GenerateDarwinCoreArchiveTask;
import javafx.concurrent.Task;

public class GenerateDWCFileEvent implements IBaseEvent {
    DarwinCoreArchiveService service;

    public GenerateDWCFileEvent(DarwinCoreArchiveService service)
    {
        this.service = service;
    }
    

    @Override
    public Task getTask() {
        return new GenerateDarwinCoreArchiveTask(service);
    }
    
}
