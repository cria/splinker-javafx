package br.org.cria.splinkerapp.utils;

public class SystemConfigurationUtil {

    public static boolean runInDevelopment() {
        String env = System.getenv("env");
        return "dev".equals(env);
    }
}
