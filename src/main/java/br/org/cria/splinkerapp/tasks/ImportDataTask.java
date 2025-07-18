package br.org.cria.splinkerapp.tasks;

import br.org.cria.splinkerapp.enums.EventTypes;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.managers.FileSourceManager;
import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.services.implementations.DataSetService;
import br.org.cria.splinkerapp.utils.SQLiteTableExtractor;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.concurrent.Task;

public class ImportDataTask extends Task<Void> {
    DataSet ds;
    EventBus readRowEventBus = EventBusManager.getEvent(EventTypes.READ_ROW.name());
    FileSourceManager manager;

    public ImportDataTask(DataSet ds) throws Exception {
        this.ds = ds;
        manager = new FileSourceManager(ds);
        readRowEventBus.register(this);
    }

    @Subscribe
    void onRowCountUpdate(Integer rowCount) {
        var totalRowCount = manager.getParser().getTotalRowCount();
        updateProgress(rowCount, totalRowCount);
    }

    @Override
    protected Void call() throws Exception {
        manager.importData(SQLiteTableExtractor.extrairTabelas(DataSetService.getSQLCommandFromApi(ds.getToken())));
        return null;
    }
}
