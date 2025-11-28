package com.ada.proj.exception;

public class BannedUserException extends RuntimeException {
    public BannedUserException(String message) {
        super(message);
    }
}