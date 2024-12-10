package br.org.cria.splinkerapp.models;

public class ProxyConfiguration {

    String address;
    String password;
    String port;
    String username;

    public ProxyConfiguration(String address, String password, String port, String username) {
        this.address = address;
        this.password = password;
        this.port = port;
        this.username = username;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}