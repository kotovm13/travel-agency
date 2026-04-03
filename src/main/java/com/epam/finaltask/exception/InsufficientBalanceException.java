package com.epam.finaltask.exception;

import lombok.Getter;

@Getter
public class InsufficientBalanceException extends RuntimeException implements LocalizedException {

    private final String messageKey;
    private final transient Object[] args;

    public InsufficientBalanceException(String messageKey, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }
}
