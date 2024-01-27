package br.org.cria.splinkerapp.config;

public class SentryConfig {

    public static void setUp()
    {
        var sentryToken = "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";
        System.setProperty("SENTRY_AUTH_TOKEN", sentryToken);
    }
    
}
