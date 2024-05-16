package com.tracing.service.reports.exceptions;

public class ReportNotFoundException extends RuntimeException {

    public ReportNotFoundException(Long id) {
        super("Could not find report " + id);
    }
}