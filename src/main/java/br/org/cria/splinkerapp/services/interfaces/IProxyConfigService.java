package br.org.cria.splinkerapp.services.interfaces;

import br.org.cria.splinkerapp.models.ProxyConfiguration;

public interface IProxyConfigService {
    
    ProxyConfiguration getConfiguration();
    boolean saveProxyConfig(ProxyConfiguration proxyConfig);
    
}
