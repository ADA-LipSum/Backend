package com.ada.proj.exception;

public class AlreadyBannedException extends RuntimeException {
    public AlreadyBannedException(String message) {
        super(message);
    }
}