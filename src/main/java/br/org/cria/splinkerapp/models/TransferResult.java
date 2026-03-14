package br.org.cria.splinkerapp.models;

public class TransferResult {

    private final String token;
    private final boolean success;
    private final String message;
    private final boolean pendente;

    public TransferResult(String token, boolean success, String messagen, boolean pendente) {
        this.token = token;
        this.success = success;
        this.message = messagen;
        this.pendente = pendente;
    }

    public String getToken() {
        return token;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isPendent() {
        return pendente;
    }

    public String getStatus() {
        if (pendente) return "Pendente";

        return success ? "Sucesso" : "Erro";
    }

    public String getStatusIcon() {
        if (pendente) return "⏳";

        return success ? "✔" : "✖";
    }

    public String getMessage() {
        return message;
    }
}