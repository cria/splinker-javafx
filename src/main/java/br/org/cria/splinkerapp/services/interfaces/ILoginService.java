package br.org.cria.splinkerapp.services.interfaces;

public interface ILoginService {

    boolean login(String username, String password);
    boolean login(String token);
    
}
