package com.ning.arecibo.alert.objects;

public class Notification
{
    private final String subject;
    private final String message;

    public Notification(String subject, String message)
    {
        this.subject = subject;
        this.message = message;
    }

    public String getSubject()
    {
        return subject;
    }

    public String getMessage()
    {
        return message;
    }
}