package com.epam.finaltask.exception;

public class VoucherNotFoundException extends ResourceNotFoundException {
    public VoucherNotFoundException(String message) {
        super(message);
    }
}
