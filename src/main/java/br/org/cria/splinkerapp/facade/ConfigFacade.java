package br.org.cria.splinkerapp.facade;

import java.util.List;
import java.util.Map;
import br.org.cria.splinkerapp.repositories.CentralServiceRepository;
import br.org.cria.splinkerapp.repositories.TransferConfigRepository;
import br.org.cria.splinkerapp.services.implementations.DataSetService;

public class ConfigFacade {

    public static void HandleBackendData(String token, Map apiConfig) throws Exception
    {
        var cmd = (List<Double>) apiConfig.get("sql_command");
        var rsyncPort = (Double) apiConfig.get("rsync_port");
        var rsyncDestination = apiConfig.get("rsync_destination").toString();
        var centralServiceUrl = apiConfig.get("central_service_url").toString();
        var sentryToken = apiConfig.get("sentry_token").toString();
        System.setProperty("SENTRY_AUTH_TOKEN", sentryToken);
        //ultima versão, a ser retornada pela API para verificação de atualização disponível
        var systemVersion = 1.1;//apiConfig.get("version").toString();

        DataSetService.saveSQLCommand(token, cmd);
        TransferConfigRepository.saveRSyncConfig(rsyncPort.intValue(), rsyncDestination);
        CentralServiceRepository.saveCentralServiceData(centralServiceUrl, String.valueOf(systemVersion));
    }
    
}
