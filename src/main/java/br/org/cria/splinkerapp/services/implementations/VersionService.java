package br.org.cria.splinkerapp.services.implementations;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VersionService {

    public static String getVersion() throws IOException {
        InputStream in = VersionService.class.getResourceAsStream("/version.properties");
        Properties props = new Properties();
        props.load(in);
        String version = props.getProperty("app.version");

        return version;
    }
}
