package com.tracing.service.emails.exceptions;

public class EmailNotFoundException extends RuntimeException {

    public EmailNotFoundException(Long id) {
        super("Could not find report " + id);
    }
}