package br.org.cria.splinkerapp.managers;

import br.org.cria.splinkerapp.models.DataSet;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import javafx.concurrent.Service;

public class DatabaseSourceManager 
{

    public static Service<Void> processData(DataSet dataSet) throws Exception
    {   
           return new DarwinCoreArchiveService(dataSet)
            .readDataFromSource()
            .generateTXTFile()
            .generateZIPFile()
            .transferData();
    }
}