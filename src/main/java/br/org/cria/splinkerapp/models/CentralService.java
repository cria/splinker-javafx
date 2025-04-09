package br.org.cria.splinkerapp.models;

public class CentralService {

    String centralServiceUrl;
    String systemVersion;

    public String getCentralServiceUrl() {
        return centralServiceUrl;
    }

    public void setCentralServiceUrl(String centralServiceUrl) {
        this.centralServiceUrl = centralServiceUrl;
    }

    public CentralService(String centralServiceUrl, String systemVersion) {

        this.centralServiceUrl = centralServiceUrl;
        this.systemVersion = systemVersion;
    }

    public String getSystemVersion() {
        return systemVersion;
    }

    public void setSystemVersion(String systemVersion) {
        this.systemVersion = systemVersion;
    }


}
