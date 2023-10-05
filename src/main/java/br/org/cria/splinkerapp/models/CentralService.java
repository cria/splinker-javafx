package br.org.cria.splinkerapp.models;

public class CentralService {

    /**
     *
     */
    String centralServiceUri;
    String centralServiceUrl;
    public String getCentralServiceUri() 
    {
        return centralServiceUri;
    }
    public void setCentralServiceUri(String centralServiceUri) 
    {
        this.centralServiceUri = centralServiceUri;
    }
    public String getCentralServiceUrl() 
    {
        return centralServiceUrl;
    }
    public void setCentralServiceUrl(String centralServiceUrl) 
    {
        this.centralServiceUrl = centralServiceUrl;
    }
    public CentralService(String centralServiceUri, String centralServiceUrl) {
        this.centralServiceUri = centralServiceUri;
        this.centralServiceUrl = centralServiceUrl;
    }
    
    
    
}
