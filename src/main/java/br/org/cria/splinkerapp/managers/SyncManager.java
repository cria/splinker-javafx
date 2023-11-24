package br.org.cria.splinkerapp.managers;

import br.org.cria.splinkerapp.services.implementations.DataSetService;
import javafx.concurrent.Service;

public class SyncManager {

    public static Service<Void> SyncCollectionData(String token) throws Exception
    {
        Service transferService = null;
        var ds = DataSetService.getDataSet(token);
        if (ds != null) 
        {
            if(ds.isFile())
            {
                transferService = FileSourceManager.processData(ds);
            }
            
            if(ds.isAccessDb() || ds.isSQLDatabase())
            {
                transferService = DatabaseSourceManager.processData(ds);
            }            
        }
        return transferService;
    }
}
