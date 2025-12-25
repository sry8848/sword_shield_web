package com.swordshield.common;

public class RemoteServiceException extends RuntimeException {
    private final int code;

    public RemoteServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

