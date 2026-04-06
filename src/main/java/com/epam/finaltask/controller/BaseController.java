package com.epam.finaltask.controller;

import com.epam.finaltask.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@SuppressWarnings("java:S6813")
public abstract class BaseController {

    @Autowired
    protected MessageSource messageSource;

    @Autowired
    protected AppProperties appProperties;

    protected String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
