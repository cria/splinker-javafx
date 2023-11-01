package br.org.cria.splinkerapp.facade;

import java.util.List;
import java.util.Map;
import br.org.cria.splinkerapp.repositories.CentralServiceRepository;
import br.org.cria.splinkerapp.repositories.DataSourceRepository;
import br.org.cria.splinkerapp.repositories.TransferConfigRepository;

public class ConfigFacade {

    public static void handleConfiguration(Map apiConfig) throws Exception
    {
        var cmd = (List<Double>) apiConfig.get("sql_command");
        var rsyncPort = (Double) apiConfig.get("rsync_port");
        var rsyncDestination = apiConfig.get("rsync_destination").toString();
        var centralServiceUrl = apiConfig.get("central_service_url").toString();

        DataSourceRepository.saveSQLCommand(cmd);
        TransferConfigRepository.saveRSyncConfig(rsyncPort.intValue(), rsyncDestination);
        CentralServiceRepository.saveCentralServiceData(centralServiceUrl);
    }
    
}
