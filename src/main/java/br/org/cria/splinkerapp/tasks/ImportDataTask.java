package br.org.cria.splinkerapp.tasks;

import java.time.Instant;
import java.time.ZoneId;
import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.managers.FileSourceManager;
import br.org.cria.splinkerapp.models.DataSet;
import javafx.concurrent.Task;

public class ImportDataTask extends Task<Void> {

    DataSet ds;
    public ImportDataTask(DataSet ds)
    {
        this.ds = ds;
    }

    @Override
    protected Void call() throws Exception {
        var now = Instant.now().atZone(ZoneId.systemDefault());
        var message = now.toLocalDateTime() + " - Iniciando a importação do arquivo";
        ApplicationLog.info(message);
        FileSourceManager.importData(null);
        return null;
    }
    
}
