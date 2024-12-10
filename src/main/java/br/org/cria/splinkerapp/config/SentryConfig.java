package br.org.cria.splinkerapp.config;

import br.org.cria.splinkerapp.repositories.ProxyConfigRepository;
import io.sentry.Sentry;
import io.sentry.SentryOptions.Proxy;

public class SentryConfig {

    public static void setUp() {
        var dsn = "https://cf699813548043f5c734471240b2ec6c@o4506639520104448.ingest.sentry.io/4506639532032000";

        Sentry.init(options -> {
            options.setDsn(dsn);
            options.setEnvironment("development");
            if (ProxyConfigRepository.isBehindProxyServer()) {
                try {
                    var proxyConfig = ProxyConfigRepository.getConfiguration();
                    var proxyHost = proxyConfig.getAddress();
                    var proxyPort = proxyConfig.getPort();
                    var proxy = new Proxy(proxyHost, proxyPort);
                    options.setProxy(proxy);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }

            options.setTracesSampleRate(1.0);
        });

    }

}
