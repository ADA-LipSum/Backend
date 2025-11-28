package com.ada.proj.exception;

public class BanExpiresInvalidException extends RuntimeException {
    public BanExpiresInvalidException(String msg) {
        super(msg);
    }
}