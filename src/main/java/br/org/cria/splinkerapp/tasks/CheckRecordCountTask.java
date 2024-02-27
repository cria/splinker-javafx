package br.org.cria.splinkerapp.tasks;

import java.io.File;
import com.univocity.parsers.csv.CsvRoutines;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import javafx.concurrent.Task;

public class CheckRecordCountTask extends Task<Boolean> {

    DarwinCoreArchiveService service;

    public CheckRecordCountTask(DarwinCoreArchiveService service)
    {
        this.service = service;
    }

    @Override
    protected Boolean call() throws Exception 
    {
        
        var path = service.getTxtFilePath();
        var dimensions = new CsvRoutines().getInputDimension(new File(path));
        var lastRowCount = service.getDataSet().getLastRowCount();
        var currentRowCount = dimensions.rowCount();
        var hasRecordCountDecreased = lastRowCount > currentRowCount;    
        
        return hasRecordCountDecreased;
    }
    
}
