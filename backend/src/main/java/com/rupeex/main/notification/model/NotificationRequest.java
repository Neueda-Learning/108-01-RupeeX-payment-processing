package com.rupeex.main.notification.model;

public class NotificationRequest {

    private String toEmail;
    private String subject;
    private String recipientName;
    private String message;
    private String referenceId;

    public NotificationRequest() {
    }

    public NotificationRequest(String toEmail, String subject, String recipientName, String message, String referenceId) {
        this.toEmail = toEmail;
        this.subject = subject;
        this.recipientName = recipientName;
        this.message = message;
        this.referenceId = referenceId;
    }

    public String getToEmail() {
        return toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
}

