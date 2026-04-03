package com.epam.finaltask.exception;

import lombok.Getter;

@Getter
public class DuplicateUsernameException extends RuntimeException implements LocalizedException {

    private final String messageKey;
    private final transient Object[] args;

    public DuplicateUsernameException(String messageKey, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }
}
