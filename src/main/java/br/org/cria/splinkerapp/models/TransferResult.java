package br.org.cria.splinkerapp.models;

public class TransferResult {

    private final String token;
    private final boolean success;
    private final String message;

    public TransferResult(String token, boolean success, String message) {
        this.token = token;
        this.success = success;
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getStatus() {
        return success ? "SUCESSO" : "ERRO";
    }

    public String getMessage() {
        return message;
    }
}