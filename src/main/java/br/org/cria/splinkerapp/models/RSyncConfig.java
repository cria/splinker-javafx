package br.org.cria.splinkerapp.models;

public class RSyncConfig {
    private int rSyncPort;
    private String rSyncDestination;

    public RSyncConfig(int rSyncPort, String rSyncDestination) {
        this.rSyncPort = rSyncPort;
        this.rSyncDestination = rSyncDestination;
    }

    public int getrSyncPort() {
        return rSyncPort;
    }

    public void setrSyncPort(int rSyncPort) {
        this.rSyncPort = rSyncPort;
    }

    public String getrSyncDestination() {
        return rSyncDestination;
    }

    public void setrSyncDestination(String rSyncDestination) {
        this.rSyncDestination = rSyncDestination;
    }

}
