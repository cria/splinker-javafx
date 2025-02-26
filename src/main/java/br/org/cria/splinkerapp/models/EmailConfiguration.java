package br.org.cria.splinkerapp.models;

public class EmailConfiguration {

    private String contact_email_recipient;
    private String contact_email_send;
    private String contact_email_token;

    public void setContact_email_recipient(String contact_email_recipient) {
        this.contact_email_recipient = contact_email_recipient;
    }

    public void setContact_email_send(String contact_email_send) {
        this.contact_email_send = contact_email_send;
    }

    public void setContact_email_token(String contact_email_token) {
        this.contact_email_token = contact_email_token;
    }

    public String getContact_email_recipient() {
        return contact_email_recipient;
    }

    public String getContact_email_send() {
        return contact_email_send;
    }

    public String getContact_email_token() {
        return contact_email_token;
    }
}
