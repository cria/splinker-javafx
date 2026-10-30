package br.org.cria.splinkerapp.models;

import br.org.cria.splinkerapp.i18n.I18n;
public class TransferResult {

    private final String token;
    private final boolean success;
    private final String message;
    private final boolean pendente;
    private final String errorLog;

    public TransferResult(String token, boolean success, String message, boolean pendente) {
        this(token, success, message, pendente, null);
    }

    public TransferResult(String token, boolean success, String message, boolean pendente, String errorLog) {
        this.token = token;
        this.success = success;
        this.message = message;
        this.pendente = pendente;
        this.errorLog = errorLog;
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
        if (pendente) return I18n.text("pending");
        return success ? I18n.text("success") : I18n.text("error");
    }

    public String getStatusIcon() {
        if (pendente) return "⏳";
        return success ? "✔" : "✖";
    }

    public String getMessage() {
        return message;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public boolean hasErrorLog() {
        return errorLog != null && !errorLog.isBlank();
    }
}