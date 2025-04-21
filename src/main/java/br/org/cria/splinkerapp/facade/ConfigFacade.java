package br.org.cria.splinkerapp.facade;

import java.util.List;
import java.util.Map;

import br.org.cria.splinkerapp.repositories.CentralServiceRepository;
import br.org.cria.splinkerapp.repositories.EmailRepository;
import br.org.cria.splinkerapp.repositories.TransferConfigRepository;
import br.org.cria.splinkerapp.services.implementations.VersionService;

import static br.org.cria.splinkerapp.repositories.BaseRepository.byteArrayToString;

public class ConfigFacade {

    public static void HandleBackendData(String token, Map apiConfig) throws Exception {
        var cmd = (List<Double>) apiConfig.get("sql_command");
        var rsyncPort = (Double) apiConfig.get("rsync_port");
        var rsyncDestination = apiConfig.get("rsync_destination").toString();
        var centralServiceUrl = apiConfig.get("central_service_url").toString();
        var sentryTokenArray = (List<Double>) apiConfig.get("sentry_token");
        var sentryToken = byteArrayToString(sentryTokenArray);
        var emailSend = apiConfig.get("contact_email_sender");
        var emailToken = apiConfig.get("contact_email_token");
        var emailRecipients = apiConfig.get("contact_email_recipient");

        System.setProperty("SENTRY_AUTH_TOKEN", sentryToken);
        TransferConfigRepository.saveRSyncConfig(rsyncPort.intValue(), rsyncDestination);
        CentralServiceRepository.saveCentralServiceData(centralServiceUrl, VersionService.getVersion());
        EmailRepository.saveEmailData((String) emailSend, (String) emailToken, (String) emailRecipients);
    }
}
