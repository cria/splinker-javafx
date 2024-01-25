package br.org.cria.splinkerapp.events;

import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.tasks.ImportDataTask;
import javafx.concurrent.Task;

public class ImportDataEvent  implements IBaseEvent{

    DataSet ds;
    public ImportDataEvent(DataSet ds)
    {
        this.ds = ds;
    }

    @Override
    public Task<Void> getTask() throws Exception{
        return new ImportDataTask(ds);
    }
    
}
