package com.epam.finaltask.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final String MESSAGE_ATTR = "message";
    private static final String ERROR_VIEW_400 = "error/400";
    private static final String ERROR_VIEW_403 = "error/403";
    private static final String ERROR_VIEW_404 = "error/404";
    private static final String ERROR_VIEW_500 = "error/500";

    private final MessageSource messageSource;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException ex) {
        return buildErrorView(ERROR_VIEW_404, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ModelAndView handleDuplicateUsername(DuplicateUsernameException ex) {
        return buildErrorView(ERROR_VIEW_400, HttpStatus.BAD_REQUEST, resolveMessage(ex));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ModelAndView handleInsufficientBalance(InsufficientBalanceException ex) {
        return buildErrorView(ERROR_VIEW_400, HttpStatus.BAD_REQUEST, resolveMessage(ex));
    }

    @ExceptionHandler(InvalidOrderStatusException.class)
    public ModelAndView handleInvalidOrderStatus(InvalidOrderStatusException ex) {
        return buildErrorView(ERROR_VIEW_400, HttpStatus.BAD_REQUEST, resolveMessage(ex));
    }

    @ExceptionHandler(UserBlockedException.class)
    public ModelAndView handleUserBlocked(UserBlockedException ex) {
        return buildErrorView(ERROR_VIEW_403, HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(AccessDeniedException ex) {
        return buildErrorView(ERROR_VIEW_403, HttpStatus.FORBIDDEN,
                getMessage("error.access.denied"));
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneral(Exception ex) {
        return buildErrorView(ERROR_VIEW_500, HttpStatus.INTERNAL_SERVER_ERROR,
                getMessage("error.unexpected"));
    }

    private ModelAndView buildErrorView(String viewName, HttpStatus status, String message) {
        ModelAndView mav = new ModelAndView(viewName);
        mav.setStatus(status);
        mav.addObject(MESSAGE_ATTR, message);
        return mav;
    }

    private String resolveMessage(LocalizedException ex) {
        return getMessage(ex.getMessageKey(), ex.getArgs());
    }

    private String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
