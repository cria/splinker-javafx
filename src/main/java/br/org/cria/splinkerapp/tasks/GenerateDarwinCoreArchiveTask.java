package br.org.cria.splinkerapp.tasks;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import br.org.cria.splinkerapp.enums.EventTypes;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import javafx.concurrent.Task;

public class GenerateDarwinCoreArchiveTask extends Task<DarwinCoreArchiveService> {
    DarwinCoreArchiveService service;
    EventBus writeRowEventBus;

    public GenerateDarwinCoreArchiveTask(DarwinCoreArchiveService service)
    {
        this.service = service;
        writeRowEventBus = EventBusManager.getEvent(EventTypes.WRITE_ROW.name());
        writeRowEventBus.register(this);
    }

    @Subscribe
    void onWriteRow(Integer rowCount)
    {
        var totalRowCount = service.getTotalRowCount();
        updateProgress(rowCount, totalRowCount);// manager.getParser().getTotalRowCount());
    }


    @Override
    protected DarwinCoreArchiveService call() throws Exception {
        return service.readDataFromSource()
                      .generateTXTFile()
                      .generateZIPFile();
    }
    
}
