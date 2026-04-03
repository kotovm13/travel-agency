package com.epam.finaltask.exception;

public interface LocalizedException {
    String getMessageKey();
    Object[] getArgs();
}
