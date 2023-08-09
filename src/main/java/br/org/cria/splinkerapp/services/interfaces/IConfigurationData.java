package br.org.cria.splinkerapp.services.interfaces;

import br.org.cria.splinkerapp.models.ProxyConfiguration;

public interface IConfigurationData {

    int getRSyncPort();
    String getDarwinCoreFileSourcePath();
    String getTransferDataDestination();
    String getUserDirectory();

}
