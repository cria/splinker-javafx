package br.org.cria.splinkerapp.tasks;

import java.time.Instant;
import java.time.ZoneId;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.enums.EventTypes;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.managers.FileSourceManager;
import br.org.cria.splinkerapp.models.DataSet;
import javafx.concurrent.Task;

public class ImportDataTask extends Task<Void> {
    DataSet ds;
    EventBus readRowEventBus = EventBusManager.getEvent(EventTypes.READ_ROW.name());
    FileSourceManager manager;
    
    public ImportDataTask(DataSet ds) throws Exception
    {
        this.ds = ds;
        manager = new FileSourceManager(ds);
        readRowEventBus.register(this);
    }

    @Subscribe
    void onRowCountUpdate(Integer rowCount)
    {
        var totalRowCount = manager.getParser().getTotalRowCount();
        updateProgress(rowCount, totalRowCount);
    }

    @Override
    protected Void call() throws Exception 
    {
        var now = Instant.now().atZone(ZoneId.systemDefault());
        var message = "%s - Iniciando a importação do arquivo".formatted(now.toLocalDateTime());
        ApplicationLog.info(message);
        System.out.println(message);
        manager.importData();
        return null;
    }
}
