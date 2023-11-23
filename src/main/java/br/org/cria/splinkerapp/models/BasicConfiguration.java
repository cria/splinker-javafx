package br.org.cria.splinkerapp.models;

public class BasicConfiguration {
    String token;
    int lastRowCount;
    
    public BasicConfiguration(String token, int lastRowCount) {
        this.token = token;
        this.lastRowCount = lastRowCount;
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public int getLastRowCount() 
    {
        return lastRowCount;
    }
    public void setLastRowCount(int systemVersion) 
    {
        this.lastRowCount = systemVersion;
    }
}
