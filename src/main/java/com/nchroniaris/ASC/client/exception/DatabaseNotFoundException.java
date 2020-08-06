package com.nchroniaris.ASC.client.exception;

public class DatabaseNotFoundException extends RuntimeException {

    public DatabaseNotFoundException(String message) {
        super(message);
    }

}
