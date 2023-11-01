package br.org.cria.splinkerapp.models;

public class CentralService {

    String centralServiceUrl;
    
    public String getCentralServiceUrl() 
    {
        return centralServiceUrl;
    }
    public void setCentralServiceUrl(String centralServiceUrl) 
    {
        this.centralServiceUrl = centralServiceUrl;
    }
    public CentralService(String centralServiceUrl) {
        
        this.centralServiceUrl = centralServiceUrl;
    }
    
    
    
}
